import type { AuthRuntimeConfig } from "../config/runtime-config";
import { webCryptoProvider, type CryptoProvider } from "../crypto/crypto-provider";
import { AuthError } from "../errors/auth-error";
import { createNonce, createPkcePair, createState } from "../pkce/pkce";

export interface AuthorizationTransaction {
  readonly state: string;
  readonly nonce: string;
  readonly codeVerifier: string;
  readonly redirectUri: string;
}

export interface AuthorizationRequest {
  readonly url: URL;
  readonly transaction: AuthorizationTransaction;
}

export interface AuthorizationRequestOptions {
  readonly redirectUri?: string;
  readonly extraParameters?: Readonly<Record<string, string>>;
}

export async function createAuthorizationRequest(
  config: AuthRuntimeConfig,
  options: AuthorizationRequestOptions = {},
  crypto: CryptoProvider = webCryptoProvider,
): Promise<AuthorizationRequest> {
  const pkce = await createPkcePair(crypto);
  const transaction: AuthorizationTransaction = {
    state: createState(crypto),
    nonce: createNonce(crypto),
    codeVerifier: pkce.verifier,
    redirectUri: options.redirectUri ?? config.redirectUri,
  };
  const url = new URL(config.endpoints.authorization);
  const parameters: Record<string, string> = {
    ...options.extraParameters,
    response_type: "code",
    client_id: config.clientId,
    redirect_uri: transaction.redirectUri,
    scope: config.scopes.join(" "),
    state: transaction.state,
    nonce: transaction.nonce,
    code_challenge: pkce.challenge,
    code_challenge_method: pkce.method,
  };
  for (const [key, value] of Object.entries(parameters)) url.searchParams.set(key, value);
  return { url, transaction };
}

export function parseAuthorizationResponse(redirect: URL, expectedState: string): string {
  const parameters = redirect.searchParams;
  const error = parameters.get("error");
  if (error) {
    throw new AuthError(error, parameters.get("error_description") ?? undefined);
  }
  if (parameters.get("state") !== expectedState) {
    throw new AuthError(
      "state_mismatch",
      "The authorization response state does not match the request",
    );
  }
  const code = parameters.get("code");
  if (!code) throw new AuthError("invalid_response", "The authorization response has no code");
  return code;
}
