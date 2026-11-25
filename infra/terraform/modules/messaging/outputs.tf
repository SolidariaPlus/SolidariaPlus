output "instance_id" {
  description = "Broker instance."
  value       = aws_instance.broker.id
}

output "security_group_id" {
  description = "Security group of the broker."
  value       = aws_security_group.broker.id
}

output "private_ip" {
  description = "Private address of the broker (stable across stops)."
  value       = aws_instance.broker.private_ip
}

output "public_ip" {
  description = "Elastic IP of the current session (null outside sessions)."
  value       = one(aws_eip.broker[*].public_ip)
}

output "kafka_user_secret_arns" {
  description = "Secrets Manager ARN of the SCRAM password of each Kafka user."
  value       = { for user, secret in aws_secretsmanager_secret.kafka_user : user => secret.arn }
}

output "parameter_prefix" {
  description = "Parameter Store prefix of the broker endpoints."
  value       = local.prefix
}
