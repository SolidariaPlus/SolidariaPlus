output "cluster_name" {
  description = "Cluster name (null outside sessions)."
  value       = one(aws_eks_cluster.this[*].name)
}

output "cluster_endpoint" {
  description = "Private API endpoint (null outside sessions)."
  value       = one(aws_eks_cluster.this[*].endpoint)
}

output "cluster_security_group_id" {
  description = "Security group that EKS created for the control plane and the nodes (null outside sessions)."
  value       = one(aws_eks_cluster.this[*].vpc_config[0].cluster_security_group_id)
}

output "ci_deployer_parameter_dir" {
  description = "Parameter Store path with token, endpoint and ca of the ci-deployer ServiceAccount."
  value       = local.parameter_dir
}
