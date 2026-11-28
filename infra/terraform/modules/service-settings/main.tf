# Parameter Store entries and Secrets Manager placeholders of the services of one account
# (KAN-30). Names follow /solidaria/<env>/<service>/<key>, as LocalStack does on localhost.
# - parameters: plain settings known to Terraform (for example the broker bootstrap address).
# - secrets: entries whose value is filled in at deployment or by hand; Terraform creates them with
#   a placeholder and never overwrites the value afterwards. No value is ever written in Git.
# - initial_values: optional first value of some of those secrets (generated keys, JSON skeletons),
#   written once instead of the placeholder; later manual changes (rotation) are kept.

terraform {
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

variable "environment" {
  description = "Environment: qa or prod."
  type        = string
}

variable "parameters" {
  description = "Plain settings: \"<service>/<key>\" => value."
  type        = map(string)
  default     = {}
}

variable "secrets" {
  description = "Secrets filled in later: \"<service>/<key>\" => description."
  type        = map(string)
  default     = {}
}

variable "initial_values" {
  description = "Optional first value of a secret of var.secrets: \"<service>/<key>\" => value. Missing keys get \"PENDING\"."
  type        = map(string)
  default     = {}
  sensitive   = true
}

resource "aws_ssm_parameter" "this" {
  for_each = var.parameters

  name  = "/solidaria/${var.environment}/${each.key}"
  type  = "String"
  value = each.value
}

resource "aws_secretsmanager_secret" "this" {
  for_each = var.secrets

  name                    = "/solidaria/${var.environment}/${each.key}"
  description             = each.value
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "placeholder" {
  for_each = var.secrets

  secret_id     = aws_secretsmanager_secret.this[each.key].id
  secret_string = lookup(var.initial_values, each.key, "PENDING")

  lifecycle {
    ignore_changes = [secret_string]
  }
}

output "parameter_names" {
  description = "Created parameter names."
  value       = [for parameter in aws_ssm_parameter.this : parameter.name]
}

output "secret_names" {
  description = "Created secret names."
  value       = [for secret in aws_secretsmanager_secret.this : secret.name]
}
