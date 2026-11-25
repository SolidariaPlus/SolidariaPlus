data "aws_caller_identity" "current" {}

data "aws_partition" "current" {}

data "aws_region" "current" {}

locals {
  instances     = var.session_on ? 1 : 0
  lab_role_arn  = "arn:${data.aws_partition.current.partition}:iam::${data.aws_caller_identity.current.account_id}:role/LabRole"
  parameter_dir = "/solidaria/${var.environment}/${lower(var.slot)}/eks"
}

resource "aws_eks_cluster" "this" {
  count = local.instances

  name     = var.name
  version  = var.kubernetes_version
  role_arn = local.lab_role_arn

  vpc_config {
    subnet_ids              = var.subnet_ids
    endpoint_private_access = true
    endpoint_public_access  = false
  }

  access_config {
    authentication_mode                         = "API"
    bootstrap_cluster_creator_admin_permissions = true
  }

  enabled_cluster_log_types = ["api", "audit", "authenticator"]

  tags = {
    Name = var.name
  }
}

resource "aws_vpc_security_group_ingress_rule" "api_clients" {
  for_each = var.session_on ? var.api_client_security_group_ids : tomap({})

  security_group_id            = aws_eks_cluster.this[0].vpc_config[0].cluster_security_group_id
  description                  = "Private Kubernetes API from the runner and the bastion"
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
  referenced_security_group_id = each.value
}

# ADR-123 step 2. Re-runs whenever the cluster is recreated (every session-start).
resource "terraform_data" "ci_deployer" {
  count = local.instances

  triggers_replace = [aws_eks_cluster.this[0].id, aws_eks_cluster.this[0].created_at]

  provisioner "local-exec" {
    command     = "bash ${path.module}/scripts/bootstrap-ci-deployer.sh"
    interpreter = ["/bin/bash", "-c"]

    environment = {
      CLUSTER_NAME     = aws_eks_cluster.this[0].name
      AWS_REGION       = data.aws_region.current.region
      PARAMETER_DIR    = local.parameter_dir
      CREATOR_ARN      = local.lab_role_arn
      CLUSTER_ENDPOINT = aws_eks_cluster.this[0].endpoint
    }
  }

  depends_on = [aws_vpc_security_group_ingress_rule.api_clients]
}

resource "aws_launch_template" "nodes" {
  count = local.instances

  name_prefix            = "${var.name}-nodes-"
  update_default_version = true
  vpc_security_group_ids = [
    aws_eks_cluster.this[0].vpc_config[0].cluster_security_group_id,
    var.node_security_group_id,
  ]

  metadata_options {
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  block_device_mappings {
    device_name = "/dev/xvda"

    ebs {
      volume_type = "gp3"
      volume_size = var.node_volume_size
      encrypted   = true
    }
  }

  tag_specifications {
    resource_type = "instance"

    tags = {
      Name = "${var.name}-node"
    }
  }
}

# ADR-123 step 3: created only after the LabRole access entry is gone.
resource "aws_eks_node_group" "default" {
  count = local.instances

  cluster_name    = aws_eks_cluster.this[0].name
  node_group_name = "default"
  node_role_arn   = local.lab_role_arn
  subnet_ids      = var.subnet_ids
  instance_types  = var.node_instance_types
  ami_type        = "AL2023_x86_64_STANDARD"
  capacity_type   = "ON_DEMAND"

  scaling_config {
    desired_size = var.node_desired_size
    min_size     = var.node_min_size
    max_size     = var.node_max_size
  }

  update_config {
    max_unavailable = 1
  }

  launch_template {
    id      = aws_launch_template.nodes[0].id
    version = aws_launch_template.nodes[0].latest_version
  }

  labels = {
    environment = var.environment
    slot        = var.slot
  }

  tags = {
    "k8s.io/cluster-autoscaler/enabled"     = "true"
    "k8s.io/cluster-autoscaler/${var.name}" = "owned"
  }

  lifecycle {
    # The Cluster Autoscaler owns the desired size during a session.
    ignore_changes = [scaling_config[0].desired_size]
  }

  depends_on = [terraform_data.ci_deployer]
}

resource "aws_eks_addon" "this" {
  for_each = var.session_on ? tomap({
    # The VPC CNI enforces the NetworkPolicies of the shared chart (KAN-2 ADR-120).
    "vpc-cni"        = jsonencode({ enableNetworkPolicy = "true" })
    "kube-proxy"     = null
    "coredns"        = null
    "metrics-server" = null
  }) : tomap({})

  cluster_name                = aws_eks_cluster.this[0].name
  addon_name                  = each.key
  configuration_values        = each.value
  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  depends_on = [aws_eks_node_group.default]
}
