import { decodeJwtPayload, parseClaims, type AuthClaims } from "./claims/claims.js";
import type { AuthRuntimeConfig } from "./config/runtime-config.js";
import { webCryptoProvider, type CryptoProvider } from "./crypto/crypto-provider.js";
import { AuthError } from "./errors/auth-error.js";
import { createAuthorizedFetch, type SignInRequiredHandler } from "./http/authorized-fetch.js";
import {
  createAuthorizationRequest,
  parseAuthorizationResponse,
  type AuthorizationTransaction,
} from "./oauth/authorization-request.js";
import { TokenEndpoint, type CodeExchanger } from "./oauth/token-endpoint.js";
import type { TokenSet } from "./oauth/token-response.js";
import type { PlatformAdapter } from "./platform/platform-adapter.js";
import { InMemoryAccessTokenStore } from "./session/access-token-store.js";
import { SessionManager } from "./session/session-manager.js";
import { SessionRevoker, type SignOutScope } from "./session/sign-out.js";
import { TokenRenewer } from "./session/token-renewer.js";
import { systemClock, type Clock, type FetchLike } from "./shared/types.js";

export type AuthStateListener = (claims: AuthClaims | undefined) => void;

export interface SignInOptions {
  readonly extraParameters?: Readonly<Record<string, string>>;
}

export interface AuthClient {
  readonly config: AuthRuntimeConfig;
  readonly fetch: FetchLike;
  signIn(options?: SignInOptions): Promise<void>;
  completeSignIn(): Promise<AuthClaims>;
  restoreSession(): Promise<AuthClaims | undefined>;
  signOut(scope?: SignOutScope): Promise<void>;
  getAccessToken(): Promise<string>;
  getClaims(): AuthClaims | undefined;
  isAuthenticated(): boolean;
  subscribe(listener: AuthStateListener): () => void;
}

export interface AuthClientDependencies {
  readonly crypto?: CryptoProvider;
  readonly fetch?: FetchLike;
  readonly clock?: Clock;
  readonly renewBeforeExpiryMs?: number;
  readonly onSignInRequired?: SignInRequiredHandler;
}

interface OAuthClientCollaborators {
  readonly config: AuthRuntimeConfig;
  readonly platform: PlatformAdapter;
  readonly crypto: CryptoProvider;
  readonly codeExchanger: CodeExchanger;
  readonly session: SessionManager;
  readonly revoker: SessionRevoker;
  readonly authorizedFetch: FetchLike;
}

class OAuthClient implements AuthClient {
  readonly config: AuthRuntimeConfig;
  readonly fetch: FetchLike;
  private readonly deps: OAuthClientCollaborators;
  private cachedClaims: { readonly token: string; readonly claims: AuthClaims } | undefined;

  constructor(collaborators: OAuthClientCollaborators) {
    this.deps = collaborators;
    this.config = collaborators.config;
    this.fetch = collaborators.authorizedFetch;
  }

  async signIn(options: SignInOptions = {}): Promise<void> {
    const { platform, crypto } = this.deps;
    const redirectUri = platform.prepareRedirectUri
      ? await platform.prepareRedirectUri(this.config.redirectUri)
      : this.config.redirectUri;
    const request = await createAuthorizationRequest(
      this.config,
      { redirectUri, extraParameters: options.extraParameters },
      crypto,
    );
    await platform.transactionStore.save(request.transaction);
    await platform.openAuthorization(request.url);
  }

  async completeSignIn(): Promise<AuthClaims> {
    const { platform, codeExchanger, session } = this.deps;
    const transaction = await platform.transactionStore.take();
    if (!transaction)
      throw new AuthError("state_mismatch", "There is no pending authorization request");
    const code = parseAuthorizationResponse(await platform.receiveRedirect(), transaction.state);
    const tokens = await codeExchanger.exchangeCode(
      code,
      transaction.codeVerifier,
      transaction.redirectUri,
    );
    verifyNonce(tokens, transaction);
    if (tokens.refreshToken) await platform.refreshTokenVault.write(tokens.refreshToken);
    session.accept(tokens);
    return this.claimsOf(tokens.accessToken);
  }

  async restoreSession(): Promise<AuthClaims | undefined> {
    try {
      return this.claimsOf(await this.deps.session.getValidAccessToken());
    } catch (error) {
      if (error instanceof AuthError && error.requiresSignIn) return undefined;
      throw error;
    }
  }

  async signOut(scope: SignOutScope = "current"): Promise<void> {
    const { session, revoker, platform } = this.deps;
    const accessToken =
      session.currentAccessToken() ?? (await session.getValidAccessToken().catch(() => undefined));
    if (accessToken) await revoker.revoke(scope, accessToken);
    await platform.refreshTokenVault.clear();
    session.clear();
  }

  getAccessToken(): Promise<string> {
    return this.deps.session.getValidAccessToken();
  }

  getClaims(): AuthClaims | undefined {
    const token = this.deps.session.currentAccessToken();
    return token ? this.claimsOf(token) : undefined;
  }

  isAuthenticated(): boolean {
    return this.deps.session.currentAccessToken() !== undefined;
  }

  subscribe(listener: AuthStateListener): () => void {
    return this.deps.session.subscribe((token) =>
      listener(token ? this.claimsOf(token) : undefined),
    );
  }

  private claimsOf(token: string): AuthClaims {
    if (this.cachedClaims?.token !== token)
      this.cachedClaims = { token, claims: parseClaims(token) };
    return this.cachedClaims.claims;
  }
}

function verifyNonce(tokens: TokenSet, transaction: AuthorizationTransaction): void {
  if (!tokens.idToken) return;
  if (decodeJwtPayload(tokens.idToken)["nonce"] !== transaction.nonce) {
    throw new AuthError("nonce_mismatch", "The ID token nonce does not match the request");
  }
}

export function createAuthClient(
  config: AuthRuntimeConfig,
  platform: PlatformAdapter,
  dependencies: AuthClientDependencies = {},
): AuthClient {
  const fetchFn: FetchLike = dependencies.fetch ?? ((input, init) => fetch(input, init));
  const clock = dependencies.clock ?? systemClock;
  const tokenEndpoint = new TokenEndpoint({
    tokenUrl: config.endpoints.token,
    clientId: config.clientId,
    fetch: fetchFn,
    clock,
    sendCredentials: platform.refreshTokenVault.managedByCookie === true,
  });
  const session = new SessionManager({
    store: new InMemoryAccessTokenStore(),
    renewer: new TokenRenewer(tokenEndpoint, platform.refreshTokenVault),
    clock,
    renewBeforeExpiryMs: dependencies.renewBeforeExpiryMs,
  });
  const authorizedFetch = createAuthorizedFetch({
    tokens: session,
    fetch: fetchFn,
    onSignInRequired: (error) => dependencies.onSignInRequired?.(error),
  });
  return new OAuthClient({
    config,
    platform,
    crypto: dependencies.crypto ?? webCryptoProvider,
    codeExchanger: tokenEndpoint,
    session,
    revoker: new SessionRevoker({ sessionsUrl: config.endpoints.sessions, fetch: fetchFn }),
    authorizedFetch,
  });
}
