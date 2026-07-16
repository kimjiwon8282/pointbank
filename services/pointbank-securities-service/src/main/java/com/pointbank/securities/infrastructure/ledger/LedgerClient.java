package com.pointbank.securities.infrastructure.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.securities.global.exception.CustomException;
import com.pointbank.securities.global.exception.ErrorCode;
import com.pointbank.securities.global.response.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LedgerClient {
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final RestClient ledgerRestClient;
    private final ObjectMapper objectMapper;

    public LedgerSecuritiesCashAccountResponse createSecuritiesCashAccount(Long memberId) {
        try {
            LedgerApiResponse<LedgerSecuritiesCashAccountResponse> response = ledgerRestClient.post()
                    .uri("/internal/ledger/securities/cash/accounts")
                    .body(new LedgerSecuritiesCashAccountCreateRequest(memberId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        ErrorResponse error = readError(clientResponse);
                        throw toCustomException(error);
                    })
                    .body(new ParameterizedTypeReference<LedgerApiResponse<LedgerSecuritiesCashAccountResponse>>() {
                    });
            if (response == null || response.data() == null) {
                throw new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
        }
    }

    public LedgerSecuritiesCashAccountCleanupResponse cleanupSecuritiesCashAccount(Long memberId) {
        try {
            LedgerApiResponse<LedgerSecuritiesCashAccountCleanupResponse> response = ledgerRestClient.delete()
                    .uri("/internal/ledger/securities/cash/accounts?memberId={memberId}", memberId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        ErrorResponse error = readError(clientResponse);
                        throw toCustomException(error);
                    })
                    .body(new ParameterizedTypeReference<LedgerApiResponse<LedgerSecuritiesCashAccountCleanupResponse>>() {
                    });
            if (response == null || response.data() == null) {
                throw new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
        }
    }

    public LedgerTradeFundsResponse debitBuyFunds(LedgerBuyDebitRequest request) {
        return executeTradeFundsRequest(
                "/internal/ledger/securities/trades/buy/debit",
                request.orderNo(),
                request
        );
    }

    public LedgerTradeFundsResponse creditSellFunds(LedgerSellCreditRequest request) {
        return executeTradeFundsRequest(
                "/internal/ledger/securities/trades/sell/credit",
                request.orderNo(),
                request
        );
    }

    public LedgerBuyReversalResponse reverseBuyFunds(LedgerBuyReversalRequest request) {
        try {
            LedgerApiResponse<LedgerBuyReversalResponse> response = ledgerRestClient.post()
                    .uri("/internal/ledger/securities/trades/buy/reversal")
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (httpRequest, clientResponse) -> {
                        ErrorResponse error = readError(clientResponse);
                        throw toCustomException(error);
                    })
                    .body(new ParameterizedTypeReference<LedgerApiResponse<LedgerBuyReversalResponse>>() {
                    });
            if (response == null || response.data() == null) {
                throw new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
        }
    }

    private LedgerTradeFundsResponse executeTradeFundsRequest(String uri, String orderNo, Object requestBody) {
        try {
            LedgerApiResponse<LedgerTradeFundsResponse> response = ledgerRestClient.post()
                    .uri(uri)
                    .header(IDEMPOTENCY_KEY_HEADER, orderNo)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        ErrorResponse error = readError(clientResponse);
                        throw toCustomException(error);
                    })
                    .body(new ParameterizedTypeReference<LedgerApiResponse<LedgerTradeFundsResponse>>() {
                    });
            if (response == null || response.data() == null) {
                throw new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
        }
    }

    private CustomException toCustomException(ErrorResponse error) {
        if (error == null || error.code() == null) {
            return new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
        }
        return switch (error.code()) {
            case "INSUFFICIENT_BALANCE" -> new CustomException(ErrorCode.INSUFFICIENT_CASH_BALANCE);
            case "IDEMPOTENCY_KEY_CONFLICT" -> new CustomException(ErrorCode.ORDER_IDEMPOTENCY_CONFLICT);
            case "FUND_TRANSFER_IN_PROGRESS" -> new CustomException(ErrorCode.ORDER_IN_PROGRESS);
            case "SECURITIES_CASH_ACCOUNT_NOT_FOUND" ->
                    new CustomException(ErrorCode.SECURITIES_CASH_ACCOUNT_NOT_FOUND);
            case "BAD_REQUEST" -> new CustomException(ErrorCode.BAD_REQUEST);
            default -> new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
        };
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
}
