# infra/messaging

## Overview

Definitions and scripts of the Kafka and RabbitMQ brokers. The **same files** configure the local
containers (`compose.yaml`, KAN-26) and the broker instance of account A1 in QA and Production
(`infra/terraform/modules/messaging`, KAN-31). That instance is one EC2 t3.medium that runs both
brokers in containers (KAN-2 ADR-122, which replaces Amazon MSK and Amazon MQ because the Learner Lab
denies them).

| Path                                | Contents                                                             |
| ----------------------------------- | -------------------------------------------------------------------- |
| `kafka/topics.conf`                 | Topics: `<topic> <partitions> <retention-ms>`                        |
| `kafka/users.conf`                  | SCRAM-SHA-512 users, one per service                                 |
| `kafka/acls.conf`                   | ACLs: `<user> <operation> <resource-type> <resource-name> <pattern>` |
| `kafka/start-kafka.sh`              | Starts one KRaft node (broker and controller) with SCRAM and ACLs    |
| `rabbitmq/definitions.json`         | Vhost `solidaria`, exchanges, work, wait and dead-letter queues      |
| `rabbitmq/conf.d/20-solidaria.conf` | OAuth 2.0 backend (MS-17 tokens) followed by the internal backend    |
| `rabbitmq/enabled_plugins`          | Management, OAuth 2.0 backend, Prometheus                            |
| `scripts/provision-kafka.sh`        | Creates topics, users and ACLs from `kafka/*.conf`                   |
| `scripts/provision-rabbitmq.sh`     | Imports `rabbitmq/definitions.json` through the management API       |

## Interface

- **Kafka listeners** (`start-kafka.sh`):
  - `INTERNAL` on port 9092: `kafka:9092` on localhost, the private address in A1.
  - `EXTERNAL` on port 9094: `localhost:9094` on localhost, the Elastic IP in A1.
  - `REPLICATION` (port 9091) and `CONTROLLER` (port 9093): loopback only.
  - Clients use `SASL_PLAINTEXT` on localhost and `SASL_SSL` in AWS, always with SCRAM-SHA-512.
- **Kafka naming:** `<producer>.events` for domain events (KAN-2 §4.4). The retry and dead-letter
  topics of a consumer are `<topic>.<consumer>.retry` and `<topic>.<consumer>.dlt`.
- **RabbitMQ naming:** exchanges `solidaria.commands`, `solidaria.jobs`, `solidaria.wait` and
  `solidaria.dlx`. Each work queue `<service>.<work>` has a `.wait` queue for retries and a `.dlq` queue.
- **Passwords:** never in these files. `provision-kafka.sh` reads them from the environment on localhost
  (`KAFKA_PASSWORD_SOURCE=env`) and from files that the broker host fetches from AWS Secrets Manager in
  the cloud (`KAFKA_PASSWORD_SOURCE=dir`). Generated passwords are alphanumeric, so they need no escaping.

## Invariants

- Only add records. The scripts never delete a topic, user, ACL or queue, so removing a line does not
  remove the resource; remove it by hand with the Kafka or RabbitMQ CLI.
- `auto.create.topics.enable=false` and `allow.everyone.if.no.acl.found=false`: a topic or permission
  that is not declared here does not exist.
- The ACLs must match the event catalog of KAN-2 §4.4.1 and the AsyncAPI contracts in `contracts/asyncapi`.
- Kafka has one broker without replication (accepted risk OI-15). Its volume keeps topics, offsets
  and queues across `session-stop`, because the instance is stopped, not deleted.

## Side Effects

- `start-kafka.sh` formats the data directory the first time and bootstraps the super user `admin`.
- The provisioning scripts change broker state: topics, users, ACLs and RabbitMQ definitions.

## Verification

- Localhost: `docker compose --profile identity up -d --wait`. The `kafka-provisioner` and
  `rabbitmq-provisioner` containers become healthy only after their script has finished without errors.
- AWS: `infra/learner-lab-tests/50-broker-checks.sh` (KAN-35) produces and consumes with SCRAM over
  `SASL_SSL` and checks that an ACL denies a foreign topic.
