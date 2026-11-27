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
- The `local` and `qa` profiles also load `db/seed/R__synthetic_seed.sql` (KAN-16 §9): members 1001 to
  1004 waiting for their activation code and six staff profiles of the synthetic directory
  (`infra/ad/staff.csv`). It holds no credential and keeps rows that already exist; Production never
  loads it.
- `V1__init.sql` is not released yet, so it is still edited. After pulling a change to it, drop the
  local database (`docker compose --profile identity down -v`, then `up` again) or Flyway refuses its
  checksum.
- `postman/identity-access.postman_collection.json` holds the functional tests: public endpoints, sign-in
  with PKCE and TOTP (automated, no browser), member onboarding, negative cases, refresh token rotation
  and sessions. Run its folders in order; its description explains the setup.
- `build` starts PostgreSQL and Valkey with Testcontainers, so Docker must be running. The integration
  tests cover the repositories, the uniqueness rules, the column comments, the seed, the registered
  clients and the Valkey TTLs. The domain layer needs 80 % line coverage (`domainCoverage`).

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

## Stores

Aurora database `identity_access` (`V1__init.sql`, a `COMMENT` on every column): `subjects`,
`member_accounts`, `activation_codes`, `staff_profiles`, `sessions`, `refresh_token_families`, the
default Spring Authorization Server tables `oauth2_registered_client` and `oauth2_authorization`, and
`outbox` and `processed_events`. The clients of `application.yml` are saved in
`oauth2_registered_client` at start-up, so the configuration stays their only source.

Valkey keys, under the prefix `ia:` that the ACL user of MS-17 may access (ADR-055):

| Key                            | TTL                              | Use                                                                                                             |
| ------------------------------ | -------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| `ia:fail:{sha256(identifier)}` | 15 min from the first failure    | Failed sign-ins; 5 lock the identifier                                                                          |
| `ia:lock:{sha256(identifier)}` | 15 min                           | Locked identifier (`AUTH_ACCOUNT_LOCKED`)                                                                       |
| `ia:rl:{ip}:{epochMinute}`     | 120 s                            | More than 20 form posts a minute to `/login`, `/activate` or `/mfa` from one address get 429 with `Retry-After` |
| `ia:revoked:{sid}`             | 10 min, the longest access token | The REST API and userinfo refuse tokens of a revoked session                                                    |

## Configuration

| Profile      | Secrets and parameters                                                   | Adapters                                    |
| ------------ | ------------------------------------------------------------------------ | ------------------------------------------- |
| `local`      | LocalStack Secrets Manager `/solidaria/local/...`                        | Local directory, raw AES keyring, Flyway on |
| `qa`, `prod` | Secrets Manager and Parameter Store `/solidaria/<env>/...` (group `aws`) | LDAPS directory, KMS keyring, TLS to Valkey |

| Source                                                 | Properties                                                                         |
| ------------------------------------------------------ | ---------------------------------------------------------------------------------- |
| `/solidaria/<env>/identity-access-service/db`          | `db.*`: Aurora credentials                                                         |
| `/solidaria/<env>/identity-access-service/cache`       | `cache.*`: Valkey user and key prefix                                              |
| `/solidaria/<env>/kafka/users/identity-access-service` | SCRAM-SHA-512 password                                                             |
| `/solidaria/<env>/identity-access-service/signing-key` | `signing.kid`, `signing.privateKey`, `signing.publicKey` (ES256, DER in Base64)    |
| `/solidaria/<env>/identity-access-service/clients`     | `solidaria.clients.services-secret`: secret of the M2M client `solidaria-services` |
| `/solidaria/<env>/identity-access-service/ldap`        | `ldap.username`, `ldap.password` (QA and PROD)                                     |
| `/solidaria/<env>/identity-access-service/ldap/url`    | `ldap.url` (Parameter Store)                                                       |
| `KMS_KEY_ARN`, `SENTRY_DSN`                            | Environment variables of the Helm release                                          |

- The service is published under `/auth`; `server.forward-headers-strategy=framework` makes the issuer
  and the redirects use the public URL behind CloudFront, the HTTP API and the ALB.
- Actuator: `/actuator/health/liveness`, `/actuator/health/readiness` (database and Valkey) and
  `/actuator/prometheus`. Logs are ECS JSON without personal data; unhandled errors go to Sentry.
- No secret is written in a file of this repository (RNF-SEG-05).

## M2M client secret (KAN-48)

The confidential client `solidaria-services` (grant `client_credentials`, scope `solidaria.internal`)
authenticates with `client_secret_basic`. Its secret is read from
`/solidaria/<env>/identity-access-service/clients`, whose value is a JSON object:

```json
{ "services-secret": "{bcrypt}<hash>" }
```

- **local**: `infra/local/localstack/init.sh` writes `{noop}${SERVICES_CLIENT_SECRET}`
  (default `local-services-secret`, see `.env.example`).
- **qa / prod**: Terraform (`service_settings` in `infra/terraform/envs/<env>/a1`, KAN-51) generates
  a random secret once. It stores the bcrypt hash in `.../clients` and the plain value for the
  calling services in `/solidaria/<env>/identity-access-service/m2m-client`
  (`{"clientId":"solidaria-services","clientSecret":"..."}`). Terraform never overwrites either
  secret afterwards.

To rotate the secret by hand, update both secrets:

```bash
SECRET="$(openssl rand -base64 32)"   # plain value: give it to the calling services
HASH="$(htpasswd -bnBC 10 "" "$SECRET" | tr -d ':\n' | sed 's/^\$2y/$2a/')"
aws secretsmanager put-secret-value \
  --secret-id /solidaria/<env>/identity-access-service/clients \
  --secret-string "{\"services-secret\":\"{bcrypt}${HASH}\"}"
aws secretsmanager put-secret-value \
  --secret-id /solidaria/<env>/identity-access-service/m2m-client \
  --secret-string "{\"clientId\":\"solidaria-services\",\"clientSecret\":\"${SECRET}\"}"
```

Never write the plain value in Git.

The initial `signing-key` of QA and PROD is also generated by Terraform (EC P-256, `kid` =
`<env>-<8 hex>`). Rotate it with the runbook below. The `ldap` secret starts as
`{"username":"PENDING","password":"PENDING"}`. The service starts with it, because the LDAP
health check is disabled, but staff sign-in fails until the MS-17 bind account of the directory
is set:

```bash
aws secretsmanager put-secret-value --secret-id /solidaria/<env>/identity-access-service/ldap \
  --secret-string '{"username":"<bind user>@ad.solidaria.internal","password":"<password>"}'
```

## Signing key rotation (RNF-17-04)

The authorization server uses an EC P-256 key pair (`signing-key` in Secrets Manager) to sign every
access token with ES256. The key rotates every 90 days. During the overlap period both keys appear in
JWKS so resource servers can verify tokens signed by either key without a forced cache-bust.

### Secret layout in Secrets Manager

| Secret path (env = `local` | `qa` | `prod`) | Keys in secret |
| ---------------------------------------------------------- | ------------------------------------------- |
| `/solidaria/<env>/identity-access-service/signing-key` | `kid`, `privateKey`, `publicKey` (DER, Base64) |
| `/solidaria/<env>/identity-access-service/signing-key-previous` | `kid`, `publicKey` (DER, Base64, public only) |

The `signing-key-previous` secret is **absent until the first rotation**; the application starts
without it. It is **removed after the overlap period** (90 days after the previous rotation).

### Rotation runbook

1. **Generate new key pair** (AWS CLI or console):

   ```bash
   # EC P-256, DER encoding
   openssl ecparam -name prime256v1 -genkey -noout | openssl pkcs8 -topk8 -nocrypt -outform DER | base64 -w0
   openssl ecparam -name prime256v1 -genkey -noout | openssl pkey -pubout -outform DER | base64 -w0
   ```

2. **Record the new kid** (e.g. `2026-02`) and the current kid (e.g. `2025-11`).

3. **Create or update `signing-key-previous`** with the CURRENT key's `kid` and `publicKey` only —
   no private key. This is the key that signed tokens that will still be valid during the overlap.

4. **Update `signing-key`** with the NEW key's `kid`, `privateKey`, and `publicKey`.

5. **Rolling-restart the service** (ECS rolling update). Every new pod picks up the new signing key.
   The JWKS endpoint now returns both keys. Consumers re-fetch JWKS within 24 h (Cache-Control max-age).

6. **Wait 90 days** (longer than the longest refresh-token TTL, which is 30 d for mobile). After that,
   no token signed by the old key can still be valid.

7. **Delete `signing-key-previous`** from Secrets Manager. The next rolling-restart removes the old key
   from JWKS. Consumers will drop it from their cache within 24 h.

### Verifying the rotation

```bash
curl -s https://<cloudfront-domain>/auth/oauth2/jwks | jq '.keys | length'   # 2 during overlap, 1 otherwise
curl -s https://<cloudfront-domain>/auth/.well-known/oauth-authorization-server | jq .issuer
```

A token minted by the new key validates on a resource server that last fetched the JWKS up to 24 h ago,
because the old verification key is still published. After 24 h all caches hold the latest JWKS.

## ADRs

KAN-2 ADR-060, ADR-082, ADR-083 (OAuth 2.1, rotation with reuse detection), ADR-084 (directory and
activation codes), ADR-100 (Active Directory over LDAPS), ADR-108 (README), ADR-111 (Spring Boot 4,
revised by CR-24), ADR-124 (client secrets of services), ADR-125 (member password policy), ADR-126
(in-person authenticator reset).
