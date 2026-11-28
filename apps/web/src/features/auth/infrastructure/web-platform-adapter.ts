import {
  HttpOnlyCookieRefreshVault,
  type AuthorizationTransaction,
  type PlatformAdapter,
  type RefreshTokenVault,
  type TransactionStore,
} from "@solidariaplus/auth";

export const TRANSACTION_STORAGE_KEY = "solidaria.auth.transaction";

export interface BrowserNavigation {
  assign(url: string): void;
  readonly href: string;
}

export class SessionStorageTransactionStore implements TransactionStore {
  private readonly storage: Storage;

  constructor(storage: Storage) {
    this.storage = storage;
  }

  async save(transaction: AuthorizationTransaction): Promise<void> {
    this.storage.setItem(TRANSACTION_STORAGE_KEY, JSON.stringify(transaction));
  }

  async take(): Promise<AuthorizationTransaction | undefined> {
    const raw = this.storage.getItem(TRANSACTION_STORAGE_KEY);
    this.storage.removeItem(TRANSACTION_STORAGE_KEY);
    if (!raw) return undefined;
    try {
      return JSON.parse(raw) as AuthorizationTransaction;
    } catch {
      return undefined;
    }
  }
}

export class WebPlatformAdapter implements PlatformAdapter {
  readonly refreshTokenVault: RefreshTokenVault = new HttpOnlyCookieRefreshVault();
  readonly transactionStore: TransactionStore;
  private readonly navigation: BrowserNavigation;

  constructor(navigation: BrowserNavigation, transientStorage: Storage) {
    this.navigation = navigation;
    this.transactionStore = new SessionStorageTransactionStore(transientStorage);
  }

  async openAuthorization(url: URL): Promise<void> {
    this.navigation.assign(url.toString());
  }

  async receiveRedirect(): Promise<URL> {
    return new URL(this.navigation.href);
  }
}
