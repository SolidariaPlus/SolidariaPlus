# deploy/observability

## Overview

Dashboards, alert rules and the monitoring stack values shared by every environment (KAN-11 §5.2
parity table, KAN-33). Localhost loads the dashboards and rules directly (`compose.yaml`). QA and
Production load the same files into the kube-prometheus-stack of the A1 cluster.

| Path          | Contents                                                     |
| ------------- | ------------------------------------------------------------ |
| `dashboards/` | Grafana dashboards (JSON), datasource uid `prometheus`       |
| `alerts/`     | Prometheus alert rules in plain rule-file format (`groups:`) |

## Interface

- A dashboard is a Grafana JSON model with a stable `uid` and datasource uid `prometheus`.
- Service metrics come from `/actuator/prometheus` and carry the label `service` (the Compose label on
  localhost, the Kubernetes Service name in the cloud).
- An alert rule file is a Prometheus rule file. Every alert has `severity` and a `summary`; the
  environment comes from the external label `environment`.

## Invariants

- One copy of each dashboard and rule, used by every environment; no environment-specific copy.
- No secret, account identifier or endpoint in these files.

## Side Effects

- None. These are static files.

## Verification

- Localhost: `docker compose --profile observability up -d --wait`, then open Grafana on
  `localhost:3000` (folder _Solidaria+_) and the rules on `localhost:9090/rules`.
- `promtool check rules deploy/observability/alerts/*.yaml`, for example with
  `docker run --rm -v "$PWD/deploy/observability/alerts:/r" --entrypoint promtool prom/prometheus:v3.6.0 check rules /r/service-alerts.yaml`.
