# Amazon Aurora PostgreSQL cluster of one account (KAN-30; KAN-11 section 5.6; KAN-2 section 6.5,
# ADR-054, ADR-079). One database and one dedicated user per service. Terraform generates each
# user's password and keeps it in Secrets Manager as /solidaria/<env>/<service>/db, the same shape
# that LocalStack holds on localhost. The databases and roles are created inside the cluster by
# the db-provisioner Job of modules/platform-addons, which reuses infra/local/postgres/provision.sh.
#
# Session mode: Aurora is stopped, not deleted, at session-stop and started at session-start
# (Terraform has no stop attribute, so terraform_data calls the RDS API). A stopped cluster starts
# by itself after seven days; the next session-stop stops it again (KAN-11 section 5.5).

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
  description = "Cluster identifier, for example solidaria-qa-a1."
  type        = string
}

variable "environment" {
  description = "Environment: qa or prod."
  type        = string
}

variable "session_on" {
  description = "True during a session (cluster started); false stops it."
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
  description = "Security groups allowed on port 5432, by a static key: EKS nodes and bastion only."
  type        = map(string)
}

variable "services" {
  description = "Services with a database: service name => database name (KAN-2 section 6.5)."
  type        = map(string)
}

variable "engine_version" {
  description = "Aurora PostgreSQL version; its major must equal the postgres image of compose.yaml."
  type        = string
  default     = "17.5"
}

variable "instance_class" {
  description = "Instance class (Learner Lab: nano to medium)."
  type        = string
  default     = "db.t4g.medium"
}

variable "reader_count" {
  description = "Readers in a second zone (Production A2 only, ADR-079)."
  type        = number
  default     = 0
}

variable "backup_retention_days" {
  description = "Automated backups with point-in-time recovery (RPO about 5 minutes)."
  type        = number
  default     = 7
}

data "aws_region" "current" {}

resource "aws_db_subnet_group" "this" {
  name       = var.name
  subnet_ids = var.subnet_ids
}

resource "aws_security_group" "aurora" {
  name        = "${var.name}-aurora"
  description = "Aurora PostgreSQL: EKS nodes and bastion only"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name}-aurora"
  }
}

resource "aws_vpc_security_group_ingress_rule" "aurora" {
  for_each = var.client_security_group_ids

  security_group_id            = aws_security_group.aurora.id
  description                  = "PostgreSQL from ${each.key}"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  referenced_security_group_id = each.value
}

resource "random_password" "master" {
  length  = 32
  special = false
}

resource "aws_rds_cluster" "this" {
  cluster_identifier              = var.name
  engine                          = "aurora-postgresql"
  engine_version                  = var.engine_version
  database_name                   = "postgres"
  master_username                 = "solidaria_admin"
  master_password                 = random_password.master.result
  db_subnet_group_name            = aws_db_subnet_group.this.name
  vpc_security_group_ids          = [aws_security_group.aurora.id]
  storage_encrypted               = true
  backup_retention_period         = var.backup_retention_days
  preferred_backup_window         = "07:00-08:00"
  copy_tags_to_snapshot           = true
  deletion_protection             = var.environment == "prod"
  skip_final_snapshot             = var.environment != "prod"
  final_snapshot_identifier       = var.environment == "prod" ? "${var.name}-final" : null
  enabled_cloudwatch_logs_exports = ["postgresql"]

  lifecycle {
    ignore_changes = [engine_version]
  }
}

resource "aws_rds_cluster_instance" "this" {
  count = 1 + var.reader_count

  identifier           = "${var.name}-${count.index}"
  cluster_identifier   = aws_rds_cluster.this.id
  engine               = aws_rds_cluster.this.engine
  engine_version       = aws_rds_cluster.this.engine_version
  instance_class       = var.instance_class
  db_subnet_group_name = aws_db_subnet_group.this.name
  promotion_tier       = count.index
}

# Start or stop the cluster according to the session (ADR-080).
resource "terraform_data" "session_state" {
  triggers_replace = [var.session_on]

  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    command     = <<-EOT
      set -euo pipefail
      status=$(aws rds describe-db-clusters --region "$REGION" --db-cluster-identifier "$CLUSTER" --query 'DBClusters[0].Status' --output text)
      if [ "$SESSION_ON" = "true" ] && [ "$status" = "stopped" ]; then
        aws rds start-db-cluster --region "$REGION" --db-cluster-identifier "$CLUSTER" >/dev/null
      elif [ "$SESSION_ON" = "false" ] && [ "$status" = "available" ]; then
        aws rds stop-db-cluster --region "$REGION" --db-cluster-identifier "$CLUSTER" >/dev/null
      fi
      if [ "$SESSION_ON" = "true" ]; then
        aws rds wait db-cluster-available --region "$REGION" --db-cluster-identifier "$CLUSTER"
      fi
    EOT

    environment = {
      REGION     = data.aws_region.current.region
      CLUSTER    = aws_rds_cluster.this.cluster_identifier
      SESSION_ON = tostring(var.session_on)
    }
  }

  depends_on = [aws_rds_cluster_instance.this]
}

resource "aws_secretsmanager_secret" "master" {
  name                    = "/solidaria/${var.environment}/aurora/${var.name}/master"
  description             = "Master user of the Aurora cluster ${var.name}; used only by the db-provisioner Job."
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "master" {
  secret_id = aws_secretsmanager_secret.master.id
  secret_string = jsonencode({
    username = aws_rds_cluster.this.master_username
    password = random_password.master.result
    host     = aws_rds_cluster.this.endpoint
    port     = aws_rds_cluster.this.port
    dbname   = "postgres"
  })
}

resource "random_password" "service" {
  for_each = var.services

  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "service" {
  for_each = var.services

  name                    = "/solidaria/${var.environment}/${each.key}/db"
  description             = "Database credentials of ${each.key}."
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "service" {
  for_each = var.services

  secret_id = aws_secretsmanager_secret.service[each.key].id
  secret_string = jsonencode({
    username = replace(each.key, "-", "_")
    password = random_password.service[each.key].result
    host     = aws_rds_cluster.this.endpoint
    port     = aws_rds_cluster.this.port
    dbname   = each.value
  })
}

output "endpoint" {
  description = "Writer endpoint."
  value       = aws_rds_cluster.this.endpoint
}

output "reader_endpoint" {
  description = "Reader endpoint."
  value       = aws_rds_cluster.this.reader_endpoint
}

output "cluster_identifier" {
  description = "Cluster identifier."
  value       = aws_rds_cluster.this.cluster_identifier
}

output "master_secret_name" {
  description = "Secrets Manager name of the master credentials."
  value       = aws_secretsmanager_secret.master.name
}

output "service_secret_names" {
  description = "Secrets Manager name of the credentials of each service."
  value       = { for service, secret in aws_secretsmanager_secret.service : service => secret.name }
}
