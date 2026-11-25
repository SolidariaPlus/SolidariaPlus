# Client entry of the environment, in A1 (KAN-32; KAN-2 section 4.2, section 6.4, ADR-115).
#   CloudFront ── default behavior ──> private S3 bucket of the web client (origin access control)
#              └─ /auth/*, /api/*  ──> HTTP API of A1 > VPC link > ALB
# CloudFront adds a secret origin header (X-Origin-Verify) on the API origin. The HTTP API forwards
# it, and the ALB listener rule of the shared Helm chart accepts only requests that carry it, so
# the HTTP API URL cannot be used to bypass CloudFront.
# Cloudflare goes in front of CloudFront once the domain is delegated (OI-19); until then the
# entry point is the CloudFront domain. No resource here has an hourly price.

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
  }
}

variable "name" {
  description = "Name prefix, for example solidaria-qa-a1."
  type        = string
}

variable "environment" {
  description = "Environment: qa or prod."
  type        = string
}

variable "api_endpoint" {
  description = "Invoke URL of the HTTP API (https://<id>.execute-api.<region>.amazonaws.com)."
  type        = string
}

variable "price_class" {
  description = "CloudFront price class."
  type        = string
  default     = "PriceClass_100"
}

data "aws_caller_identity" "current" {}

locals {
  api_domain = replace(var.api_endpoint, "https://", "")
  # AWS managed policies
  caching_optimized             = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  caching_disabled              = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
  all_viewer_except_host_header = "b689b0a8-53d0-40ab-baf2-68738e2966ac"
  security_headers              = "67f7725c-6f97-4210-82d7-5512b31e9d03"
}

module "web_bucket" {
  source = "../s3-bucket"

  name          = "${var.name}-web-${data.aws_caller_identity.current.account_id}"
  force_destroy = var.environment != "prod"
}

resource "random_password" "origin_verify" {
  length  = 40
  special = false
}

resource "aws_secretsmanager_secret" "origin_verify" {
  name                    = "/solidaria/${var.environment}/edge/origin-verify"
  description             = "Value of the X-Origin-Verify header that CloudFront sends and the ALB requires."
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "origin_verify" {
  secret_id     = aws_secretsmanager_secret.origin_verify.id
  secret_string = random_password.origin_verify.result
}

resource "aws_cloudfront_origin_access_control" "web" {
  name                              = "${var.name}-web"
  description                       = "Web client bucket of ${var.name}"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "this" {
  enabled             = true
  comment             = "Solidaria+ ${var.environment} client entry"
  default_root_object = "index.html"
  price_class         = var.price_class
  http_version        = "http2and3"

  origin {
    origin_id                = "web"
    domain_name              = module.web_bucket.regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.web.id
  }

  origin {
    origin_id   = "api"
    domain_name = local.api_domain

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }

    custom_header {
      name  = "X-Origin-Verify"
      value = random_password.origin_verify.result
    }
  }

  default_cache_behavior {
    target_origin_id           = "web"
    viewer_protocol_policy     = "redirect-to-https"
    allowed_methods            = ["GET", "HEAD", "OPTIONS"]
    cached_methods             = ["GET", "HEAD"]
    compress                   = true
    cache_policy_id            = local.caching_optimized
    response_headers_policy_id = local.security_headers
  }

  dynamic "ordered_cache_behavior" {
    for_each = ["/auth/*", "/api/*"]

    content {
      path_pattern             = ordered_cache_behavior.value
      target_origin_id         = "api"
      viewer_protocol_policy   = "https-only"
      allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
      cached_methods           = ["GET", "HEAD"]
      compress                 = true
      cache_policy_id          = local.caching_disabled
      origin_request_policy_id = local.all_viewer_except_host_header
    }
  }

  # Client-side routing of the single-page application.
  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }

  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }
}

data "aws_iam_policy_document" "web" {
  statement {
    sid       = "CloudFrontRead"
    actions   = ["s3:GetObject"]
    resources = ["${module.web_bucket.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.this.arn]
    }
  }

  statement {
    sid       = "DenyInsecureTransport"
    effect    = "Deny"
    actions   = ["s3:*"]
    resources = [module.web_bucket.arn, "${module.web_bucket.arn}/*"]

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

resource "aws_s3_bucket_policy" "web" {
  bucket = module.web_bucket.id
  policy = data.aws_iam_policy_document.web.json
}

resource "aws_ssm_parameter" "edge" {
  for_each = {
    "web-bucket"      = module.web_bucket.id
    "distribution-id" = aws_cloudfront_distribution.this.id
    "domain-name"     = aws_cloudfront_distribution.this.domain_name
  }

  name  = "/solidaria/${var.environment}/edge/${each.key}"
  type  = "String"
  value = each.value
}

output "distribution_id" {
  description = "CloudFront distribution (deploy-qa.yml invalidates it after publishing the web build)."
  value       = aws_cloudfront_distribution.this.id
}

output "domain_name" {
  description = "Public entry point of the environment."
  value       = aws_cloudfront_distribution.this.domain_name
}

output "web_bucket" {
  description = "Bucket of the web build."
  value       = module.web_bucket.id
}

output "origin_verify_secret_name" {
  description = "Secrets Manager name of the origin header value (read by the Helm deploy for the Ingress rule)."
  value       = aws_secretsmanager_secret.origin_verify.name
}
