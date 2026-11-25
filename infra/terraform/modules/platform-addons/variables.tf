variable "name" {
  description = "Cluster name, for example solidaria-qa-a1."
  type        = string
}

variable "environment" {
  description = "Environment: qa or prod."
  type        = string
}

variable "region" {
  description = "AWS region."
  type        = string
}

variable "vpc_id" {
  description = "VPC of the cluster (AWS Load Balancer Controller)."
  type        = string
}

variable "apps_log_group" {
  description = "CloudWatch Logs group of the application logs (modules/alerting)."
  type        = string
}

variable "alerts_topic_arn" {
  description = "SNS topic of the alerts (modules/alerting)."
  type        = string
}

variable "observability_dir" {
  description = "Path of deploy/observability (values, dashboards, alert rules)."
  type        = string
}

variable "central_monitoring" {
  description = "True in A1: install the central Prometheus, Alertmanager and Grafana of the environment."
  type        = bool
  default     = true
}

variable "enable_xray" {
  description = "Install the OpenTelemetry collector with the X-Ray exporter (only if X-Ray is available, OI-02)."
  type        = bool
  default     = false
}

variable "chart_versions" {
  description = "Pinned chart versions. Update them in one pull request and check the release notes."
  type = object({
    aws_load_balancer_controller = string
    cluster_autoscaler           = string
    keda                         = string
    kube_prometheus_stack        = string
    fluent_bit                   = string
    opentelemetry_collector      = string
  })
  default = {
    aws_load_balancer_controller = "1.13.4"
    cluster_autoscaler           = "9.50.1"
    keda                         = "2.17.2"
    kube_prometheus_stack        = "77.6.2"
    fluent_bit                   = "0.52.0"
    opentelemetry_collector      = "0.131.0"
  }
}

variable "database_host" {
  description = "Writer endpoint of the Aurora cluster of the account; empty in accounts without Aurora."
  type        = string
  default     = ""
}

variable "database_master_user" {
  description = "Master user of the Aurora cluster (used only by the db-provisioner Job)."
  type        = string
  default     = ""
}

variable "database_services" {
  description = "Services with a database: service name => database name."
  type        = map(string)
  default     = {}
}

variable "database_passwords" {
  description = "Passwords from Secrets Manager: \"master\" and one entry per service."
  type        = map(string)
  default     = {}
  sensitive   = true
}

variable "db_provisioner_script" {
  description = "Path of infra/local/postgres/provision.sh, reused by the db-provisioner Job."
  type        = string
}

variable "postgres_image" {
  description = "psql client image of the db-provisioner Job; same major as Aurora and compose.yaml."
  type        = string
  default     = "postgres:17.6-alpine"
}
