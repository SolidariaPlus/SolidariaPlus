export { createAuthClient } from "./auth-client.js";
export type {
  AuthClient,
  AuthClientDependencies,
  AuthStateListener,
  SignInOptions,
} from "./auth-client.js";

export {
  jsonConfigSource,
  loadRuntimeConfig,
  parseRuntimeConfig,
} from "./config/runtime-config.js";
export type {
  AuthEndpoints,
  AuthRuntimeConfig,
  RuntimeConfigSource,
} from "./config/runtime-config.js";

export { webCryptoProvider } from "./crypto/crypto-provider.js";
export type { CryptoProvider } from "./crypto/crypto-provider.js";

export {
  createNonce,
  createPkcePair,
  createState,
  deriveCodeChallenge,
  PKCE_METHOD,
} from "./pkce/pkce.js";
export type { PkcePair } from "./pkce/pkce.js";

export {
  createAuthorizationRequest,
  parseAuthorizationResponse,
} from "./oauth/authorization-request.js";
export type {
  AuthorizationRequest,
  AuthorizationRequestOptions,
  AuthorizationTransaction,
} from "./oauth/authorization-request.js";
export { TokenEndpoint } from "./oauth/token-endpoint.js";
export type {
  CodeExchanger,
  TokenEndpointOptions,
  TokenRefresher,
} from "./oauth/token-endpoint.js";
export { parseTokenResponse } from "./oauth/token-response.js";
export type { TokenSet } from "./oauth/token-response.js";

export { InMemoryAccessTokenStore, isExpiring } from "./session/access-token-store.js";
export type { AccessToken, AccessTokenStore } from "./session/access-token-store.js";
export { TokenRenewer } from "./session/token-renewer.js";
export { DEFAULT_RENEW_BEFORE_EXPIRY_MS, SessionManager } from "./session/session-manager.js";
export type { AccessTokenListener, SessionManagerOptions } from "./session/session-manager.js";
export { SessionRevoker } from "./session/sign-out.js";
export type { SignOutScope } from "./session/sign-out.js";

export {
  decodeJwtPayload,
  hasAnyRole,
  usedSecondFactor,
  hasFunction,
  hasRole,
  isMember,
  parseClaims,
} from "./claims/claims.js";
export type { AuthClaims } from "./claims/claims.js";

export {
  AuthError,
  CLIENT_ERROR_CODES,
  DOMAIN_ERROR_CODES,
  isAuthError,
  isAuthErrorCode,
  OAUTH_ERROR_CODES,
  SIGN_IN_PAGE_ERROR_CODES,
} from "./errors/auth-error.js";
export type {
  AuthErrorCode,
  ClientErrorCode,
  DomainErrorCode,
  OAuthErrorCode,
  SignInPageErrorCode,
} from "./errors/auth-error.js";
export {
  DEFAULT_ERROR_MESSAGE,
  ERROR_MESSAGES_ES,
  messageForCode,
  toUserMessage,
} from "./errors/messages-es.js";

export { createAuthorizedFetch } from "./http/authorized-fetch.js";
export type {
  AccessTokenProvider,
  AuthorizedFetchOptions,
  SignInRequiredHandler,
} from "./http/authorized-fetch.js";
export { createGraphQLClient, GraphQLRequestError } from "./http/graphql-client.js";
export type {
  GraphQLClient,
  GraphQLClientOptions,
  GraphQLErrorEntry,
  GraphQLRequest,
} from "./http/graphql-client.js";

export {
  HttpOnlyCookieRefreshVault,
  MemoryRefreshTokenVault,
} from "./platform/platform-adapter.js";
export type {
  BrowserLauncher,
  PlatformAdapter,
  RedirectReceiver,
  RefreshTokenVault,
  TransactionStore,
} from "./platform/platform-adapter.js";

export { systemClock } from "./shared/types.js";
export type { Clock, FetchLike } from "./shared/types.js";
