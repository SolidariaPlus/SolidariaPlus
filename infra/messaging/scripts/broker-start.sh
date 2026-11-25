#!/bin/bash
# Boot script of the broker instance of A1 (KAN-31, KAN-2 ADR-122). systemd runs it at every
# start of the instance (solidaria-broker.service), after session-start has allocated the new
# Elastic IP and written the address and a certificate for it to Parameter Store.
#
# It runs Apache Kafka (KRaft, SASL_SSL with SCRAM, ACLs) and RabbitMQ (AMQPS, OAuth 2.0 backend)
# as two containers on the host network, with the same images, scripts and definitions as
# compose.yaml on localhost, then applies infra/messaging/kafka/*.conf and
# infra/messaging/rabbitmq/definitions.json. Topics, offsets and queues live in Docker volumes on
# the root volume, which survives session-stop because the instance is stopped, not deleted.
#
# Settings: /etc/solidaria/broker.env (written by the user data of modules/messaging).
set -euo pipefail

# shellcheck disable=SC1091
source /etc/solidaria/broker.env
: "${ENVIRONMENT:?}" "${AWS_REGION:?}" "${ARTIFACT_BUCKET:?}" "${KAFKA_IMAGE:?}" "${RABBITMQ_IMAGE:?}"

PREFIX="/solidaria/${ENVIRONMENT}/broker"
MESSAGING=/opt/solidaria/messaging
RUNTIME=/run/solidaria
TLS="$RUNTIME/tls"
USERS="$RUNTIME/kafka-users"

parameter() {
  aws ssm get-parameter --region "$AWS_REGION" --name "$1" --with-decryption --query Parameter.Value --output text
}
secret() {
  aws secretsmanager get-secret-value --region "$AWS_REGION" --secret-id "$1" --query SecretString --output text
}

echo "Syncing the broker definitions from s3://${ARTIFACT_BUCKET}/messaging"
mkdir -p "$MESSAGING"
aws s3 sync --region "$AWS_REGION" --delete --only-show-errors "s3://${ARTIFACT_BUCKET}/messaging" "$MESSAGING"

TOKEN="$(curl -fsS -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 300')"
PRIVATE_IP="$(curl -fsS -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/local-ipv4)"

# session-start writes the Elastic IP and its certificate before it starts the instance. On the
# very first apply the instance boots while Terraform is still issuing them, so wait for both.
PUBLIC_IP=""
CERTIFICATE=""
for _ in $(seq 1 90); do
  PUBLIC_IP="$(parameter "$PREFIX/public-address" 2>/dev/null || true)"
  CERTIFICATE="$(parameter "$PREFIX/certificate" 2>/dev/null || true)"
  if [ -n "$PUBLIC_IP" ] && [ "$PUBLIC_IP" != "none" ] && [ -n "$CERTIFICATE" ] && [ "$CERTIFICATE" != "none" ]; then
    break
  fi
  sleep 10
done
[ -n "$PUBLIC_IP" ] && [ "$PUBLIC_IP" != "none" ] || { echo "No public address in $PREFIX/public-address" >&2; exit 1; }
[ -n "$CERTIFICATE" ] && [ "$CERTIFICATE" != "none" ] || { echo "No certificate in $PREFIX/certificate" >&2; exit 1; }
echo "Private address ${PRIVATE_IP}, public address ${PUBLIC_IP}"

install -d -m 0755 "$RUNTIME" "$TLS"
install -d -m 0700 "$USERS"
printf '%s\n' "$CERTIFICATE" > "$TLS/server.pem"
secret "$PREFIX/private-key" > "$TLS/server.key"
parameter "/solidaria/${ENVIRONMENT}/pki/ca-certificate" > "$TLS/ca.pem"
KEYSTORE_PASSWORD="$(secret "$PREFIX/keystore-password")"
openssl pkcs12 -export -in "$TLS/server.pem" -inkey "$TLS/server.key" -certfile "$TLS/ca.pem" \
  -name broker -out "$TLS/server.p12" -passout "pass:${KEYSTORE_PASSWORD}"
# The container users (kafka uid 1000, rabbitmq uid 100) read these files on this dedicated host.
chmod 0644 "$TLS"/*

KAFKA_ADMIN_PASSWORD="$(secret "$PREFIX/kafka-admin-password")"
RABBITMQ_ADMIN_PASSWORD="$(secret "$PREFIX/rabbitmq-admin-password")"
grep -Ev '^[[:space:]]*(#|$)' "$MESSAGING/kafka/users.conf" | while read -r user; do
  secret "/solidaria/${ENVIRONMENT}/kafka/users/${user}" > "$USERS/$user"
done
chmod 0644 "$USERS"/*

cat > "$RUNTIME/30-tls.conf" <<EOF
listeners.tcp = none
listeners.ssl.default = 5671
ssl_options.cacertfile = /etc/rabbitmq/tls/ca.pem
ssl_options.certfile = /etc/rabbitmq/tls/server.pem
ssl_options.keyfile = /etc/rabbitmq/tls/server.key
ssl_options.verify = verify_none
ssl_options.fail_if_no_peer_cert = false
ssl_options.versions.1 = tlsv1.3
ssl_options.versions.2 = tlsv1.2
EOF
if [ -n "${RABBITMQ_JWKS_URL:-}" ]; then
  printf 'auth_oauth2.jwks_url = %s\n' "$RABBITMQ_JWKS_URL" > "$RUNTIME/40-oauth.conf"
else
  # MS-17 does not issue tokens yet (KAN-18): keep the internal backend only.
  printf 'auth_backends.1 = internal\n' > "$RUNTIME/40-oauth.conf"
fi
chmod 0644 "$RUNTIME"/*.conf

docker rm -f kafka rabbitmq >/dev/null 2>&1 || true

docker run -d --name kafka --network host --restart on-failure \
  -v kafka-data:/var/lib/kafka/data \
  -v "$MESSAGING:/opt/solidaria/messaging:ro" \
  -v "$TLS:/opt/solidaria/tls:ro" \
  -e KAFKA_ADMIN_PASSWORD="$KAFKA_ADMIN_PASSWORD" \
  -e KAFKA_INTERNAL_ADVERTISED="${PRIVATE_IP}:9092" \
  -e KAFKA_EXTERNAL_ADVERTISED="${PUBLIC_IP}:9094" \
  -e KAFKA_CLIENT_PROTOCOL=SASL_SSL \
  -e KAFKA_KEYSTORE_FILE=/opt/solidaria/tls/server.p12 \
  -e KAFKA_KEYSTORE_PASSWORD="$KEYSTORE_PASSWORD" \
  -e KAFKA_HEAP_OPTS="-Xms1g -Xmx1g" \
  --entrypoint bash "$KAFKA_IMAGE" /opt/solidaria/messaging/kafka/start-kafka.sh

docker run -d --name rabbitmq --network host --restart on-failure \
  -v rabbitmq-data:/var/lib/rabbitmq \
  -v "$MESSAGING/rabbitmq/enabled_plugins:/etc/rabbitmq/enabled_plugins:ro" \
  -v "$MESSAGING/rabbitmq/conf.d/20-solidaria.conf:/etc/rabbitmq/conf.d/20-solidaria.conf:ro" \
  -v "$RUNTIME/30-tls.conf:/etc/rabbitmq/conf.d/30-tls.conf:ro" \
  -v "$RUNTIME/40-oauth.conf:/etc/rabbitmq/conf.d/40-oauth.conf:ro" \
  -v "$TLS:/etc/rabbitmq/tls:ro" \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS="$RABBITMQ_ADMIN_PASSWORD" \
  -e RABBITMQ_DEFAULT_VHOST=solidaria \
  "$RABBITMQ_IMAGE"

echo "Waiting for Kafka"
for _ in $(seq 1 60); do
  docker exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server 127.0.0.1:9091 >/dev/null 2>&1 && break
  sleep 5
done

docker run --rm --network host \
  -v "$MESSAGING:/opt/solidaria/messaging:ro" \
  -v "$TLS:/opt/solidaria/tls:ro" \
  -v "$USERS:/opt/solidaria/kafka-users:ro" \
  -e KAFKA_BOOTSTRAP="${PRIVATE_IP}:9092" \
  -e KAFKA_ADMIN_PASSWORD="$KAFKA_ADMIN_PASSWORD" \
  -e KAFKA_CLIENT_PROTOCOL=SASL_SSL \
  -e KAFKA_TRUSTSTORE_FILE=/opt/solidaria/tls/ca.pem \
  -e KAFKA_PASSWORD_SOURCE=dir \
  -e KAFKA_PASSWORD_DIR=/opt/solidaria/kafka-users \
  -e DEFINITIONS_DIR=/opt/solidaria/messaging/kafka \
  -e KAFKA_HEAP_OPTS=-Xmx256m \
  --entrypoint bash "$KAFKA_IMAGE" /opt/solidaria/messaging/scripts/provision-kafka.sh

RABBITMQ_API=http://127.0.0.1:15672 \
  RABBITMQ_ADMIN_PASSWORD="$RABBITMQ_ADMIN_PASSWORD" \
  DEFINITIONS_FILE="$MESSAGING/rabbitmq/definitions.json" \
  sh "$MESSAGING/scripts/provision-rabbitmq.sh"

aws ssm put-parameter --region "$AWS_REGION" --overwrite --type String \
  --name "$PREFIX/status" --value "ready ${PUBLIC_IP} $(date -u +%Y-%m-%dT%H:%M:%SZ)" >/dev/null
echo "Broker ready"
