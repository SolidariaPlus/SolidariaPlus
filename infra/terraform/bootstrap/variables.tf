variable "environment" {
  description = "Environment of the account: qa or prod."
  type        = string

  validation {
    condition     = contains(["qa", "prod"], var.environment)
    error_message = "environment must be qa or prod."
  }
}

variable "slot" {
  description = "Account slot of infra/accounts.yml: A1 to A5."
  type        = string

  validation {
    condition     = can(regex("^A[1-5]$", var.slot))
    error_message = "slot must be one of A1 to A5."
  }
}

variable "region" {
  description = "AWS region of the environment: us-west-2 for qa, us-east-1 for prod (KAN-2 section 6.2)."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block of the VPC of the account (infra/accounts.yml, vpc_cidr)."
  type        = string
}

variable "github_repository" {
  description = "Repository that the runner registers with, as owner/name."
  type        = string
  default     = "SolidariaPlus/SolidariaPlus"
}

variable "runner_registration_token" {
  description = "Short-lived registration token from GitHub (Settings > Actions > Runners > New self-hosted runner). Needed only on the apply that creates or replaces the runner; never stored in the repository or in GitHub."
  type        = string
  default     = null
  sensitive   = true
}

variable "runner_instance_type" {
  description = "Instance type of the runner. It builds the service images, so it needs at least 2 GiB."
  type        = string
  default     = "t3.small"
}

variable "runner_volume_size" {
  description = "Root volume of the runner in GiB (Docker build cache and images)."
  type        = number
  default     = 40
}

variable "runner_version" {
  description = "Version of the GitHub Actions runner (https://github.com/actions/runner/releases)."
  type        = string
  default     = "2.329.0"
}

variable "tool_versions" {
  description = "Versions of the tools installed on the runner; keep them equal to .tool-versions."
  type = object({
    terraform = string
    helm      = string
    kubectl   = string
  })
  default = {
    terraform = "1.17.2"
    helm      = "4.3.2"
    kubectl   = "1.37.2"
  }
}

variable "budget_limit_usd" {
  description = "Credit of the Learner Lab account in USD; the budget alarms fire at 50 % and 80 % of it (KAN-2 OI-11)."
  type        = number
  default     = 50
}

variable "budget_email" {
  description = "E-mail that receives the budget alarms. Leave empty to skip the budget."
  type        = string
  default     = ""
}
