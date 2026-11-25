# modules/alerting

## Overview

Alerting and log destinations of one account (KAN-33; KAN-2 §6.7, ADR-107, ADR-121): SNS topic with an e-mail subscription, application and EKS log groups with retention, and CloudWatch alarms for what Prometheus cannot see.

## Interface

Inputs: `name`, `environment`, `slot`, `alert_email`, `log_retention_days`, `broker_instance_id`, `aurora_cluster_identifier`. Outputs: `topic_arn`, `apps_log_group`. Parameter: `/solidaria/<env>/<slot>/alerts/topic-arn`.

## Invariants

- Every alarm name starts with `[QA]` or `[PRODUCTION]` (principle E-07); Alertmanager adds the same prefix to its subjects.
- The e-mail address is passed with `TF_VAR_alert_email`, never committed.

## Side Effects

- The subscription sends a confirmation e-mail that must be accepted once.

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
