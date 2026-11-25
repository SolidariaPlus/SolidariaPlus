terraform {
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # Partial configuration. The workflows pass the bucket of the account and the key:
  #   -backend-config=bucket=solidaria-tfstate-<env>-<slot>-<account>
  #   -backend-config=key=envs/<env>/<slot>/terraform.tfstate
  #   -backend-config=region=<region>
  backend "s3" {
    use_lockfile = true
    encrypt      = true
  }
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
