# scripts

## Overview

Developer helper scripts for the SolidariaPlus monorepo. KAN-11 §4.2 describes
this directory as "developer helpers (no credential handling)". It currently
holds one placeholder script with no content.

## Interface

- `refresh-aws-credentials.sh`: empty file (0 bytes). It defines no functions,
  flags, or commands yet.

## Invariants

- None. The directory contains no executable logic.
- Line endings: `.gitattributes` forces `*.sh text eol=lf`, so scripts must be
  committed with LF endings.

## Side Effects

- None. The script is empty.

## Verification

- No test target covers this directory. The repository-level `pnpm test`
  (`turbo run test`) does not run shell scripts.
- Run: `<test-command>`
