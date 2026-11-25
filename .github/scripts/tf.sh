#!/bin/bash
# Runs Terraform for one root of one account on its self-hosted runner (infra.yml, session.yml).
#
#   .github/scripts/tf.sh <qa|prod> <A1..A5> <root: infra|platform> <plan|apply|destroy> [terraform args]
#
# The backend bucket is derived from the account of the runner (created by the bootstrap):
#   solidaria-tfstate-<env>-<slot>-<account>, key envs/<env>/<slot>[-platform]/terraform.tfstate.
set -euo pipefail

ENVIRONMENT="$1"
SLOT_LOWER="$(printf '%s' "$2" | tr '[:upper:]' '[:lower:]')"
ROOT="$3"
COMMAND="$4"
shift 4

case "$ROOT" in
  infra) DIR="infra/terraform/envs/${ENVIRONMENT}/${SLOT_LOWER}" ;;
  platform) DIR="infra/terraform/envs/${ENVIRONMENT}/${SLOT_LOWER}-platform" ;;
  *) echo "root must be infra or platform" >&2; exit 2 ;;
esac
KEY="${DIR#infra/terraform/}/terraform.tfstate"
ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
REGION="$(python3 .github/scripts/accounts.py region "$ENVIRONMENT")"
BUCKET="solidaria-tfstate-${ENVIRONMENT}-${SLOT_LOWER}-${ACCOUNT}"

export TF_IN_AUTOMATION=1
export TF_INPUT=0
terraform -chdir="$DIR" init -reconfigure \
  -backend-config="bucket=${BUCKET}" \
  -backend-config="key=${KEY}" \
  -backend-config="region=${REGION}" >/dev/null
echo "Terraform ${COMMAND} of ${DIR} (state s3://${BUCKET}/${KEY})"

case "$COMMAND" in
  plan) terraform -chdir="$DIR" plan -lock-timeout=5m "$@" ;;
  apply) terraform -chdir="$DIR" apply -auto-approve -lock-timeout=5m "$@" ;;
  destroy) terraform -chdir="$DIR" destroy -auto-approve -lock-timeout=5m "$@" ;;
  output) terraform -chdir="$DIR" output "$@" ;;
  *) echo "command must be plan, apply, destroy or output" >&2; exit 2 ;;
esac
