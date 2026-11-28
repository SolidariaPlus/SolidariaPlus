import { describe, expect, it, vi } from "vitest";
import { AuthError } from "../errors/auth-error.js";
import type { FetchLike } from "../shared/types.js";
import { createAuthorizedFetch, type AccessTokenProvider } from "./authorized-fetch.js";

function tokens(overrides: Partial<AccessTokenProvider> = {}): AccessTokenProvider {
  return {
    getValidAccessToken: vi.fn(async () => "at-1"),
    renew: vi.fn(async () => "at-2"),
    ...overrides,
  };
}

function authorizationOf(call: unknown[] | undefined): string | null {
  const init = call?.[1] as RequestInit | undefined;
  return new Headers(init?.headers).get("Authorization");
}

describe("createAuthorizedFetch", () => {
  it("adds the bearer token and keeps existing headers", async () => {
    const send = vi.fn<FetchLike>(async () => new Response("ok"));
    const authorizedFetch = createAuthorizedFetch({
      tokens: tokens(),
      fetch: send,
      onSignInRequired: vi.fn(),
    });

    await authorizedFetch("https://api.test/x", { headers: { "X-Trace": "1" } });

    expect(new Headers(send.mock.calls[0]?.[1]?.headers).get("X-Trace")).toBe("1");
    expect(authorizationOf(send.mock.calls[0])).toBe("Bearer at-1");
  });

  it("renews and retries exactly once after a 401", async () => {
    const send = vi
      .fn<(input: string | URL | Request, init?: RequestInit) => Promise<Response>>()
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response("ok", { status: 200 }));
    const provider = tokens();
    const authorizedFetch = createAuthorizedFetch({
      tokens: provider,
      fetch: send,
      onSignInRequired: vi.fn(),
    });

    const response = await authorizedFetch("https://api.test/x");

    expect(response.status).toBe(200);
    expect(provider.renew).toHaveBeenCalledOnce();
    expect(authorizationOf(send.mock.calls[1])).toBe("Bearer at-2");
  });

  it("returns the second 401 without looping", async () => {
    const send = vi.fn(async () => new Response(null, { status: 401 }));
    const authorizedFetch = createAuthorizedFetch({
      tokens: tokens(),
      fetch: send,
      onSignInRequired: vi.fn(),
    });

    const response = await authorizedFetch("https://api.test/x");

    expect(response.status).toBe(401);
    expect(send).toHaveBeenCalledTimes(2);
  });

  it("sends the user back to sign-in on invalid_grant", async () => {
    const onSignInRequired = vi.fn();
    const send = vi.fn(async () => new Response(null, { status: 401 }));
    const provider = tokens({
      renew: vi.fn(async () => Promise.reject(new AuthError("invalid_grant"))),
    });
    const authorizedFetch = createAuthorizedFetch({
      tokens: provider,
      fetch: send,
      onSignInRequired,
    });

    await expect(authorizedFetch("https://api.test/x")).rejects.toMatchObject({
      code: "invalid_grant",
    });

    expect(onSignInRequired).toHaveBeenCalledWith(
      expect.objectContaining({ code: "invalid_grant" }),
    );
    expect(send).toHaveBeenCalledOnce();
  });

  it("does not ask for sign-in on transient renewal failures", async () => {
    const onSignInRequired = vi.fn();
    const provider = tokens({
      getValidAccessToken: vi.fn(async () =>
        Promise.reject(new AuthError("temporarily_unavailable")),
      ),
    });
    const authorizedFetch = createAuthorizedFetch({
      tokens: provider,
      fetch: vi.fn(),
      onSignInRequired,
    });

    await expect(authorizedFetch("https://api.test/x")).rejects.toMatchObject({
      code: "temporarily_unavailable",
    });
    expect(onSignInRequired).not.toHaveBeenCalled();
  });

  it("replays Request bodies on retry", async () => {
    const bodies: string[] = [];
    const send = vi.fn(async (input: string | URL | Request) => {
      bodies.push(await (input as Request).text());
      return new Response(null, { status: bodies.length === 1 ? 401 : 200 });
    });
    const authorizedFetch = createAuthorizedFetch({
      tokens: tokens(),
      fetch: send,
      onSignInRequired: vi.fn(),
    });

    await authorizedFetch(new Request("https://api.test/x", { method: "POST", body: "payload" }));

    expect(bodies).toEqual(["payload", "payload"]);
  });
});
