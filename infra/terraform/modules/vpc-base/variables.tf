variable "name" {
  description = "Name prefix of the resources, for example solidaria-qa-a1."
  type        = string
}

variable "cidr_block" {
  description = "CIDR block of the VPC (infra/accounts.yml, vpc_cidr)."
  type        = string
}

variable "availability_zone_count" {
  description = "Number of Availability Zones (KAN-2 section 6.2: two)."
  type        = number
  default     = 2
}
