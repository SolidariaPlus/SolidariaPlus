import { createAuthClient, type FetchLike } from "@solidariaplus/auth";
import { afterEach, describe, expect, it, vi } from "vitest";
import { testConfig } from "../../../test/fake-auth-client";
import { TRANSACTION_STORAGE_KEY, WebPlatformAdapter } from "./web-platform-adapter";

function encode(payload: unknown): string {
  return btoa(JSON.stringify(payload)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

afterEach(() => {
  sessionStorage.clear();
  localStorage.clear();
});

describe("WebPlatformAdapter", () => {
  it("navigates to the authorization URL and stores only the transient transaction", async () => {
    const navigation = { assign: vi.fn(), href: "https://app.test/login" };
    const adapter = new WebPlatformAdapter(navigation, sessionStorage);
    const client = createAuthClient(testConfig, adapter, { fetch: vi.fn() });

    await client.signIn();

    const url = new URL(navigation.assign.mock.calls[0]?.[0] as string);
    expect(url.searchParams.get("code_challenge_method")).toBe("S256");
    const stored = JSON.parse(sessionStorage.getItem(TRANSACTION_STORAGE_KEY) ?? "{}");
    expect(stored.state).toBe(url.searchParams.get("state"));
  });

  it("never writes the refresh token to script-readable storage", async () => {
    const navigation = { assign: vi.fn(), href: "" };
    const adapter = new WebPlatformAdapter(navigation, sessionStorage);
    const tokenResponse = {
      access_token: `${encode({ alg: "ES256" })}.${encode({ sub: "u-1" })}.sig`,
      token_type: "Bearer",
      expires_in: 600,
      refresh_token: "secret-refresh-token",
    };
    const client = createAuthClient(testConfig, adapter, {
      fetch: vi.fn(async () => new Response(JSON.stringify(tokenResponse))),
    });
    await client.signIn();
    const state = new URL(navigation.assign.mock.calls[0]?.[0] as string).searchParams.get("state");
    navigation.href = `https://app.test/callback?code=c&state=${state}`;

    await client.completeSignIn();

    expect(adapter.refreshTokenVault.managedByCookie).toBe(true);
    expect(await adapter.refreshTokenVault.read()).toBeUndefined();
    const persisted = [
      ...Object.values({ ...sessionStorage }),
      ...Object.values({ ...localStorage }),
      document.cookie,
    ].join("|");
    expect(persisted).not.toContain("secret-refresh-token");
    expect(sessionStorage.getItem(TRANSACTION_STORAGE_KEY)).toBeNull();
  });

  it("renews through the HttpOnly cookie after a reload", async () => {
    const adapter = new WebPlatformAdapter({ assign: vi.fn(), href: "" }, sessionStorage);
    const send = vi.fn<FetchLike>(
      async () =>
        new Response(
          JSON.stringify({
            access_token: `${encode({ alg: "ES256" })}.${encode({ sub: "u-1" })}.sig`,
            token_type: "Bearer",
            expires_in: 600,
          }),
        ),
    );
    const client = createAuthClient(testConfig, adapter, { fetch: send });

    const claims = await client.restoreSession();

    expect(claims?.subject).toBe("u-1");
    const init = send.mock.calls[0]?.[1];
    expect(init?.credentials).toBe("include");
    expect((init?.body as URLSearchParams).has("refresh_token")).toBe(false);
  });
});
