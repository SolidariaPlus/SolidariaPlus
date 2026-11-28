import { AuthError } from "../errors/auth-error";
import type { FetchLike } from "../shared/types";

export interface AccessTokenProvider {
  getValidAccessToken(): Promise<string>;
  renew(): Promise<string>;
}

export type SignInRequiredHandler = (error: AuthError) => void;

export interface AuthorizedFetchOptions {
  readonly tokens: AccessTokenProvider;
  readonly onSignInRequired: SignInRequiredHandler;
  readonly fetch?: FetchLike;
}

const UNAUTHORIZED = 401;

export function createAuthorizedFetch(options: AuthorizedFetchOptions): FetchLike {
  const send: FetchLike = options.fetch ?? ((input, init) => fetch(input, init));

  const obtain = async (source: () => Promise<string>): Promise<string> => {
    try {
      return await source();
    } catch (error) {
      if (error instanceof AuthError && error.requiresSignIn) options.onSignInRequired(error);
      throw error;
    }
  };

  const attempt = (input: string | URL | Request, init: RequestInit | undefined, token: string) => {
    const request = input instanceof Request ? input.clone() : input;
    return send(request, withAuthorization(input, init, token));
  };

  return async (input, init) => {
    const response = await attempt(
      input,
      init,
      await obtain(() => options.tokens.getValidAccessToken()),
    );
    if (response.status !== UNAUTHORIZED) return response;
    return attempt(input, init, await obtain(() => options.tokens.renew()));
  };
}

function withAuthorization(
  input: string | URL | Request,
  init: RequestInit | undefined,
  token: string,
): RequestInit {
  const headers = new Headers(
    init?.headers ?? (input instanceof Request ? input.headers : undefined),
  );
  headers.set("Authorization", `Bearer ${token}`);
  return { ...init, headers };
}
