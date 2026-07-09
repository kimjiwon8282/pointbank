package com.pointbank.banking.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.banking.account.dto.AccountCreateRequest;
import com.pointbank.banking.account.dto.AccountDepositRequest;
import com.pointbank.banking.account.dto.AccountDepositResponse;
import com.pointbank.banking.account.dto.AccountResponse;
import com.pointbank.banking.funds.dto.SecuritiesCashAccountResponse;
import com.pointbank.banking.funds.dto.SecuritiesDepositRequest;
import com.pointbank.banking.funds.dto.SecuritiesDepositResponse;
import com.pointbank.banking.global.exception.CustomException;
import com.pointbank.banking.global.exception.ErrorCode;
import com.pointbank.banking.global.response.ErrorResponse;
import com.pointbank.banking.transaction.dto.TransactionHistorySliceResponse;
import com.pointbank.banking.transfer.dto.TransferCreateRequest;
import com.pointbank.banking.transfer.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LedgerClient {
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final RestClient ledgerRestClient;
    private final ObjectMapper objectMapper;

    public AccountResponse createAccount(Long memberId, AccountCreateRequest request) {
        LedgerBankingAccountResponse response = post(
                "/internal/ledger/banking/accounts",
                null,
                new LedgerCreateAccountRequest(memberId, request.accountPassword()),
                new ParameterizedTypeReference<LedgerApiResponse<LedgerBankingAccountResponse>>() {
                });
        return new AccountResponse(response.accountId(), response.accountNumber(), response.balance(), response.status());
    }

    public AccountResponse getAccount(Long memberId) {
        LedgerBankingAccountResponse response = get(
                "/internal/ledger/banking/accounts/me?memberId={memberId}",
                new ParameterizedTypeReference<LedgerApiResponse<LedgerBankingAccountResponse>>() {
                },
                memberId);
        return new AccountResponse(response.accountId(), response.accountNumber(), response.balance(), response.status());
    }

    public AccountDepositResponse deposit(Long memberId, String idempotencyKey, AccountDepositRequest request) {
        LedgerAccountDepositResponse response = post(
                "/internal/ledger/banking/accounts/deposit",
                idempotencyKey,
                new LedgerDepositRequest(memberId, request.amount()),
                new ParameterizedTypeReference<LedgerApiResponse<LedgerAccountDepositResponse>>() {
                });
        AccountResponse account = getAccount(memberId);
        return new AccountDepositResponse(
                response.accountId(), account.accountNumber(), response.amount(),
                response.balanceAfter(), "DEPOSIT");
    }

    public TransferResponse transfer(Long memberId, String idempotencyKey, TransferCreateRequest request) {
        LedgerTransferResponse response = post(
                "/internal/ledger/banking/transfers",
                idempotencyKey,
                new LedgerTransferRequest(memberId, request.toAccountNumber(), request.amount(), request.accountPassword()),
                new ParameterizedTypeReference<LedgerApiResponse<LedgerTransferResponse>>() {
                });
        return new TransferResponse(
                response.transferNo(), response.fromAccountNumber(), response.toAccountNumber(),
                response.amount(), response.fromBalanceAfter(), response.status(), response.completedAt());
    }

    public TransactionHistorySliceResponse getHistories(
            Long memberId,
            String type,
            LocalDate from,
            LocalDate to,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            Integer size
    ) {
        try {
            LedgerApiResponse<TransactionHistorySliceResponse> response = ledgerRestClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/internal/ledger/banking/transactions")
                                .queryParam("memberId", memberId);
                        if (type != null) builder.queryParam("type", type);
                        if (from != null) builder.queryParam("from", from);
                        if (to != null) builder.queryParam("to", to);
                        if (cursorCreatedAt != null) builder.queryParam("cursorCreatedAt", cursorCreatedAt);
                        if (cursorId != null) builder.queryParam("cursorId", cursorId);
                        if (size != null) builder.queryParam("size", size);
                        return builder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        ErrorResponse error = readError(clientResponse);
                        throw toCustomException(error);
                    })
                    .body(new ParameterizedTypeReference<LedgerApiResponse<TransactionHistorySliceResponse>>() {
                    });
            if (response == null || response.data() == null) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            return response.data();
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public SecuritiesCashAccountResponse createSecuritiesCashAccount(Long memberId) {
        return post(
                "/internal/ledger/securities/cash/accounts",
                null,
                new LedgerSecuritiesCashCreateRequest(memberId),
                new ParameterizedTypeReference<LedgerApiResponse<SecuritiesCashAccountResponse>>() {
                });
    }

    public SecuritiesCashAccountResponse getSecuritiesCashAccount(Long memberId) {
        return get(
                "/internal/ledger/securities/cash/me?memberId={memberId}",
                new ParameterizedTypeReference<LedgerApiResponse<SecuritiesCashAccountResponse>>() {
                },
                memberId);
    }

    public SecuritiesDepositResponse depositSecuritiesCash(Long memberId, String idempotencyKey, SecuritiesDepositRequest request) {
        return post(
                "/internal/ledger/securities/cash/deposit",
                idempotencyKey,
                new LedgerSecuritiesDepositRequest(memberId, request.amount(), request.accountPassword()),
                new ParameterizedTypeReference<LedgerApiResponse<SecuritiesDepositResponse>>() {
                });
    }

    private <T> T get(String uri, ParameterizedTypeReference<LedgerApiResponse<T>> responseType, Object... variables) {
        try {
            LedgerApiResponse<T> response = ledgerRestClient.get()
                    .uri(uri, variables)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        ErrorResponse error = readError(clientResponse);
                        throw toCustomException(error);
                    })
                    .body(responseType);
            if (response == null || response.data() == null) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            return response.data();
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private <T> T post(
            String uri,
            String idempotencyKey,
            Object body,
            ParameterizedTypeReference<LedgerApiResponse<T>> responseType
    ) {
        try {
            RestClient.RequestBodySpec spec = ledgerRestClient.post().uri(uri);
            if (idempotencyKey != null) {
                spec.header(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
            }
            LedgerApiResponse<T> response = spec.body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        ErrorResponse error = readError(clientResponse);
                        throw toCustomException(error);
                    })
                    .body(responseType);
            if (response == null || response.data() == null) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            return response.data();
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private CustomException toCustomException(ErrorResponse error) {
        if (error == null || error.code() == null) {
            return new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        try {
            return new CustomException(ErrorCode.valueOf(error.code()));
        } catch (IllegalArgumentException exception) {
            return new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private ErrorResponse readError(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return objectMapper.readValue(response.getBody(), ErrorResponse.class);
        } catch (IOException exception) {
            return null;
        }
    }

    private record LedgerApiResponse<T>(boolean success, String message, T data) {
    }

    private record LedgerCreateAccountRequest(Long memberId, String accountPassword) {
    }

    private record LedgerBankingAccountResponse(
            Long accountId,
            Long memberId,
            String accountNumber,
            long balance,
            String status,
            LocalDateTime createdAt
    ) {
    }

    private record LedgerDepositRequest(Long memberId, Long amount) {
    }

    private record LedgerAccountDepositResponse(
            String requestNo,
            Long memberId,
            Long accountId,
            long amount,
            long balanceAfter,
            String status,
            LocalDateTime completedAt
    ) {
    }

    private record LedgerTransferRequest(
            Long memberId,
            String toAccountNumber,
            Long amount,
            String accountPassword
    ) {
    }

    private record LedgerTransferResponse(
            Long transferRequestId,
            String requestNo,
            String transferNo,
            Long fromAccountId,
            Long toAccountId,
            String fromAccountNumber,
            String toAccountNumber,
            long amount,
            long fromBalanceAfter,
            long toBalanceAfter,
            String status,
            LocalDateTime completedAt
    ) {
    }

    private record LedgerSecuritiesCashCreateRequest(Long memberId) {
    }

    private record LedgerSecuritiesDepositRequest(Long memberId, Long amount, String accountPassword) {
    }
}
