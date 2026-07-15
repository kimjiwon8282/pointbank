package com.pointbank.securities.infrastructure.quote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.securities.global.exception.CustomException;
import com.pointbank.securities.global.exception.ErrorCode;
import com.pointbank.securities.global.response.ErrorResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;

@Component
public class QuoteClient {
    private final RestClient quoteRestClient;
    private final ObjectMapper objectMapper;

    public QuoteClient(
            RestClient.Builder restClientBuilder,
            QuoteClientProperties properties,
            ObjectMapper objectMapper
    ) {
        this.quoteRestClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.objectMapper = objectMapper;
    }

    public QuoteLatestResponse getLatestQuote(String stockCode) {
        try {
            QuoteApiResponse<QuoteLatestResponse> response = quoteRestClient.get()
                    .uri("/internal/quotes/{stockCode}/latest", stockCode)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        ErrorResponse error = readError(clientResponse);
                        throw toCustomException(error);
                    })
                    .body(new ParameterizedTypeReference<QuoteApiResponse<QuoteLatestResponse>>() {
                    });
            if (response == null || response.data() == null) {
                throw new CustomException(ErrorCode.QUOTE_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.QUOTE_SERVICE_UNAVAILABLE);
        }
    }

    public QuoteBulkResponse getLatestQuotes(List<String> stockCodes) {
        try {
            QuoteApiResponse<QuoteBulkResponse> response = quoteRestClient.post()
                    .uri("/internal/quotes/latest/bulk")
                    .body(new QuoteBulkRequest(stockCodes))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        ErrorResponse error = readError(clientResponse);
                        throw toCustomException(error);
                    })
                    .body(new ParameterizedTypeReference<QuoteApiResponse<QuoteBulkResponse>>() {
                    });
            if (response == null || response.data() == null) {
                throw new CustomException(ErrorCode.QUOTE_SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CustomException(ErrorCode.QUOTE_SERVICE_UNAVAILABLE);
        }
    }

    private CustomException toCustomException(ErrorResponse error) {
        if (error == null || error.code() == null) {
            return new CustomException(ErrorCode.QUOTE_SERVICE_UNAVAILABLE);
        }
        return switch (error.code()) {
            case "QUOTE_NOT_FOUND" -> new CustomException(ErrorCode.QUOTE_NOT_FOUND);
            case "STALE_QUOTE" -> new CustomException(ErrorCode.STALE_QUOTE);
            case "BAD_REQUEST" -> new CustomException(ErrorCode.BAD_REQUEST);
            default -> new CustomException(ErrorCode.QUOTE_SERVICE_UNAVAILABLE);
        };
    }

    private ErrorResponse readError(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return objectMapper.readValue(response.getBody(), ErrorResponse.class);
        } catch (IOException exception) {
            return null;
        }
    }
}
