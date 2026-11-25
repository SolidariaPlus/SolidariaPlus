# Self-hosted GitHub Actions runner of the account (KAN-2 ADR-077, KAN-11 section 5.4).
# - LabInstanceProfile: the instance role gives LabRole credentials that AWS rotates, so no AWS
#   credential is stored in GitHub (GitHub OIDC is not possible: the lab cannot create roles).
# - Public subnet with a public IP and no inbound rule: it reaches GitHub without a NAT gateway,
#   so it can run session-start and session-stop of its own account.
# - Registered with the label <env>-<slot> (for example qa-A1). The registration token is passed
#   once at apply, kept in Parameter Store only until the instance has registered, and deleted
#   by the instance itself.

data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

locals {
  registration_parameter = "/solidaria/${var.environment}/${local.slot_lower}/runner/registration-token"
}

resource "aws_ssm_parameter" "registration_token" {
  count = var.runner_registration_token == null ? 0 : 1

  name        = local.registration_parameter
  description = "One-time GitHub registration token of runner ${local.runner_label}; deleted by the runner after it registers."
  type        = "SecureString"
  value       = var.runner_registration_token
}

resource "aws_security_group" "runner" {
  name        = "${local.name}-runner"
  description = "Self-hosted GitHub Actions runner: no inbound rule"
  vpc_id      = module.vpc_base.vpc_id

  tags = {
    Name = "${local.name}-runner"
  }
}

# The runner reaches GitHub, the AWS APIs and public registries without a NAT gateway.
#trivy:ignore:AVD-AWS-0104
resource "aws_vpc_security_group_egress_rule" "runner_all" {
  security_group_id = aws_security_group.runner.id
  description       = "Outbound to GitHub, AWS APIs, container registries and the private EKS endpoint"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_instance" "runner" {
  ami                         = data.aws_ssm_parameter.al2023.value
  instance_type               = var.runner_instance_type
  subnet_id                   = module.vpc_base.public_subnet_ids[0]
  vpc_security_group_ids      = [aws_security_group.runner.id]
  associate_public_ip_address = true
  iam_instance_profile        = "LabInstanceProfile"

  user_data = templatefile("${path.module}/templates/runner-user-data.sh.tftpl", {
    region                 = var.region
    repository             = var.github_repository
    runner_version         = var.runner_version
    runner_name            = local.name
    runner_label           = local.runner_label
    registration_parameter = local.registration_parameter
    terraform_version      = var.tool_versions.terraform
    helm_version           = var.tool_versions.helm
    kubectl_version        = var.tool_versions.kubectl
  })
  user_data_replace_on_change = false

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = var.runner_volume_size
    encrypted   = true
  }

  tags = {
    Name = "${local.name}-runner"
    role = "github-runner"
  }

  lifecycle {
    # A newer AMI or tool version must not replace a registered runner. Replace it on purpose
    # with: terraform apply -replace=aws_instance.runner -var runner_registration_token=<new token>
    ignore_changes = [ami, user_data]
  }

  depends_on = [aws_ssm_parameter.registration_token]
}
