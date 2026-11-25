# S3 bucket owned by one service or by the platform (KAN-2 section 6.5): private, encrypted,
# TLS only, versioning on by default (KAN-30). Object Lock is opt-in (MS-05, MS-13, MS-15).

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
  description = "Allow deleting a non-empty bucket (QA test buckets only)."
  type        = bool
  default     = false
}

resource "aws_s3_bucket" "this" {
  bucket              = var.name
  force_destroy       = var.force_destroy
  object_lock_enabled = var.object_lock_mode != null
}

resource "aws_s3_bucket_versioning" "this" {
  bucket = aws_s3_bucket.this.id

  versioning_configuration {
    status = var.versioning || var.object_lock_mode != null ? "Enabled" : "Suspended"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.this.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "this" {
  bucket = aws_s3_bucket.this.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "this" {
  bucket = aws_s3_bucket.this.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_object_lock_configuration" "this" {
  count = var.object_lock_mode == null ? 0 : 1

  bucket = aws_s3_bucket.this.id

  rule {
    default_retention {
      mode = var.object_lock_mode
      days = var.object_lock_days
    }
  }
}

output "id" {
  description = "Bucket name."
  value       = aws_s3_bucket.this.id
}

output "arn" {
  description = "Bucket ARN."
  value       = aws_s3_bucket.this.arn
}

output "regional_domain_name" {
  description = "Regional domain name (CloudFront origin)."
  value       = aws_s3_bucket.this.bucket_regional_domain_name
}
