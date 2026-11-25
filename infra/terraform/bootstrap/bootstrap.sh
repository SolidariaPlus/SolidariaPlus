#!/bin/bash
# Bootstraps one Learner Lab account: state bucket, permanent network, self-hosted runner and
# budget alarms (KAN-28). Run it once per account, from the workstation or AWS CloudShell, with
# the credentials of the lab session of that account (AWS Details > AWS CLI in the Learner Lab).
#
#   infra/terraform/bootstrap/bootstrap.sh <qa|prod> <A1..A5> [extra terraform apply arguments]
#
# Examples:
#   bootstrap.sh qa A1 -var runner_registration_token=AAAA... -var budget_email=me@example.com
#   bootstrap.sh qa A1                       # later re-applies, the runner is already registered
set -euo pipefail

if [ $# -lt 2 ]; then
  sed -n '2,12p' "$0"
  exit 2
fi

ENVIRONMENT="$1"
SLOT="$2"
shift 2
SLOT_LOWER="$(printf '%s' "$SLOT" | tr '[:upper:]' '[:lower:]')"
HERE="$(cd "$(dirname "$0")" && pwd)"
VAR_FILE="$HERE/accounts/${ENVIRONMENT}-${SLOT_LOWER}.tfvars"

case "$ENVIRONMENT" in
  qa) REGION=us-west-2 ;;
  prod) REGION=us-east-1 ;;
  *) echo "environment must be qa or prod" >&2; exit 2 ;;
esac
[ -f "$VAR_FILE" ] || { echo "Missing $VAR_FILE" >&2; exit 2; }

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
BUCKET="solidaria-tfstate-${ENVIRONMENT}-${SLOT_LOWER}-${ACCOUNT_ID}"
echo "Account ${ACCOUNT_ID}, environment ${ENVIRONMENT}, slot ${SLOT}, region ${REGION}"
read -r -p "Is this the ${ENVIRONMENT^^}-${SLOT} account? [y/N] " answer
[ "$answer" = "y" ] || { echo "Aborted"; exit 1; }

if ! aws s3api head-bucket --bucket "$BUCKET" --region "$REGION" 2>/dev/null; then
  echo "Creating the state bucket ${BUCKET}"
  if [ "$REGION" = "us-east-1" ]; then
    aws s3api create-bucket --bucket "$BUCKET" --region "$REGION" >/dev/null
  else
    aws s3api create-bucket --bucket "$BUCKET" --region "$REGION" \
      --create-bucket-configuration "LocationConstraint=${REGION}" >/dev/null
  fi
  aws s3api put-bucket-versioning --bucket "$BUCKET" --region "$REGION" \
    --versioning-configuration Status=Enabled
  aws s3api put-public-access-block --bucket "$BUCKET" --region "$REGION" \
    --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
fi

export TF_DATA_DIR="$HERE/.terraform-${ENVIRONMENT}-${SLOT_LOWER}"
terraform -chdir="$HERE" init -input=false -reconfigure \
  -backend-config="bucket=${BUCKET}" \
  -backend-config="key=bootstrap/terraform.tfstate" \
  -backend-config="region=${REGION}"
terraform -chdir="$HERE" apply -input=false -var-file="$VAR_FILE" "$@"
