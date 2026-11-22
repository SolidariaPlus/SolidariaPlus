# SolidariaPlus

Solidaria is a project focused on the management of savings and credit cooperatives, assisting with tasks such as recording savings, granting credit, managing members, handling financial operations, and predicting fraud.

## Monorepo layout

| Path                               | Contents                                                                  | Build tool              |
| ---------------------------------- | ------------------------------------------------------------------------- | ----------------------- |
| `apps/web`                         | Web client — React + Vite                                                 | pnpm + Turborepo        |
| `apps/mobile`                      | Mobile client — Expo (React Native)                                       | pnpm + Turborepo        |
| `apps/desktop`                     | Desktop client — Electron + electron-vite                                 | pnpm + Turborepo        |
| `packages/*`                       | Shared TS packages: `tsconfig`, `eslint-config`, `ui`                     | pnpm + Turborepo        |
| `services/dNN-<context>/<service>` | 17 Java 21 / Spring Boot 3 microservices (MS-01 … MS-17)                  | Gradle                  |
| `libs/*`                           | Shared Java libraries (auto-included when they have a `build.gradle.kts`) | Gradle                  |
| `build-logic/`                     | Gradle convention plugins                                                 | Gradle (included build) |
| `contracts/`                       | OpenAPI / AsyncAPI / GraphQL / Proto contracts                            | —                       |
| `infra/`, `deploy/`                | Terraform, Helm, n8n                                                      | —                       |
| `tests/`                           | e2e, functional, load and oracle tests                                    | —                       |

## Toolchain

Versions are pinned in [`.tool-versions`](./.tool-versions) (compatible with [mise](https://mise.jdx.dev) and [asdf](https://asdf-vm.com)).

| Tool           | Version       | Pinned in                                                        |
| -------------- | ------------- | ---------------------------------------------------------------- |
| Java (Temurin) | 21.0.13       | `.tool-versions`, Gradle toolchain (`gradle/libs.versions.toml`) |
| Gradle         | 9.6.0         | `gradle/wrapper/gradle-wrapper.properties`                       |
| Node.js        | 24.22.3 (LTS) | `.tool-versions`, `package.json#engines`                         |
| pnpm           | 11.20.0       | `.tool-versions`, `package.json#packageManager` (Corepack)       |
| Docker         | 29.x          | manual install (Docker Desktop / Engine)                         |
| Terraform      | 1.17.2        | `.tool-versions`                                                 |
| Helm           | 4.3.2         | `.tool-versions`                                                 |
| kubectl        | 1.37.2        | `.tool-versions`                                                 |
| AWS CLI        | 2.42.1        | `.tool-versions`                                                 |

Main backend libraries (`gradle/libs.versions.toml`): Spring Boot 3.5, Spring Cloud AWS 3.4, Resilience4j 2.4,
Micrometer and Flyway (Spring Boot BOM), JUnit 5, Testcontainers 1.21, ArchUnit 1.5.

## Quick start

```bash
mise install                         # or: asdf install
corepack enable
pnpm install --frozen-lockfile       # also installs the lefthook Git hooks

./gradlew build                      # backend: compile, Spotless, Checkstyle, tests, JaCoCo
pnpm turbo run lint test             # frontend: ESLint + Vitest/Jest
```

> If Java 21 is not installed, Gradle downloads it automatically (foojay toolchain resolver).

### Common commands

| Goal                         | Command                                        |
| ---------------------------- | ---------------------------------------------- |
| Run one service              | `./gradlew :member-management-service:bootRun` |
| Format Java / Gradle         | `./gradlew spotlessApply`                      |
| Format TS / JSON / YAML / MD | `pnpm format`                                  |
| Web dev server               | `pnpm --filter @solidariaplus/web dev`         |
| Desktop dev                  | `pnpm --filter @solidariaplus/desktop dev`     |
| Mobile (Metro)               | `pnpm --filter @solidariaplus/mobile dev`      |
| Frontend full pipeline       | `pnpm turbo run lint typecheck build test`     |

## Backend build conventions (`build-logic/`)

- `solidaria.java-conventions` — Java toolchain 21, JUnit 5, JaCoCo (XML + HTML), Spotless
  (Palantir Java Format, ktlint for `*.gradle.kts`) and Checkstyle (`config/checkstyle/checkstyle.xml`).
  `spotlessCheck` and `checkstyle*` run as part of `check`/`build`.
- `solidaria.spring-boot-service` — Spring Boot web/validation, Actuator + Micrometer (Prometheus, OTel tracing),
  BOMs for Spring Cloud AWS, Resilience4j and Testcontainers; produces `build/libs/app.jar`.
- `solidaria.java-library` — shared libraries under `libs/`.

Services opt into extra modules without versions, e.g.
`implementation(libs.spring.cloud.aws.starter.sqs)`, `implementation(libs.bundles.resilience)`,
`implementation(libs.flyway.core)`.

## Git conventions

- **Commits**: Conventional Commits with a Jira key, e.g. `feat(credit): add scoring endpoint KAN-42`
  (`commitlint.config.js`, run by the `commit-msg` hook).
- **Pre-commit** (`lefthook.yml`): Prettier on staged TS/JS/JSON/YAML/MD files and `spotlessApply` on staged
  Java/Kotlin DSL files; fixed files are re-staged automatically.
- **Branches**: the name Jira creates, `feature/kan-<number>-<summary>` (`^feature/kan-[0-9]+-[a-z0-9-]{1,40}$`).
- **PR titles**: Conventional Commits with the Jira key at the start of the subject, e.g.
  `feat(credit): KAN-42 add scoring endpoint`.
- **Releases**: release-please per component (`release-please-config.json`, `.release-please-manifest.json`).

## CI

Every pull request into `dev`, `qa` and `main` runs two workflows on GitHub-hosted runners. Their checks are
required status checks in the rulesets of the three branches, so a PR that breaks any gate cannot be merged.

`.github/workflows/pull-request.yml`: `pr-title` and `branch-name` enforce the conventions above. Dependabot and
release-please PRs are exempt, and so are promotion PRs `dev` → `qa` and `qa` → `main` from the branch check.

`.github/workflows/ci.yml` runs each job only for the affected packages; skipped jobs count as passing:

| Job            | Runs when                             | Checks                                                                   |
| -------------- | ------------------------------------- | ------------------------------------------------------------------------ |
| `changes`      | always                                | Detects affected areas and Gradle services                               |
| `format`       | always                                | `pnpm format:check` (Prettier)                                           |
| `frontend`     | `apps/`, `packages/`, root JS files   | `turbo run lint typecheck build --affected`, unit tests with coverage    |
| `backend`      | affected services                     | `check` (Spotless, Checkstyle, unit tests) and JaCoCo coverage           |
| `architecture` | affected services                     | `architectureTest` (ArchUnit rules tagged `architecture`)                |
| `codeql`       | always                                | CodeQL for Actions, Java/Kotlin and JavaScript/TypeScript                |
| `trivy-config` | Dockerfiles, compose, Helm, Terraform | `trivy config` (HIGH, CRITICAL)                                          |
| `trivy-image`  | affected services                     | Builds each service image and runs `trivy image` (HIGH, CRITICAL, fixed) |
| `compose`      | `compose.yaml`                        | `docker compose config`                                                  |
| `helm`         | `deploy/helm/`, service `helm/`       | `helm lint --strict` and `kubeconform` on the rendered manifests         |
| `terraform`    | `infra/terraform/`                    | `terraform fmt -check`, `terraform validate`, `tflint`                   |
| `ci-passed`    | always                                | Fails when any job above failed or was cancelled                         |

Changes to `build-logic/`, `gradle/`, `libs/`, `settings.gradle.kts`, `.tool-versions` or `ci.yml` itself mark
every service as affected. Run the backend gates locally with `./gradlew check` (it includes `architectureTest`).

Dependabot (`.github/dependabot.yml`) opens weekly grouped updates against `dev`, and secret scanning with push
protection is enabled on the repository.
