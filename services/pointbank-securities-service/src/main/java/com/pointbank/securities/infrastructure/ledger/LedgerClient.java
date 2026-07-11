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

    private CustomException toCustomException(ErrorResponse error) {
        if (error == null || error.code() == null) {
            return new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
        }
        try {
            return new CustomException(ErrorCode.valueOf(error.code()));
        } catch (IllegalArgumentException exception) {
            return new CustomException(ErrorCode.LEDGER_SERVICE_UNAVAILABLE);
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
}
