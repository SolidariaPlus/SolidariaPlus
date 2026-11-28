import { describe, expect, it } from "vitest";
import type { CryptoProvider } from "../crypto/crypto-provider";
import { webCryptoProvider } from "../crypto/crypto-provider";
import { createNonce, createPkcePair, createState, deriveCodeChallenge, PKCE_METHOD } from "./pkce";

const UNRESERVED = /^[A-Za-z0-9\-._~]+$/;

describe("PKCE", () => {
  it("derives the RFC 7636 appendix B challenge", async () => {
    const challenge = await deriveCodeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");
    expect(challenge).toBe("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
  });

  it("creates a verifier between 43 and 128 unreserved characters with an S256 challenge", async () => {
    const pair = await createPkcePair();
    expect(pair.method).toBe(PKCE_METHOD);
    expect(pair.verifier.length).toBeGreaterThanOrEqual(43);
    expect(pair.verifier.length).toBeLessThanOrEqual(128);
    expect(pair.verifier).toMatch(UNRESERVED);
    expect(pair.challenge).toBe(await deriveCodeChallenge(pair.verifier));
    expect(pair.challenge).not.toContain("=");
  });

  it("uses the injected crypto provider", async () => {
    const fixed: CryptoProvider = {
      randomBytes: (length) => new Uint8Array(length).fill(7),
      sha256: webCryptoProvider.sha256,
    };
    const first = await createPkcePair(fixed);
    const second = await createPkcePair(fixed);
    expect(first.verifier).toBe(second.verifier);
  });

  it("generates unique state and nonce values", () => {
    const values = new Set(Array.from({ length: 50 }, () => [createState(), createNonce()]).flat());
    expect(values.size).toBe(100);
    for (const value of values) expect(value).toMatch(UNRESERVED);
  });
});
