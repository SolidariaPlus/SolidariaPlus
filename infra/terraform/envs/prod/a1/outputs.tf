output "session" {
  description = "Session state applied."
  value       = var.session
}

output "cluster_name" {
  description = "EKS cluster (null outside sessions)."
  value       = module.eks.cluster_name
}

output "ecr_repositories" {
  description = "ECR repository URL by service."
  value       = module.ecr.repository_urls
}

output "http_api_endpoint" {
  description = "HTTP API invoke URL."
  value       = module.http_api.api_endpoint
}

output "http_api_routes_wired" {
  description = "True when the HTTP API routes reach the ALB (second pass of session-start)."
  value       = module.http_api.routes_wired
}

output "cloudfront_domain" {
  description = "Public entry point (A1 only)."
  value       = try(module.edge[0].domain_name, null)
}

output "broker_public_ip" {
  description = "Elastic IP of the broker during the session (A1 only)."
  value       = try(module.messaging[0].public_ip, null)
}

output "alerts_topic_arn" {
  description = "SNS topic of the alerts."
  value       = module.alerting.topic_arn
}

output "hourly_resources" {
  description = "Resources with an hourly price that remain; outside sessions only the stopped instances remain, which have no instance-hour price."
  value = {
    nat_gateways  = module.network.nat_gateway_ids
    ssm_endpoints = module.network.ssm_endpoint_ids
    eks_cluster   = module.eks.cluster_name
    valkey        = try(module.valkey[0].primary_endpoint, null)
    broker_eip    = try(module.messaging[0].public_ip, null)
  }
}
