variable "name" {
  description = "Name prefix of the resources, for example solidaria-qa-a1."
  type        = string
}

variable "vpc_id" {
  description = "VPC created by the bootstrap (modules/vpc-base)."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block of the VPC."
  type        = string
}

variable "availability_zones" {
  description = "Availability Zones of the public subnets, in the same order."
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "Public subnets of the VPC, one per Availability Zone (NAT gateways live there)."
  type        = list(string)
}

variable "session_on" {
  description = "True while a working session is on; false removes every resource with an hourly price (KAN-2 ADR-080)."
  type        = bool
}

variable "nat_gateway_count" {
  description = "NAT gateways during a session: 1 per account, or one per zone in Production A2 (ADR-079)."
  type        = number
  default     = 1

  validation {
    condition     = var.nat_gateway_count >= 0 && var.nat_gateway_count <= 2
    error_message = "nat_gateway_count must be 0, 1 or 2."
  }
}

variable "enable_ssm_endpoints" {
  description = "Create the interface endpoints ssm, ssmmessages and ec2messages during sessions (A1 only, ADR-120)."
  type        = bool
  default     = false
}

variable "cluster_name" {
  description = "Name of the EKS cluster of the account, used in the subnet discovery tags."
  type        = string
}
