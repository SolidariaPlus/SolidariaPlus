# Alerting and log destinations of one account (KAN-33; KAN-2 section 6.7, ADR-107, ADR-121).
# - Amazon SNS topic with an e-mail subscription: Alertmanager (kube-prometheus-stack) and the
#   CloudWatch alarms publish to it. Every message names its environment (principle E-07).
# - CloudWatch log groups for the application logs (Fluent Bit) and the EKS control plane, with
#   retention, created before the cluster so that EKS does not create them without retention.
# - CloudWatch alarms on resources that Prometheus does not see: broker instance health and
#   Aurora CPU. The Telegram route is added with n8n in A4.
# Nothing here has an hourly price.

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
  description = "Name prefix, for example solidaria-qa-a1; it is also the EKS cluster name."
  type        = string
}

variable "environment" {
  description = "Environment: qa or prod."
  type        = string
}

variable "slot" {
  description = "Account slot: A1 to A5."
  type        = string
}

variable "alert_email" {
  description = "E-mail of the developer that receives every alert; empty to skip the subscription."
  type        = string
  default     = ""
}

variable "log_retention_days" {
  description = "Retention of the application and control plane logs."
  type        = number
  default     = 14
}

variable "broker_instance_id" {
  description = "Broker instance to watch; empty in accounts without a broker."
  type        = string
  default     = ""
}

variable "aurora_cluster_identifier" {
  description = "Aurora cluster to watch; empty in accounts without Aurora."
  type        = string
  default     = ""
}

locals {
  label = upper(var.environment == "prod" ? "production" : var.environment)
}

# Not encrypted: CloudWatch alarms cannot publish to a topic encrypted with the AWS managed key,
# and alert messages carry no personal data (KAN-2 P-05).
#trivy:ignore:AVD-AWS-0095
resource "aws_sns_topic" "alerts" {
  name = "${var.name}-alerts"
}

resource "aws_sns_topic_subscription" "email" {
  count = var.alert_email == "" ? 0 : 1

  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

resource "aws_cloudwatch_log_group" "apps" {
  name              = "/solidaria/${var.name}/apps"
  retention_in_days = var.log_retention_days
}

resource "aws_cloudwatch_log_group" "eks" {
  name              = "/aws/eks/${var.name}/cluster"
  retention_in_days = var.log_retention_days
}

resource "aws_cloudwatch_metric_alarm" "broker_status" {
  count = var.broker_instance_id == "" ? 0 : 1

  alarm_name          = "[${local.label}] ${var.name} broker instance status check failed"
  alarm_description   = "The Kafka and RabbitMQ broker instance fails its status checks (ADR-122, OI-15)."
  namespace           = "AWS/EC2"
  metric_name         = "StatusCheckFailed"
  dimensions          = { InstanceId = var.broker_instance_id }
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 3
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
}

resource "aws_cloudwatch_metric_alarm" "aurora_cpu" {
  count = var.aurora_cluster_identifier == "" ? 0 : 1

  alarm_name          = "[${local.label}] ${var.name} Aurora CPU above 80 %"
  alarm_description   = "The Aurora cluster has used more than 80 % CPU for 10 minutes."
  namespace           = "AWS/RDS"
  metric_name         = "CPUUtilization"
  dimensions          = { DBClusterIdentifier = var.aurora_cluster_identifier }
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  threshold           = 80
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"
  alarm_actions       = [aws_sns_topic.alerts.arn]
}

resource "aws_ssm_parameter" "topic" {
  name  = "/solidaria/${var.environment}/${lower(var.slot)}/alerts/topic-arn"
  type  = "String"
  value = aws_sns_topic.alerts.arn
}

output "topic_arn" {
  description = "SNS topic of the alerts (Alertmanager SNS receiver)."
  value       = aws_sns_topic.alerts.arn
}

output "apps_log_group" {
  description = "Log group of the application logs (Fluent Bit output)."
  value       = aws_cloudwatch_log_group.apps.name
}
