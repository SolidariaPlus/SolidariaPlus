# Client entry of the environment, in A1 (KAN-32; KAN-2 section 4.2, section 6.4, ADR-115).
# Cloudflare sits in front as CDN/WAF (OI-19). It routes:
#   /* (SPA)        → S3 web bucket (Cloudflare R2 mirror or direct S3 origin)
#   /auth/*, /api/* → HTTP API endpoint → VPC Link → ALB
#
# CloudFront was removed: Cloudflare replaces it with equivalent WAF, caching, and HTTPS
# termination at zero additional AWS hourly cost. The X-Origin-Verify secret is kept so the
# ALB listener rule can still require an authenticated header from Cloudflare (ADR-115).
# No resource here has an hourly price.

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

variable "vpc_id" {
  description = "VPC where the edge proxy lives."
  type        = string
}

variable "subnet_id" {
  description = "Public subnet for the edge proxy EC2."
  type        = string
}

data "aws_caller_identity" "current" {}

data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

locals {
  api_domain = replace(var.api_endpoint, "https://", "")
}

module "web_bucket" {
  source = "../s3-bucket"

  name = "${var.name}-web-${data.aws_caller_identity.current.account_id}"
}

resource "random_password" "origin_verify" {
  length  = 40
  special = false
}

resource "aws_secretsmanager_secret" "origin_verify" {
  name                    = "/solidaria/${var.environment}/edge/origin-verify"
  description             = "Value of the X-Origin-Verify header that Cloudflare sends and the ALB requires."
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "origin_verify" {
  secret_id     = aws_secretsmanager_secret.origin_verify.id
  secret_string = random_password.origin_verify.result
}

# ---------------------------------------------------------------------------------------------
# Edge proxy: permanent EIP + nginx EC2. Clients reach this IP; Cloudflare connects to it.
# The EIP is never released between sessions — give it to the domain registrar as the A record.
# ---------------------------------------------------------------------------------------------

resource "aws_eip" "entry" {
  domain = "vpc"

  tags = {
    Name = "${var.name}-entry"
  }
}

#trivy:ignore:AVD-AWS-0104
resource "aws_security_group" "edge_proxy" {
  name        = "${var.name}-edge-proxy"
  description = "Edge proxy EC2; admits HTTP/HTTPS from the internet and all egress."
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name}-edge-proxy"
  }
}

resource "aws_vpc_security_group_ingress_rule" "edge_proxy_http" {
  security_group_id = aws_security_group.edge_proxy.id
  description       = "HTTP from Cloudflare / domain registrar"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_vpc_security_group_ingress_rule" "edge_proxy_https" {
  security_group_id = aws_security_group.edge_proxy.id
  description       = "HTTPS from Cloudflare"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_vpc_security_group_egress_rule" "edge_proxy_all" {
  security_group_id = aws_security_group.edge_proxy.id
  description       = "Outbound to API Gateway and S3"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_instance" "edge_proxy" {
  ami                    = data.aws_ssm_parameter.al2023.value
  instance_type          = "t3.micro"
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [aws_security_group.edge_proxy.id]
  iam_instance_profile   = "LabInstanceProfile"

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  user_data_base64 = base64encode(templatefile("${path.module}/templates/edge-proxy-init.sh.tftpl", {
    api_host = local.api_domain
  }))

  lifecycle {
    ignore_changes = [ami, user_data]
  }

  tags = {
    Name = "${var.name}-edge-proxy"
  }
}

resource "aws_eip_association" "entry" {
  allocation_id = aws_eip.entry.id
  instance_id   = aws_instance.edge_proxy.id
}

# ---------------------------------------------------------------------------------------------

resource "aws_ssm_parameter" "edge" {
  for_each = {
    "entry-ip"          = aws_eip.entry.public_ip
    "web-bucket"        = module.web_bucket.id
    "web-bucket-domain" = module.web_bucket.regional_domain_name
    "api-endpoint"      = var.api_endpoint
  }

  name  = "/solidaria/${var.environment}/edge/${each.key}"
  type  = "String"
  value = each.value
}

output "entry_ip" {
  description = "Static EIP of the edge proxy. Give this IP to the domain registrar as the A record."
  value       = aws_eip.entry.public_ip
}

output "web_bucket" {
  description = "Bucket of the web build (Cloudflare static origin)."
  value       = module.web_bucket.id
}

output "web_bucket_domain" {
  description = "Regional S3 domain for the web bucket (configure as Cloudflare static origin)."
  value       = module.web_bucket.regional_domain_name
}
