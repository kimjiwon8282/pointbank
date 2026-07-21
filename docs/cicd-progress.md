# CI/CD · 인프라 진행 노트 (세션 인수인계)

> 이 문서는 **다른 랩탑/새 Claude 세션에서 이 작업을 이어가기 위한 기준**이다.
> 새 세션을 열면 이 문서를 먼저 읽히고 "여기 이어서 진행하자"라고 지시하면 된다.
> (Claude Code 세션 기록은 머신 로컬이라 랩탑 간 이동하지 않는다 → **git에 커밋된 이 문서가 유일한 다리**다.)

## 목표 & 확정된 결정

MSA를 CI/CD + 쿠버네티스(k3s/k8s)로 클라우드 배포하기 위한 인프라 구축. MVP는 "동작하는 배포까지".

| 항목 | 결정 |
|---|---|
| 배포 대상 | 우선 로컬 kind → 최종 AWS(EKS) 지향 |
| CI/CD 도구 | GitHub Actions |
| 레지스트리 | 처음부터 ECR (AWS 통일) |
| CD 방식 | A안 — CI는 GitHub(빌드→ECR push), CD는 로컬 스크립트(`make deploy-local`). ArgoCD(GitOps)는 EKS 이관 단계 |
| 브랜치 전략 | **클래식 Git Flow** (main/develop/feature/release/hotfix) |
| 협업 | 팀 + Jira (이슈 키를 브랜치/커밋에 연결) |

## 실행 순서 & 상태

| # | 단계 | 상태 |
|---|---|---|
| 1 | 순수 CI (`.github/workflows/ci.yml`, push/PR build+test) | ✅ 완료 (develop에 반영, GitHub Actions 초록불 확인) |
| 2 | 컨테이너화 (공용 Dockerfile + `.dockerignore`) | ✅ 완료 (PR #1 → develop 머지, compose e2e 검증) |
| 3 | 로컬 통합 기동 (앱 포함 `infra/docker-compose.yml`) | ✅ 완료 (게이트웨이 경유 송금 복식부기까지 검증) |
| 4 | k8s 매니페스트 (Kustomize base + overlays/local) + kind 검증 | ✅ 완료 (feature/k8s-local, kind e2e 9/9 통과) → [`k8s/README.md`](../k8s/README.md) |
| 5 | ECR + OIDC 준비 → Release CD (`release.yml` matrix→ECR) + `Makefile deploy-local` | ⬜ 예정 |
| 6 | (MVP 이후) EKS 오버레이 · ArgoCD · Prometheus/Grafana · HPA · Terraform | ⬜ 백로그 |

## Git Flow 규칙 (이 프로젝트)

- feature 브랜치는 **`develop`에서 따고 `develop`으로 PR**. `main`은 릴리스 전용 — 평소 개발/최신화는 develop 기준.
- 최신화: `git checkout develop && git pull origin develop` → `git checkout feature/xxx && git merge develop`.
- `origin/develop`은 **팀 리드가 최초 생성** 예정이었으나, 이 세션에서 develop을 만들어 CI를 반영함.
- 브랜치 보호(리드 설정 대상): main/develop 직접 push 금지 + PR 필수 + status check `Build & Test (JDK 21)` 통과 필수.
- Jira 연동: 브랜치 `feature/<ISSUE-KEY>-...`, 커밋 메시지에 이슈 키 포함 → 티켓에 자동 연결.

## 현재 브랜치 / 워크트리 레이아웃

```
pointbank/              (feature/k8s-local)      ← k8s/ (base + overlays/local + kind + deploy-local.sh)
pointbank-dockerize/    (feature/docker-compose) ← develop에 머지 완료(PR #1). 워크트리는 정리 가능
origin: main, develop, feature/ci-pipeline·feature/docker-compose(병합됨), feature/k8s-local
```

> 로컬 도구: Docker Engine은 WSL(Ubuntu 24.04, systemd)에 직접 설치. kubectl·kind는 `~/.local/bin`.
> compose MySQL healthcheck가 `-proot` 하드코딩이라 로컬 `.env`의 `MYSQL_ROOT_PASSWORD=root` 필수.

## CI 상세 (`.github/workflows/ci.yml`)

- 트리거: PR(develop·main) + push(develop·main·`feature/**`).
- job `Build & Test (JDK 21)`: checkout → temurin 21 → setup-gradle(캐시) → `./gradlew build --no-daemon`.
- Testcontainers 통합테스트는 러너 Docker로 수행. 실패 시 테스트 리포트 아티팩트 업로드.
- 개선 백로그: top-level `permissions: contents: read`, gradlew 실행비트 커밋해 chmod 스텝 제거, path filter, 액션 Node20→최신.

## 컨테이너화 상세 (feature/docker-compose)

- 루트 **공용 `Dockerfile`**: 멀티스테이지(JDK build → JRE runtime), `--build-arg SERVICE=<모듈명>`으로 5개 서비스 커버. 비루트 유저, `java -jar`, `JAVA_OPTS` 주입.
- `infra/docker-compose.yml`에 앱 5종 추가 — MySQL/LocalStack과 컨테이너 네트워크로 연결(호스트명=container_name). DB URL은 `pointbank-mysql`, SQS는 `pointbank-localstack:4566`.
- **gateway `application.yml`**: 라우트 URI를 하드코딩 localhost → env 변수(로컬 기본값 유지)로 파라미터화(`AUTH_SERVICE_URI` 등). 컨테이너에선 서비스명으로 라우팅.
- ✅ **compose 전체 기동 실검증 완료** — `docker compose --env-file ../.env up -d --build` 후 게이트웨이(8080) 경유 회원가입→로그인→계좌개설→충전→A→B 송금→거래내역까지 통과(복식부기 TRANSFER_OUT/IN 확인). PR #1로 develop 머지.

## k8s 상세 (4단계, feature/k8s-local)

- `k8s/base`(앱 5종) + `k8s/overlays/local`(MySQL/LocalStack/Secret/NodePort) Kustomize. → [`k8s/README.md`](../k8s/README.md)
- DB 의존 서비스(auth/ledger/securities)엔 `wait-for-mysql` initContainer, banking/gateway는 지연 바인딩이라 없음.
- MySQL init SQL·SQS 스크립트는 `infra/`를 `configMapGenerator`로 재사용(단일 소스). 상위 경로라 `--load-restrictor LoadRestrictionsNone` 필요.
- 이미지는 compose 빌드본 `infra-pointbank-*-service:latest`를 `kind load`. 게이트웨이 NodePort 30080 → 호스트 8080.
- ✅ **kind 검증 완료**: 7 파드 `Running 1/1`(재시작 0), compose와 동일 e2e 9/9 통과.
- ingress는 MVP에서 NodePort로 대체(ingress-nginx 설치 회피). EKS 오버레이 단계에서 ALB Ingress로 교체 예정.

## 다음에 할 일 (재개 시)

1. `feature/k8s-local` → `develop` PR (CI 통과 후 머지).
2. (선택) `pointbank-dockerize` 워크트리 정리: `git worktree remove`.
3. **5단계**: ECR 리포 5개 + GitHub OIDC IAM Role → `release.yml`(matrix→ECR) + `Makefile deploy-local`(현 `k8s/deploy-local.sh`를 승격).
4. 6단계(백로그): `overlays/eks`(RDS/실 SQS/ALB Ingress, base 재사용) · ArgoCD · 관측성 · HPA · Terraform.

## 다른 랩탑에서 재개하는 법

```bash
git clone <repo> && cd pointbank      # 또는 git pull
git checkout develop && git pull
# (선택) 로컬 Claude 지침: cp docs/project-guide.md CLAUDE.md
# 새 Claude 세션 열고: "docs/cicd-progress.md 읽고 다음 단계 이어서 하자"
```
