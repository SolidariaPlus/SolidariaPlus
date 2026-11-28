import { describe, expect, it, vi } from "vitest";
import { createAuthClient } from "./auth-client";
import { parseRuntimeConfig } from "./config/runtime-config";
import { base64UrlEncode } from "./crypto/base64url";
import type { AuthorizationTransaction } from "./oauth/authorization-request";
import { MemoryRefreshTokenVault, type PlatformAdapter } from "./platform/platform-adapter";

const config = parseRuntimeConfig({
  issuer: "https://id.test",
  clientId: "solidaria-web",
  redirectUri: "https://app.test/auth/callback",
  apiEndpoint: "https://api.test",
});

function jwt(payload: Record<string, unknown>): string {
  const encode = (value: unknown) =>
    base64UrlEncode(new TextEncoder().encode(JSON.stringify(value)));
  return `${encode({ alg: "ES256" })}.${encode(payload)}.sig`;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function fakePlatform() {
  let pending: AuthorizationTransaction | undefined;
  let opened: URL | undefined;
  const platform: PlatformAdapter = {
    refreshTokenVault: new MemoryRefreshTokenVault(),
    transactionStore: {
      save: async (transaction) => {
        pending = transaction;
      },
      take: async () => {
        const transaction = pending;
        pending = undefined;
        return transaction;
      },
    },
    openAuthorization: async (url) => {
      opened = url;
    },
    receiveRedirect: async () => {
      const state = opened?.searchParams.get("state") ?? "";
      return new URL(`https://app.test/auth/callback?code=code-1&state=${state}`);
    },
  };
  return { platform, nonce: () => opened?.searchParams.get("nonce") ?? "" };
}

describe("AuthClient", () => {
  it("signs in with PKCE, keeps the refresh token in the vault and exposes claims", async () => {
    const { platform, nonce } = fakePlatform();
    const send = vi.fn(async (_input: string | URL | Request, init?: RequestInit) => {
      const body = init?.body as URLSearchParams;
      expect(body.get("code_verifier")).toBeTruthy();
      return json({
        access_token: jwt({ sub: "u-1", roles: ["MEMBER"], mid: "m-1" }),
        token_type: "Bearer",
        expires_in: 600,
        refresh_token: "rt-1",
        id_token: jwt({ sub: "u-1", nonce: nonce() }),
      });
    });
    const client = createAuthClient(config, platform, { fetch: send });
    const listener = vi.fn();
    client.subscribe(listener);

    await client.signIn();
    const claims = await client.completeSignIn();

    expect(claims).toMatchObject({ subject: "u-1", roles: ["MEMBER"], memberId: "m-1" });
    expect(client.isAuthenticated()).toBe(true);
    expect(await platform.refreshTokenVault.read()).toBe("rt-1");
    expect(listener).toHaveBeenCalledWith(expect.objectContaining({ subject: "u-1" }));
  });

  it("rejects an ID token with a different nonce", async () => {
    const { platform } = fakePlatform();
    const send = vi.fn(async () =>
      json({
        access_token: jwt({ sub: "u-1" }),
        token_type: "Bearer",
        expires_in: 600,
        id_token: jwt({ sub: "u-1", nonce: "other" }),
      }),
    );
    const client = createAuthClient(config, platform, { fetch: send });

    await client.signIn();
    await expect(client.completeSignIn()).rejects.toMatchObject({ code: "nonce_mismatch" });
    expect(client.isAuthenticated()).toBe(false);
  });

  it("fails completeSignIn without a pending transaction", async () => {
    const client = createAuthClient(config, fakePlatform().platform, { fetch: vi.fn() });
    await expect(client.completeSignIn()).rejects.toMatchObject({ code: "state_mismatch" });
  });

  it("returns undefined from restoreSession when there is no refresh token", async () => {
    const client = createAuthClient(config, fakePlatform().platform, { fetch: vi.fn() });
    expect(await client.restoreSession()).toBeUndefined();
  });

  it.each([
    ["current", "https://id.test/sessions/current"],
    ["all", "https://id.test/sessions"],
  ] as const)("signs out of the %s session(s)", async (scope, expectedUrl) => {
    const { platform } = fakePlatform();
    await platform.refreshTokenVault.write("rt-1");
    const send = vi.fn(async (_input: string | URL | Request, init?: RequestInit) => {
      if (init?.method === "DELETE") return new Response(null, { status: 204 });
      return json({
        access_token: jwt({ sub: "u-1" }),
        token_type: "Bearer",
        expires_in: 600,
        refresh_token: "rt-2",
      });
    });
    const client = createAuthClient(config, platform, { fetch: send });
    await client.restoreSession();

    await client.signOut(scope);

    const deleteCall = send.mock.calls.find(([, init]) => init?.method === "DELETE");
    expect(deleteCall?.[0]).toBe(expectedUrl);
    expect(new Headers(deleteCall?.[1]?.headers).get("Authorization")).toMatch(/^Bearer /);
    expect(await platform.refreshTokenVault.read()).toBeUndefined();
    expect(client.isAuthenticated()).toBe(false);
  });
});
