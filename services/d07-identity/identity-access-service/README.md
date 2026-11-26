# identity-access-service (MS-17)

## Purpose

OAuth 2.1 and OpenID Connect authorization server of Solidaria+ (bounded context D-07, KAN-2 §5.7).
It signs in members (member number and password) and staff (Active Directory account, password and
TOTP), issues ES256 access tokens of 10 minutes and rotating refresh tokens, and revokes sessions.
It holds no personal data beyond the staff directory account.

Hexagonal layout under `com.solidariaplus.identity.identityaccess`:

| Package                         | Content                                                                              |
| ------------------------------- | ------------------------------------------------------------------------------------ |
| `domain`                        | Subjects, member accounts, staff profiles, sessions, activation codes, rules, events |
| `application`                   | Use cases (`port.in`), required ports (`port.output`) and their services             |
| `infrastructure.adapter.in`     | OAuth endpoints (`oauth`), pages (`web`), REST (`rest`), Kafka consumers (`kafka`)   |
| `infrastructure.adapter.output` | Persistence, directory, cache, messaging (outbox) and crypto (Argon2id, TOTP, KMS)   |
| `infrastructure.config`         | Clock, scheduling and correlation identifier                                         |

ArchUnit keeps the domain free of Spring, AWS SDK and Jackson classes and enforces the layer order
(RNF-MNT-01).

## Run and test locally

```bash
docker compose --profile identity up -d --wait                       # PostgreSQL, Valkey, Kafka, LocalStack
SPRING_PROFILES_ACTIVE=local ./gradlew :identity-access-service:bootRun
curl -s localhost:8097/actuator/health                               # {"status":"UP",...}
./gradlew :identity-access-service:build                             # tests, ArchUnit, Checkstyle, Spotless, coverage
```

- The `local` profile reads every secret from LocalStack (`infra/local/localstack/init.sh`) and uses
  the local directory adapter: the synthetic accounts of `application-local.yml` sign in with
  `STAFF_LOCAL_PASSWORD` of `.env.example`.
- Members are created by `MemberAdmitted` on `member-management.events`; a teller then issues an
  activation code with `POST /members/{memberId}/activation-codes`.
- `postman/identity-access.postman_collection.json` holds the functional tests: public endpoints, sign-in
  with PKCE and TOTP (automated, no browser), member onboarding, negative cases, refresh token rotation
  and sessions. Run its folders in order; its description explains the setup.
- `build` starts PostgreSQL with Testcontainers, so Docker must be running. The domain layer needs
  80 % line coverage (`domainCoverage`).

## Contracts

| Contract                                           | Content                                                      |
| -------------------------------------------------- | ------------------------------------------------------------ |
| `contracts/openapi/identity-access.openapi.yaml`   | OAuth endpoints, pages, activation codes, resets, sessions   |
| `contracts/asyncapi/identity-access.asyncapi.yaml` | SessionRevoked, AuditRecord, MemberAdmitted, MembershipEnded |

- Published through the outbox: `SessionRevoked` on `identity-access.events`, `AuditRecord` on
  `audit.records` (CloudEvents 1.0, structured mode).
- Consumed: `MemberAdmitted` and `MembershipEnded`, idempotent by CloudEvents `id`, five attempts then
  `member-management.events.identity-access.dlt`.
- Refresh tokens have the form `<sid>.<random>`; presenting a rotated one revokes the session.

## Configuration

| Profile      | Secrets and parameters                                                   | Adapters                                    |
| ------------ | ------------------------------------------------------------------------ | ------------------------------------------- |
| `local`      | LocalStack Secrets Manager `/solidaria/local/...`                        | Local directory, raw AES keyring, Flyway on |
| `qa`, `prod` | Secrets Manager and Parameter Store `/solidaria/<env>/...` (group `aws`) | LDAPS directory, KMS keyring, TLS to Valkey |

| Source                                                 | Properties                                                                      |
| ------------------------------------------------------ | ------------------------------------------------------------------------------- |
| `/solidaria/<env>/identity-access-service/db`          | `db.*`: Aurora credentials                                                      |
| `/solidaria/<env>/identity-access-service/cache`       | `cache.*`: Valkey user and key prefix                                           |
| `/solidaria/<env>/kafka/users/identity-access-service` | SCRAM-SHA-512 password                                                          |
| `/solidaria/<env>/identity-access-service/signing-key` | `signing.kid`, `signing.privateKey`, `signing.publicKey` (ES256, DER in Base64) |
| `/solidaria/<env>/identity-access-service/ldap`        | `ldap.username`, `ldap.password` (QA and PROD)                                  |
| `/solidaria/<env>/identity-access-service/ldap/url`    | `ldap.url` (Parameter Store)                                                    |
| `KMS_KEY_ARN`, `SENTRY_DSN`                            | Environment variables of the Helm release                                       |

- The service is published under `/auth`; `server.forward-headers-strategy=framework` makes the issuer
  and the redirects use the public URL behind CloudFront, the HTTP API and the ALB.
- Actuator: `/actuator/health/liveness`, `/actuator/health/readiness` (database and Valkey) and
  `/actuator/prometheus`. Logs are ECS JSON without personal data; unhandled errors go to Sentry.
- No secret is written in a file of this repository (RNF-SEG-05).

## ADRs

KAN-2 ADR-060, ADR-082, ADR-083 (OAuth 2.1, rotation with reuse detection), ADR-084 (directory and
activation codes), ADR-100 (Active Directory over LDAPS), ADR-108 (README), ADR-111 (Spring Boot 4,
revised by CR-24), ADR-124 (client secrets of services), ADR-125 (member password policy), ADR-126
(in-person authenticator reset).
