import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthError } from "../errors/auth-error";
import type { TokenSet } from "../oauth/token-response";
import { InMemoryAccessTokenStore, isExpiring } from "./access-token-store";
import { SessionManager } from "./session-manager";

const TEN_MINUTES = 600_000;

function tokenSet(accessToken: string, expiresAt: number): TokenSet {
  return { accessToken, expiresAt, refreshToken: undefined, idToken: undefined, scope: undefined };
}

describe("isExpiring", () => {
  it("is true only inside the renewal window", () => {
    const token = { value: "at", expiresAt: 100_000 };
    expect(isExpiring(token, 39_999, 60_000)).toBe(false);
    expect(isExpiring(token, 40_000, 60_000)).toBe(true);
    expect(isExpiring(token, 100_001, 0)).toBe(true);
  });
});

describe("SessionManager", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(0);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function setup(renew: () => Promise<TokenSet>) {
    const renewer = { renew: vi.fn(renew) };
    const manager = new SessionManager({
      store: new InMemoryAccessTokenStore(),
      renewer,
      clock: () => Date.now(),
      renewBeforeExpiryMs: 60_000,
    });
    return { manager, renewer };
  }

  it("returns the stored token while it is far from expiry", async () => {
    const { manager, renewer } = setup(async () => tokenSet("renewed", Date.now() + TEN_MINUTES));
    manager.accept(tokenSet("initial", TEN_MINUTES));

    vi.advanceTimersByTime(TEN_MINUTES - 60_001);

    expect(await manager.getValidAccessToken()).toBe("initial");
    expect(renewer.renew).not.toHaveBeenCalled();
  });

  it("renews shortly before expiry", async () => {
    const { manager, renewer } = setup(async () => tokenSet("renewed", Date.now() + TEN_MINUTES));
    manager.accept(tokenSet("initial", TEN_MINUTES));

    vi.advanceTimersByTime(TEN_MINUTES - 60_000);

    expect(await manager.getValidAccessToken()).toBe("renewed");
    expect(renewer.renew).toHaveBeenCalledOnce();
  });

  it("renews when there is no token yet", async () => {
    const { manager } = setup(async () => tokenSet("fresh", TEN_MINUTES));
    expect(await manager.getValidAccessToken()).toBe("fresh");
  });

  it("notifies listeners on change and clears the token when renewal fails", async () => {
    const { manager } = setup(async () => Promise.reject(new AuthError("invalid_grant")));
    const listener = vi.fn();
    manager.subscribe(listener);
    manager.accept(tokenSet("initial", 1_000));

    await expect(manager.getValidAccessToken()).rejects.toMatchObject({ code: "invalid_grant" });

    expect(listener.mock.calls).toEqual([["initial"], [undefined]]);
    expect(manager.currentAccessToken()).toBeUndefined();
  });
});
