#!/bin/bash
# Deploys one service by digest with the shared chart (KAN-11 section 6.6; KAN-33, KAN-34).
#
#   .github/scripts/helm-deploy.sh <qa|prod> <A1..A5> <service> <digest>
#
# Values, later wins: chart values.yaml < chart values-<env>.yaml < service values.yaml
# < service values-<env>.yaml < values read here from Parameter Store and Secrets Manager
# (repository, region, ALB group and security group, origin header, VPC CIDR) < image digest.
# KUBECONFIG must point to the cluster (.github/actions/kube-auth). helm upgrade --atomic rolls
# the release back if its pods do not become ready.
set -euo pipefail

ENVIRONMENT="$1"
SLOT="$2"
SERVICE="$3"
DIGEST="$4"
SLOT_LOWER="$(printf '%s' "$SLOT" | tr '[:upper:]' '[:lower:]')"
CHART=deploy/helm/solidaria-service
NAMESPACE=apps

[[ "$DIGEST" =~ ^sha256:[0-9a-f]{64}$ ]] || { echo "Invalid digest: $DIGEST" >&2; exit 2; }
SERVICE_DIR="$(find services -mindepth 2 -maxdepth 2 -type d -name "$SERVICE" | head -1)"
[ -n "$SERVICE_DIR" ] || { echo "Unknown service $SERVICE" >&2; exit 2; }

REGION="$(python3 .github/scripts/accounts.py region "$ENVIRONMENT")"
ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
REPOSITORY="${ACCOUNT}.dkr.ecr.${REGION}.amazonaws.com/${SERVICE}"
parameter() { aws ssm get-parameter --name "$1" --with-decryption --query Parameter.Value --output text; }
PLATFORM="/solidaria/${ENVIRONMENT}/${SLOT_LOWER}/platform"
CLUSTER="$(parameter "$PLATFORM/cluster-name")"
ALB_SG="$(parameter "$PLATFORM/alb-security-group-id")"
VPC_CIDR="$(aws ec2 describe-vpcs --vpc-ids "$(parameter "$PLATFORM/vpc-id")" --query 'Vpcs[0].CidrBlock' --output text)"
ORIGIN_VERIFY="$(aws secretsmanager get-secret-value --secret-id "/solidaria/${ENVIRONMENT}/edge/origin-verify" \
  --query SecretString --output text 2>/dev/null || true)"

env_file="$ENVIRONMENT"
values=(-f "$CHART/values.yaml" -f "$CHART/values-${env_file}.yaml" -f "$SERVICE_DIR/helm/values.yaml")
[ -f "$SERVICE_DIR/helm/values-${env_file}.yaml" ] && values+=(-f "$SERVICE_DIR/helm/values-${env_file}.yaml")

generated="$(mktemp)"
trap 'rm -f "$generated"' EXIT
chmod 600 "$generated"
cat > "$generated" <<EOF
slot: ${SLOT}
awsRegion: ${REGION}
image:
  repository: ${REPOSITORY}
  digest: "${DIGEST}"
ingress:
  groupName: ${CLUSTER}
  securityGroupId: ${ALB_SG}
  originVerify:
    value: "${ORIGIN_VERIFY}"
networkPolicy:
  allowFromCidrs: ["${VPC_CIDR}"]
EOF

kubectl get namespace "$NAMESPACE" >/dev/null 2>&1 || kubectl create namespace "$NAMESPACE"
helm upgrade --install "$SERVICE" "$CHART" \
  --namespace "$NAMESPACE" \
  "${values[@]}" -f "$generated" \
  --atomic --wait --timeout 10m --history-max 10

aws ssm put-parameter --overwrite --type String \
  --name "/solidaria/${ENVIRONMENT}/${SLOT_LOWER}/deploy/${SERVICE}/digest" --value "$DIGEST" >/dev/null
echo "Deployed ${SERVICE}@${DIGEST} to ${ENVIRONMENT}-${SLOT}"
