package com.pointbank.gateway.auth.client;

import com.pointbank.gateway.auth.config.AuthValidationProperties;
import com.pointbank.gateway.auth.dto.AuthValidationResult;
import com.pointbank.gateway.auth.exception.CustomException;
import com.pointbank.gateway.auth.exception.ErrorCode;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthValidationClientTest {

    @Test
    void 성공_응답에서_memberId와_role을_반환한다() {
        WebClient webClient = webClient(HttpStatus.OK, """
                {
                  "success": true,
                  "message": "토큰 검증이 완료되었습니다.",
                  "data": {"memberId": 1, "role": "USER", "status": "ACTIVE"}
                }
                """);
        AuthValidationClient client = client(webClient);

        AuthValidationResult result = client.validate("access-token").block();

        assertThat(result).isEqualTo(new AuthValidationResult(1L, "USER"));
    }

    @Test
    void Auth_검증의_403은_인증_거부로_전달한다() {
        AuthValidationClient client = client(webClient(HttpStatus.FORBIDDEN, "{}"));

        assertThatThrownBy(() -> client.validate("access-token").block())
                .isInstanceOf(CustomException.class)
                .extracting(throwable -> ((CustomException) throwable).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void Auth_Service의_5xx는_서비스_장애로_처리한다() {
        AuthValidationClient client = client(webClient(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));

        assertThatThrownBy(() -> client.validate("access-token").block())
                .isInstanceOf(CustomException.class)
                .extracting(throwable -> ((CustomException) throwable).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_SERVICE_UNAVAILABLE);
    }

    private AuthValidationClient client(WebClient webClient) {
        return new AuthValidationClient(
                webClient,
                new AuthValidationProperties("http://auth/internal/auth/validate", Duration.ofSeconds(2))
        );
    }

    private WebClient webClient(HttpStatus status, String body) {
        return WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(status)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .build()))
                .build();
    }
}
