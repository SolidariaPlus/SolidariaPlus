import { base64UrlDecodeToString } from "../crypto/base64url";
import { AuthError } from "../errors/auth-error";

export interface AuthClaims {
  readonly subject: string;
  readonly roles: readonly string[];
  readonly functions: readonly string[];
  readonly memberId: string | undefined;
  readonly expiresAt: number | undefined;
}

export function decodeJwtPayload(token: string): Record<string, unknown> {
  const segments = token.split(".");
  const payload = segments[1];
  if (segments.length !== 3 || !payload) throw invalid("the token is not a JWT");
  try {
    const parsed: unknown = JSON.parse(base64UrlDecodeToString(payload));
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed))
      throw invalid("the payload is not an object");
    return parsed as Record<string, unknown>;
  } catch (error) {
    throw error instanceof AuthError ? error : invalid("the payload cannot be decoded");
  }
}

export function parseClaims(accessToken: string): AuthClaims {
  const payload = decodeJwtPayload(accessToken);
  const subject = payload["sub"];
  if (typeof subject !== "string" || subject.length === 0) throw invalid("sub is missing");
  const memberId = payload["mid"];
  const expiration = payload["exp"];
  return Object.freeze({
    subject,
    roles: Object.freeze(stringList(payload["roles"])),
    functions: Object.freeze(stringList(payload["fn"])),
    memberId: typeof memberId === "string" && memberId.length > 0 ? memberId : undefined,
    expiresAt: typeof expiration === "number" ? expiration * 1000 : undefined,
  });
}

export function hasRole(claims: AuthClaims | undefined, role: string): boolean {
  return claims?.roles.includes(role) ?? false;
}

export function hasAnyRole(claims: AuthClaims | undefined, roles: readonly string[]): boolean {
  return roles.some((role) => hasRole(claims, role));
}

export function hasFunction(claims: AuthClaims | undefined, fn: string): boolean {
  return claims?.functions.includes(fn) ?? false;
}

export function isMember(claims: AuthClaims | undefined): boolean {
  return claims?.memberId !== undefined;
}

function stringList(value: unknown): string[] {
  if (typeof value === "string") return value.length > 0 ? [value] : [];
  if (!Array.isArray(value)) return [];
  return value.filter((item): item is string => typeof item === "string" && item.length > 0);
}

function invalid(detail: string): AuthError {
  return new AuthError("invalid_response", `Invalid token claims: ${detail}`);
}
