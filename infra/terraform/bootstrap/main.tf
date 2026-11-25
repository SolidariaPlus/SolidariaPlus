# Bootstrap of one Learner Lab account (KAN-28, KAN-11 sections 5.4 and 6.8; KAN-2 ADR-059,
# ADR-076, ADR-077, ADR-118). Applied once per account by bootstrap.sh, from the workstation or
# AWS CloudShell with the credentials of the lab session. It creates:
#   - the Terraform state bucket of the account (state.tf);
#   - the permanent network of the account: VPC, internet gateway and public subnets (modules/vpc-base);
#   - the self-hosted GitHub Actions runner (runner.tf);
#   - the budget alarms at 50 % and 80 % of the credit (budget.tf).
# Every later change to the account goes through infra.yml and session.yml on that runner.

data "aws_caller_identity" "current" {}

locals {
  environment_tag = var.environment == "prod" ? "production" : "qa"
  slot_lower      = lower(var.slot)
  name            = "solidaria-${var.environment}-${local.slot_lower}"
  runner_label    = "${var.environment}-${var.slot}"
  account_id      = data.aws_caller_identity.current.account_id
}

module "vpc_base" {
  source = "../modules/vpc-base"

  name       = local.name
  cidr_block = var.vpc_cidr
}
