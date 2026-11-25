variable "environment" {
  description = "Environment: qa or prod."
  type        = string

  validation {
    condition     = contains(["qa", "prod"], var.environment)
    error_message = "environment must be qa or prod."
  }
}

variable "slot" {
  description = "Account slot of infra/accounts.yml."
  type        = string
}

variable "region" {
  description = "Region of the environment (us-west-2 for qa, us-east-1 for prod)."
  type        = string
}

variable "session" {
  description = "on during a working session; off turns off every resource with an hourly price (KAN-2 ADR-080, KAN-11 section 5.5)."
  type        = string
  default     = "off"

  validation {
    condition     = contains(["on", "off"], var.session)
    error_message = "session must be on or off."
  }
}

# --- Compute (KAN-11 section 5.3) ------------------------------------------------------------
variable "node_instance_types" {
  description = "Instance types of the EKS node group."
  type        = list(string)
}

variable "node_desired_size" {
  description = "Nodes during a session."
  type        = number
}

variable "node_min_size" {
  description = "Minimum nodes during a session."
  type        = number
}

variable "node_max_size" {
  description = "Maximum nodes (Cluster Autoscaler), within the 9 instances of the account."
  type        = number
}

variable "kubernetes_version" {
  description = "EKS version."
  type        = string
  default     = "1.37"
}

variable "nat_gateway_count" {
  description = "NAT gateways during a session."
  type        = number
  default     = 1
}

# --- Services and stores -----------------------------------------------------------------------
variable "services" {
  description = "Services of the slot that are deployed in this increment (one ECR repository each)."
  type        = list(string)
}

variable "database_services" {
  description = "Services with a database in the Aurora cluster: service => database name."
  type        = map(string)
  default     = {}
}

variable "cache_services" {
  description = "Services with a Valkey user: service => key prefix."
  type        = map(string)
  default     = {}
}

variable "aurora_instance_class" {
  description = "Aurora instance class."
  type        = string
  default     = "db.t4g.medium"
}

variable "aurora_reader_count" {
  description = "Aurora readers (Production A2 only)."
  type        = number
  default     = 0
}

variable "valkey_node_type" {
  description = "Valkey node type."
  type        = string
  default     = "cache.t4g.micro"
}

variable "valkey_replicas" {
  description = "Valkey replicas (Production A2 only)."
  type        = number
  default     = 0
}

# --- Platform components of A1 -----------------------------------------------------------------
variable "platform_slot" {
  description = "True in A1: environment CA, broker instance, directory, client entry and SSM endpoints."
  type        = bool
  default     = false
}

variable "broker_public_client_cidrs" {
  description = "CIDRs allowed on the public Kafka and AMQPS listeners of the broker."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "rabbitmq_jwks_url" {
  description = "JWKS URL of MS-17 for the RabbitMQ OAuth 2.0 backend; empty until KAN-18."
  type        = string
  default     = ""
}

variable "cors_allow_origins" {
  description = "CORS allow-list of the HTTP API."
  type        = list(string)
  default     = []
}

variable "api_throttling_rate_limit" {
  description = "Requests per second of the HTTP API stage."
  type        = number
  default     = 200
}

variable "api_throttling_burst_limit" {
  description = "Burst of the HTTP API stage."
  type        = number
  default     = 400
}

# --- Promotion and alerting --------------------------------------------------------------------
variable "promotion_pull_account_ids" {
  description = "QA only: the matching Production account, allowed to pull images by digest (ADR-119)."
  type        = list(string)
  default     = []
}

variable "alert_email" {
  description = "E-mail that receives the alerts; pass it with -var or TF_VAR_alert_email, not in Git."
  type        = string
  default     = ""
}

variable "log_retention_days" {
  description = "Retention of the application logs."
  type        = number
  default     = 14
}
