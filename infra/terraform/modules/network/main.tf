# Session-dependent network of one account (KAN-29; KAN-2 section 6.4, ADR-070, ADR-079, ADR-120).
# The VPC, internet gateway and public subnets come from the bootstrap (modules/vpc-base).
#
# Subnet plan inside the VPC CIDR (a /16):
#   public        /24 blocks 0-1   (modules/vpc-base)
#   private data  /24 blocks 10-11 Aurora, Valkey, domain controller, SSM endpoints; no internet route
#   private app   /20 blocks 1-2   EKS nodes and pods (VPC CNI gives each pod a VPC address), VPC link
#
# Hourly-priced resources, present only while session_on is true: NAT gateways with their
# Elastic IPs, and the SSM interface endpoints.

locals {
  az_count  = length(var.availability_zones)
  nat_count = var.session_on ? var.nat_gateway_count : 0
}

resource "aws_subnet" "private_app" {
  count = local.az_count

  vpc_id            = var.vpc_id
  availability_zone = var.availability_zones[count.index]
  cidr_block        = cidrsubnet(var.vpc_cidr, 4, count.index + 1)

  tags = {
    Name                                        = "${var.name}-app-${var.availability_zones[count.index]}"
    tier                                        = "private-app"
    "kubernetes.io/role/internal-elb"           = "1"
    "kubernetes.io/cluster/${var.cluster_name}" = "shared"
  }
}

resource "aws_subnet" "private_data" {
  count = local.az_count

  vpc_id            = var.vpc_id
  availability_zone = var.availability_zones[count.index]
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index + 10)

  tags = {
    Name = "${var.name}-data-${var.availability_zones[count.index]}"
    tier = "private-data"
  }
}

resource "aws_eip" "nat" {
  count = local.nat_count

  domain = "vpc"

  tags = {
    Name = "${var.name}-nat-${var.availability_zones[count.index]}"
  }
}

resource "aws_nat_gateway" "this" {
  count = local.nat_count

  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = var.public_subnet_ids[count.index]

  tags = {
    Name = "${var.name}-nat-${var.availability_zones[count.index]}"
  }
}

# One route table per zone, so that Production A2 can use the NAT gateway of its own zone.
resource "aws_route_table" "private_app" {
  count = local.az_count

  vpc_id = var.vpc_id

  tags = {
    Name = "${var.name}-app-${var.availability_zones[count.index]}"
  }
}

resource "aws_route" "private_app_internet" {
  count = local.nat_count > 0 ? local.az_count : 0

  route_table_id         = aws_route_table.private_app[count.index].id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.this[min(count.index, local.nat_count - 1)].id
}

resource "aws_route_table_association" "private_app" {
  count = local.az_count

  subnet_id      = aws_subnet.private_app[count.index].id
  route_table_id = aws_route_table.private_app[count.index].id
}

# The data tier never has an internet route (ADR-120).
resource "aws_route_table" "private_data" {
  vpc_id = var.vpc_id

  tags = {
    Name = "${var.name}-data"
  }
}

resource "aws_route_table_association" "private_data" {
  count = local.az_count

  subnet_id      = aws_subnet.private_data[count.index].id
  route_table_id = aws_route_table.private_data.id
}

# Gateway endpoints have no hourly price; they keep S3 and DynamoDB traffic off the NAT gateway.
resource "aws_vpc_endpoint" "gateway" {
  for_each = toset(["s3", "dynamodb"])

  vpc_id            = var.vpc_id
  service_name      = "com.amazonaws.${data.aws_region.current.region}.${each.key}"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = concat(aws_route_table.private_app[*].id, [aws_route_table.private_data.id])

  tags = {
    Name = "${var.name}-${each.key}"
  }
}

data "aws_region" "current" {}

resource "aws_security_group" "endpoints" {
  name        = "${var.name}-endpoints"
  description = "Interface endpoints: HTTPS from inside the VPC"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name}-endpoints"
  }
}

resource "aws_vpc_security_group_ingress_rule" "endpoints_https" {
  security_group_id = aws_security_group.endpoints.id
  description       = "HTTPS from the VPC"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = var.vpc_cidr
}

# Systems Manager endpoints for the domain controller in the data tier (ADR-120), in one zone
# only to halve their cost; present only during sessions.
resource "aws_vpc_endpoint" "ssm" {
  for_each = var.session_on && var.enable_ssm_endpoints ? toset(["ssm", "ssmmessages", "ec2messages"]) : toset([])

  vpc_id              = var.vpc_id
  service_name        = "com.amazonaws.${data.aws_region.current.region}.${each.key}"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = [aws_subnet.private_data[0].id]
  security_group_ids  = [aws_security_group.endpoints.id]
  private_dns_enabled = true

  tags = {
    Name = "${var.name}-${each.key}"
  }
}
