# Terraform state bucket of the account (KAN-2 ADR-118, KAN-11 section 6.8): versioned,
# encrypted, private, TLS only, with native S3 locking (use_lockfile) and no DynamoDB table.
# bootstrap.sh creates the empty bucket with the AWS CLI before the first init, because the
# backend of this root lives in it; the resources below configure it without reading it as a
# managed resource (Learner Lab SCP denies s3:GetBucketObjectLockConfiguration, so the
# aws_s3_bucket resource + import block cannot be used here).

locals {
  state_bucket     = "solidaria-tfstate-${var.environment}-${local.slot_lower}-${local.account_id}"
  state_bucket_arn = "arn:aws:s3:::${local.state_bucket}"
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = local.state_bucket

  versioning_configuration {
    status = "Enabled"
  }
}

# SSE-S3: a customer managed KMS key costs USD 1 a month per account and adds a key policy that
# LabRole may not manage; the bucket is private, TLS only and versioned.
#trivy:ignore:AVD-AWS-0132
resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = local.state_bucket

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket = local.state_bucket

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "state" {
  bucket = local.state_bucket

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "state" {
  bucket = local.state_bucket

  rule {
    id     = "expire-old-state-versions"
    status = "Enabled"

    filter {}

    noncurrent_version_expiration {
      noncurrent_days           = 90
      newer_noncurrent_versions = 20
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

data "aws_iam_policy_document" "state" {
  statement {
    sid     = "DenyInsecureTransport"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      local.state_bucket_arn,
      "${local.state_bucket_arn}/*",
    ]

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "state" {
  bucket = local.state_bucket
  policy = data.aws_iam_policy_document.state.json

  depends_on = [aws_s3_bucket_public_access_block.state]
}
