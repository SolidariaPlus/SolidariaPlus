# modules/service-settings

## Overview

Parameter Store entries and Secrets Manager placeholders of the services of one account (KAN-30).

## Interface

Inputs: `environment`, `parameters` (`"<service>/<key>" => value`), `secrets` (`"<service>/<key>" => description`). Outputs: the names created.

## Invariants

- Names follow `/solidaria/<env>/<service>/<key>`.
- Secret values are filled in at deployment or by hand: Terraform writes `PENDING` once and never overwrites them. No value is written in Git.

## Side Effects

- Creates parameters and secrets.

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
