import { base64UrlEncode } from "../crypto/base64url";
import { webCryptoProvider, type CryptoProvider } from "../crypto/crypto-provider";

export const PKCE_METHOD = "S256";

const VERIFIER_ENTROPY_BYTES = 32;
const STATE_ENTROPY_BYTES = 16;

export interface PkcePair {
  readonly verifier: string;
  readonly challenge: string;
  readonly method: typeof PKCE_METHOD;
}

export async function deriveCodeChallenge(
  verifier: string,
  crypto: CryptoProvider = webCryptoProvider,
): Promise<string> {
  const digest = await crypto.sha256(new TextEncoder().encode(verifier));
  return base64UrlEncode(digest);
}

export async function createPkcePair(
  crypto: CryptoProvider = webCryptoProvider,
): Promise<PkcePair> {
  const verifier = base64UrlEncode(crypto.randomBytes(VERIFIER_ENTROPY_BYTES));
  return { verifier, challenge: await deriveCodeChallenge(verifier, crypto), method: PKCE_METHOD };
}

export function createState(crypto: CryptoProvider = webCryptoProvider): string {
  return base64UrlEncode(crypto.randomBytes(STATE_ENTROPY_BYTES));
}

export function createNonce(crypto: CryptoProvider = webCryptoProvider): string {
  return base64UrlEncode(crypto.randomBytes(STATE_ENTROPY_BYTES));
}
