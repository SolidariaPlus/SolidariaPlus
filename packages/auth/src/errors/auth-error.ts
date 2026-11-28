export const OAUTH_ERROR_CODES = [
  "invalid_request",
  "unauthorized_client",
  "access_denied",
  "unsupported_response_type",
  "invalid_scope",
  "server_error",
  "temporarily_unavailable",
  "invalid_client",
  "invalid_grant",
  "unsupported_grant_type",
] as const;

export const DOMAIN_ERROR_CODES = [
  "AUTH_ACCOUNT_LOCKED",
  "AUTH_ACTIVATION_CODE_INVALID",
  "AUTH_MFA_REQUIRED",
] as const;

export const CLIENT_ERROR_CODES = [
  "state_mismatch",
  "nonce_mismatch",
  "invalid_response",
  "network_error",
  "session_expired",
  "configuration_error",
] as const;

export type OAuthErrorCode = (typeof OAUTH_ERROR_CODES)[number];
export type DomainErrorCode = (typeof DOMAIN_ERROR_CODES)[number];
export type ClientErrorCode = (typeof CLIENT_ERROR_CODES)[number];
export type AuthErrorCode = OAuthErrorCode | DomainErrorCode | ClientErrorCode;

const KNOWN_CODES: ReadonlySet<string> = new Set<string>([
  ...OAUTH_ERROR_CODES,
  ...DOMAIN_ERROR_CODES,
  ...CLIENT_ERROR_CODES,
]);

const SIGN_IN_REQUIRED_CODES: ReadonlySet<string> = new Set<string>([
  "invalid_grant",
  "session_expired",
]);

export function isAuthErrorCode(value: unknown): value is AuthErrorCode {
  return typeof value === "string" && KNOWN_CODES.has(value);
}

export class AuthError extends Error {
  readonly code: string;
  readonly status: number | undefined;

  constructor(code: string, message?: string, status?: number) {
    super(message ?? code);
    this.name = "AuthError";
    this.code = code;
    this.status = status;
  }

  get requiresSignIn(): boolean {
    return SIGN_IN_REQUIRED_CODES.has(this.code);
  }

  static fromResponseBody(body: unknown, status?: number): AuthError {
    const record = isRecord(body) ? body : {};
    const code = pickString(record, "code") ?? pickString(record, "error");
    const description =
      pickString(record, "error_description") ??
      pickString(record, "detail") ??
      pickString(record, "title");
    return new AuthError(code ?? fallbackCode(status), description, status);
  }
}

export function isAuthError(value: unknown): value is AuthError {
  return value instanceof AuthError;
}

function fallbackCode(status: number | undefined): string {
  if (status === undefined) return "invalid_response";
  if (status >= 500) return "server_error";
  if (status === 401) return "invalid_grant";
  return "invalid_request";
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function pickString(record: Record<string, unknown>, key: string): string | undefined {
  const value = record[key];
  return typeof value === "string" && value.length > 0 ? value : undefined;
}
