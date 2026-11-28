import { describe, expect, it } from "vitest";
import { AuthError, CLIENT_ERROR_CODES, DOMAIN_ERROR_CODES, OAUTH_ERROR_CODES } from "./auth-error";
import { DEFAULT_ERROR_MESSAGE, ERROR_MESSAGES_ES, toUserMessage } from "./messages-es";

describe("Spanish error messages", () => {
  it.each([...OAUTH_ERROR_CODES, ...DOMAIN_ERROR_CODES, ...CLIENT_ERROR_CODES])(
    "maps %s to a specific message",
    (code) => {
      const message = toUserMessage(new AuthError(code));
      expect(message).toBe(ERROR_MESSAGES_ES[code]);
      expect(message).not.toBe(DEFAULT_ERROR_MESSAGE);
    },
  );

  it("maps the domain codes to the expected texts", () => {
    expect(toUserMessage(new AuthError("AUTH_ACCOUNT_LOCKED"))).toMatch(/bloqueada/);
    expect(toUserMessage(new AuthError("AUTH_ACTIVATION_CODE_INVALID"))).toMatch(
      /código de activación/,
    );
    expect(toUserMessage(new AuthError("AUTH_MFA_REQUIRED"))).toMatch(/verificación en dos pasos/);
  });

  it("accepts raw codes from redirect parameters", () => {
    expect(toUserMessage("access_denied")).toBe(ERROR_MESSAGES_ES.access_denied);
  });

  it("falls back to a generic message for unknown errors", () => {
    expect(toUserMessage(new AuthError("SOMETHING_ELSE"))).toBe(DEFAULT_ERROR_MESSAGE);
    expect(toUserMessage(new Error("boom"))).toBe(DEFAULT_ERROR_MESSAGE);
    expect(toUserMessage(undefined)).toBe(DEFAULT_ERROR_MESSAGE);
  });
});

describe("AuthError.fromResponseBody", () => {
  it("reads RFC 6749 error bodies", () => {
    const error = AuthError.fromResponseBody(
      { error: "invalid_grant", error_description: "reused" },
      400,
    );
    expect(error).toMatchObject({ code: "invalid_grant", message: "reused", status: 400 });
    expect(error.requiresSignIn).toBe(true);
  });

  it("reads RFC 9457 problem details with a domain code", () => {
    const error = AuthError.fromResponseBody(
      { code: "AUTH_ACCOUNT_LOCKED", detail: "locked" },
      423,
    );
    expect(error).toMatchObject({ code: "AUTH_ACCOUNT_LOCKED", message: "locked", status: 423 });
  });

  it("derives a code from the status when the body is unusable", () => {
    expect(AuthError.fromResponseBody(undefined, 503).code).toBe("server_error");
    expect(AuthError.fromResponseBody("oops", 400).code).toBe("invalid_request");
  });
});
