output "vpc_id" {
  description = "Identifier of the VPC."
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "Identifiers of the public subnets, one per Availability Zone."
  value       = aws_subnet.public[*].id
}

output "availability_zones" {
  description = "Availability Zones used by the subnets."
  value       = local.azs
}
