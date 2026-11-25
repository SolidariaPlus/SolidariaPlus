# modules/aurora

## Overview

Amazon Aurora PostgreSQL cluster of one account (KAN-30; KAN-2 §6.5, ADR-054, ADR-079).

## Interface

Inputs: `name`, `environment`, `session_on`, `vpc_id`, `subnet_ids`, `client_security_group_ids` (nodes, bastion), `services` (service => database), `engine_version`, `instance_class`, `reader_count`, `backup_retention_days`. Outputs: endpoints, `cluster_identifier`, `master_secret_name`, `service_secret_names`.

## Invariants

- One database and one user per service; credentials in Secrets Manager `/solidaria/<env>/<service>/db`, with the same JSON shape as LocalStack.
- Port 5432 admits only the nodes and the bastion.
- Automated backups with point-in-time recovery (7 days); deletion protection and a final snapshot in Production.
- Session mode stops the cluster; a session never deletes it.

## Side Effects

- Stops and starts the cluster through the RDS API from the runner (`terraform_data`).

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
