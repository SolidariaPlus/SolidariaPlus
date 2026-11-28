import type { AuthorizationTransaction } from "../oauth/authorization-request.js";

export interface BrowserLauncher {
  openAuthorization(url: URL): Promise<void>;
}

export interface RedirectReceiver {
  prepareRedirectUri?(configuredRedirectUri: string): Promise<string>;
  receiveRedirect(): Promise<URL>;
}

export interface RefreshTokenVault {
  readonly managedByCookie?: boolean;
  read(): Promise<string | undefined>;
  write(refreshToken: string): Promise<void>;
  clear(): Promise<void>;
}

export interface TransactionStore {
  save(transaction: AuthorizationTransaction): Promise<void>;
  take(): Promise<AuthorizationTransaction | undefined>;
}

export interface PlatformAdapter extends BrowserLauncher, RedirectReceiver {
  readonly refreshTokenVault: RefreshTokenVault;
  readonly transactionStore: TransactionStore;
}

export class MemoryRefreshTokenVault implements RefreshTokenVault {
  private token: string | undefined;

  async read(): Promise<string | undefined> {
    return this.token;
  }

  async write(refreshToken: string): Promise<void> {
    this.token = refreshToken;
  }

  async clear(): Promise<void> {
    this.token = undefined;
  }
}

export class HttpOnlyCookieRefreshVault implements RefreshTokenVault {
  readonly managedByCookie = true;

  async read(): Promise<string | undefined> {
    return undefined;
  }

  async write(): Promise<void> {}

  async clear(): Promise<void> {}
}
