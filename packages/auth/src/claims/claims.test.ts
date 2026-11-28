import { describe, expect, it } from "vitest";
import { base64UrlEncode } from "../crypto/base64url";
import { hasAnyRole, hasFunction, hasRole, isMember, parseClaims } from "./claims";

function jwt(payload: Record<string, unknown>): string {
  const encode = (value: unknown) =>
    base64UrlEncode(new TextEncoder().encode(JSON.stringify(value)));
  return `${encode({ alg: "ES256" })}.${encode(payload)}.signature`;
}

describe("parseClaims", () => {
  it("reads roles, fn and mid", () => {
    const claims = parseClaims(
      jwt({
        sub: "u-1",
        roles: ["MEMBER"],
        fn: ["CASHIER", "AUDITOR"],
        mid: "m-9",
        exp: 1_700_000_000,
      }),
    );

    expect(claims).toEqual({
      subject: "u-1",
      roles: ["MEMBER"],
      functions: ["CASHIER", "AUDITOR"],
      memberId: "m-9",
      expiresAt: 1_700_000_000_000,
    });
    expect(hasRole(claims, "MEMBER")).toBe(true);
    expect(hasAnyRole(claims, ["STAFF", "MEMBER"])).toBe(true);
    expect(hasFunction(claims, "AUDITOR")).toBe(true);
    expect(hasFunction(claims, "MANAGER")).toBe(false);
    expect(isMember(claims)).toBe(true);
  });

  it("tolerates absent claims and non-ASCII values", () => {
    const claims = parseClaims(jwt({ sub: "josé" }));
    expect(claims.subject).toBe("josé");
    expect(claims.roles).toEqual([]);
    expect(claims.functions).toEqual([]);
    expect(isMember(claims)).toBe(false);
  });

  it("rejects malformed tokens", () => {
    expect(() => parseClaims("not-a-jwt")).toThrowError(
      expect.objectContaining({ code: "invalid_response" }),
    );
    expect(() => parseClaims("a.%%%.c")).toThrowError(
      expect.objectContaining({ code: "invalid_response" }),
    );
    expect(() => parseClaims(jwt({ roles: [] }))).toThrowError(
      expect.objectContaining({ code: "invalid_response" }),
    );
  });

  it("denies guards without claims", () => {
    expect(hasRole(undefined, "MEMBER")).toBe(false);
    expect(hasFunction(undefined, "X")).toBe(false);
  });
});
