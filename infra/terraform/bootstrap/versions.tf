terraform {
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # Partial configuration: bootstrap.sh passes bucket, key and region (-backend-config).
  # The bucket is created by bootstrap.sh before the first init and then imported (state.tf).
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
      stack        = "bootstrap"
    }
  }
}
