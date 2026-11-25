# Certificate authority of one environment (KAN-2 ADR-088, ADR-122). It signs the broker
# certificate and, later, the mutual-TLS certificates of the gRPC calls between accounts.
# It lives in the state of A1. The certificate is published in Parameter Store so that clients
# can trust it, and the private key is kept in AWS Secrets Manager of A1.

terraform {
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

variable "environment" {
  description = "Environment: qa or prod."
  type        = string
}

variable "validity_hours" {
  description = "Validity of the CA certificate (two years by default)."
  type        = number
  default     = 17520
}

resource "tls_private_key" "ca" {
  algorithm   = "ECDSA"
  ecdsa_curve = "P256"
}

resource "tls_self_signed_cert" "ca" {
  private_key_pem       = tls_private_key.ca.private_key_pem
  is_ca_certificate     = true
  validity_period_hours = var.validity_hours
  early_renewal_hours   = 720

  subject {
    common_name  = "Solidaria+ ${var.environment} CA"
    organization = "Solidaria+"
  }

  allowed_uses = ["cert_signing", "crl_signing", "digital_signature"]
}

resource "aws_ssm_parameter" "ca_certificate" {
  name        = "/solidaria/${var.environment}/pki/ca-certificate"
  description = "Certificate of the ${var.environment} certificate authority (PEM)."
  type        = "String"
  tier        = "Advanced"
  value       = tls_self_signed_cert.ca.cert_pem
}

resource "aws_secretsmanager_secret" "ca_key" {
  name                    = "/solidaria/${var.environment}/pki/ca-key"
  description             = "Private key of the ${var.environment} certificate authority (PEM)."
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "ca_key" {
  secret_id     = aws_secretsmanager_secret.ca_key.id
  secret_string = tls_private_key.ca.private_key_pem
}

output "ca_cert_pem" {
  description = "CA certificate (PEM)."
  value       = tls_self_signed_cert.ca.cert_pem
}

output "ca_private_key_pem" {
  description = "CA private key (PEM), used to sign the broker certificate."
  value       = tls_private_key.ca.private_key_pem
  sensitive   = true
}

output "ca_certificate_parameter" {
  description = "Parameter Store name of the CA certificate."
  value       = aws_ssm_parameter.ca_certificate.name
}
