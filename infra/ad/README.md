# infra/ad

## Overview

Bootstrap of the staff directory: the Active Directory domain controller of account A1 in each
environment (KAN-31; KAN-2 ADR-084, ADR-100). `infra/terraform/modules/directory` creates the
Windows Server 2022 Core instance and an SSM document that runs these scripts through Systems Manager.
No RDP port is opened.

| File                    | Content                                                                                                                                       |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `Install-Domain.ps1`    | Promotes the server to a new forest (`ad.solidaria.internal`, NetBIOS `SOLIDARIA`), then installs the LDAPS certificate of the environment CA |
| `New-StaffAccounts.ps1` | Creates the OU `Staff`, the 6 groups and the 30 accounts of `staff.csv`                                                                       |
| `staff.csv`             | 30 **synthetic** staff accounts (fictitious names) in 6 groups                                                                                |

| Group                     | MS-17 role (ADR-084)                | Accounts |
| ------------------------- | ----------------------------------- | -------- |
| `sp-tellers`              | Teller                              | 12       |
| `sp-board`                | Board of Directors                  | 6        |
| `sp-board-president`      | Board, President function           | 1        |
| `sp-compliance-delegate`  | Board, Compliance delegate function | 1        |
| `sp-accountants`          | Accountant                          | 5        |
| `sp-external-consultants` | External Consultant (read-only)     | 5        |

## Interface

- SecureString parameters under `/solidaria/<env>/ad/`: `safe-mode-password`, `staff-initial-password`,
  `ldaps-pfx` and `ldaps-pfx-password` (written by Terraform), and `key-pair-private-key` (Fleet Manager).
- MS-17 reads `/solidaria/<env>/identity-access-service/ldap/url` (`ldaps://<private address>:636`) and
  trusts `/solidaria/<env>/pki/ca-certificate`.
- To administer: Systems Manager > Fleet Manager > the instance `solidaria-<env>-a1-dc` > Remote Desktop,
  with the key pair private key, or `aws ssm start-session --target <instance id>` for PowerShell.

## Invariants

- Synthetic data only (RNF-PRIV-04, principle E-05): no real person appears in `staff.csv`.
- Both scripts are idempotent. A second run keeps existing objects and refreshes the LDAPS certificate.
- Secrets come from Parameter Store, because the data subnet reaches only the Systems Manager endpoints
  (ADR-120). They are never written in this directory.
- PowerShell files use CRLF line endings (`.gitattributes`).

## Side Effects

- The promotion reboots the instance once. Systems Manager resumes the document after the reboot
  (exit code 3010).
- Creates directory objects in the domain.

## Verification

- In QA-A1 during a session, a test client authenticates over LDAPS. From the bastion:
  `ldapwhoami -H ldaps://<dc private address> -x -D 'ana.prue01@ad.solidaria.internal' -W`, with
  `LDAPTLS_CACERT` set to the environment CA.
- `Get-ADGroupMember sp-tellers | Measure-Object` returns 12, through Session Manager.
