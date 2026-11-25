# modules/ecr

## Overview

Amazon ECR repositories of the services of one account (KAN-29; KAN-11 §6.5; KAN-2 ADR-119).

## Interface

Inputs: `repositories` (set of service names), `pull_account_ids` (QA only: the matching Production account), `keep_images`. Output: `repository_urls`.

## Invariants

- Immutable tags, scan on push; lifecycle: untagged images expire after one day, at most `keep_images` `sha-*` images are kept.
- The repository policy grants pull only to the listed accounts, for the promotion by digest.

## Side Effects

- Storage only (no hourly price).

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
