# modules/pki

## Overview

Certificate authority of one environment (KAN-2 ADR-088, ADR-122). It signs the broker certificate, the LDAPS certificate of the directory and, later, the mutual-TLS certificates between accounts.

## Interface

Inputs: `environment`, `validity_hours`. Outputs: `ca_cert_pem`, `ca_private_key_pem` (sensitive), `ca_certificate_parameter` (`/solidaria/<env>/pki/ca-certificate`).

## Invariants

- One CA per environment, in the state of A1; QA and Production never share it.
- The private key lives only in the Terraform state and in Secrets Manager.

## Side Effects

- One Secrets Manager secret (monthly price) and one advanced parameter.

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
