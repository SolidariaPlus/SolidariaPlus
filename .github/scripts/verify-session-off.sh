#!/bin/bash
# Fails if any resource with an hourly price remains in the account after session-stop
# (KAN-21 acceptance criterion 5; KAN-2 ADR-080). Allowed: the self-hosted runner (it runs this
# script) and stopped instances, whose volumes cost by the month, not by the hour.
#
#   .github/scripts/verify-session-off.sh <region>
set -euo pipefail
REGION="$1"
problems=0
report() {
  if [ -n "$2" ] && [ "$2" != "None" ]; then
    echo "::error::$1: $2"
    problems=1
  else
    echo "ok   $1"
  fi
}

report "NAT gateways" "$(aws ec2 describe-nat-gateways --region "$REGION" \
  --filter Name=state,Values=pending,available --query 'NatGateways[].NatGatewayId' --output text)"
report "EKS clusters" "$(aws eks list-clusters --region "$REGION" --query 'clusters[]' --output text)"
report "ElastiCache replication groups" "$(aws elasticache describe-replication-groups --region "$REGION" \
  --query 'ReplicationGroups[].ReplicationGroupId' --output text)"
report "Load balancers" "$(aws elbv2 describe-load-balancers --region "$REGION" \
  --query 'LoadBalancers[].LoadBalancerName' --output text)"
report "Interface VPC endpoints" "$(aws ec2 describe-vpc-endpoints --region "$REGION" \
  --filters Name=vpc-endpoint-type,Values=Interface Name=vpc-endpoint-state,Values=pending,available \
  --query 'VpcEndpoints[].VpcEndpointId' --output text)"
report "Elastic IPs" "$(aws ec2 describe-addresses --region "$REGION" --query 'Addresses[].PublicIp' --output text)"
# shellcheck disable=SC2016 # JMESPath literals use backticks
report "Running instances other than the runner" "$(aws ec2 describe-instances --region "$REGION" \
  --filters Name=instance-state-name,Values=pending,running \
  --query 'Reservations[].Instances[?!(Tags[?Key==`role` && Value==`github-runner`])].InstanceId' --output text)"
# shellcheck disable=SC2016 # JMESPath literals use backticks
report "Available Aurora clusters" "$(aws rds describe-db-clusters --region "$REGION" \
  --query 'DBClusters[?Status!=`stopped` && Status!=`stopping`].DBClusterIdentifier' --output text)"

if [ "$problems" -ne 0 ]; then
  echo "::error::Resources with an hourly price remain; they keep charging the credit of the account"
  exit 1
fi
echo "Session is off: no resource with an hourly price remains"
