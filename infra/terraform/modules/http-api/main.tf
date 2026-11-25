# Amazon API Gateway HTTP API of one account (KAN-32; KAN-2 section 6.4, ADR-095, ADR-115).
# Routes of MS-17 (/auth/*) and, later, the GraphQL APIs (/api/*) go through a VPC link to the ALB
# that the AWS Load Balancer Controller creates from the Ingress of the shared Helm chart.
#
# The ALB is recreated in every session, so the integration exists only when the caller passes the
# listener ARN of the current ALB: session-start applies twice (ADR-088): the first pass creates
# the cluster and the Ingress, the second finds the ALB and wires the routes.
# The API itself, its stage and the VPC link have no hourly price and stay between sessions.
# The JWT authorizer of the MS-17 issuer is attached when MS-17 issues tokens (KAN-18).

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
  description = "Private application subnets for the VPC link."
  type        = list(string)
}

variable "alb_security_group_id" {
  description = "Security group of the VPC link, admitted by the ALB (it is the backend security group of the Ingress)."
  type        = string
}

variable "alb_listener_arn" {
  description = "HTTP listener of the current ALB; empty outside sessions or in the first pass of session-start."
  type        = string
  default     = ""
}

variable "route_keys" {
  description = "Routes sent to the ALB."
  type        = list(string)
  default     = ["ANY /auth/{proxy+}", "ANY /api/{proxy+}"]
}

variable "cors_allow_origins" {
  description = "Strict CORS allow-list. The web client is served from the same CloudFront domain and needs no entry."
  type        = list(string)
  default     = []
}

variable "throttling_rate_limit" {
  description = "Steady-state requests per second of the stage."
  type        = number
  default     = 200
}

variable "throttling_burst_limit" {
  description = "Burst of the stage."
  type        = number
  default     = 400
}

resource "aws_security_group" "vpc_link" {
  name        = "${var.name}-vpc-link"
  description = "VPC link of the HTTP API towards the ALB"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name}-vpc-link"
  }
}

resource "aws_vpc_security_group_egress_rule" "vpc_link_alb" {
  security_group_id            = aws_security_group.vpc_link.id
  description                  = "HTTP to the ALB"
  ip_protocol                  = "tcp"
  from_port                    = 80
  to_port                      = 80
  referenced_security_group_id = var.alb_security_group_id
}

resource "aws_vpc_security_group_ingress_rule" "alb_from_vpc_link" {
  security_group_id            = var.alb_security_group_id
  description                  = "HTTP from the VPC link of the HTTP API"
  ip_protocol                  = "tcp"
  from_port                    = 80
  to_port                      = 80
  referenced_security_group_id = aws_security_group.vpc_link.id
}

resource "aws_apigatewayv2_vpc_link" "this" {
  name               = var.name
  subnet_ids         = var.subnet_ids
  security_group_ids = [aws_security_group.vpc_link.id]
}

resource "aws_apigatewayv2_api" "this" {
  name          = var.name
  protocol_type = "HTTP"
  description   = "Client entry of ${var.name} (CloudFront > HTTP API > VPC link > ALB)"

  dynamic "cors_configuration" {
    for_each = length(var.cors_allow_origins) > 0 ? [1] : []

    content {
      allow_origins     = var.cors_allow_origins
      allow_methods     = ["GET", "POST", "OPTIONS"]
      allow_headers     = ["authorization", "content-type", "x-correlation-id"]
      allow_credentials = true
      max_age           = 600
    }
  }
}

resource "aws_cloudwatch_log_group" "access" {
  name              = "/solidaria/${var.name}/http-api"
  retention_in_days = 30
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.this.id
  name        = "$default"
  auto_deploy = true

  default_route_settings {
    throttling_rate_limit  = var.throttling_rate_limit
    throttling_burst_limit = var.throttling_burst_limit
  }

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.access.arn
    format = jsonencode({
      requestId = "$context.requestId"
      ip        = "$context.identity.sourceIp"
      method    = "$context.httpMethod"
      path      = "$context.path"
      status    = "$context.status"
      latency   = "$context.integrationLatency"
      error     = "$context.integrationErrorMessage"
    })
  }
}

resource "aws_apigatewayv2_integration" "alb" {
  count = var.alb_listener_arn == "" ? 0 : 1

  api_id                 = aws_apigatewayv2_api.this.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = var.alb_listener_arn
  connection_type        = "VPC_LINK"
  connection_id          = aws_apigatewayv2_vpc_link.this.id
  payload_format_version = "1.0"
  timeout_milliseconds   = 29000
}

resource "aws_apigatewayv2_route" "alb" {
  for_each = var.alb_listener_arn == "" ? toset([]) : toset(var.route_keys)

  api_id    = aws_apigatewayv2_api.this.id
  route_key = each.value
  target    = "integrations/${aws_apigatewayv2_integration.alb[0].id}"
}

output "api_endpoint" {
  description = "Invoke URL of the HTTP API (origin of CloudFront)."
  value       = aws_apigatewayv2_api.this.api_endpoint
}

output "api_id" {
  description = "HTTP API identifier."
  value       = aws_apigatewayv2_api.this.id
}

output "routes_wired" {
  description = "True when the routes point to an ALB."
  value       = var.alb_listener_arn != ""
}
