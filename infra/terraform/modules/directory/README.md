# modules/directory

## Overview

Active Directory domain controller of A1 (KAN-31; KAN-2 ADR-084, ADR-100, ADR-120): Windows Server 2022 Core on a t3.small in the private data subnet, bootstrapped by `infra/ad` through Systems Manager.

## Interface

Inputs: `name`, `environment`, `session_on`, `vpc_id`, `vpc_cidr`, `subnet_id`, `ldaps_client_security_group_ids`, `instance_type`, `domain_name`, `netbios_name`, `ad_dir`, and the CA. Outputs: `instance_id`, `private_ip`, `ldaps_url`, `domain_name`.

## Invariants

- No RDP port: administration with Session Manager and the Fleet Manager remote desktop (the key pair private key is in `/solidaria/<env>/ad/key-pair-private-key`).
- LDAPS (636) admits only the nodes of A1; a NetworkPolicy limits it further to MS-17.
- Secrets are SecureString parameters, because the data subnet reaches only the Systems Manager endpoints.
- The LDAPS certificate is issued by the environment CA for the DC name and its private address.

## Side Effects

- The SSM association promotes the server (one reboot) and loads the 30 synthetic accounts. The instance is stopped at session-stop.

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
