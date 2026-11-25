terraform {
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 3.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.38, < 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Partial configuration (key envs/<env>/<slot>-platform/terraform.tfstate), see envs/<env>/a1.
  backend "s3" {
    use_lockfile = true
    encrypt      = true
  }
}

variable "environment" {
  description = "Environment: qa or prod."
  type        = string
}

variable "slot" {
  description = "Account slot."
  type        = string
}

variable "region" {
  description = "AWS region."
  type        = string
}

variable "database_services" {
  description = "Services with a database (same map as envs/<env>/<slot>): service => database name."
  type        = map(string)
  default     = {}
}

variable "enable_xray" {
  description = "Install the OpenTelemetry collector with the X-Ray exporter (OI-02)."
  type        = bool
  default     = false
}

locals {
  environment_tag = var.environment == "prod" ? "production" : "qa"
  name            = "solidaria-${var.environment}-${lower(var.slot)}"
  slot_dir        = "/solidaria/${var.environment}/${lower(var.slot)}"
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      project      = "solidaria-plus"
      environment  = local.environment_tag
      slot         = var.slot
      "managed-by" = "terraform"
    }
  }
}

data "aws_ssm_parameter" "eks" {
  for_each = toset(["endpoint", "ca", "ci-deployer-token"])

  name            = "${local.slot_dir}/eks/${each.key}"
  with_decryption = true
}

data "aws_ssm_parameter" "platform" {
  for_each = toset(["vpc-id", "apps-log-group"])

  name = "${local.slot_dir}/platform/${each.key}"
}

data "aws_ssm_parameter" "alerts_topic" {
  name = "${local.slot_dir}/alerts/topic-arn"
}

provider "kubernetes" {
  host                   = data.aws_ssm_parameter.eks["endpoint"].value
  cluster_ca_certificate = base64decode(data.aws_ssm_parameter.eks["ca"].value)
  token                  = data.aws_ssm_parameter.eks["ci-deployer-token"].value
}

provider "helm" {
  kubernetes = {
    host                   = data.aws_ssm_parameter.eks["endpoint"].value
    cluster_ca_certificate = base64decode(data.aws_ssm_parameter.eks["ca"].value)
    token                  = data.aws_ssm_parameter.eks["ci-deployer-token"].value
  }
}

# Aurora credentials written by modules/aurora.
data "aws_secretsmanager_secret_version" "master" {
  count = length(var.database_services) > 0 ? 1 : 0

  secret_id = "/solidaria/${var.environment}/aurora/${local.name}/master"
}

data "aws_secretsmanager_secret_version" "service_db" {
  for_each = var.database_services

  secret_id = "/solidaria/${var.environment}/${each.key}/db"
}

locals {
  master = length(var.database_services) > 0 ? jsondecode(data.aws_secretsmanager_secret_version.master[0].secret_string) : null
}

module "platform" {
  source = "../../../modules/platform-addons"

  name                  = local.name
  environment           = var.environment
  region                = var.region
  vpc_id                = data.aws_ssm_parameter.platform["vpc-id"].value
  apps_log_group        = data.aws_ssm_parameter.platform["apps-log-group"].value
  alerts_topic_arn      = data.aws_ssm_parameter.alerts_topic.value
  observability_dir     = "${path.root}/../../../../../deploy/observability"
  db_provisioner_script = "${path.root}/../../../../local/postgres/provision.sh"
  central_monitoring    = var.slot == "A1"
  enable_xray           = var.enable_xray

  database_host        = try(nonsensitive(local.master.host), "")
  database_master_user = try(nonsensitive(local.master.username), "")
  database_services    = var.database_services
  database_passwords = merge(
    length(var.database_services) > 0 ? { master = local.master.password } : {},
    { for service, version in data.aws_secretsmanager_secret_version.service_db : service => jsondecode(version.secret_string).password },
  )
}
