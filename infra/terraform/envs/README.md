# infra/terraform/envs

## Overview

Root modules of the cloud environments (KAN-29 to KAN-33; KAN-11 §5.3, §6.8). Each account slot has
two roots:

| Root                    | Contents                                                                                                                    | Applied by                                                                 |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `<env>/<slot>`          | AWS resources: network, EKS, ECR, Aurora, Valkey, bastion, alerting, HTTP API and, in A1, CA, broker, directory, CloudFront | `infra.yml`, `session.yml`                                                 |
| `<env>/<slot>-platform` | Kubernetes side: add-ons, observability, db-provisioner                                                                     | `session.yml` (start applies, stop destroys), `infra.yml` during a session |

Only slot A1 exists in this increment (KAN-21). Slots A2 to A5 reuse the same modules when their
services start.

## Interface

- `terraform.tfvars` holds every value of the environment. Secrets are never stored there. The alert
  e-mail comes from `TF_VAR_alert_email`, and the Production account allowed to pull QA images comes
  from `infra/accounts.yml`.
- `session` (`on`/`off`, default `off`) turns every resource with an hourly price on or off (ADR-080).
- The backend is partial: `.github/scripts/tf.sh` passes the bucket of the account
  (`solidaria-tfstate-<env>-<slot>-<account>`), the key `envs/<env>/<slot>[-platform]/terraform.tfstate`
  and the region.

```bash
.github/scripts/tf.sh qa A1 infra plan -var session=off     # on the runner of QA-A1
.github/scripts/tf.sh qa A1 platform apply                  # during a session
```

## Invariants

- **Principle E-03:** `envs/qa/<root>` and `envs/prod/<root>` contain the same `.tf` files and differ
  only in `terraform.tfvars`. The job `static` of `infra.yml` fails otherwise.
- Applied only by the runner of the account (`[self-hosted, <env>-<slot>]`), never from a laptop after
  the bootstrap. Manual changes in the console are forbidden (P-07).
- Terraform owns the clusters, node groups, add-ons and repositories. The pipelines own the Helm
  releases of the services.

## Side Effects

- `session=on` creates billable resources (QA-A1: about USD 0.4 per session hour; Production about
  USD 0.5). `session=off` leaves only resources without an hourly price and stopped instances.

## Verification

- `terraform validate`, `tflint` and `trivy config` (`ci.yml`, `infra.yml`).
- In QA-A1: `session.yml` start and then stop both succeed, and the last step of the stop
  (`.github/scripts/verify-session-off.sh`) finds no resource with an hourly price (KAN-21 AC 5,
  KAN-29 "Done when").
