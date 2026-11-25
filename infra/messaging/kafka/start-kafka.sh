#!/bin/bash
# Starts a single Apache Kafka node in KRaft mode (combined broker and controller) with
# SCRAM-SHA-512 authentication and ACLs. The same script runs on localhost (compose.yaml)
# and on the broker instance of A1 (KAN-2 ADR-122); only the environment differs.
#
# Listeners:
#   REPLICATION  127.0.0.1:9091  PLAINTEXT, loopback only: inter-broker traffic and health check
#   CONTROLLER   127.0.0.1:9093  PLAINTEXT, loopback only: KRaft controller
#   INTERNAL     0.0.0.0:9092    SCRAM: same network (containers on localhost, VPC of A1 in AWS)
#   EXTERNAL     0.0.0.0:9094    SCRAM: host on localhost, Elastic IP in AWS
#
# Environment:
#   KAFKA_ADMIN_PASSWORD          password of the super user "admin" (required)
#   KAFKA_INTERNAL_ADVERTISED     host:port advertised on INTERNAL (required)
#   KAFKA_EXTERNAL_ADVERTISED     host:port advertised on EXTERNAL (required)
#   KAFKA_CLIENT_PROTOCOL         SASL_PLAINTEXT (localhost) or SASL_SSL (AWS); default SASL_PLAINTEXT
#   KAFKA_KEYSTORE_FILE           PKCS12 keystore with the server certificate chain and key, and
#   KAFKA_KEYSTORE_PASSWORD       its password; both required when KAFKA_CLIENT_PROTOCOL=SASL_SSL
#                                 (the broker host builds the keystore, see broker-start.sh)
#   KAFKA_LOG_RETENTION_HOURS     default 168 (7 days)
set -euo pipefail

: "${KAFKA_ADMIN_PASSWORD:?KAFKA_ADMIN_PASSWORD is required}"
: "${KAFKA_INTERNAL_ADVERTISED:?KAFKA_INTERNAL_ADVERTISED is required}"
: "${KAFKA_EXTERNAL_ADVERTISED:?KAFKA_EXTERNAL_ADVERTISED is required}"
PROTOCOL="${KAFKA_CLIENT_PROTOCOL:-SASL_PLAINTEXT}"
RETENTION_HOURS="${KAFKA_LOG_RETENTION_HOURS:-168}"
KAFKA_HOME="${KAFKA_HOME:-/opt/kafka}"
DATA_DIR="${KAFKA_DATA_DIR:-/var/lib/kafka/data}"
CONFIG_FILE="${KAFKA_CONFIG_FILE:-/tmp/server.properties}"

scram_jaas='org.apache.kafka.common.security.scram.ScramLoginModule required;'

cat > "$CONFIG_FILE" <<EOF
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@127.0.0.1:9093
listeners=REPLICATION://127.0.0.1:9091,CONTROLLER://127.0.0.1:9093,INTERNAL://0.0.0.0:9092,EXTERNAL://0.0.0.0:9094
advertised.listeners=REPLICATION://127.0.0.1:9091,INTERNAL://${KAFKA_INTERNAL_ADVERTISED},EXTERNAL://${KAFKA_EXTERNAL_ADVERTISED}
listener.security.protocol.map=REPLICATION:PLAINTEXT,CONTROLLER:PLAINTEXT,INTERNAL:${PROTOCOL},EXTERNAL:${PROTOCOL}
inter.broker.listener.name=REPLICATION
controller.listener.names=CONTROLLER
sasl.enabled.mechanisms=SCRAM-SHA-512
listener.name.internal.sasl.enabled.mechanisms=SCRAM-SHA-512
listener.name.external.sasl.enabled.mechanisms=SCRAM-SHA-512
listener.name.internal.scram-sha-512.sasl.jaas.config=${scram_jaas}
listener.name.external.scram-sha-512.sasl.jaas.config=${scram_jaas}
authorizer.class.name=org.apache.kafka.metadata.authorizer.StandardAuthorizer
super.users=User:admin;User:ANONYMOUS
allow.everyone.if.no.acl.found=false
auto.create.topics.enable=false
log.dirs=${DATA_DIR}
log.retention.hours=${RETENTION_HOURS}
num.partitions=3
default.replication.factor=1
min.insync.replicas=1
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1
group.initial.rebalance.delay.ms=0
EOF

if [ "$PROTOCOL" = "SASL_SSL" ]; then
  : "${KAFKA_KEYSTORE_FILE:?KAFKA_KEYSTORE_FILE is required with SASL_SSL}"
  : "${KAFKA_KEYSTORE_PASSWORD:?KAFKA_KEYSTORE_PASSWORD is required with SASL_SSL}"
  cat >> "$CONFIG_FILE" <<EOF
ssl.keystore.type=PKCS12
ssl.keystore.location=${KAFKA_KEYSTORE_FILE}
ssl.keystore.password=${KAFKA_KEYSTORE_PASSWORD}
ssl.enabled.protocols=TLSv1.3,TLSv1.2
ssl.client.auth=none
EOF
fi

# Format the storage only once; the SCRAM credential of the super user is bootstrapped here,
# because a KRaft cluster with ACLs has no other way to create its first user.
if [ ! -f "${DATA_DIR}/meta.properties" ]; then
  cluster_id="$("${KAFKA_HOME}/bin/kafka-storage.sh" random-uuid)"
  "${KAFKA_HOME}/bin/kafka-storage.sh" format \
    --config "$CONFIG_FILE" \
    --cluster-id "$cluster_id" \
    --add-scram "SCRAM-SHA-512=[name=admin,password=${KAFKA_ADMIN_PASSWORD}]"
fi

exec "${KAFKA_HOME}/bin/kafka-server-start.sh" "$CONFIG_FILE"
