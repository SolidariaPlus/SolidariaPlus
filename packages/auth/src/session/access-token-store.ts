export interface AccessToken {
  readonly value: string;
  readonly expiresAt: number;
}

export interface AccessTokenStore {
  get(): AccessToken | undefined;
  set(token: AccessToken): void;
  clear(): void;
}

export class InMemoryAccessTokenStore implements AccessTokenStore {
  private token: AccessToken | undefined;

  get(): AccessToken | undefined {
    return this.token;
  }

  set(token: AccessToken): void {
    this.token = token;
  }

  clear(): void {
    this.token = undefined;
  }
}

export function isExpiring(token: AccessToken, now: number, renewBeforeExpiryMs: number): boolean {
  return token.expiresAt - renewBeforeExpiryMs <= now;
}
