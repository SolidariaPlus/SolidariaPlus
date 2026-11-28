export { createAuthClient } from "./auth-client";
export type {
  AuthClient,
  AuthClientDependencies,
  AuthStateListener,
  SignInOptions,
} from "./auth-client";

export { jsonConfigSource, loadRuntimeConfig, parseRuntimeConfig } from "./config/runtime-config";
export type {
  AuthEndpoints,
  AuthRuntimeConfig,
  RuntimeConfigSource,
} from "./config/runtime-config";

export { webCryptoProvider } from "./crypto/crypto-provider";
export type { CryptoProvider } from "./crypto/crypto-provider";

export {
  createNonce,
  createPkcePair,
  createState,
  deriveCodeChallenge,
  PKCE_METHOD,
} from "./pkce/pkce";
export type { PkcePair } from "./pkce/pkce";

export {
  createAuthorizationRequest,
  parseAuthorizationResponse,
} from "./oauth/authorization-request";
export type {
  AuthorizationRequest,
  AuthorizationRequestOptions,
  AuthorizationTransaction,
} from "./oauth/authorization-request";
export { TokenEndpoint } from "./oauth/token-endpoint";
export type { CodeExchanger, TokenEndpointOptions, TokenRefresher } from "./oauth/token-endpoint";
export { parseTokenResponse } from "./oauth/token-response";
export type { TokenSet } from "./oauth/token-response";

export { InMemoryAccessTokenStore, isExpiring } from "./session/access-token-store";
export type { AccessToken, AccessTokenStore } from "./session/access-token-store";
export { TokenRenewer } from "./session/token-renewer";
export { DEFAULT_RENEW_BEFORE_EXPIRY_MS, SessionManager } from "./session/session-manager";
export type { AccessTokenListener, SessionManagerOptions } from "./session/session-manager";
export { SessionRevoker } from "./session/sign-out";
export type { SignOutScope } from "./session/sign-out";

export {
  decodeJwtPayload,
  hasAnyRole,
  hasFunction,
  hasRole,
  isMember,
  parseClaims,
} from "./claims/claims";
export type { AuthClaims } from "./claims/claims";

export {
  AuthError,
  CLIENT_ERROR_CODES,
  DOMAIN_ERROR_CODES,
  isAuthError,
  isAuthErrorCode,
  OAUTH_ERROR_CODES,
} from "./errors/auth-error";
export type {
  AuthErrorCode,
  ClientErrorCode,
  DomainErrorCode,
  OAuthErrorCode,
} from "./errors/auth-error";
export {
  DEFAULT_ERROR_MESSAGE,
  ERROR_MESSAGES_ES,
  messageForCode,
  toUserMessage,
} from "./errors/messages-es";

export { createAuthorizedFetch } from "./http/authorized-fetch";
export type {
  AccessTokenProvider,
  AuthorizedFetchOptions,
  SignInRequiredHandler,
} from "./http/authorized-fetch";
export { createGraphQLClient, GraphQLRequestError } from "./http/graphql-client";
export type {
  GraphQLClient,
  GraphQLClientOptions,
  GraphQLErrorEntry,
  GraphQLRequest,
} from "./http/graphql-client";

export { MemoryRefreshTokenVault } from "./platform/platform-adapter";
export type {
  BrowserLauncher,
  PlatformAdapter,
  RedirectReceiver,
  RefreshTokenVault,
  TransactionStore,
} from "./platform/platform-adapter";

export { systemClock } from "./shared/types";
export type { Clock, FetchLike } from "./shared/types";
