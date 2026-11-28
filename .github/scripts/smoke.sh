#!/bin/bash
# Smoke tests after a deployment (KAN-11 section 6.7): health of the rollout inside the cluster and
# a request through the HTTP API (HTTP API > VPC link > ALB > pod), with the origin header that the
# edge adds in front of the ALB.
#
#   .github/scripts/smoke.sh <qa|prod> <service>
set -euo pipefail
ENVIRONMENT="$1"
SERVICE="$2"
EXPECTED_ENV="$([ "$ENVIRONMENT" = "prod" ] && echo production || echo qa)"

kubectl -n apps rollout status "deployment/${SERVICE}" --timeout=5m

slot_lower="$(python3 .github/scripts/accounts.py slot-of "$ENVIRONMENT" "$SERVICE" | tr '[:upper:]' '[:lower:]')"
endpoint="$(aws ssm get-parameter --name "/solidaria/${ENVIRONMENT}/${slot_lower}/platform/http-api-endpoint" \
  --query Parameter.Value --output text)"
origin_verify="$(aws secretsmanager get-secret-value --secret-id "/solidaria/${ENVIRONMENT}/edge/origin-verify" \
  --query SecretString --output text 2>/dev/null || true)"

case "$SERVICE" in
  sample-service)
    url="${endpoint}/auth/ping"
    check() { jq -e --arg env "$EXPECTED_ENV" '.status == "ok" and .environment == $env' >/dev/null; }
    ;;
  identity-access-service)
    url="${endpoint}/auth/.well-known/openid-configuration"
    check() { jq -e '.issuer | endswith("/auth")' >/dev/null; }
    ;;
  *)
    echo "No external smoke test defined for ${SERVICE}; the rollout is healthy"
    exit 0
    ;;
esac

echo "GET $url"
for attempt in $(seq 1 20); do
  if body="$(curl -fsS --max-time 10 -H "X-Origin-Verify: ${origin_verify}" "$url")"; then
    echo "$body" | head -c 600
    echo
    echo "$body" | check
    echo "Smoke test passed"
    exit 0
  fi
  echo "Attempt ${attempt}: entry not ready yet"
  sleep 15
done
echo "::error::$url did not answer"
exit 1
