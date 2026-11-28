import type { TokenSet } from "../oauth/token-response.js";
import type { Clock } from "../shared/types.js";
import { isExpiring, type AccessTokenStore } from "./access-token-store.js";
import type { TokenRenewer } from "./token-renewer.js";

export const DEFAULT_RENEW_BEFORE_EXPIRY_MS = 60_000;

export type AccessTokenListener = (accessToken: string | undefined) => void;

export interface SessionManagerOptions {
  readonly store: AccessTokenStore;
  readonly renewer: Pick<TokenRenewer, "renew">;
  readonly clock: Clock;
  readonly renewBeforeExpiryMs?: number;
}

export class SessionManager {
  private readonly options: SessionManagerOptions;
  private readonly renewBeforeExpiryMs: number;
  private readonly listeners = new Set<AccessTokenListener>();

  constructor(options: SessionManagerOptions) {
    this.options = options;
    this.renewBeforeExpiryMs = options.renewBeforeExpiryMs ?? DEFAULT_RENEW_BEFORE_EXPIRY_MS;
  }

  currentAccessToken(): string | undefined {
    return this.options.store.get()?.value;
  }

  async getValidAccessToken(): Promise<string> {
    const token = this.options.store.get();
    if (token && !isExpiring(token, this.options.clock(), this.renewBeforeExpiryMs))
      return token.value;
    return this.renew();
  }

  async renew(): Promise<string> {
    try {
      const tokens = await this.options.renewer.renew();
      this.accept(tokens);
      return tokens.accessToken;
    } catch (error) {
      this.clear();
      throw error;
    }
  }

  accept(tokens: TokenSet): void {
    const previous = this.options.store.get()?.value;
    this.options.store.set({ value: tokens.accessToken, expiresAt: tokens.expiresAt });
    if (previous !== tokens.accessToken) this.notify(tokens.accessToken);
  }

  clear(): void {
    const hadToken = this.options.store.get() !== undefined;
    this.options.store.clear();
    if (hadToken) this.notify(undefined);
  }

  subscribe(listener: AccessTokenListener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private notify(accessToken: string | undefined): void {
    for (const listener of this.listeners) listener(accessToken);
  }
}
