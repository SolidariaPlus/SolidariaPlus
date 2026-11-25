#!/bin/sh
# Starts Valkey with one ACL user per service, restricted to its own key prefix,
# as in Amazon ElastiCache (KAN-2 ADR-055). The default user requires the admin password.
set -eu

SERVICES_FILE="${SERVICES_FILE:-/opt/solidaria/services.conf}"
: "${VALKEY_ADMIN_PASSWORD:?VALKEY_ADMIN_PASSWORD is required}"
: "${SERVICE_CACHE_PASSWORD:?SERVICE_CACHE_PASSWORD is required}"

acl_file=/tmp/users.acl
printf 'user default on >%s ~* &* +@all\n' "$VALKEY_ADMIN_PASSWORD" > "$acl_file"
grep -Ev '^[[:space:]]*(#|$)' "$SERVICES_FILE" | while read -r service _database prefix; do
  printf 'user %s on >%s ~%s:* &%s:* +@all -@dangerous\n' \
    "$service" "$SERVICE_CACHE_PASSWORD" "$prefix" "$prefix" >> "$acl_file"
done
chmod 600 "$acl_file"

exec valkey-server --aclfile "$acl_file" --appendonly yes --dir /data
