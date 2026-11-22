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
- **PR titles** must contain the Jira key (`.github/workflows/ci.yml` → `pr-title`).
- **Releases**: release-please per component (`release-please-config.json`, `.release-please-manifest.json`).

## CI

`.github/workflows/ci.yml` runs two jobs: `frontend` (`pnpm install --frozen-lockfile` +
`turbo run lint typecheck build test`) and `backend` (`./gradlew build` on Temurin 21).
