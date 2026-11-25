#!/bin/sh
# Creates one database and one login role per service listed in services.conf.
# Idempotent: it runs at every `docker compose up` and only creates what is missing.
# Each role owns its database and has no privileges on the others (KAN-2 ADR-054).
# The same script runs on localhost (postgres-provisioner in compose.yaml), with one shared local
# password, and in QA and Production (db-provisioner Job of modules/platform-addons), where the
# password of each service is the file $PASSWORD_DIR/<service>, mounted from Secrets Manager.
set -eu

SERVICES_FILE="${SERVICES_FILE:-/opt/solidaria/services.conf}"
: "${PGHOST:?PGHOST is required}"
: "${PGUSER:?PGUSER is required}"
: "${PGPASSWORD:?PGPASSWORD is required}"
if [ -z "${PASSWORD_DIR:-}" ]; then
  : "${SERVICE_DB_PASSWORD:?SERVICE_DB_PASSWORD or PASSWORD_DIR is required}"
fi

grep -Ev '^[[:space:]]*(#|$)' "$SERVICES_FILE" | while read -r service database _prefix; do
  role="$(printf '%s' "$service" | tr '-' '_')"
  if [ -n "${PASSWORD_DIR:-}" ]; then
    password="$(cat "$PASSWORD_DIR/$service")"
  else
    password="$SERVICE_DB_PASSWORD"
  fi
  echo "Provisioning database '$database' for role '$role'"
  psql -v ON_ERROR_STOP=1 -d postgres \
    -v role="$role" -v database="$database" -v password="$password" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'role', :'password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'role')
\gexec
SELECT format('ALTER ROLE %I PASSWORD %L', :'role', :'password')
\gexec
-- Aurora: the master user is not a superuser, so it must be a member of the role to make it the
-- owner of the database (PostgreSQL 16 and later).
SELECT format('GRANT %I TO CURRENT_USER', :'role')
WHERE NOT pg_has_role(current_user, :'role', 'MEMBER')
\gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'database', :'role')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'database')
\gexec
SELECT format('REVOKE ALL ON DATABASE %I FROM PUBLIC', :'database')
\gexec
SQL
done

touch /tmp/provisioned
echo "PostgreSQL provisioning complete"
