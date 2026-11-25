# Active Directory domain controller of A1 (KAN-31; KAN-2 ADR-084, ADR-100, ADR-120).
# Windows Server 2022 Core, t3.small, LabInstanceProfile, private data subnet with no internet
# route. No RDP port: it is administered with Session Manager and the Fleet Manager remote desktop,
# through the Systems Manager interface endpoints that modules/network creates during sessions.
# A key pair (created here, private key in Parameter Store) is needed by Fleet Manager to fetch
# the Windows administrator password; the vockey key pair exists only in us-east-1.
#
# Bootstrap (SSM document + association): infra/ad/Install-Domain.ps1 promotes the server and
# installs the LDAPS certificate issued by the environment CA; infra/ad/New-StaffAccounts.ps1 loads
# the 30 synthetic staff accounts of infra/ad/staff.csv in 6 groups.
# Port 636 (LDAPS) accepts only the EKS nodes; a NetworkPolicy then limits it to MS-17.
# Session mode: the instance is stopped at session-stop and started at session-start.

terraform {
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

variable "name" {
  description = "Name prefix, for example solidaria-qa-a1."
  type        = string
}

variable "environment" {
  description = "Environment: qa or prod."
  type        = string
}

variable "session_on" {
  description = "True during a session (instance running)."
  type        = bool
}

variable "vpc_id" {
  description = "VPC of A1."
  type        = string
}

variable "subnet_id" {
  description = "Private data subnet of A1."
  type        = string
}

variable "ldaps_client_security_group_ids" {
  description = "Security groups allowed on LDAPS (636), by a static key: the EKS nodes of A1."
  type        = map(string)
}

variable "vpc_cidr" {
  description = "VPC CIDR (Kerberos, LDAP and DNS for the bastion during administration)."
  type        = string
}

variable "instance_type" {
  description = "Instance type (ADR-100: t3.small)."
  type        = string
  default     = "t3.small"
}

variable "domain_name" {
  description = "DNS name of the staff domain."
  type        = string
  default     = "ad.solidaria.internal"
}

variable "netbios_name" {
  description = "NetBIOS name of the staff domain."
  type        = string
  default     = "SOLIDARIA"
}

variable "ad_dir" {
  description = "Path of infra/ad."
  type        = string
}

variable "ca_cert_pem" {
  description = "Certificate of the environment CA (modules/pki)."
  type        = string
}

variable "ca_private_key_pem" {
  description = "Private key of the environment CA (modules/pki)."
  type        = string
  sensitive   = true
}

data "aws_region" "current" {}

data "aws_ssm_parameter" "windows" {
  name = "/aws/service/ami-windows-latest/Windows_Server-2022-English-Core-Base"
}

locals {
  prefix   = "/solidaria/${var.environment}/ad"
  dc_fqdn  = "dc01.${var.domain_name}"
  staff    = file("${var.ad_dir}/staff.csv")
  install  = file("${var.ad_dir}/Install-Domain.ps1")
  accounts = file("${var.ad_dir}/New-StaffAccounts.ps1")
}

# ---------------------------------------------------------------------------------------------
# Secrets (SecureString parameters: the data subnet reaches Systems Manager only)
# ---------------------------------------------------------------------------------------------
resource "random_password" "ad" {
  for_each = toset(["safe-mode-password", "staff-initial-password", "ldaps-pfx-password"])

  length           = 24
  special          = true
  override_special = "!#%*-_=+"
  min_upper        = 2
  min_lower        = 2
  min_numeric      = 2
  min_special      = 2
}

resource "aws_ssm_parameter" "ad_secret" {
  for_each = random_password.ad

  name  = "${local.prefix}/${each.key}"
  type  = "SecureString"
  value = each.value.result
}

resource "tls_private_key" "key_pair" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "dc" {
  key_name   = "${var.name}-dc"
  public_key = tls_private_key.key_pair.public_key_openssh
}

resource "aws_ssm_parameter" "key_pair" {
  name        = "${local.prefix}/key-pair-private-key"
  description = "Private key that Fleet Manager uses to decrypt the administrator password of the domain controller."
  type        = "SecureString"
  tier        = "Advanced"
  value       = tls_private_key.key_pair.private_key_pem
}

# ---------------------------------------------------------------------------------------------
# Instance
# ---------------------------------------------------------------------------------------------
resource "aws_security_group" "dc" {
  name        = "${var.name}-dc"
  description = "Domain controller: LDAPS from the EKS nodes; no RDP"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name}-dc"
  }
}

resource "aws_vpc_security_group_ingress_rule" "ldaps" {
  for_each = var.ldaps_client_security_group_ids

  security_group_id            = aws_security_group.dc.id
  description                  = "LDAPS from ${each.key}"
  ip_protocol                  = "tcp"
  from_port                    = 636
  to_port                      = 636
  referenced_security_group_id = each.value
}

resource "aws_vpc_security_group_egress_rule" "dc_https" {
  security_group_id = aws_security_group.dc.id
  description       = "Systems Manager interface endpoints"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = var.vpc_cidr
}

resource "aws_vpc_security_group_egress_rule" "dc_dns" {
  security_group_id = aws_security_group.dc.id
  description       = "Amazon DNS resolver (DNS forwarder of the domain)"
  ip_protocol       = "udp"
  from_port         = 53
  to_port           = 53
  cidr_ipv4         = var.vpc_cidr
}

resource "aws_instance" "dc" {
  ami                    = data.aws_ssm_parameter.windows.value
  instance_type          = var.instance_type
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [aws_security_group.dc.id]
  iam_instance_profile   = "LabInstanceProfile"
  key_name               = aws_key_pair.dc.key_name

  user_data = <<-EOT
    <powershell>
    Rename-Computer -NewName 'DC01' -Force -Restart
    </powershell>
  EOT

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 40
    encrypted   = true
  }

  tags = {
    Name = "${var.name}-dc"
    role = "domain-controller"
  }

  lifecycle {
    ignore_changes = [ami, user_data]
  }
}

resource "aws_ec2_instance_state" "dc" {
  instance_id = aws_instance.dc.id
  state       = var.session_on ? "running" : "stopped"
}

# ---------------------------------------------------------------------------------------------
# LDAPS certificate from the environment CA, for the DC name and its private address
# ---------------------------------------------------------------------------------------------
resource "tls_private_key" "ldaps" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "tls_cert_request" "ldaps" {
  private_key_pem = tls_private_key.ldaps.private_key_pem
  dns_names       = [local.dc_fqdn, var.domain_name]
  ip_addresses    = [aws_instance.dc.private_ip]

  subject {
    common_name  = local.dc_fqdn
    organization = "Solidaria+"
  }
}

resource "tls_locally_signed_cert" "ldaps" {
  cert_request_pem      = tls_cert_request.ldaps.cert_request_pem
  ca_private_key_pem    = var.ca_private_key_pem
  ca_cert_pem           = var.ca_cert_pem
  validity_period_hours = 8760
  early_renewal_hours   = 720

  allowed_uses = ["digital_signature", "key_encipherment", "server_auth"]
}

# Windows imports PKCS#12; openssl on the runner packs the PEM certificate and key.
resource "terraform_data" "ldaps_pfx" {
  triggers_replace = [tls_locally_signed_cert.ldaps.cert_pem]

  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-c"]
    command     = <<-EOT
      set -euo pipefail
      dir=$(mktemp -d)
      trap 'rm -rf "$dir"' EXIT
      printf '%s' "$CERT" > "$dir/cert.pem"
      printf '%s' "$KEY" > "$dir/key.pem"
      openssl pkcs12 -export -in "$dir/cert.pem" -inkey "$dir/key.pem" -out "$dir/ldaps.pfx" \
        -passout env:PFX_PASSWORD -keypbe AES-256-CBC -certpbe AES-256-CBC -macalg sha256
      aws ssm put-parameter --region "$REGION" --overwrite --type SecureString --tier Advanced \
        --name "$PARAMETER" --value "$(base64 -w0 "$dir/ldaps.pfx")" >/dev/null
    EOT

    environment = {
      CERT         = tls_locally_signed_cert.ldaps.cert_pem
      KEY          = tls_private_key.ldaps.private_key_pem
      PFX_PASSWORD = random_password.ad["ldaps-pfx-password"].result
      REGION       = data.aws_region.current.region
      PARAMETER    = "${local.prefix}/ldaps-pfx"
    }
  }
}

# ---------------------------------------------------------------------------------------------
# Bootstrap through Systems Manager
# ---------------------------------------------------------------------------------------------
resource "aws_ssm_document" "bootstrap" {
  name            = "${var.name}-ad-bootstrap"
  document_type   = "Command"
  document_format = "JSON"

  content = jsonencode({
    schemaVersion = "2.2"
    description   = "Promote the domain controller and load the synthetic staff directory (infra/ad)"
    mainSteps = [
      {
        action = "aws:runPowerShellScript"
        name   = "InstallDomain"
        inputs = {
          timeoutSeconds = "3600"
          runCommand = concat(
            ["New-Item -ItemType Directory -Force -Path C:\\solidaria | Out-Null", "@'"],
            split("\n", local.install),
            ["'@ | Set-Content -Path C:\\solidaria\\Install-Domain.ps1 -Encoding UTF8"],
            ["& C:\\solidaria\\Install-Domain.ps1 -DomainName '${var.domain_name}' -NetbiosName '${var.netbios_name}' -SecretPrefix '${local.prefix}' -Region '${data.aws_region.current.region}'", "exit $LASTEXITCODE"],
          )
        }
      },
      {
        action = "aws:runPowerShellScript"
        name   = "StaffAccounts"
        inputs = {
          timeoutSeconds = "1800"
          runCommand = concat(
            ["@'"], split("\n", local.staff), ["'@ | Set-Content -Path C:\\solidaria\\staff.csv -Encoding UTF8"],
            ["@'"], split("\n", local.accounts), ["'@ | Set-Content -Path C:\\solidaria\\New-StaffAccounts.ps1 -Encoding UTF8"],
            ["& C:\\solidaria\\New-StaffAccounts.ps1 -CsvPath C:\\solidaria\\staff.csv -SecretPrefix '${local.prefix}' -Region '${data.aws_region.current.region}'"],
          )
        }
      },
    ]
  })
}

resource "aws_ssm_association" "bootstrap" {
  count = var.session_on ? 1 : 0

  name             = aws_ssm_document.bootstrap.name
  association_name = "${var.name}-ad-bootstrap"

  targets {
    key    = "InstanceIds"
    values = [aws_instance.dc.id]
  }

  depends_on = [aws_ec2_instance_state.dc, terraform_data.ldaps_pfx, aws_ssm_parameter.ad_secret]
}

output "instance_id" {
  description = "Domain controller instance."
  value       = aws_instance.dc.id
}

output "private_ip" {
  description = "Private address of the domain controller (LDAPS endpoint of MS-17)."
  value       = aws_instance.dc.private_ip
}

output "ldaps_url" {
  description = "LDAPS URL for MS-17. The private address is in the certificate, so no DNS record is needed."
  value       = "ldaps://${aws_instance.dc.private_ip}:636"
}

output "domain_name" {
  description = "DNS name of the staff domain."
  value       = var.domain_name
}
