# infra/local

## Overview

Configuration of the local development environment (KAN-11 §5.2, KAN-26). `compose.yaml` at the
repository root mounts these files into the containers. Everything runs on localhost and needs no AWS
account or credential (principle E-01). Each local component stands for a cloud component:

| Local container | Stands for (QA and Production)                         | Files here                     |
| --------------- | ------------------------------------------------------ | ------------------------------ |
| `postgres`      | Amazon Aurora PostgreSQL (same major version)          | `postgres/provision.sh`        |
| `valkey`        | Amazon ElastiCache (Valkey), one ACL user per service  | `valkey/start-valkey.sh`       |
| `localstack`    | S3, DynamoDB, Lambda, Secrets Manager, Parameter Store | `localstack/init.sh`           |
| `rabbitmq`      | RabbitMQ on the broker instance of A1 (ADR-122)        | `rabbitmq/30-local-oauth.conf` |
| `prometheus`    | kube-prometheus-stack                                  | `prometheus/prometheus.yml`    |
| `grafana`       | Central Grafana of A1                                  | `grafana/provisioning/`        |

Kafka and RabbitMQ definitions are shared with the cloud broker and live in [`../messaging`](../messaging).
Dashboards and alert rules are shared with QA and Production and live in
[`../../deploy/observability`](../../deploy/observability).

## Interface

```bash
cp .env.example .env                                   # optional: override local passwords
docker compose --profile identity up -d --wait         # dependencies of MS-17
docker compose --profile observability up -d --wait    # Prometheus and Grafana only
docker compose ps                                      # every container must be "healthy"
docker compose --profile identity down -v              # stop and drop the data volumes
```

| Endpoint on localhost | Use                                                                    |
| --------------------- | ---------------------------------------------------------------------- |
| `localhost:5432`      | PostgreSQL. Admin `postgres`; one database and one role per service    |
| `localhost:6379`      | Valkey. Users named after the service, keys under `<prefix>:`          |
| `localhost:9094`      | Kafka, SASL_PLAINTEXT with SCRAM-SHA-512 (containers use `kafka:9092`) |
| `localhost:5672`      | RabbitMQ AMQP, vhost `solidaria`; management UI on `localhost:15672`   |
| `localhost:4566`      | LocalStack (`aws --endpoint-url http://localhost:4566 ...`)            |
| `localhost:9090`      | Prometheus                                                             |
| `localhost:3000`      | Grafana (admin user `admin`)                                           |

`services.conf` lists the services that own a database, a cache user and AWS settings:
`<service> <database> <key-prefix>`. Add a line when a service starts using these stores.

## Invariants

- Every container has a health check and every `depends_on` waits for `service_healthy`. CI checks both,
  together with the absence of the obsolete `version` key (`ci.yml`, job `compose`).
- The one-shot provisioners (`postgres-provisioner`, `kafka-provisioner`, `rabbitmq-provisioner`) stay
  up and idle after they finish, so that `up --wait` and the health-check rule apply to them too.
- Local RabbitMQ authenticates with the internal backend only (`rabbitmq/30-local-oauth.conf`):
  RabbitMQ 4.1+ rejects a non-https `auth_oauth2.jwks_url`, and MS-17 serves plain HTTP on localhost.
  The OAuth 2.0 backend is enabled once MS-17 issues tokens (KAN-18) behind an https endpoint.
- Provisioning is idempotent: it runs at every `up` and only creates what is missing.
- Secret and parameter names follow the cloud pattern `/solidaria/<env>/<service>/<kind>`, with
  `<env> = local`.
- Passwords in `.env.example` and in the `compose.yaml` defaults are for localhost only. Real
  credentials exist only in AWS Secrets Manager of each account.
- Image versions match the cloud components. A version change updates `compose.yaml`, the Terraform
  modules and the Helm values in the same pull request (KAN-11 §5.2).

## Side Effects

- Docker named volumes `solidaria-plus_*` keep the data between runs.
- LocalStack mounts the Docker socket to run Lambda functions in containers.
- Ports 3000, 4566, 5432, 5672, 6379, 9090, 9094 and 15672 are bound on the workstation.

## Verification

```bash
docker compose --profile '*' config --quiet
docker compose --profile identity up -d --wait
docker compose exec postgres psql -U postgres -c '\l'
docker compose exec kafka-provisioner bash -c 'printf "security.protocol=SASL_PLAINTEXT\nsasl.mechanism=SCRAM-SHA-512\nsasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username=\"admin\" password=\"$KAFKA_ADMIN_PASSWORD\";\n" > /tmp/a.properties && /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --command-config /tmp/a.properties --list'
docker compose exec rabbitmq rabbitmqctl list_queues -p solidaria
docker compose exec localstack awslocal secretsmanager list-secrets --query 'SecretList[].Name'
```
