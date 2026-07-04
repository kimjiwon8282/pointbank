package com.pointbank.gateway.auth.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.gateway.auth.client.AuthValidationClient;
import com.pointbank.gateway.auth.dto.AuthValidationResult;
import com.pointbank.gateway.auth.dto.ErrorResponse;
import com.pointbank.gateway.auth.exception.CustomException;
import com.pointbank.gateway.auth.exception.ErrorCode;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class BankingAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<BankingAuthenticationGatewayFilterFactory.Config> {

    static final String MEMBER_ID_HEADER = "X-Member-Id";
    static final String ROLE_HEADER = "X-Role";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthValidationClient authValidationClient;
    private final ObjectMapper objectMapper;

    public BankingAuthenticationGatewayFilterFactory(
            AuthValidationClient authValidationClient,
            ObjectMapper objectMapper
    ) {
        super(Config.class);
        this.authValidationClient = authValidationClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }

            String accessToken = resolveAccessToken(exchange.getRequest().getHeaders());
            if (accessToken == null) {
                return writeError(exchange, new CustomException(ErrorCode.UNAUTHORIZED));
            }

            return authValidationClient.validate(accessToken)
                    .flatMap(result -> chain.filter(withAuthenticatedHeaders(exchange, result)))
                    .onErrorResume(CustomException.class, exception -> writeError(exchange, exception));
        };
    }

    private String resolveAccessToken(HttpHeaders headers) {
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private ServerWebExchange withAuthenticatedHeaders(
            ServerWebExchange exchange,
            AuthValidationResult result
    ) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.remove(MEMBER_ID_HEADER);
                    headers.remove(ROLE_HEADER);
                    headers.set(MEMBER_ID_HEADER, result.memberId().toString());
                    headers.set(ROLE_HEADER, result.role());
                }))
                .build();
    }

    private Mono<Void> writeError(ServerWebExchange exchange, CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        exchange.getResponse().setStatusCode(errorCode.getStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsBytes(ErrorResponse.of(errorCode));
            return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(body)
            ));
        } catch (JsonProcessingException serializationException) {
            exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return exchange.getResponse().setComplete();
        }
    }

    public static class Config {
    }
}
