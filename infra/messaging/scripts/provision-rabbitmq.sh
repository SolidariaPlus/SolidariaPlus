#!/bin/sh
# Imports infra/messaging/rabbitmq/definitions.json (vhost, exchanges, queues, dead-letter
# routing) through the RabbitMQ management API. The same script runs on localhost
# (rabbitmq-provisioner in compose.yaml) and on the broker instance of A1 (broker-start.sh).
# Idempotent: the import merges with what exists and never deletes a queue.
#
# Environment:
#   RABBITMQ_API             base URL of the management API, for example http://rabbitmq:15672 (required)
#   RABBITMQ_ADMIN_USER      administrator user (default admin)
#   RABBITMQ_ADMIN_PASSWORD  administrator password (required)
#   DEFINITIONS_FILE         default /opt/solidaria/messaging/rabbitmq/definitions.json
set -eu

: "${RABBITMQ_API:?RABBITMQ_API is required}"
: "${RABBITMQ_ADMIN_PASSWORD:?RABBITMQ_ADMIN_PASSWORD is required}"
USER_NAME="${RABBITMQ_ADMIN_USER:-admin}"
FILE="${DEFINITIONS_FILE:-/opt/solidaria/messaging/rabbitmq/definitions.json}"

echo "Waiting for the RabbitMQ management API at ${RABBITMQ_API}"
i=0
until curl -fsS -u "${USER_NAME}:${RABBITMQ_ADMIN_PASSWORD}" "${RABBITMQ_API}/api/overview" >/dev/null 2>&1; do
  i=$((i + 1))
  if [ "$i" -ge 60 ]; then
    echo "RabbitMQ management API not reachable" >&2
    exit 1
  fi
  sleep 2
done

curl -fsS -u "${USER_NAME}:${RABBITMQ_ADMIN_PASSWORD}" \
  -H 'content-type: application/json' \
  -X POST --data-binary "@${FILE}" \
  "${RABBITMQ_API}/api/definitions" >/dev/null

echo "RabbitMQ definitions imported from ${FILE}"
