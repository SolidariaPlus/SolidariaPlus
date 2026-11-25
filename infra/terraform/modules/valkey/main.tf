# Amazon ElastiCache (Valkey) of one account (KAN-30; KAN-2 section 6.5, ADR-055, ADR-079).
# One ACL user per service, limited to its own key prefix, as on localhost
# (infra/local/valkey/start-valkey.sh). The users and their passwords have no hourly price and stay
# between sessions; the replication group exists only during sessions (ADR-080), so the cache
# starts empty in every session.

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

variable "session_on" {
  description = "True during a session."
  type        = bool
}

variable "vpc_id" {
  description = "VPC of the account."
  type        = string
}

variable "subnet_ids" {
  description = "Private data subnets."
  type        = list(string)
}

variable "client_security_group_ids" {
  description = "Security groups allowed on port 6379, by a static key: EKS nodes and bastion only."
  type        = map(string)
}

variable "services" {
  description = "Services with a cache user: service name => key prefix."
  type        = map(string)
}

variable "engine_version" {
  description = "Valkey version; its major must equal the valkey image of compose.yaml."
  type        = string
  default     = "8.1"
}

variable "node_type" {
  description = "Cache node type."
  type        = string
  default     = "cache.t4g.micro"
}

variable "replicas" {
  description = "Replicas with automatic failover (Production A2 only, ADR-079)."
  type        = number
  default     = 0
}

resource "aws_elasticache_subnet_group" "this" {
  name       = var.name
  subnet_ids = var.subnet_ids
}

resource "aws_security_group" "valkey" {
  name        = "${var.name}-valkey"
  description = "Valkey: EKS nodes and bastion only"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name}-valkey"
  }
}

resource "aws_vpc_security_group_ingress_rule" "valkey" {
  for_each = var.client_security_group_ids

  security_group_id            = aws_security_group.valkey.id
  description                  = "Valkey from ${each.key}"
  ip_protocol                  = "tcp"
  from_port                    = 6379
  to_port                      = 6379
  referenced_security_group_id = each.value
}

# ElastiCache requires a user named "default" in every user group; it is disabled.
resource "random_password" "default" {
  length  = 32
  special = false
}

resource "aws_elasticache_user" "default" {
  user_id       = "${var.name}-default"
  user_name     = "default"
  engine        = "valkey"
  access_string = "off -@all"
  passwords     = [random_password.default.result]
}

resource "random_password" "service" {
  for_each = var.services

  length  = 32
  special = false
}

resource "aws_elasticache_user" "service" {
  for_each = var.services

  user_id       = substr("${var.name}-${each.key}", 0, 40)
  user_name     = each.key
  engine        = "valkey"
  access_string = "on ~${each.value}:* &${each.value}:* +@all -@dangerous"
  passwords     = [random_password.service[each.key].result]
}

resource "aws_elasticache_user_group" "this" {
  user_group_id = var.name
  engine        = "valkey"
  user_ids      = concat([aws_elasticache_user.default.user_id], [for user in aws_elasticache_user.service : user.user_id])
}

resource "aws_elasticache_replication_group" "this" {
  count = var.session_on ? 1 : 0

  replication_group_id       = var.name
  description                = "Valkey cache of ${var.name}"
  engine                     = "valkey"
  engine_version             = var.engine_version
  node_type                  = var.node_type
  num_cache_clusters         = 1 + var.replicas
  automatic_failover_enabled = var.replicas > 0
  multi_az_enabled           = var.replicas > 0
  port                       = 6379
  subnet_group_name          = aws_elasticache_subnet_group.this.name
  security_group_ids         = [aws_security_group.valkey.id]
  user_group_ids             = [aws_elasticache_user_group.this.user_group_id]
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  apply_immediately          = true
}

resource "aws_secretsmanager_secret" "service" {
  for_each = var.services

  name                    = "/solidaria/${var.environment}/${each.key}/cache"
  description             = "Valkey credentials of ${each.key}; the host is in /solidaria/${var.environment}/${each.key}/cache/host."
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "service" {
  for_each = var.services

  secret_id = aws_secretsmanager_secret.service[each.key].id
  secret_string = jsonencode({
    username  = each.key
    password  = random_password.service[each.key].result
    port      = 6379
    tls       = true
    keyPrefix = "${each.value}:"
  })
}

# The endpoint changes in every session, so it is a parameter, not part of the secret.
resource "aws_ssm_parameter" "host" {
  for_each = var.services

  name  = "/solidaria/${var.environment}/${each.key}/cache/host"
  type  = "String"
  value = var.session_on ? aws_elasticache_replication_group.this[0].primary_endpoint_address : "none"
}

output "primary_endpoint" {
  description = "Primary endpoint (null outside sessions)."
  value       = one(aws_elasticache_replication_group.this[*].primary_endpoint_address)
}
