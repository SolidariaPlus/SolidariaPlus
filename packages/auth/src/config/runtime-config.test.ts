import { describe, expect, it, vi } from "vitest";
import { jsonConfigSource, loadRuntimeConfig, parseRuntimeConfig } from "./runtime-config";

const valid = {
  issuer: "https://id.test/auth/",
  clientId: "solidaria-web",
  redirectUri: "https://app.test/auth/callback",
  apiEndpoint: "https://api.test/",
};

describe("runtime configuration", () => {
  it("derives the endpoints from the issuer and applies default scopes", () => {
    const config = parseRuntimeConfig(valid);

    expect(config.issuer).toBe("https://id.test/auth");
    expect(config.apiEndpoint).toBe("https://api.test");
    expect(config.scopes).toEqual(["openid", "offline_access"]);
    expect(config.endpoints).toEqual({
      authorization: "https://id.test/auth/oauth2/authorize",
      token: "https://id.test/auth/oauth2/token",
      sessions: "https://id.test/auth/sessions",
      activation: "https://id.test/auth/activate",
    });
    expect(Object.isFrozen(config)).toBe(true);
  });

  it("accepts endpoint overrides", () => {
    const config = parseRuntimeConfig({
      ...valid,
      endpoints: { sessions: "https://api.test/auth/sessions" },
    });
    expect(config.endpoints.sessions).toBe("https://api.test/auth/sessions");
  });

  it.each([
    [{ ...valid, issuer: "not a url" }],
    [{ ...valid, clientId: "" }],
    [{ ...valid, scopes: ["openid", 3] }],
    [null],
  ])("rejects invalid configuration %#", (raw) => {
    expect(() => parseRuntimeConfig(raw)).toThrowError(
      expect.objectContaining({ code: "configuration_error" }),
    );
  });

  it("loads JSON from a URL", async () => {
    const send = vi.fn(async () => new Response(JSON.stringify(valid)));
    const config = await loadRuntimeConfig(jsonConfigSource("/runtime-config.json", send));
    expect(config.clientId).toBe("solidaria-web");
  });

  it("fails when the configuration cannot be fetched", async () => {
    const send = vi.fn(async () => new Response(null, { status: 404 }));
    await expect(loadRuntimeConfig(jsonConfigSource("/missing.json", send))).rejects.toMatchObject({
      code: "configuration_error",
    });
  });
});
