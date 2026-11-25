#!/bin/bash
# Creates the Kafka topics, SCRAM users and ACLs declared in infra/messaging/kafka.
# The same script runs on localhost (kafka-provisioner in compose.yaml) and on the broker
# instance of A1 (broker-start.sh), so both environments get the same definitions (KAN-26, KAN-31).
# Idempotent: existing topics are kept (their retention is re-applied), user passwords are
# re-set and ACLs are added only when missing. It never deletes a topic, user or ACL.
#
# Environment:
#   KAFKA_BOOTSTRAP          host:port of a SCRAM listener (required)
#   KAFKA_ADMIN_PASSWORD     password of the super user "admin" (required)
#   KAFKA_CLIENT_PROTOCOL    SASL_PLAINTEXT or SASL_SSL; default SASL_PLAINTEXT
#   KAFKA_TRUSTSTORE_FILE    PEM file of the environment CA, required with SASL_SSL
#   KAFKA_PASSWORD_SOURCE    "env": every user gets KAFKA_SERVICE_PASSWORD (localhost only);
#                            "dir": the password of user <u> is the content of $KAFKA_PASSWORD_DIR/<u>
#   DEFINITIONS_DIR          directory with topics.conf, users.conf and acls.conf
set -euo pipefail

: "${KAFKA_BOOTSTRAP:?KAFKA_BOOTSTRAP is required}"
: "${KAFKA_ADMIN_PASSWORD:?KAFKA_ADMIN_PASSWORD is required}"
PROTOCOL="${KAFKA_CLIENT_PROTOCOL:-SASL_PLAINTEXT}"
SOURCE="${KAFKA_PASSWORD_SOURCE:-env}"
DEFS="${DEFINITIONS_DIR:-/opt/solidaria/messaging/kafka}"
BIN="${KAFKA_HOME:-/opt/kafka}/bin"
ADMIN_CONFIG="$(mktemp)"
trap 'rm -f "$ADMIN_CONFIG"' EXIT

cat > "$ADMIN_CONFIG" <<EOF
security.protocol=${PROTOCOL}
sasl.mechanism=SCRAM-SHA-512
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="admin" password="${KAFKA_ADMIN_PASSWORD}";
EOF
if [ "$PROTOCOL" = "SASL_SSL" ]; then
  : "${KAFKA_TRUSTSTORE_FILE:?KAFKA_TRUSTSTORE_FILE is required with SASL_SSL}"
  printf 'ssl.truststore.type=PEM\nssl.truststore.location=%s\n' "$KAFKA_TRUSTSTORE_FILE" >> "$ADMIN_CONFIG"
fi

records() { grep -Ev '^[[:space:]]*(#|$)' "$1"; }

password_of() {
  case "$SOURCE" in
    env) printf '%s' "${KAFKA_SERVICE_PASSWORD:?KAFKA_SERVICE_PASSWORD is required}" ;;
    dir) cat "${KAFKA_PASSWORD_DIR:?KAFKA_PASSWORD_DIR is required}/$1" ;;
    *) echo "Unknown KAFKA_PASSWORD_SOURCE: $SOURCE" >&2; return 1 ;;
  esac
}

echo "Waiting for Kafka at ${KAFKA_BOOTSTRAP}"
for _ in $(seq 1 60); do
  if "$BIN/kafka-topics.sh" --bootstrap-server "$KAFKA_BOOTSTRAP" --command-config "$ADMIN_CONFIG" --list >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

while read -r topic partitions retention; do
  "$BIN/kafka-topics.sh" --bootstrap-server "$KAFKA_BOOTSTRAP" --command-config "$ADMIN_CONFIG" \
    --create --if-not-exists --topic "$topic" --partitions "$partitions" --replication-factor 1 \
    --config "retention.ms=${retention}" >/dev/null
  "$BIN/kafka-configs.sh" --bootstrap-server "$KAFKA_BOOTSTRAP" --command-config "$ADMIN_CONFIG" \
    --alter --entity-type topics --entity-name "$topic" --add-config "retention.ms=${retention}" >/dev/null
  echo "topic   ${topic}"
done < <(records "$DEFS/topics.conf")

while read -r user; do
  password="$(password_of "$user")"
  "$BIN/kafka-configs.sh" --bootstrap-server "$KAFKA_BOOTSTRAP" --command-config "$ADMIN_CONFIG" \
    --alter --entity-type users --entity-name "$user" \
    --add-config "SCRAM-SHA-512=[iterations=8192,password=${password}]" >/dev/null
  echo "user    ${user}"
done < <(records "$DEFS/users.conf")

while read -r user operation type name pattern; do
  case "$type" in
    topic) resource=(--topic "$name") ;;
    group) resource=(--group "$name") ;;
    cluster) resource=(--cluster) ;;
    *) echo "Unknown resource type '$type' in acls.conf" >&2; exit 1 ;;
  esac
  "$BIN/kafka-acls.sh" --bootstrap-server "$KAFKA_BOOTSTRAP" --command-config "$ADMIN_CONFIG" \
    --add --allow-principal "User:${user}" --operation "$operation" \
    "${resource[@]}" --resource-pattern-type "$pattern" >/dev/null
  echo "acl     ${user} ${operation} ${type}:${name} (${pattern})"
done < <(records "$DEFS/acls.conf")

echo "Kafka provisioning complete"
