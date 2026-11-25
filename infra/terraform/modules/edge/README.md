# modules/edge

## Overview

Client entry of the environment in A1 (KAN-32; KAN-2 §4.2, §6.4, ADR-115): private S3 bucket of the web client behind CloudFront with origin access control, and `/auth/*` and `/api/*` to the HTTP API.

## Interface

Inputs: `name`, `environment`, `api_endpoint`, `price_class`. Outputs: `distribution_id`, `domain_name`, `web_bucket`, `origin_verify_secret_name`. Parameters: `/solidaria/<env>/edge/{web-bucket,distribution-id,domain-name}`.

## Invariants

- CloudFront sends the secret header `X-Origin-Verify`; the ALB rule of the shared chart accepts only requests that carry it.
- Single-page application routing: 403 and 404 return `index.html`.
- Cloudflare goes in front once the domain is delegated (OI-19).

## Side Effects

- No hourly price (requests and transfer only).

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
