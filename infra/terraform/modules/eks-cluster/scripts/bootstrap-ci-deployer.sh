#!/bin/bash
# ADR-123 step 2, run by Terraform (terraform_data.ci_deployer) on the self-hosted runner right
# after the cluster is created and before the node group exists:
#   1. use the bootstrap admin access entry of the creator (LabRole) once;
#   2. create the ServiceAccount ci-deployer, its ClusterRoleBinding and a token Secret;
#   3. store token, endpoint and CA in Parameter Store (SecureString) under $PARAMETER_DIR;
#   4. delete the access entry of LabRole, so that the node group can map LabRole to system:node.
# Fallback if step 1 fails (KAN-35): the owner runs this script from the console session.
#
# Environment: CLUSTER_NAME, AWS_REGION, PARAMETER_DIR, CREATOR_ARN, CLUSTER_ENDPOINT.
set -euo pipefail

: "${CLUSTER_NAME:?}" "${AWS_REGION:?}" "${PARAMETER_DIR:?}" "${CREATOR_ARN:?}" "${CLUSTER_ENDPOINT:?}"

KUBECONFIG="$(mktemp)"
export KUBECONFIG
trap 'rm -f "$KUBECONFIG"' EXIT

aws eks wait cluster-active --region "$AWS_REGION" --name "$CLUSTER_NAME"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME" --kubeconfig "$KUBECONFIG" >/dev/null

echo "Waiting for the Kubernetes API of $CLUSTER_NAME"
for _ in $(seq 1 60); do
  kubectl get --raw /readyz >/dev/null 2>&1 && break
  sleep 5
done

kubectl apply -f - <<'EOF'
apiVersion: v1
kind: ServiceAccount
metadata:
  name: ci-deployer
  namespace: kube-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: ci-deployer
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
  - kind: ServiceAccount
    name: ci-deployer
    namespace: kube-system
---
apiVersion: v1
kind: Secret
metadata:
  name: ci-deployer-token
  namespace: kube-system
  annotations:
    kubernetes.io/service-account.name: ci-deployer
type: kubernetes.io/service-account-token
EOF

token=""
for _ in $(seq 1 30); do
  token="$(kubectl -n kube-system get secret ci-deployer-token -o jsonpath='{.data.token}' 2>/dev/null || true)"
  [ -n "$token" ] && break
  sleep 2
done
[ -n "$token" ] || { echo "The token of ci-deployer was not issued" >&2; exit 1; }
ca="$(kubectl -n kube-system get secret ci-deployer-token -o jsonpath='{.data.ca\.crt}')"

aws ssm put-parameter --region "$AWS_REGION" --overwrite --type SecureString \
  --name "$PARAMETER_DIR/ci-deployer-token" --value "$(printf '%s' "$token" | base64 -d)" >/dev/null
aws ssm put-parameter --region "$AWS_REGION" --overwrite --type String \
  --name "$PARAMETER_DIR/endpoint" --value "$CLUSTER_ENDPOINT" >/dev/null
aws ssm put-parameter --region "$AWS_REGION" --overwrite --type String \
  --name "$PARAMETER_DIR/ca" --value "$ca" >/dev/null
echo "Token, endpoint and CA of ci-deployer stored under $PARAMETER_DIR"

if aws eks describe-access-entry --region "$AWS_REGION" --cluster-name "$CLUSTER_NAME" \
  --principal-arn "$CREATOR_ARN" >/dev/null 2>&1; then
  aws eks delete-access-entry --region "$AWS_REGION" --cluster-name "$CLUSTER_NAME" --principal-arn "$CREATOR_ARN"
  echo "Access entry of $CREATOR_ARN deleted"
fi
