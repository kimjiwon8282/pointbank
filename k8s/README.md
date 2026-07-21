# PointBank on Kubernetes (로컬 kind)

CI/CD 로드맵 **4단계** — Kustomize 매니페스트로 MSA 전체를 로컬 kind 클러스터에 배포한다.

## 구조

```
k8s/
├─ kind-cluster.yaml        # kind 클러스터 정의 (게이트웨이 NodePort 30080 → 호스트 8080)
├─ deploy-local.sh          # 이미지 load → apply → 롤아웃 대기 (make deploy-local 전신)
├─ base/                    # 환경 무관 앱 5종 (Deployment + Service)
│  ├─ auth / ledger / securities   # DB 의존 → wait-for-mysql initContainer 포함
│  ├─ banking / gateway            # 지연 바인딩 → initContainer 불필요
│  └─ kustomization.yaml
└─ overlays/local/          # 로컬 전용 인프라 주입
   ├─ mysql.yaml            # MySQL 8.4 (init SQL configMap 마운트, emptyDir)
   ├─ localstack.yaml       # LocalStack SQS (큐 생성 스크립트 마운트)
   ├─ gateway-nodeport.yaml # 게이트웨이 Service → NodePort 30080
   └─ kustomization.yaml    # secret/configMap 생성 + base 조합
```

- **스키마 단일 소스**: MySQL init SQL·LocalStack 큐 스크립트는 `infra/`의 파일을 `configMapGenerator`로 그대로 재사용한다. 상위 경로 참조라 렌더 시 `--load-restrictor LoadRestrictionsNone`가 필요하다.
- **이미지**: compose가 빌드한 `infra-pointbank-*-service:latest`를 `kind load`로 노드에 주입한다 (`imagePullPolicy: IfNotPresent`).

## 배포 순서

```bash
# 0) (최초 1회) 앱 이미지 빌드 — compose 재사용
cd infra && docker compose --env-file ../.env build && cd ..

# 1) kind 클러스터 생성
kind create cluster --config k8s/kind-cluster.yaml

# 2) 배포 (이미지 load → apply → 롤아웃 대기)
bash k8s/deploy-local.sh

# 3) 확인
kubectl -n pointbank get pods
curl http://localhost:8080/api/auth/health     # 게이트웨이 경유
```

## 정리

```bash
kubectl delete -k k8s/overlays/local --load-restrictor LoadRestrictionsNone   # 리소스만
kind delete cluster --name pointbank                                          # 클러스터째
```

## 검증 결과

`docker compose`와 동일한 end-to-end 시나리오(회원가입→로그인→계좌개설→충전→A→B 송금→거래내역)를 게이트웨이(`localhost:8080`) 경유로 통과 확인. 7개 파드 전부 `Running 1/1`, 재시작 0회.

## 다음 단계 (5단계)

ECR 리포 + GitHub OIDC → `release.yml`(matrix build→ECR push) + `Makefile deploy-local`. EKS 오버레이는 이 `base`를 재사용하고 `overlays/eks`에서 RDS/실 SQS/ingress로 교체.
