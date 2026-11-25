# Bastion of one account (KAN-30; KAN-11 section 5.6; KAN-2 ADR-096, ADR-120): a t3.nano
# instance in an Auto Scaling group whose desired capacity Terraform leaves at zero. No inbound
# rule, no SSH key, no public address. It is reached only with AWS Systems Manager Session Manager
# (shell or port forwarding to Aurora, Valkey and the private EKS endpoint), and Session Manager
# logs every session to CloudWatch Logs. scripts/bastion.sh turns it on and off;
# session-stop turns it off if it was left on.

terraform {
  required_version = ">= 1.11"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

variable "name" {
  description = "Name prefix, for example solidaria-qa-a1."
  type        = string
}

variable "vpc_id" {
  description = "VPC of the account."
  type        = string
}

variable "subnet_ids" {
  description = "Private application subnets (egress through the NAT gateway during sessions)."
  type        = list(string)
}

variable "instance_type" {
  description = "Instance type (ADR-096: t3.nano)."
  type        = string
  default     = "t3.nano"
}

variable "kubectl_version" {
  description = "kubectl installed on the bastion; keep it equal to .tool-versions."
  type        = string
  default     = "1.37.2"
}

variable "log_retention_days" {
  description = "Retention of the Session Manager logs."
  type        = number
  default     = 90
}

data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

resource "aws_security_group" "bastion" {
  name        = "${var.name}-bastion"
  description = "Bastion: no inbound rule, reached with Session Manager"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name}-bastion"
  }
}

# Systems Manager, Aurora, Valkey and the EKS API; the bastion has no inbound rule.
#trivy:ignore:AVD-AWS-0104
resource "aws_vpc_security_group_egress_rule" "bastion_all" {
  security_group_id = aws_security_group.bastion.id
  description       = "Systems Manager, Aurora, Valkey, EKS API"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_launch_template" "bastion" {
  name_prefix            = "${var.name}-bastion-"
  image_id               = data.aws_ssm_parameter.al2023.value
  instance_type          = var.instance_type
  update_default_version = true

  iam_instance_profile {
    name = "LabInstanceProfile"
  }

  network_interfaces {
    associate_public_ip_address = false
    security_groups             = [aws_security_group.bastion.id]
  }

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  user_data = base64encode(<<-EOT
    #!/bin/bash
    set -euo pipefail
    dnf -y install postgresql16 jq
    curl -fsSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/v${var.kubectl_version}/bin/linux/amd64/kubectl"
    chmod 0755 /usr/local/bin/kubectl
  EOT
  )

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "${var.name}-bastion"
      role = "bastion"
    }
  }
}

resource "aws_autoscaling_group" "bastion" {
  name                = "${var.name}-bastion"
  min_size            = 0
  max_size            = 1
  desired_capacity    = 0
  vpc_zone_identifier = var.subnet_ids
  health_check_type   = "EC2"

  launch_template {
    id      = aws_launch_template.bastion.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "${var.name}-bastion"
    propagate_at_launch = true
  }

  lifecycle {
    # scripts/bastion.sh changes the desired capacity during an administration session.
    ignore_changes = [desired_capacity]
  }
}

resource "aws_cloudwatch_log_group" "sessions" {
  name              = "/solidaria/${var.name}/session-manager"
  retention_in_days = var.log_retention_days
}

# Account-wide Session Manager preferences: log every session to CloudWatch Logs.
resource "aws_ssm_document" "session_preferences" {
  name            = "SSM-SessionManagerRunShell"
  document_type   = "Session"
  document_format = "JSON"

  content = jsonencode({
    schemaVersion = "1.0"
    description   = "Session Manager preferences of ${var.name}"
    sessionType   = "Standard_Stream"
    inputs = {
      cloudWatchLogGroupName      = aws_cloudwatch_log_group.sessions.name
      cloudWatchEncryptionEnabled = false
      cloudWatchStreamingEnabled  = true
      idleSessionTimeout          = "20"
      runAsEnabled                = false
      shellProfile = {
        linux   = "cd ~ && bash"
        windows = ""
      }
    }
  })
}

output "security_group_id" {
  description = "Security group of the bastion (admitted by Aurora, Valkey and the EKS API)."
  value       = aws_security_group.bastion.id
}

output "autoscaling_group_name" {
  description = "Auto Scaling group that scripts/bastion.sh scales between 0 and 1."
  value       = aws_autoscaling_group.bastion.name
}
