variable "name" {
  description = "Name prefix, for example solidaria-qa-a1."
  type        = string
}

variable "environment" {
  description = "Environment: qa or prod."
  type        = string
}

variable "session_on" {
  description = "True during a session: the instance runs and holds an Elastic IP. False stops it and releases the address (ADR-122)."
  type        = bool
}

variable "vpc_id" {
  description = "VPC of A1."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR of the VPC: the management and metrics ports are open to it only."
  type        = string
}

variable "subnet_id" {
  description = "Public subnet of A1 that holds the instance."
  type        = string
}

variable "client_security_group_ids" {
  description = "Security groups of the clients inside the VPC (EKS nodes, bastion) for the private SASL_SSL listener, by a static key."
  type        = map(string)
}

variable "public_client_cidrs" {
  description = "CIDRs allowed on the public SASL_SSL (9094) and AMQPS (5671) listeners: the other accounts of the environment and the Lambda functions (ADR-122)."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "instance_type" {
  description = "Instance type of the broker (ADR-122: t3.medium)."
  type        = string
  default     = "t3.medium"
}

variable "volume_size" {
  description = "Root volume in GiB: Kafka log (7-day retention) and RabbitMQ queues (ADR-122: 20)."
  type        = number
  default     = 20
}

variable "kafka_image" {
  description = "Kafka image; must equal the kafka image of compose.yaml."
  type        = string
  default     = "apache/kafka:4.1.0"
}

variable "rabbitmq_image" {
  description = "RabbitMQ image; must equal the rabbitmq image of compose.yaml."
  type        = string
  default     = "rabbitmq:4.1.4-management-alpine"
}

variable "rabbitmq_jwks_url" {
  description = "JWKS URL of MS-17 that the RabbitMQ OAuth 2.0 backend trusts. Empty until MS-17 issues tokens (KAN-18)."
  type        = string
  default     = ""

  validation {
    condition     = var.rabbitmq_jwks_url == "" || startswith(var.rabbitmq_jwks_url, "https://")
    error_message = "rabbitmq_jwks_url must be empty or an https:// URL (RabbitMQ 4.1+ rejects other schemes at boot)."
  }
}

variable "messaging_dir" {
  description = "Path of infra/messaging (definitions and scripts uploaded to the instance)."
  type        = string
}

variable "ca_cert_pem" {
  description = "Certificate of the environment CA (modules/pki)."
  type        = string
}

variable "ca_private_key_pem" {
  description = "Private key of the environment CA (modules/pki)."
  type        = string
  sensitive   = true
}
