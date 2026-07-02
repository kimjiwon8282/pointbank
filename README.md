# PointBank Server

PointBank 백엔드 MSA 실험용 루트 프로젝트입니다.

## 구조

```text
pointbank-server
├─ services
│  ├─ pointbank-auth-service
│  └─ pointbank-banking-service
└─ infra
   └─ docker-compose.yml
```

## IntelliJ에서 열기

`pointbank-server` 루트 폴더를 여는 것을 권장합니다.

루트를 열면 Gradle 멀티 모듈로 `pointbank-auth-service`, `pointbank-banking-service`를 함께 관리할 수 있습니다.

## 실행 순서

1. MySQL 실행

```bash
cd infra
docker compose up -d
```

2. IntelliJ에서 각 서비스 실행

- `AuthServiceApplication` 실행: 8081
- `BankingServiceApplication` 실행: 8082

## DB

로컬 MySQL 컨테이너 하나에 스키마를 분리합니다.

- `auth_db`
- `banking_db`

## 구현 방향

- 인증 기능: `pointbank-auth-service`
- 계좌, 송금, 원장, 거래내역: `pointbank-banking-service`
- Gateway, 증권, AI 서비스는 이후 단계에서 추가
