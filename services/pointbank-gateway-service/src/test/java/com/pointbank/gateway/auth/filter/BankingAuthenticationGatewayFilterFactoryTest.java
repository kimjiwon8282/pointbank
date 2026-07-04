package com.pointbank.gateway.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pointbank.gateway.auth.client.AuthValidationClient;
import com.pointbank.gateway.auth.dto.AuthValidationResult;
import com.pointbank.gateway.auth.exception.CustomException;
import com.pointbank.gateway.auth.exception.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BankingAuthenticationGatewayFilterFactoryTest {

    private AuthValidationClient authValidationClient;
    private GatewayFilter filter;

    @BeforeEach
    void setUp() {
        authValidationClient = mock(AuthValidationClient.class);
        BankingAuthenticationGatewayFilterFactory factory =
                new BankingAuthenticationGatewayFilterFactory(authValidationClient, new ObjectMapper());
        filter = factory.apply(new BankingAuthenticationGatewayFilterFactory.Config());
    }

    @Test
    void Authorization_헤더가_없으면_401을_반환한다() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/banking/me").build());

        filter.filter(exchange, unusedChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("\"code\":\"UNAUTHORIZED\"");
        verify(authValidationClient, never()).validate(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void Bearer_형식이_아니면_401을_반환한다() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/banking/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic credentials")
                .build());

        filter.filter(exchange, unusedChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(authValidationClient, never()).validate(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void Auth_검증이_거부되면_downstream을_호출하지_않고_해당_상태를_반환한다() {
        MockServerWebExchange exchange = bearerExchange("invalid-token");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(authValidationClient.validate("invalid-token"))
                .thenReturn(Mono.error(new CustomException(ErrorCode.UNAUTHORIZED)));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void Auth_Service가_응답하지_않으면_503을_반환한다() {
        MockServerWebExchange exchange = bearerExchange("access-token");
        when(authValidationClient.validate("access-token"))
                .thenReturn(Mono.error(new CustomException(ErrorCode.AUTH_SERVICE_UNAVAILABLE)));

        filter.filter(exchange, unusedChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"code\":\"AUTH_SERVICE_UNAVAILABLE\"");
    }

    @Test
    void 검증_성공_시_클라이언트_내부_헤더와_Authorization을_제거하고_검증값을_설정한다() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/banking/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .header(BankingAuthenticationGatewayFilterFactory.MEMBER_ID_HEADER, "999")
                .header(BankingAuthenticationGatewayFilterFactory.ROLE_HEADER, "ADMIN")
                .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = forwardedExchange -> {
            forwarded.set(forwardedExchange);
            return Mono.empty();
        };
        when(authValidationClient.validate("access-token"))
                .thenReturn(Mono.just(new AuthValidationResult(1L, "USER")));

        filter.filter(exchange, chain).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
        assertThat(headers.getFirst(BankingAuthenticationGatewayFilterFactory.MEMBER_ID_HEADER)).isEqualTo("1");
        assertThat(headers.getFirst(BankingAuthenticationGatewayFilterFactory.ROLE_HEADER)).isEqualTo("USER");
        verify(authValidationClient).validate("access-token");
    }

    @Test
    void OPTIONS_요청은_인증하지_않고_통과시킨다() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest
                .method(HttpMethod.OPTIONS, "/api/banking/me")
                .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = forwardedExchange -> {
            forwarded.set(forwardedExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isSameAs(exchange);
        verify(authValidationClient, never()).validate(org.mockito.ArgumentMatchers.anyString());
    }

    private MockServerWebExchange bearerExchange(String token) {
        return exchange(MockServerHttpRequest.get("/api/banking/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());
    }

    private MockServerWebExchange exchange(MockServerHttpRequest request) {
        return MockServerWebExchange.from(request);
    }

    private GatewayFilterChain unusedChain() {
        return ignored -> Mono.empty();
    }
}
