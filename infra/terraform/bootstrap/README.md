# infra/terraform/bootstrap

## Overview

One-time bootstrap of each Learner Lab account (KAN-28; KAN-11 §5.4 and §6.8; KAN-2 ADR-059, ADR-076,
ADR-077, ADR-118). For one account (environment and slot) it creates:

| Resource                                                                                    | File        | Hourly price                   |
| ------------------------------------------------------------------------------------------- | ----------- | ------------------------------ |
| Terraform state bucket (versioned, encrypted, TLS only, native locking with `use_lockfile`) | `state.tf`  | No                             |
| VPC, internet gateway and two public subnets (`modules/vpc-base`)                           | `main.tf`   | No                             |
| Self-hosted GitHub Actions runner on EC2 with the LabInstanceProfile, label `<env>-<slot>`  | `runner.tf` | Yes (t3.small, about USD 0.02) |
| AWS Budgets alarms at 50 % and 80 % of the USD 50 credit                                    | `budget.tf` | No                             |

The runner and the network stay up between sessions. The runner is the only EC2 instance that
`session-stop` leaves on, and the lab suspends it when the lab session ends. Everything else in the
account is created by `infra/terraform/envs/<env>/<slot>` through `infra.yml` and `session.yml`, which run
on this runner.

## Interface

Prerequisites: Terraform, the AWS CLI and the credentials of the lab session of the target account
(Learner Lab > AWS Details > AWS CLI). Use the workstation or AWS CloudShell. The credentials stay in
your shell; nothing is stored in the repository or in GitHub.

1. In GitHub, open **Settings > Actions > Runners > New self-hosted runner** and copy the registration
   token. It expires after one hour.
2. Run:

   ```bash
   infra/terraform/bootstrap/bootstrap.sh qa A1 \
     -var runner_registration_token=<token> \
     -var budget_email=<your e-mail>
   ```

   The script shows the account and asks for confirmation. If the bucket
   `solidaria-tfstate-<env>-<slot>-<account>` does not exist, the script creates it, initializes the
   backend in it and applies `accounts/<env>-<slot>.tfvars`.

3. Confirm the e-mail subscription of the budget.
4. Wait about 3 minutes and check that the runner `solidaria-<env>-<slot>` appears as **Idle** with the
   label `<env>-<slot>`.
5. Repeat for `prod A1`. The other slots (A2 to A5) use the same script and their own `.tfvars` when
   their services start.

Re-running `bootstrap.sh <env> <slot>` without a token updates the bucket settings and the budget, and
leaves the registered runner as it is. To replace the runner, run
`bootstrap.sh <env> <slot> -replace=aws_instance.runner -var runner_registration_token=<new token>`.

| Output         | Use                                                   |
| -------------- | ----------------------------------------------------- |
| `state_bucket` | Backend bucket of `infra/terraform/envs/<env>/<slot>` |
| `runner_label` | `runs-on: [self-hosted, <label>]` in the workflows    |
| `vpc_id`       | VPC that the environment roots find by its `Name` tag |

## Invariants

- No credential in GitHub. The runner uses the rotated LabRole credentials of its instance profile.
  The registration token lives in Parameter Store only until the instance has registered, and the
  instance then deletes it.
- The runner has no inbound rule, no SSH key and IMDSv2 only.
- Runners only take jobs from workflows triggered by a push to `qa` or `main`, or by `session.yml`.
  No workflow targets a self-hosted runner on `pull_request`, because the repository is public
  (KAN-11 §5.4). Also enable **Settings > Actions > General > Require approval for all outside
  collaborators**.
- The state bucket has `prevent_destroy`. Losing it means rebuilding the account (RNF-DISP-05).
- The bootstrap state lives in the same bucket under `bootstrap/terraform.tfstate`. The environment
  roots use `envs/<env>/<slot>/...` keys.

## Side Effects

- Creates billable resources in the account (the runner instance and its public IPv4 address).
- Registers a runner in the GitHub repository.
- Writes and later deletes `/solidaria/<env>/<slot>/runner/registration-token` in Parameter Store.

## Verification

- Offline: `terraform -chdir=infra/terraform/bootstrap init -backend=false && terraform -chdir=infra/terraform/bootstrap validate`
  (also run by `ci.yml`, job `terraform`).
- In the account: the runner shows as online, and the first job of `infra.yml` or `session.yml` on
  `[self-hosted, qa-A1]` prints the expected account with `aws sts get-caller-identity`
  (`.github/actions/assert-account`). This is the "Done when" of KAN-28.
- `aws s3api get-bucket-versioning --bucket <state_bucket>` returns `Enabled`.
