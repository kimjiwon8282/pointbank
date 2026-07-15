# PointBank 프로젝트 가이드 (커밋되는 CLAUDE.md 역할 문서)

> `CLAUDE.md`는 `.gitignore` 처리되어 직접 커밋되지 않는다. 이 문서가 그 역할을 대신하며,
> **다른 랩탑/새 Claude 세션에서 맥락을 복원하는 기준 문서**다.
> 로컬에서 Claude Code를 쓰려면 이 문서를 참고해 각자 `CLAUDE.md`를 두면 된다
> (또는 `cp docs/project-guide.md CLAUDE.md`).

## 아키텍처 한눈에

Gradle 멀티모듈 MSA(`pointbank-server`), Java 21, Spring Boot 3.5, MyBatis(XML 매퍼),
MySQL 8.4 단일 컨테이너에 스키마 분리(`auth_db` / `banking_db` / `ledger_db` / `securities_db`),
비동기 원장 연동은 SQS(FIFO, 로컬은 LocalStack).

| 서비스 | 포트 | 역할 |
|---|---|---|
| pointbank-gateway-service | 8080 | Spring Cloud Gateway(WebFlux). `/api/auth/**`→auth, `/api/banking/**`→banking, `/api/securities/**`→securities. |
| pointbank-auth-service | 8081 | 유일하게 Spring Security + JWT. 회원/인증/토큰. `POST /internal/auth/validate` 게이트웨이 전용. |
| pointbank-banking-service | 8082 | 계좌/송금/거래내역. Security 없음. 원장 데이터는 ledger 서비스로 이관됨. |
| pointbank-securities-service | 8083 | 증권. ledger로 예수금 연동(SQS 비동기 + DLQ 보상). |
| pointbank-ledger-service | 8084 | 원장(ledger). SQS 커맨드/결과 큐 소비·발행. |

## 인증 흐름 (MSA 핵심 설계)

게이트웨이 `BankingAuthentication` 필터가 Bearer 토큰 추출 → auth-service `/internal/auth/validate` 호출 →
**Authorization 헤더 제거 후 `X-Member-Id` / `X-Role` 주입**. 다운스트림은 이 헤더만 신뢰
(`CurrentMemberHeaderResolver`), 없거나 위조면 UNAUTHORIZED. **banking-service 안에서 JWT를 직접 검증하지 않는다.**

## 송금/원장 불변식 (banking)

`TransferService.transfer()`가 잔액 변경의 기준 패턴:
- 두 계좌를 한 쿼리로 락(`findAllByIdsForUpdate`, `SELECT ... FOR UPDATE`) 후 상태/비밀번호/잔액 검증.
- 잔액은 `BIGINT` 포인트, `Math.addExact/subtractExact`, DB `CHECK (balance >= 0)`.
- 모든 잔액 변경은 `account_transactions`에 `balance_after` 기록(복식부기: TRANSFER_OUT+TRANSFER_IN, `transfer_id` 공유).
- `transfer_no` 유니크, `DuplicateKeyException` 시 새 번호로 재시도.

## 컨벤션

- 서비스별 feature 패키지: `<feature>/{controller,service,mapper,domain,dto}` + `global/`(ApiResponse, ErrorCode, GlobalExceptionHandler). 서비스마다 자체 복사(공유 모듈 없음).
- 성공 응답은 `ApiResponse.success(message, data)`, 오류는 서비스별 `ErrorCode` + `CustomException`/`BusinessException`. 사용자 메시지/커밋 메시지는 한국어.
- 스키마는 `infra/mysql/init/`의 수기 DDL로 관리(마이그레이션 도구 없음). 스키마 변경 = 해당 DDL + `src/test/resources/schema-*-test.sql` 동시 수정.
- 통합테스트는 Testcontainers(MySQL 8.4) → Docker 필요.

## 자주 쓰는 명령

```bash
# 인프라만 (MySQL + LocalStack)
cd infra && docker compose --env-file ../.env up -d
# 앱 포함 전체 기동 (컨테이너화 이후)
cd infra && docker compose --env-file ../.env up -d --build

# 전체 빌드 / 특정 모듈 테스트
./gradlew build
./gradlew :services:pointbank-banking-service:test

# 서비스 로컬 실행
./gradlew :services:pointbank-auth-service:bootRun
```

## CI/CD·인프라 진행 상황

→ 별도 문서 [`docs/cicd-progress.md`](./cicd-progress.md) 참고 (세션 인수인계·태스크·다음 단계).
