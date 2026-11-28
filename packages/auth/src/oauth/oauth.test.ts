import { describe, expect, it, vi } from "vitest";
import type { FetchLike } from "../shared/types.js";
import { parseRuntimeConfig } from "../config/runtime-config.js";
import { deriveCodeChallenge } from "../pkce/pkce.js";
import { createAuthorizationRequest, parseAuthorizationResponse } from "./authorization-request.js";
import { TokenEndpoint } from "./token-endpoint.js";
import { parseTokenResponse } from "./token-response.js";

const config = parseRuntimeConfig({
  issuer: "https://id.test/auth/",
  clientId: "solidaria-web",
  redirectUri: "https://app.test/auth/callback",
  apiEndpoint: "https://api.test",
});

describe("createAuthorizationRequest", () => {
  it("builds an authorization code request with PKCE S256, state and nonce", async () => {
    const { url, transaction } = await createAuthorizationRequest(config, {
      extraParameters: { prompt: "login" },
    });
    const parameters = url.searchParams;

    expect(`${url.origin}${url.pathname}`).toBe("https://id.test/auth/oauth2/authorize");
    expect(parameters.get("response_type")).toBe("code");
    expect(parameters.get("client_id")).toBe("solidaria-web");
    expect(parameters.get("redirect_uri")).toBe("https://app.test/auth/callback");
    expect(parameters.get("scope")).toBe("openid offline_access");
    expect(parameters.get("code_challenge_method")).toBe("S256");
    expect(parameters.get("code_challenge")).toBe(
      await deriveCodeChallenge(transaction.codeVerifier),
    );
    expect(parameters.get("state")).toBe(transaction.state);
    expect(parameters.get("nonce")).toBe(transaction.nonce);
    expect(parameters.get("prompt")).toBe("login");
  });

  it("does not let extra parameters override protocol parameters", async () => {
    const { url } = await createAuthorizationRequest(config, {
      extraParameters: { client_id: "evil" },
    });
    expect(url.searchParams.get("client_id")).toBe("solidaria-web");
  });
});

describe("parseAuthorizationResponse", () => {
  it("returns the code when the state matches", () => {
    expect(parseAuthorizationResponse(new URL("https://app.test/cb?code=abc&state=s1"), "s1")).toBe(
      "abc",
    );
  });

  it("rejects a state mismatch", () => {
    expect(() =>
      parseAuthorizationResponse(new URL("https://app.test/cb?code=abc&state=s2"), "s1"),
    ).toThrowError(expect.objectContaining({ code: "state_mismatch" }));
  });

  it("surfaces the RFC 6749 error from the redirect", () => {
    expect(() =>
      parseAuthorizationResponse(new URL("https://app.test/cb?error=access_denied&state=s1"), "s1"),
    ).toThrowError(expect.objectContaining({ code: "access_denied" }));
  });
});

describe("parseTokenResponse", () => {
  it("computes the absolute expiry", () => {
    const tokens = parseTokenResponse(
      {
        access_token: "at",
        token_type: "Bearer",
        expires_in: 600,
        refresh_token: "rt",
        id_token: "it",
      },
      1_000,
    );
    expect(tokens).toMatchObject({
      accessToken: "at",
      refreshToken: "rt",
      idToken: "it",
      expiresAt: 601_000,
    });
  });

  it.each([
    [{ token_type: "Bearer", expires_in: 600 }],
    [{ access_token: "at", token_type: "mac", expires_in: 600 }],
    [{ access_token: "at", token_type: "Bearer", expires_in: 0 }],
    [null],
  ])("rejects malformed responses %#", (body) => {
    expect(() => parseTokenResponse(body, 0)).toThrowError(
      expect.objectContaining({ code: "invalid_response" }),
    );
  });
});

describe("TokenEndpoint", () => {
  it("exchanges the code with the verifier as a form post", async () => {
    const send = vi.fn(
      async () =>
        new Response(JSON.stringify({ access_token: "at", token_type: "Bearer", expires_in: 600 })),
    );
    const endpoint = new TokenEndpoint({
      tokenUrl: config.endpoints.token,
      clientId: "solidaria-web",
      fetch: send,
      clock: () => 0,
    });

    await endpoint.exchangeCode("code-1", "verifier-1", "https://app.test/auth/callback");

    const [url, init] = send.mock.calls[0] as unknown as [string, RequestInit];
    const body = init.body as URLSearchParams;
    expect(url).toBe("https://id.test/auth/oauth2/token");
    expect(body.get("grant_type")).toBe("authorization_code");
    expect(body.get("code_verifier")).toBe("verifier-1");
    expect(body.get("client_id")).toBe("solidaria-web");
  });

  it("maps error responses to AuthError", async () => {
    const send = vi.fn(
      async () => new Response(JSON.stringify({ error: "invalid_grant" }), { status: 400 }),
    );
    const endpoint = new TokenEndpoint({
      tokenUrl: config.endpoints.token,
      clientId: "c",
      fetch: send,
      clock: () => 0,
    });

    await expect(endpoint.refresh("rt")).rejects.toMatchObject({
      code: "invalid_grant",
      status: 400,
    });
  });

  it("refreshes through the cookie without a refresh_token parameter", async () => {
    const send = vi.fn<FetchLike>(
      async () =>
        new Response(JSON.stringify({ access_token: "at", token_type: "Bearer", expires_in: 600 })),
    );
    const endpoint = new TokenEndpoint({
      tokenUrl: config.endpoints.token,
      clientId: "solidaria-web",
      fetch: send,
      clock: () => 0,
      sendCredentials: true,
    });

    await endpoint.refresh(undefined);

    const init = send.mock.calls[0]?.[1];
    const body = init?.body as URLSearchParams;
    expect(body.get("grant_type")).toBe("refresh_token");
    expect(body.has("refresh_token")).toBe(false);
    expect(init?.credentials).toBe("include");
  });

  it("maps network failures to network_error", async () => {
    const send = vi.fn(async () => Promise.reject(new TypeError("offline")));
    const endpoint = new TokenEndpoint({
      tokenUrl: config.endpoints.token,
      clientId: "c",
      fetch: send,
      clock: () => 0,
    });

    await expect(endpoint.refresh("rt")).rejects.toMatchObject({ code: "network_error" });
  });
});
