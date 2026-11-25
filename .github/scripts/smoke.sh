#!/bin/bash
# Smoke tests after a deployment (KAN-11 section 6.7): health of the rollout inside the cluster and
# a request through the public entry (CloudFront > HTTP API > VPC link > ALB > pod).
#
#   .github/scripts/smoke.sh <qa|prod> <service>
set -euo pipefail
ENVIRONMENT="$1"
SERVICE="$2"
EXPECTED_ENV="$([ "$ENVIRONMENT" = "prod" ] && echo production || echo qa)"

kubectl -n apps rollout status "deployment/${SERVICE}" --timeout=5m

domain="$(aws ssm get-parameter --name "/solidaria/${ENVIRONMENT}/edge/domain-name" --query Parameter.Value --output text)"
if [ "$SERVICE" = "sample-service" ]; then
  url="https://${domain}/auth/ping"
  echo "GET $url"
  for attempt in $(seq 1 20); do
    if body="$(curl -fsS --max-time 10 "$url")"; then
      echo "$body"
      echo "$body" | jq -e --arg env "$EXPECTED_ENV" '.status == "ok" and .environment == $env' >/dev/null
      echo "Smoke test passed"
      exit 0
    fi
    echo "Attempt ${attempt}: entry not ready yet"
    sleep 15
  done
  echo "::error::$url did not answer"
  exit 1
fi
echo "No external smoke test defined for ${SERVICE}; the rollout is healthy"
