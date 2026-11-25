#!/bin/bash
# Turns the bastion of one account on or off (KAN-11 section 5.6, KAN-2 ADR-096).
#
#   scripts/bastion.sh on|off|status <qa|prod> <A1..A5>
#
# Run it with the credentials of the lab session of that account. "on" waits for the instance to
# register with Systems Manager and prints the commands to open a shell or a port forward.
set -euo pipefail

usage() { sed -n '2,7p' "$0"; exit 2; }
[ $# -eq 3 ] || usage
ACTION="$1"
ENVIRONMENT="$2"
SLOT_LOWER="$(printf '%s' "$3" | tr '[:upper:]' '[:lower:]')"
case "$ENVIRONMENT" in
  qa) REGION=us-west-2 ;;
  prod) REGION=us-east-1 ;;
  *) usage ;;
esac
GROUP="solidaria-${ENVIRONMENT}-${SLOT_LOWER}-bastion"

echo "Account $(aws sts get-caller-identity --query Account --output text), region ${REGION}, group ${GROUP}"

# shellcheck disable=SC2016 # JMESPath literals use backticks
instance_id() {
  aws autoscaling describe-auto-scaling-groups --region "$REGION" --auto-scaling-group-names "$GROUP" \
    --query 'AutoScalingGroups[0].Instances[?LifecycleState==`InService`].InstanceId | [0]' --output text
}

case "$ACTION" in
  on)
    aws autoscaling set-desired-capacity --region "$REGION" --auto-scaling-group-name "$GROUP" --desired-capacity 1
    echo "Waiting for the bastion"
    id="None"
    for _ in $(seq 1 60); do
      id="$(instance_id)"
      if [ "$id" != "None" ] && aws ssm describe-instance-information --region "$REGION" \
        --filters "Key=InstanceIds,Values=${id}" --query 'InstanceInformationList[0].PingStatus' \
        --output text 2>/dev/null | grep -q Online; then
        break
      fi
      sleep 10
    done
    echo "Bastion ${id} is online. Examples:"
    echo "  aws ssm start-session --region ${REGION} --target ${id}"
    echo "  aws ssm start-session --region ${REGION} --target ${id} \\"
    echo "    --document-name AWS-StartPortForwardingSessionToRemoteHost \\"
    echo "    --parameters host=<aurora-endpoint>,portNumber=5432,localPortNumber=15432"
    echo "Turn it off when finished: scripts/bastion.sh off ${ENVIRONMENT} $3"
    ;;
  off)
    aws autoscaling set-desired-capacity --region "$REGION" --auto-scaling-group-name "$GROUP" --desired-capacity 0
    echo "Bastion desired capacity set to 0"
    ;;
  status)
    aws autoscaling describe-auto-scaling-groups --region "$REGION" --auto-scaling-group-names "$GROUP" \
      --query 'AutoScalingGroups[0].{desired:DesiredCapacity,instances:Instances[].InstanceId}' --output table
    ;;
  *) usage ;;
esac
