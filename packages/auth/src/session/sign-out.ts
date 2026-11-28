import type { FetchLike } from "../shared/types";

export type SignOutScope = "current" | "all";

export interface SessionRevokerOptions {
  readonly sessionsUrl: string;
  readonly fetch: FetchLike;
}

export class SessionRevoker {
  private readonly options: SessionRevokerOptions;

  constructor(options: SessionRevokerOptions) {
    this.options = options;
  }

  async revoke(scope: SignOutScope, accessToken: string): Promise<boolean> {
    const url = scope === "all" ? this.options.sessionsUrl : `${this.options.sessionsUrl}/current`;
    try {
      const response = await this.options.fetch(url, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      return response.ok;
    } catch {
      return false;
    }
  }
}
