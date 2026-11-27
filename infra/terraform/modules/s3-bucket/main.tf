# S3 bucket owned by one service or by the platform (KAN-2 section 6.5): private, encrypted,
# TLS only, versioning on by default (KAN-30). Object Lock is opt-in (MS-05, MS-13, MS-15).
#
# Learner Lab SCP denies s3:GetBucketObjectLockConfiguration. The AWS provider v6.x calls that
# API on every refresh of aws_s3_bucket, causing plan/apply to fail. This module therefore does
# NOT manage the aws_s3_bucket resource; the caller must create the bucket once with the CLI:
#
#   aws s3api create-bucket --bucket <name> --region <region> \
#     --create-bucket-configuration LocationConstraint=<region>
#
# All bucket sub-resources (versioning, encryption, public-access-block, ownership, object-lock)
# are still managed here by referencing the bucket by its name string.

terraform {
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

variable "name" {
  description = "Globally unique bucket name."
  type        = string
}

variable "versioning" {
  description = "Versioning; keep it on outside throwaway buckets."
  type        = bool
  default     = true
}

variable "object_lock_mode" {
  description = "GOVERNANCE (QA) or COMPLIANCE (Production) to enable Object Lock; null for none."
  type        = string
  default     = null
}

variable "object_lock_days" {
  description = "Default retention when Object Lock is on."
  type        = number
  default     = 30
}

variable "force_destroy" {
  description = "Allow deleting a non-empty bucket (QA test buckets only). Not enforced by this module; caller must empty the bucket before destroy."
  type        = bool
  default     = false
}

data "aws_region" "current" {}

locals {
  bucket_id              = var.name
  bucket_arn             = "arn:aws:s3:::${var.name}"
  bucket_regional_domain = "${var.name}.s3.${data.aws_region.current.name}.amazonaws.com"
}

resource "aws_s3_bucket_versioning" "this" {
  bucket = local.bucket_id

  versioning_configuration {
    status = var.versioning || var.object_lock_mode != null ? "Enabled" : "Suspended"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = local.bucket_id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = local.bucket_id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "this" {
  bucket = local.bucket_id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_object_lock_configuration" "this" {
  count = var.object_lock_mode == null ? 0 : 1

  bucket = local.bucket_id

  rule {
    default_retention {
      mode = var.object_lock_mode
      days = var.object_lock_days
    }
  }
}

output "id" {
  description = "Bucket name."
  value       = local.bucket_id
}

output "arn" {
  description = "Bucket ARN."
  value       = local.bucket_arn
}

output "regional_domain_name" {
  description = "Regional domain name (S3 origin for Cloudflare or other CDN)."
  value       = local.bucket_regional_domain
}
