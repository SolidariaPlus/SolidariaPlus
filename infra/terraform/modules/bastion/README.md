# modules/bastion

## Overview

Bastion of one account (KAN-30; KAN-11 §5.6; KAN-2 ADR-096, ADR-120): t3.nano in an Auto Scaling group with desired capacity 0, reached only with Session Manager.

## Interface

Inputs: `name`, `vpc_id`, `subnet_ids`, `instance_type`, `kubectl_version`, `log_retention_days`. Outputs: `security_group_id`, `autoscaling_group_name`. Operated with `scripts/bastion.sh on|off|status <env> <slot>`.

## Invariants

- No inbound rule, no SSH key, no public address; Session Manager logs every session to CloudWatch Logs.
- Terraform never changes the desired capacity after creation (`ignore_changes`); session-stop sets it back to 0.

## Side Effects

- Sets the account-wide Session Manager preferences (`SSM-SessionManagerRunShell`).

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
