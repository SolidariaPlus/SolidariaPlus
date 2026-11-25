output "private_app_subnet_ids" {
  description = "Private application subnets (EKS nodes, pods, VPC link), one per zone."
  value       = aws_subnet.private_app[*].id
}

output "private_data_subnet_ids" {
  description = "Private data subnets (Aurora, Valkey, domain controller), one per zone."
  value       = aws_subnet.private_data[*].id
}

output "nat_gateway_ids" {
  description = "NAT gateways currently present (empty outside sessions)."
  value       = aws_nat_gateway.this[*].id
}

output "ssm_endpoint_ids" {
  description = "Systems Manager interface endpoints currently present (empty outside sessions)."
  value       = [for endpoint in aws_vpc_endpoint.ssm : endpoint.id]
}
