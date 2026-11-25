# infra

## Overview

Infrastructure of Solidaria+ as code. Nothing in QA or Production is created by hand.

| Path                  | Contents                                                                                | Task             |
| --------------------- | --------------------------------------------------------------------------------------- | ---------------- |
| `accounts.yml`        | Account slots A1 to A5 of `qa` and `prod`, apply order, runner labels                   | KAN-28           |
| `local/`              | Configuration of the local stack (`compose.yaml`)                                       | KAN-26           |
| `messaging/`          | Kafka and RabbitMQ definitions and scripts, shared by localhost and the broker instance | KAN-26, KAN-31   |
| `terraform/bootstrap` | Once per account: state bucket, VPC, self-hosted runner, budget alarms                  | KAN-28           |
| `terraform/modules`   | Modules shared by QA and Production                                                     | KAN-29 to KAN-33 |
| `terraform/envs`      | Root modules per environment and slot; only `terraform.tfvars` differs                  | KAN-29 to KAN-33 |
| `ad/`                 | Bootstrap of the staff directory (PowerShell, synthetic accounts)                       | KAN-31           |
| `learner-lab-tests/`  | Learner Lab follow-up checks                                                            | KAN-35           |

**Brokers.** Kafka and RabbitMQ run self-hosted, in containers, on **one EC2 instance
in A1** of each environment (`terraform/modules/messaging`). The Learner Lab denies Amazon MSK and
Amazon MQ, so this replaces them. The instance is stopped at session-stop, not deleted or drained.

## Interface: runbook for QA-A1 and PROD-A1

The steps below are run by the developer with the Learner Lab credentials of each account. After
step 2, every change goes through the workflows.

| #   | Step                | Command or action                                                                                                                                                                                                           | Proves                  |
| --- | ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------- |
| 0   | Repository settings | Variable `ALERT_EMAIL` (Settings > Variables). GitHub environments `qa` (branch `qa`) and `production` (branch `main`). Require approval for outside collaborators. Rulesets for `dev`, `qa`, `main` (KAN-22)               | AC 3                    |
| 1   | Bootstrap QA-A1     | Lab session of QA-A1 on, then `infra/terraform/bootstrap/bootstrap.sh qa A1 -var runner_registration_token=<token> -var budget_email=<e-mail>`                                                                              | AC 4, KAN-28            |
| 2   | Bootstrap PROD-A1   | The same with `prod A1` in the PROD-A1 account. Write both account ids in `accounts.yml` (`account_id`)                                                                                                                     | AC 4, KAN-28            |
| 3   | First apply         | Merge to `qa` (and later `main`): `infra.yml` applies `envs/<env>/a1` with `session=off`. Its first job prints `aws sts get-caller-identity` on the runner                                                                  | KAN-28 "Done when"      |
| 4   | Start a session     | Actions > session > `qa`, `start`: about 15 minutes (EKS control plane, ADR-123 order, add-ons, broker)                                                                                                                     | AC 5, KAN-29, KAN-31    |
| 5   | Deploy to QA        | Push to `qa` (merge of `dev`): `deploy-qa.yml` builds `sample-service` once, pushes `sha-<commit>` to ECR, deploys it by digest and runs `curl https://<cloudfront>/auth/ping`                                              | AC 6, KAN-32, KAN-34    |
| 6   | Observe             | Grafana by port forwarding from the bastion (`scripts/bastion.sh on qa A1`), CloudWatch Logs `/solidaria/solidaria-qa-a1/apps`, an alert e-mail with `[QA]` (scale `sample-service` to 0 for 2 minutes)                     | AC 8, KAN-33            |
| 7   | Promote             | Pull request `qa` → `main`, approved by the professor: `infra.yml` applies Production and `promote-prod.yml` copies the same digest with `skopeo copy --preserve-digests` and deploys it (start a Production session first) | AC 6, AC 7, KAN-34 (M1) |
| 8   | Learner Lab checks  | `infra/learner-lab-tests` in QA-A1 (and PROD-A1 where the region matters), then `99-cleanup.sh`                                                                                                                             | KAN-35                  |
| 9   | Stop                | Actions > session > `qa` (and `production`), `stop`. The last step verifies that nothing with an hourly price remains. Then end the lab sessions                                                                            | AC 5                    |

**The stop is never skipped.** Ending a lab session only suspends EC2 instances; NAT gateways, EKS
and Aurora keep charging (KAN-11 §5.5). The budget alarms at 50 % and 80 % are the safety net.

## Invariants

- No credential in GitHub or in the repository. Runners use the LabRole credentials of their instance
  profile; passwords are generated by Terraform into AWS Secrets Manager or Parameter Store
  (`/solidaria/<env>/...`).
- Every resource carries the tags `environment` (`qa` or `production`), `slot` and `managed-by`
  (principle E-07).
- QA and Production differ only in `terraform/envs/*/*/terraform.tfvars` and the Helm values
  (principle E-03, AC 7).
- Versions of PostgreSQL, Valkey, Kafka and RabbitMQ are the same in `compose.yaml`,
  `terraform/modules` and the Helm values. Change them together.

## Side Effects

- The bootstrap and `session=on` create billable resources in the Learner Lab accounts (USD 50 credit
  each).

## Verification

- Offline (CI): `ci.yml` (`compose`, `helm`, `terraform`, `trivy-config` jobs) and `infra.yml` (`static`
  job). Locally: `docker compose --profile '*' config`, `terraform validate`, `tflint`, `helm lint`,
  `kubeconform`, `shellcheck`.
- In AWS: the runbook above; each row names the acceptance criterion of KAN-21 or the subtask whose
  "Done when" it proves.
