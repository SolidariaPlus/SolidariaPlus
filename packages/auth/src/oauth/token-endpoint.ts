import { AuthError } from "../errors/auth-error.js";
import type { Clock, FetchLike } from "../shared/types.js";
import { parseTokenResponse, type TokenSet } from "./token-response.js";

export interface CodeExchanger {
  exchangeCode(code: string, codeVerifier: string, redirectUri: string): Promise<TokenSet>;
}

export interface TokenRefresher {
  refresh(refreshToken: string | undefined): Promise<TokenSet>;
}

export interface TokenEndpointOptions {
  readonly tokenUrl: string;
  readonly clientId: string;
  readonly fetch: FetchLike;
  readonly clock: Clock;
  readonly sendCredentials?: boolean;
}

export class TokenEndpoint implements CodeExchanger, TokenRefresher {
  private readonly options: TokenEndpointOptions;

  constructor(options: TokenEndpointOptions) {
    this.options = options;
  }

  exchangeCode(code: string, codeVerifier: string, redirectUri: string): Promise<TokenSet> {
    return this.post({
      grant_type: "authorization_code",
      code,
      code_verifier: codeVerifier,
      redirect_uri: redirectUri,
    });
  }

  refresh(refreshToken: string | undefined): Promise<TokenSet> {
    return this.post(
      refreshToken
        ? { grant_type: "refresh_token", refresh_token: refreshToken }
        : { grant_type: "refresh_token" },
    );
  }

  private async post(parameters: Record<string, string>): Promise<TokenSet> {
    const body = new URLSearchParams({ ...parameters, client_id: this.options.clientId });
    const response = await this.send(body);
    const payload = await readJson(response);
    if (!response.ok) throw AuthError.fromResponseBody(payload, response.status);
    return parseTokenResponse(payload, this.options.clock());
  }

  private async send(body: URLSearchParams): Promise<Response> {
    try {
      return await this.options.fetch(this.options.tokenUrl, {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          Accept: "application/json",
        },
        body,
        credentials: this.options.sendCredentials ? "include" : "same-origin",
      });
    } catch (cause) {
      throw new AuthError("network_error", cause instanceof Error ? cause.message : undefined);
    }
  }
}

async function readJson(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return undefined;
  }
}
