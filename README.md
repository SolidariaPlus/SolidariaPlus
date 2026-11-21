# SolidariaPlus

Solidaria is a project focused on the management of savings and credit cooperatives, assisting with tasks such as recording savings, granting credit, managing members, handling financial operations, and predicting fraud.

## Overview

SolidariaPlus is a polyglot monorepo orchestrated by Turborepo (`turbo.json`)
over pnpm workspaces (`pnpm-workspace.yaml`). Gradle (`settings.gradle.kts`), 
workspaces register the native services. The repository is currently a **scaffold**: every service, client,
contract, Terraform, and test directory contains only manifests, Dockerfiles,
or `.gitkeep` placeholders. The only source file with logic is
`commitlint.config.js`.

## Interface

### Root source

- `commitlint.config.js` (`module.exports`):
  - `extends: ["@commitlint/config-conventional"]`
  - Inline plugin rule `jira-key({ header, body })`. It returns `true` when the
    header or body matches `/\b[A-Z][A-Z0-9]+-\d+\b/`.
  - `rules`: `"jira-key": [2, "always"]`, `"header-max-length": [2, "always", 100]`.

### Root scripts (`package.json`)

| Script | Command |
| --- | --- |
| `build` | `turbo run build` |
| `dev` | `turbo run dev` |
| `lint` | `turbo run lint` |
| `test` | `turbo run test` |
| `clean` | `turbo run clean` |
| `prepare` | `lefthook install` |


### Subdirectories

- `scripts`: [./scripts/README.md](./scripts/README.md). Developer helpers;
  holds one empty placeholder script.
- The following directories hold no source code, so the bootstrap pruned them
  and they have no contract here:
  - `services/`: 17 microservice scaffolds (`MS-01` to `MS-17`) in seven
    bounded contexts (`d01-membership` to `d07-auth`). Each has a
    `Dockerfile` (base image only), a thin `package.json` whose
    `build`/`dev`/`lint`/`clean` scripts only `echo`, a stub `README.md`, a
    `CHANGELOG.md`, and an empty `src`/`cmd`/`internal`.
  - `apps/`: `web`, `mobile`, and `desktop`. Each holds `package.json` with
    `echo` scripts only.
  - `contracts/`: `openapi/` and `asyncapi/` (`.gitkeep` only).
  - `infra/`: `accounts.yml` (empty), `local/docker-compose.yml` (only
    `services:`), and `terraform/{bootstrap,envs/qa,envs/prod,modules}`
    (`.gitkeep` only).
  - `packages/` and `tests/{api,e2e,oracles}`: `.gitkeep` only.
  - `.github/workflows`: `ci.yml`, `deploy-qa.yml`, `infra.yml`,
    `promote-prod.yml`, and `release.yml` (CI configuration, not source).

## Invariants

- Every commit message must contain a Jira-style key matching
  `[A-Z][A-Z0-9]+-\d+` and have a header of at most 100 characters
  (`commitlint.config.js`, enforced by `lefthook.yml` `commit-msg` →
  `pnpm exec commitlint --edit {1}`).
- Every pushed branch except `main` must contain a key matching
  `[A-Z][A-Z0-9]+-[0-9]+` (`lefthook.yml` `pre-push.branch-jira-key`).
- Pull request titles must contain a key matching `[A-Z][A-Z0-9]+-[0-9]+`
  (`.github/workflows/ci.yml` job `pr-title`).
- The Turbo `test` task depends on `build` (`turbo.json`).
- Text files use LF line endings, except `*.bat`, `*.cmd`, and `*.ps1`, which
  use CRLF (`.gitattributes`).
- Java toolchain is 21 (`gradle/libs.versions.toml`, each `build.gradle.kts`).

## Side Effects

- `pnpm install` triggers `prepare` → `lefthook install`, which writes Git hooks
  into `.git/hooks`.
- `.github/workflows/release.yml` (push to `main`) runs
  `googleapis/release-please-action@v4` with `contents: write` and
  `pull-requests: write`. It opens or updates release PRs and creates tags.
- `.github/workflows/ci.yml` runs `pnpm install --frozen-lockfile` and
  `pnpm turbo run lint build test` on GitHub-hosted `ubuntu-latest` runners.
- `deploy-qa.yml`, `promote-prod.yml`, and `infra.yml` only `echo` TODO
  messages. They do not touch AWS yet, although they request
  `id-token: write`.

## Verification

- Run: `pnpm test` (alias of `turbo run test`).
- No workspace package defines a `test` script yet, so this command currently
  runs no tests. CI runs `pnpm turbo run lint build test`.
