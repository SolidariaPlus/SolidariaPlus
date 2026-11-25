# modules/platform-addons

## Overview

Kubernetes side of one account (KAN-29, KAN-33): AWS Load Balancer Controller, Cluster Autoscaler, KEDA, kube-prometheus-stack (A1: central Prometheus, Alertmanager to SNS, Grafana), Fluent Bit to CloudWatch Logs, optional OpenTelemetry collector to X-Ray, the dashboards and rules of `deploy/observability`, and the `db-provisioner` Job.

## Interface

Applied by `envs/<env>/<slot>-platform`. Inputs: `name`, `environment`, `region`, `vpc_id`, `apps_log_group`, `alerts_topic_arn`, `observability_dir`, `central_monitoring`, `enable_xray`, `chart_versions`, `database_*`, `db_provisioner_script`, `postgres_image`.

## Invariants

- Chart versions are pinned in `chart_versions`; update them in one pull request after reading the release notes.
- The `db-provisioner` Job runs `infra/local/postgres/provision.sh`, the same script as on localhost, inside the cluster, because Aurora admits only the nodes and the bastion.
- The Grafana admin password is generated and kept in Parameter Store; Grafana has no Ingress and is reached by port forwarding through the bastion.

## Side Effects

- Installs Helm releases and Kubernetes objects. The Ingress of each service makes the controller create the ALB of the slot.

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
