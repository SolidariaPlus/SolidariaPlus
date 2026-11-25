# modules/network

## Overview

Session-dependent network of one account (KAN-29; KAN-2 §6.4, ADR-070, ADR-079, ADR-120): private application subnets (/20, EKS nodes and pods), private data subnets (/24, no internet route), NAT gateways and the Systems Manager interface endpoints. The VPC and the public subnets come from `modules/vpc-base` (bootstrap).

## Interface

Inputs: `name`, `vpc_id`, `vpc_cidr`, `availability_zones`, `public_subnet_ids`, `session_on`, `nat_gateway_count` (0 to 2), `enable_ssm_endpoints` (A1), `cluster_name`. Outputs: `private_app_subnet_ids`, `private_data_subnet_ids`, `nat_gateway_ids`, `ssm_endpoint_ids`.

## Invariants

- NAT gateways, their Elastic IPs and the interface endpoints exist only while `session_on` is true.
- The data tier never has a route to the internet; the S3 and DynamoDB gateway endpoints (no hourly price) serve every tier.
- Subnets carry the discovery tags of the AWS Load Balancer Controller and EKS.

## Side Effects

- During a session: one NAT gateway (about USD 0.045 per hour plus data) and, in A1, three interface endpoints in one zone.

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
