# scripts

## Overview

Developer helper scripts for the SolidariaPlus monorepo. KAN-11 §4.2 describes this directory as
"developer helpers (no credential handling)": no script stores, copies or refreshes AWS credentials.
They run with the credentials of the Learner Lab session that the developer already has in the shell.

## Interface

| Script       | Use                                                                                                                      |
| ------------ | ------------------------------------------------------------------------------------------------------------------------ |
| `bastion.sh` | `bastion.sh on\|off\|status <qa\|prod> <A1..A5>`: turns the bastion of an account on or off (KAN-11 §5.6, KAN-2 ADR-096) |

## Invariants

- No secret, account identifier or credential is written in a script or read from a file.
- Line endings: `.gitattributes` forces `*.sh text eol=lf`, so scripts must be committed with LF endings.
- Every script checks the account and region with `aws sts get-caller-identity` before it changes anything.

## Side Effects

- `bastion.sh on` sets the desired capacity of the bastion Auto Scaling group of the account to 1, and
  `off` sets it back to 0. A running bastion costs about USD 0.005 per hour; `session-stop` also turns
  it off.

## Verification

- `shellcheck scripts/*.sh`.
- `scripts/bastion.sh status qa A1` prints the desired capacity and the instance, without changes.
