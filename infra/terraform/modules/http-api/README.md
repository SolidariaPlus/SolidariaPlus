# modules/http-api

## Overview

Amazon API Gateway HTTP API of one account (KAN-32): routes `/auth/*` and `/api/*` through a VPC link to the ALB of the Ingress.

## Interface

Inputs: `name`, `vpc_id`, `subnet_ids`, `alb_security_group_id`, `alb_listener_arn` (empty outside sessions and in the first pass of session-start), `route_keys`, `cors_allow_origins`, throttling. Outputs: `api_endpoint`, `api_id`, `routes_wired`.

## Invariants

- The routes exist only when an ALB listener is given: the second Terraform pass of session-start wires them (ADR-088).
- Strict CORS allow-list (empty by default, because the web client is same-origin through CloudFront); throttling on the default stage; access logs in CloudWatch Logs.
- The JWT authorizer of MS-17 is added with KAN-18.

## Side Effects

- No hourly price (requests only).

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
