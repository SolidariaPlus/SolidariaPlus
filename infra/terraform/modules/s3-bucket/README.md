# modules/s3-bucket

## Overview

S3 bucket owned by one service or by the platform (KAN-30; KAN-2 §6.5): private, encrypted, versioned by default, optional Object Lock.

## Interface

Inputs: `name`, `versioning`, `object_lock_mode` (GOVERNANCE in QA, COMPLIANCE in Production), `object_lock_days`. Outputs: `id`, `arn`, `regional_domain_name`.

The bucket itself is created once with the CLI (Learner Lab SCP, see `main.tf`); this module manages only its settings. Empty it before a destroy: `aws s3 rm s3://<name> --recursive`.

## Invariants

- Public access is blocked and object ownership is enforced.
- Object Lock forces versioning.

## Side Effects

- None beyond the bucket.

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
