# modules/valkey

## Overview

Amazon ElastiCache (Valkey) of one account (KAN-30; KAN-2 §6.5, ADR-055, ADR-079).

## Interface

Inputs: `name`, `environment`, `session_on`, `vpc_id`, `subnet_ids`, `client_security_group_ids`, `services` (service => key prefix), `engine_version`, `node_type`, `replicas`. Output: `primary_endpoint`.

## Invariants

- One ACL user per service, limited to `<prefix>:*`, as `infra/local/valkey/start-valkey.sh` does on localhost; the mandatory `default` user is disabled.
- TLS in transit and encryption at rest.
- The replication group exists only during sessions; the endpoint of the session is published in `/solidaria/<env>/<service>/cache/host`.

## Side Effects

- During a session: one cache.t4g.micro node (about USD 0.016 per hour).

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
