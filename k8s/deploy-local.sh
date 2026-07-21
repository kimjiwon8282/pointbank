#!/usr/bin/env bash
# 로컬 kind 클러스터에 PointBank 전체 배포 (5단계 Makefile deploy-local 전신).
# 전제: compose로 이미지가 빌드돼 있어야 함 (infra-pointbank-*-service:latest).
#        kind 클러스터 'pointbank'가 이미 생성돼 있어야 함 (k8s/kind-cluster.yaml).
set -euo pipefail
CLUSTER=pointbank
NS=pointbank
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERVICES=(auth banking securities ledger gateway)

echo "== 1) 로컬 이미지를 kind 노드로 로드 =="
for s in "${SERVICES[@]}"; do
  kind load docker-image "infra-pointbank-${s}-service:latest" --name "$CLUSTER"
done

echo "== 2) 매니페스트 적용 (overlays/local) =="
kubectl kustomize --load-restrictor LoadRestrictionsNone "$ROOT/k8s/overlays/local" | kubectl apply -f -

echo "== 3) 롤아웃 대기 =="
kubectl -n "$NS" rollout status deploy/mysql --timeout=180s
kubectl -n "$NS" rollout status deploy/localstack --timeout=180s
for s in "${SERVICES[@]}"; do
  kubectl -n "$NS" rollout status "deploy/$s" --timeout=240s
done

echo "== 완료 == 게이트웨이: http://localhost:8080 (NodePort 30080)"
kubectl -n "$NS" get pods
