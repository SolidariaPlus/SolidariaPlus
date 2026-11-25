output "account_id" {
  description = "Account that was bootstrapped."
  value       = local.account_id
}

output "state_bucket" {
  description = "Terraform state bucket of the account; the environment roots use it as their backend."
  value       = aws_s3_bucket.state.bucket
}

output "runner_label" {
  description = "Label of the self-hosted runner (runs-on: [self-hosted, <label>])."
  value       = local.runner_label
}

output "runner_instance_id" {
  description = "Instance of the runner."
  value       = aws_instance.runner.id
}

output "vpc_id" {
  description = "VPC of the account, found by the environment roots through its Name tag."
  value       = module.vpc_base.vpc_id
}
