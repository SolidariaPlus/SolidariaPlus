import {
  parseRuntimeConfig,
  type AuthClaims,
  type AuthClient,
  type AuthStateListener,
} from "@solidariaplus/auth";
import type { IdentityAccessApi } from "@solidariaplus/identity-api";
import { vi } from "vitest";

export const testConfig = parseRuntimeConfig({
  issuer: "https://app.test/auth",
  clientId: "solidaria-web",
  redirectUri: "https://app.test/callback",
  apiEndpoint: "https://app.test/api",
});

export function claimsFor(overrides: Partial<AuthClaims> = {}): AuthClaims {
  return {
    subject: "u-1",
    roles: ["TELLER"],
    functions: [],
    memberId: undefined,
    expiresAt: undefined,
    authenticatedAt: Date.parse("2026-11-28T13:02:00Z"),
    authenticationMethods: ["pwd", "otp", "mfa"],
    sessionId: "s-1",
    ...overrides,
  };
}

export function createFakeAuthClient(initialClaims?: AuthClaims, signedInClaims = claimsFor()) {
  let claims = initialClaims;
  const listeners = new Set<AuthStateListener>();
  const emit = (next: AuthClaims | undefined) => {
    claims = next;
    for (const listener of listeners) listener(next);
  };
  const client = {
    config: testConfig,
    fetch: vi.fn(),
    signIn: vi.fn(async () => undefined),
    completeSignIn: vi.fn(async () => {
      emit(signedInClaims);
      return signedInClaims;
    }),
    restoreSession: vi.fn(async () => claims),
    signOut: vi.fn<AuthClient["signOut"]>(async () => emit(undefined)),
    getAccessToken: vi.fn(async () => "token"),
    getClaims: () => claims,
    isAuthenticated: () => claims !== undefined,
    subscribe: (listener: AuthStateListener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    expire: () => emit(undefined),
  } satisfies AuthClient & { expire(): void };
  return client;
}

export function createFakeIdentityApi() {
  return {
    issueActivationCode: vi.fn<IdentityAccessApi["issueActivationCode"]>(async (memberNumber) => ({
      activationCodeId: "c-1",
      memberId: "m-uuid",
      memberNumber,
      code: "K8M4P9X2",
      issuedAt: new Date("2026-11-28T15:15:00Z"),
      expiresAt: new Date("2026-12-01T15:15:00Z"),
    })),
    resetStaffAuthenticator: vi.fn<IdentityAccessApi["resetStaffAuthenticator"]>(async () => ({
      staffSubject: "s-9",
      resetAt: new Date("2026-11-28T15:15:00Z"),
      resetBySubject: "u-1",
      enrolmentDeadline: new Date("2026-11-29T15:15:00Z"),
      sessionsRevoked: 2,
    })),
  } satisfies IdentityAccessApi;
}
