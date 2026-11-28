import { describe, expect, it, vi } from "vitest";
import { AuthError } from "../errors/auth-error";
import type { TokenSet } from "../oauth/token-response";
import { MemoryRefreshTokenVault } from "../platform/platform-adapter";
import { TokenRenewer } from "./token-renewer";

function tokenSet(accessToken: string, refreshToken?: string): TokenSet {
  return { accessToken, refreshToken, expiresAt: 1_000_000, idToken: undefined, scope: undefined };
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

async function vaultWith(token: string | undefined) {
  const vault = new MemoryRefreshTokenVault();
  if (token) await vault.write(token);
  return vault;
}

describe("TokenRenewer", () => {
  it("shares a single in-flight refresh between concurrent callers", async () => {
    const pending = deferred<TokenSet>();
    const refresher = { refresh: vi.fn(() => pending.promise) };
    const renewer = new TokenRenewer(refresher, await vaultWith("rt-1"));

    const calls = Array.from({ length: 5 }, () => renewer.renew());
    await Promise.resolve();
    pending.resolve(tokenSet("at-2", "rt-2"));
    const results = await Promise.all(calls);

    expect(refresher.refresh).toHaveBeenCalledOnce();
    expect(new Set(results.map((result) => result.accessToken))).toEqual(new Set(["at-2"]));
  });

  it("starts a new refresh once the previous one settled", async () => {
    const refresher = {
      refresh: vi.fn(async (token: string) => tokenSet(`at-for-${token}`, `${token}+`)),
    };
    const renewer = new TokenRenewer(refresher, await vaultWith("rt"));

    await renewer.renew();
    const second = await renewer.renew();

    expect(refresher.refresh).toHaveBeenCalledTimes(2);
    expect(refresher.refresh).toHaveBeenLastCalledWith("rt+");
    expect(second.accessToken).toBe("at-for-rt+");
  });

  it("stores the rotated refresh token", async () => {
    const vault = await vaultWith("rt-1");
    const renewer = new TokenRenewer({ refresh: async () => tokenSet("at", "rt-2") }, vault);

    await renewer.renew();

    expect(await vault.read()).toBe("rt-2");
  });

  it("fails with session_expired when there is no refresh token", async () => {
    const refresher = { refresh: vi.fn() };
    const renewer = new TokenRenewer(refresher, await vaultWith(undefined));

    await expect(renewer.renew()).rejects.toMatchObject({ code: "session_expired" });
    expect(refresher.refresh).not.toHaveBeenCalled();
  });

  it("clears the vault on invalid_grant and propagates the same error to every caller", async () => {
    const vault = await vaultWith("rt-reused");
    const renewer = new TokenRenewer(
      { refresh: async () => Promise.reject(new AuthError("invalid_grant")) },
      vault,
    );

    const results = await Promise.allSettled([renewer.renew(), renewer.renew()]);

    expect(results.every((result) => result.status === "rejected")).toBe(true);
    expect(await vault.read()).toBeUndefined();
  });

  it("keeps the refresh token on transient failures", async () => {
    const vault = await vaultWith("rt");
    const renewer = new TokenRenewer(
      { refresh: async () => Promise.reject(new AuthError("temporarily_unavailable")) },
      vault,
    );

    await expect(renewer.renew()).rejects.toMatchObject({ code: "temporarily_unavailable" });
    expect(await vault.read()).toBe("rt");
  });
});
