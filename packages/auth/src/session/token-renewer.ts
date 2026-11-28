import { AuthError } from "../errors/auth-error";
import type { TokenRefresher } from "../oauth/token-endpoint";
import type { TokenSet } from "../oauth/token-response";
import type { RefreshTokenVault } from "../platform/platform-adapter";

export class TokenRenewer {
  private readonly refresher: TokenRefresher;
  private readonly vault: RefreshTokenVault;
  private inFlight: Promise<TokenSet> | undefined;

  constructor(refresher: TokenRefresher, vault: RefreshTokenVault) {
    this.refresher = refresher;
    this.vault = vault;
  }

  renew(): Promise<TokenSet> {
    this.inFlight ??= this.run().finally(() => {
      this.inFlight = undefined;
    });
    return this.inFlight;
  }

  private async run(): Promise<TokenSet> {
    const refreshToken = await this.vault.read();
    if (!refreshToken)
      throw new AuthError("session_expired", "There is no refresh token to renew the session");
    try {
      const tokens = await this.refresher.refresh(refreshToken);
      if (tokens.refreshToken) await this.vault.write(tokens.refreshToken);
      return tokens;
    } catch (error) {
      if (error instanceof AuthError && error.requiresSignIn) await this.vault.clear();
      throw error;
    }
  }
}
