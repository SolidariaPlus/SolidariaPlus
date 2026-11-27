#!/bin/sh
# LocalStack ready hook: creates the secrets and parameters each service reads in AWS,
# with the same names as in QA and Production but the environment "local".
# Names: /solidaria/<env>/<service>/<kind> (KAN-11 section 5.2, parity table).
set -eu

ENVIRONMENT=local
SERVICES_FILE="${SERVICES_FILE:-/opt/solidaria/services.conf}"
DB_PASSWORD="${SERVICE_DB_PASSWORD:?SERVICE_DB_PASSWORD is required}"
CACHE_PASSWORD="${SERVICE_CACHE_PASSWORD:?SERVICE_CACHE_PASSWORD is required}"
KAFKA_PASSWORD="${KAFKA_SERVICE_PASSWORD:?KAFKA_SERVICE_PASSWORD is required}"

put_secret() {
  if awslocal secretsmanager describe-secret --secret-id "$1" >/dev/null 2>&1; then
    awslocal secretsmanager put-secret-value --secret-id "$1" --secret-string "$2" >/dev/null
  else
    awslocal secretsmanager create-secret --name "$1" --secret-string "$2" >/dev/null
  fi
}

put_parameter() {
  awslocal ssm put-parameter --name "$1" --type String --value "$2" --overwrite >/dev/null
}

grep -Ev '^[[:space:]]*(#|$)' "$SERVICES_FILE" | while read -r service database prefix; do
  base="/solidaria/${ENVIRONMENT}/${service}"
  role="$(printf '%s' "$service" | tr '-' '_')"
  put_secret "${base}/db" \
    "{\"username\":\"${role}\",\"password\":\"${DB_PASSWORD}\",\"host\":\"postgres\",\"port\":5432,\"dbname\":\"${database}\"}"
  put_secret "${base}/cache" \
    "{\"username\":\"${service}\",\"password\":\"${CACHE_PASSWORD}\",\"host\":\"valkey\",\"port\":6379,\"keyPrefix\":\"${prefix}:\"}"
  put_secret "${base}/kafka" \
    "{\"username\":\"${service}\",\"password\":\"${KAFKA_PASSWORD}\",\"mechanism\":\"SCRAM-SHA-512\"}"
  put_secret "/solidaria/${ENVIRONMENT}/kafka/users/${service}" "${KAFKA_PASSWORD}"
  put_parameter "${base}/kafka/bootstrap-servers" "kafka:9092"
  put_parameter "${base}/rabbitmq/endpoint" "amqp://rabbitmq:5672/solidaria"
  echo "LocalStack: settings of ${service} ready"
done

# MS-17 keys, generated once per volume so tokens and encrypted secrets survive a restart.
put_secret_once() {
  awslocal secretsmanager describe-secret --secret-id "$1" >/dev/null 2>&1 || put_secret "$1" "$2"
}
identity="/solidaria/${ENVIRONMENT}/identity-access-service"
key_file="$(mktemp)"
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out "$key_file" 2>/dev/null
put_secret_once "${identity}/signing-key" \
  "{\"kid\":\"local-$(date +%Y%m%d)\",\"privateKey\":\"$(openssl pkcs8 -topk8 -nocrypt -in "$key_file" -outform DER | base64 | tr -d '\n')\",\"publicKey\":\"$(openssl pkey -in "$key_file" -pubout -outform DER | base64 | tr -d '\n')\"}"
rm -f "$key_file"
put_secret_once "${identity}/field-encryption-key" "{\"aesKey\":\"$(head -c 32 /dev/urandom | base64 | tr -d '\n')\"}"
put_secret "${identity}/directory" "{\"password\":\"${STAFF_LOCAL_PASSWORD:?STAFF_LOCAL_PASSWORD is required}\"}"

put_parameter "/solidaria/${ENVIRONMENT}/session/state" "on"
echo "LocalStack initialization complete"
