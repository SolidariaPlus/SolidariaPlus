import { AuthError } from "../errors/auth-error";
import type { FetchLike } from "../shared/types";

export interface AuthEndpoints {
  readonly authorization: string;
  readonly token: string;
  readonly sessions: string;
  readonly activation: string;
}

export interface AuthRuntimeConfig {
  readonly issuer: string;
  readonly clientId: string;
  readonly redirectUri: string;
  readonly apiEndpoint: string;
  readonly scopes: readonly string[];
  readonly endpoints: AuthEndpoints;
}

export type RuntimeConfigSource = () => unknown;

const DEFAULT_SCOPES = ["openid", "offline_access"] as const;

export async function loadRuntimeConfig(source: RuntimeConfigSource): Promise<AuthRuntimeConfig> {
  return parseRuntimeConfig(await source());
}

export function parseRuntimeConfig(raw: unknown): AuthRuntimeConfig {
  if (!isObject(raw)) throw configurationError("the configuration must be an object");
  const issuer = trimTrailingSlash(requireUrl(raw, "issuer"));
  const endpoints = isObject(raw["endpoints"]) ? raw["endpoints"] : {};
  return Object.freeze({
    issuer,
    clientId: requireString(raw, "clientId"),
    redirectUri: requireUrl(raw, "redirectUri"),
    apiEndpoint: trimTrailingSlash(requireUrl(raw, "apiEndpoint")),
    scopes: Object.freeze(parseScopes(raw["scopes"])),
    endpoints: Object.freeze({
      authorization: optionalUrl(endpoints, "authorization") ?? `${issuer}/oauth2/authorize`,
      token: optionalUrl(endpoints, "token") ?? `${issuer}/oauth2/token`,
      sessions: optionalUrl(endpoints, "sessions") ?? `${issuer}/sessions`,
      activation: optionalUrl(endpoints, "activation") ?? `${issuer}/activate`,
    }),
  });
}

export function jsonConfigSource(
  url: string,
  fetchFn: FetchLike = (input, init) => fetch(input, init),
): RuntimeConfigSource {
  return async () => {
    const response = await fetchFn(url, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });
    if (!response.ok) throw configurationError(`unable to load ${url} (${response.status})`);
    return response.json();
  };
}

function parseScopes(value: unknown): string[] {
  if (value === undefined) return [...DEFAULT_SCOPES];
  if (
    !Array.isArray(value) ||
    value.some((scope) => typeof scope !== "string" || scope.length === 0)
  ) {
    throw configurationError("scopes must be a list of non-empty strings");
  }
  return value as string[];
}

function requireString(record: Record<string, unknown>, key: string): string {
  const value = record[key];
  if (typeof value !== "string" || value.trim().length === 0)
    throw configurationError(`${key} is required`);
  return value.trim();
}

function requireUrl(record: Record<string, unknown>, key: string): string {
  const value = requireString(record, key);
  if (!URL.canParse(value)) throw configurationError(`${key} must be an absolute URL`);
  return value;
}

function optionalUrl(record: Record<string, unknown>, key: string): string | undefined {
  return record[key] === undefined ? undefined : requireUrl(record, key);
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function trimTrailingSlash(value: string): string {
  return value.replace(/\/+$/, "");
}

function configurationError(detail: string): AuthError {
  return new AuthError("configuration_error", `Invalid auth runtime configuration: ${detail}`);
}
