# DynamoDB table owned by one service (KAN-2 section 6.5): on-demand capacity, encryption,
# point-in-time recovery on by default (KAN-30), streams optional for the outbox relays (ADR-006).

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
  description = "Table name, for example solidaria-qa-smart-kyc-onboarding-documents."
  type        = string
}

variable "hash_key" {
  description = "Partition key attribute (string)."
  type        = string
}

variable "range_key" {
  description = "Sort key attribute (string); null for none."
  type        = string
  default     = null
}

variable "point_in_time_recovery" {
  description = "Point-in-time recovery; keep it on outside tests."
  type        = bool
  default     = true
}

variable "stream_enabled" {
  description = "DynamoDB Streams with new images, for the outbox relay."
  type        = bool
  default     = false
}

variable "ttl_attribute" {
  description = "Attribute holding the expiry epoch; null for none."
  type        = string
  default     = null
}

resource "aws_dynamodb_table" "this" {
  name             = var.name
  billing_mode     = "PAY_PER_REQUEST"
  hash_key         = var.hash_key
  range_key        = var.range_key
  stream_enabled   = var.stream_enabled
  stream_view_type = var.stream_enabled ? "NEW_IMAGE" : null

  attribute {
    name = var.hash_key
    type = "S"
  }

  dynamic "attribute" {
    for_each = var.range_key == null ? [] : [var.range_key]

    content {
      name = attribute.value
      type = "S"
    }
  }

  point_in_time_recovery {
    enabled = var.point_in_time_recovery
  }

  server_side_encryption {
    enabled = true
  }

  dynamic "ttl" {
    for_each = var.ttl_attribute == null ? [] : [var.ttl_attribute]

    content {
      attribute_name = ttl.value
      enabled        = true
    }
  }
}

output "arn" {
  description = "Table ARN."
  value       = aws_dynamodb_table.this.arn
}

output "stream_arn" {
  description = "Stream ARN (null without stream)."
  value       = aws_dynamodb_table.this.stream_arn
}
