import { AuthError } from "../errors/auth-error.js";

export interface TokenSet {
  readonly accessToken: string;
  readonly expiresAt: number;
  readonly refreshToken: string | undefined;
  readonly idToken: string | undefined;
  readonly scope: string | undefined;
}

export function parseTokenResponse(body: unknown, now: number): TokenSet {
  if (typeof body !== "object" || body === null)
    throw invalid("the token response is not an object");
  const record = body as Record<string, unknown>;
  const accessToken = record["access_token"];
  const tokenType = record["token_type"];
  const expiresIn = record["expires_in"];
  if (typeof accessToken !== "string" || accessToken.length === 0)
    throw invalid("access_token is missing");
  if (typeof tokenType !== "string" || tokenType.toLowerCase() !== "bearer")
    throw invalid("token_type must be Bearer");
  if (typeof expiresIn !== "number" || !Number.isFinite(expiresIn) || expiresIn <= 0) {
    throw invalid("expires_in must be a positive number");
  }
  return {
    accessToken,
    expiresAt: now + expiresIn * 1000,
    refreshToken: optionalString(record, "refresh_token"),
    idToken: optionalString(record, "id_token"),
    scope: optionalString(record, "scope"),
  };
}

function optionalString(record: Record<string, unknown>, key: string): string | undefined {
  const value = record[key];
  return typeof value === "string" && value.length > 0 ? value : undefined;
}

function invalid(detail: string): AuthError {
  return new AuthError("invalid_response", `Invalid token response: ${detail}`);
}
