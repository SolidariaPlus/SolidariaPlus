variable "name" {
  description = "Cluster name, for example solidaria-qa-a1."
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

variable "session_on" {
  description = "True while a working session is on. False deletes the cluster and its node group (ADR-080)."
  type        = bool
}

variable "kubernetes_version" {
  description = "Kubernetes version of the control plane (KAN-27 verified 1.37)."
  type        = string
  default     = "1.37"
}

variable "subnet_ids" {
  description = "Private application subnets of the account, in two zones."
  type        = list(string)
}

variable "node_security_group_id" {
  description = "Persistent security group added to every node; the data stores and the brokers admit it."
  type        = string
}

variable "api_client_security_group_ids" {
  description = "Security groups allowed to reach the private API endpoint, by a static key: the runner and the bastion (ADR-120)."
  type        = map(string)
}

variable "node_instance_types" {
  description = "Instance types of the managed node group (KAN-11 section 5.3)."
  type        = list(string)
}

variable "node_desired_size" {
  description = "Desired nodes during a session."
  type        = number
}

variable "node_min_size" {
  description = "Minimum nodes during a session."
  type        = number
}

variable "node_max_size" {
  description = "Maximum nodes for the Cluster Autoscaler, within the 9 instances of the account."
  type        = number
}

variable "node_volume_size" {
  description = "Root volume of each node in GiB."
  type        = number
  default     = 30
}
