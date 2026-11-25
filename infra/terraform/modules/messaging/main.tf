# Broker instance of A1: Apache Kafka (KRaft) and RabbitMQ in containers on one EC2 instance
# (KAN-31; KAN-2 ADR-122, which replaces Amazon MSK and Amazon MQ of ADR-090, denied by the
# Learner Lab). t3.medium, Amazon Linux 2023, LabInstanceProfile, 20 GB gp3, automatic recovery,
# public subnet with an Elastic IP.
#
# Session mode (ADR-080, ADR-122):
#   session_on = true   allocate a new Elastic IP, issue a certificate for the private address and
#                       that IP, write them to Parameter Store and Secrets Manager, start the
#                       instance. Its boot script (infra/messaging/scripts/broker-start.sh) reads
#                       them, starts both brokers and applies the definitions.
#   session_on = false  stop the instance and release the Elastic IP. The volume keeps topics,
#                       consumer offsets and queues; nothing is drained or deleted.
#
# Listeners: Kafka SASL_SSL with SCRAM on 9092 (private address, VPC clients) and 9094 (Elastic
# IP, other accounts and Lambda); RabbitMQ AMQPS on 5671; management (15672) and metrics (9419 is
# not used, RabbitMQ Prometheus 15692) only from the VPC.

data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

locals {
  prefix       = "/solidaria/${var.environment}/broker"
  kafka_users  = [for line in split("\n", file("${var.messaging_dir}/kafka/users.conf")) : trimspace(line) if trimspace(line) != "" && !startswith(trimspace(line), "#")]
  upload_files = fileset(var.messaging_dir, "**")
}

# ---------------------------------------------------------------------------------------------
# Definitions and scripts, uploaded from infra/messaging; the instance syncs them at every boot.
# ---------------------------------------------------------------------------------------------
resource "aws_s3_bucket" "artifacts" {
  bucket        = "${var.name}-broker-${data.aws_caller_identity.current.account_id}"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_object" "messaging" {
  for_each = local.upload_files

  bucket      = aws_s3_bucket.artifacts.id
  key         = "messaging/${each.value}"
  source      = "${var.messaging_dir}/${each.value}"
  source_hash = filemd5("${var.messaging_dir}/${each.value}")
}

# ---------------------------------------------------------------------------------------------
# Credentials: generated here, kept in AWS Secrets Manager, never written in the repository.
# Alphanumeric, so they need no escaping in Kafka or RabbitMQ configuration.
# ---------------------------------------------------------------------------------------------
resource "random_password" "broker" {
  for_each = toset(["kafka-admin-password", "rabbitmq-admin-password", "keystore-password"])

  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "broker" {
  for_each = random_password.broker

  name                    = "${local.prefix}/${each.key}"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "broker" {
  for_each = random_password.broker

  secret_id     = aws_secretsmanager_secret.broker[each.key].id
  secret_string = each.value.result
}

resource "random_password" "kafka_user" {
  for_each = toset(local.kafka_users)

  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "kafka_user" {
  for_each = random_password.kafka_user

  name                    = "/solidaria/${var.environment}/kafka/users/${each.key}"
  description             = "SCRAM-SHA-512 password of the Kafka user ${each.key}."
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "kafka_user" {
  for_each = random_password.kafka_user

  secret_id     = aws_secretsmanager_secret.kafka_user[each.key].id
  secret_string = each.value.result
}

# ---------------------------------------------------------------------------------------------
# Network
# ---------------------------------------------------------------------------------------------
resource "aws_security_group" "broker" {
  name        = "${var.name}-broker"
  description = "Kafka and RabbitMQ broker instance (ADR-122)"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name}-broker"
  }
}

resource "aws_vpc_security_group_ingress_rule" "kafka_private" {
  for_each = var.client_security_group_ids

  security_group_id            = aws_security_group.broker.id
  description                  = "Kafka SASL_SSL, private listener, from ${each.key}"
  ip_protocol                  = "tcp"
  from_port                    = 9092
  to_port                      = 9092
  referenced_security_group_id = each.value
}

resource "aws_vpc_security_group_ingress_rule" "amqps_private" {
  for_each = var.client_security_group_ids

  security_group_id            = aws_security_group.broker.id
  description                  = "RabbitMQ AMQPS from ${each.key}"
  ip_protocol                  = "tcp"
  from_port                    = 5671
  to_port                      = 5671
  referenced_security_group_id = each.value
}

resource "aws_vpc_security_group_ingress_rule" "kafka_public" {
  for_each = toset(var.public_client_cidrs)

  security_group_id = aws_security_group.broker.id
  description       = "Kafka SASL_SSL, public listener (Elastic IP)"
  ip_protocol       = "tcp"
  from_port         = 9094
  to_port           = 9094
  cidr_ipv4         = each.value
}

resource "aws_vpc_security_group_ingress_rule" "amqps_public" {
  for_each = toset(var.public_client_cidrs)

  security_group_id = aws_security_group.broker.id
  description       = "RabbitMQ AMQPS, public (Elastic IP)"
  ip_protocol       = "tcp"
  from_port         = 5671
  to_port           = 5671
  cidr_ipv4         = each.value
}

resource "aws_vpc_security_group_ingress_rule" "management" {
  for_each = toset(["15672", "15692"])

  security_group_id = aws_security_group.broker.id
  description       = "RabbitMQ management and metrics from the VPC only"
  ip_protocol       = "tcp"
  from_port         = tonumber(each.key)
  to_port           = tonumber(each.key)
  cidr_ipv4         = var.vpc_cidr
}

resource "aws_vpc_security_group_egress_rule" "broker_all" {
  security_group_id = aws_security_group.broker.id
  description       = "Image pulls, AWS APIs"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

# ---------------------------------------------------------------------------------------------
# Instance
# ---------------------------------------------------------------------------------------------
resource "aws_instance" "broker" {
  ami                    = data.aws_ssm_parameter.al2023.value
  instance_type          = var.instance_type
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [aws_security_group.broker.id]
  iam_instance_profile   = "LabInstanceProfile"

  user_data = templatefile("${path.module}/templates/broker-user-data.sh.tftpl", {
    environment     = var.environment
    region          = data.aws_region.current.region
    artifact_bucket = aws_s3_bucket.artifacts.id
    kafka_image     = var.kafka_image
    rabbitmq_image  = var.rabbitmq_image
    jwks_url        = var.rabbitmq_jwks_url
  })
  user_data_replace_on_change = false

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  maintenance_options {
    auto_recovery = "default"
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.volume_size
    encrypted             = true
    delete_on_termination = false
  }

  tags = {
    Name = "${var.name}-broker"
    role = "broker"
  }

  lifecycle {
    # A replacement would lose the volume that holds topics and queues.
    ignore_changes = [ami, user_data]
  }

  depends_on = [aws_s3_object.messaging, aws_secretsmanager_secret_version.broker, aws_secretsmanager_secret_version.kafka_user]
}

resource "aws_eip" "broker" {
  count = var.session_on ? 1 : 0

  domain = "vpc"

  tags = {
    Name = "${var.name}-broker"
  }
}

resource "aws_eip_association" "broker" {
  count = var.session_on ? 1 : 0

  instance_id   = aws_instance.broker.id
  allocation_id = aws_eip.broker[0].id
}

# Server certificate for the private address and the Elastic IP of this session.
resource "tls_private_key" "broker" {
  count = var.session_on ? 1 : 0

  algorithm   = "ECDSA"
  ecdsa_curve = "P256"
}

resource "tls_cert_request" "broker" {
  count = var.session_on ? 1 : 0

  private_key_pem = tls_private_key.broker[0].private_key_pem
  ip_addresses    = [aws_instance.broker.private_ip, aws_eip.broker[0].public_ip]
  dns_names       = ["broker.${var.environment}.solidaria.internal"]

  subject {
    common_name  = "broker.${var.environment}.solidaria.internal"
    organization = "Solidaria+"
  }
}

resource "tls_locally_signed_cert" "broker" {
  count = var.session_on ? 1 : 0

  cert_request_pem      = tls_cert_request.broker[0].cert_request_pem
  ca_private_key_pem    = var.ca_private_key_pem
  ca_cert_pem           = var.ca_cert_pem
  validity_period_hours = 720

  allowed_uses = ["digital_signature", "key_encipherment", "server_auth"]
}

resource "aws_ssm_parameter" "certificate" {
  name  = "${local.prefix}/certificate"
  type  = "String"
  tier  = "Advanced"
  value = var.session_on ? tls_locally_signed_cert.broker[0].cert_pem : "none"
}

resource "aws_secretsmanager_secret" "private_key" {
  name                    = "${local.prefix}/private-key"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "private_key" {
  count = var.session_on ? 1 : 0

  secret_id     = aws_secretsmanager_secret.private_key.id
  secret_string = tls_private_key.broker[0].private_key_pem_pkcs8
}

resource "aws_ssm_parameter" "public_address" {
  name        = "${local.prefix}/public-address"
  description = "Elastic IP of the broker during the current session; none outside sessions."
  type        = "String"
  value       = var.session_on ? aws_eip.broker[0].public_ip : "none"
}

resource "aws_ssm_parameter" "endpoints" {
  for_each = {
    "private-address"          = aws_instance.broker.private_ip
    "kafka-bootstrap-internal" = "${aws_instance.broker.private_ip}:9092"
    "kafka-bootstrap-external" = var.session_on ? "${aws_eip.broker[0].public_ip}:9094" : "none"
    "amqps-endpoint-internal"  = "amqps://${aws_instance.broker.private_ip}:5671/solidaria"
    "amqps-endpoint-external"  = var.session_on ? "amqps://${aws_eip.broker[0].public_ip}:5671/solidaria" : "none"
  }

  name  = "${local.prefix}/${each.key}"
  type  = "String"
  value = each.value
}

resource "aws_ec2_instance_state" "broker" {
  instance_id = aws_instance.broker.id
  state       = var.session_on ? "running" : "stopped"

  depends_on = [
    aws_eip_association.broker,
    aws_ssm_parameter.certificate,
    aws_ssm_parameter.public_address,
    aws_secretsmanager_secret_version.private_key,
  ]
}
