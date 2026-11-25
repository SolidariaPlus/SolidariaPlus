# modules/dynamodb-table

## Overview

DynamoDB table owned by one service (KAN-30; KAN-2 §6.5): on-demand, encrypted, point-in-time recovery on by default, optional stream for the outbox relay.

## Interface

Inputs: `name`, `hash_key`, `range_key`, `point_in_time_recovery`, `stream_enabled`, `ttl_attribute`. Outputs: `arn`, `stream_arn`.

## Invariants

- Point-in-time recovery stays on outside throwaway tests.

## Side Effects

- None beyond the table.

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
