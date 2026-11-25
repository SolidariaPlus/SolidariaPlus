# modules/messaging

## Overview

Broker instance of A1: Apache Kafka (KRaft) and RabbitMQ in containers on one EC2 t3.medium (KAN-31; KAN-2 ADR-122, which replaces Amazon MSK and Amazon MQ, denied by the Learner Lab).

## Interface

Inputs: `name`, `environment`, `session_on`, `vpc_id`, `vpc_cidr`, `subnet_id` (public), `client_security_group_ids`, `public_client_cidrs`, `instance_type`, `volume_size`, `kafka_image`, `rabbitmq_image`, `rabbitmq_jwks_url`, `messaging_dir`, and the CA. Outputs: `instance_id`, `security_group_id`, `private_ip`, `public_ip`, `kafka_user_secret_arns`, `parameter_prefix`.

Parameters for clients under `/solidaria/<env>/broker/`: `private-address`, `public-address`, `kafka-bootstrap-internal`, `kafka-bootstrap-external`, `amqps-endpoint-internal`, `amqps-endpoint-external`, `certificate`, `status`.

## Invariants

- Same images, start script and definitions as `compose.yaml` (`infra/messaging`, uploaded to an S3 bucket and synced at every boot).
- Kafka: SASL_SSL with one SCRAM user per service and ACLs; 9092 for the VPC, 9094 on the Elastic IP. RabbitMQ: AMQPS 5671 only; management and metrics only from the VPC.
- session=off stops the instance and releases the Elastic IP; the root volume keeps topics, offsets and queues (`delete_on_termination = false`).
- A new certificate is issued for the private address and the new Elastic IP at every session-start.

## Side Effects

- During a session: instance and public IPv4 (about USD 0.05 per hour). Volume: about USD 1.6 per month.

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
