package com.pointbank.gateway.auth.client;

import com.pointbank.gateway.auth.config.AuthValidationProperties;
import com.pointbank.gateway.auth.dto.AuthValidationResult;
import com.pointbank.gateway.auth.exception.CustomException;
import com.pointbank.gateway.auth.exception.ErrorCode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

@Component
public class AuthValidationClient {

    private final WebClient webClient;
    private final AuthValidationProperties properties;

    public AuthValidationClient(WebClient authValidationWebClient, AuthValidationProperties properties) {
        this.webClient = authValidationWebClient;
        this.properties = properties;
    }

    public Mono<AuthValidationResult> validate(String accessToken) {
        return webClient.post()
                .uri(properties.validateUrl())
                .bodyValue(new ValidateRequest(accessToken))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(ValidateResponse.class)
                                .flatMap(this::toResult);
                    }
                    if (response.statusCode().is4xxClientError()) {
                        ErrorCode errorCode = response.statusCode().value() == HttpStatus.FORBIDDEN.value()
                                ? ErrorCode.FORBIDDEN
                                : ErrorCode.UNAUTHORIZED;
                        return response.releaseBody()
                                .then(Mono.error(new CustomException(errorCode)));
                    }
                    return response.releaseBody()
                            .then(Mono.error(new CustomException(ErrorCode.AUTH_SERVICE_UNAVAILABLE)));
                })
                .timeout(properties.timeout())
                .onErrorMap(
                        throwable -> throwable instanceof TimeoutException
                                || throwable instanceof WebClientRequestException,
                        throwable -> new CustomException(ErrorCode.AUTH_SERVICE_UNAVAILABLE, throwable)
                )
                .onErrorMap(
                        throwable -> !(throwable instanceof CustomException),
                        throwable -> new CustomException(ErrorCode.AUTH_SERVICE_UNAVAILABLE, throwable)
                );
    }

    private Mono<AuthValidationResult> toResult(ValidateResponse response) {
        if (response == null || !response.success() || response.data() == null
                || response.data().memberId() == null
                || response.data().role() == null || response.data().role().isBlank()) {
            return Mono.error(new CustomException(ErrorCode.AUTH_SERVICE_UNAVAILABLE));
        }
        return Mono.just(new AuthValidationResult(response.data().memberId(), response.data().role()));
    }

    private record ValidateRequest(String accessToken) {
    }

    private record ValidateResponse(boolean success, ValidateData data) {
    }

    private record ValidateData(Long memberId, String role, String status) {
    }
}
