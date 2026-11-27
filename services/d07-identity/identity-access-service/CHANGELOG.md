# Changelog

All notable changes to identity-access-service are documented here by release-please.

## Unreleased

### Features

- Hexagonal service base (KAN-2 §5.7): domain, application ports and adapters for OAuth 2.1, pages,
  REST, Kafka consumers, persistence, directory, cache, outbox messaging and field encryption.
- Spring Authorization Server with ES256 tokens, PKCE public clients with rotating refresh tokens and
  reuse detection, TOTP second factor for staff, activation codes and authenticator resets.
- Profiles `local`, `qa` and `prod` with every secret from AWS Secrets Manager (LocalStack locally).
- Flyway migration `V1__init.sql`, Actuator probes, Prometheus metrics, ECS JSON logs and Sentry.
