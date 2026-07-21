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
| 2 | 컨테이너화 (공용 Dockerfile + `.dockerignore`) | 🚧 진행 (feature/docker-compose) |
| 3 | 로컬 통합 기동 (앱 포함 `infra/docker-compose.yml`) | 🚧 진행 (feature/docker-compose) |
| 4 | k8s 매니페스트 (Kustomize base + overlays/local) + kind 검증 | ⬜ 예정 |
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
pointbank/              (develop)              ← CI + docs + .gitignore(CLAUDE.md)
pointbank-dockerize/    (feature/docker-compose) ← Dockerfile + .dockerignore + compose 앱 5종 + gateway env 파라미터화
origin: main, develop, feature/ci-pipeline (병합됨), feature/docker-compose
```

## CI 상세 (`.github/workflows/ci.yml`)

- 트리거: PR(develop·main) + push(develop·main·`feature/**`).
- job `Build & Test (JDK 21)`: checkout → temurin 21 → setup-gradle(캐시) → `./gradlew build --no-daemon`.
- Testcontainers 통합테스트는 러너 Docker로 수행. 실패 시 테스트 리포트 아티팩트 업로드.
- 개선 백로그: top-level `permissions: contents: read`, gradlew 실행비트 커밋해 chmod 스텝 제거, path filter, 액션 Node20→최신.

## 컨테이너화 상세 (feature/docker-compose)

- 루트 **공용 `Dockerfile`**: 멀티스테이지(JDK build → JRE runtime), `--build-arg SERVICE=<모듈명>`으로 5개 서비스 커버. 비루트 유저, `java -jar`, `JAVA_OPTS` 주입.
- `infra/docker-compose.yml`에 앱 5종 추가 — MySQL/LocalStack과 컨테이너 네트워크로 연결(호스트명=container_name). DB URL은 `pointbank-mysql`, SQS는 `pointbank-localstack:4566`.
- **gateway `application.yml`**: 라우트 URI를 하드코딩 localhost → env 변수(로컬 기본값 유지)로 파라미터화(`AUTH_SERVICE_URI` 등). 컨테이너에선 서비스명으로 라우팅.
- ⚠️ **로컬 Docker가 꺼져 있어(WSL) 실제 compose 기동은 미검증.** 검증하려면 Docker 켜고: `cd infra && docker compose --env-file ../.env up -d --build` → 게이트웨이 통해 auth 로그인 → banking 호출 end-to-end 확인.

## 다음에 할 일 (재개 시)

1. Docker 켜고 `docker compose ... up -d --build`로 앱 포함 전체 기동 **실검증** (2·3단계 마무리).
2. `feature/docker-compose` → `develop` PR (CI 통과 확인 후 머지).
3. 4단계: `k8s/base` + `overlays/local`(MySQL/LocalStack/ingress) Kustomize 작성 → kind 검증.
4. 5단계: ECR 리포 5개 + GitHub OIDC IAM Role → `release.yml`(matrix→ECR) + `Makefile deploy-local`.

## 다른 랩탑에서 재개하는 법

```bash
git clone <repo> && cd pointbank      # 또는 git pull
git checkout develop && git pull
# (선택) 로컬 Claude 지침: cp docs/project-guide.md CLAUDE.md
# 새 Claude 세션 열고: "docs/cicd-progress.md 읽고 다음 단계 이어서 하자"
```
