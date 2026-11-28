import type { AuthClaims } from "@solidariaplus/auth";

export const SESSION_START_KEY = "solidaria.session.startedAt";

const STAFF_SESSION_HOURS = 10;
const MEMBER_WEB_SESSION_HOURS = 12;

export interface SessionWindow {
  readonly startedAt: Date;
  readonly endsAt: Date;
  readonly maxHours: number;
}

export function rememberSessionStart(storage: Storage, now: number): void {
  storage.setItem(SESSION_START_KEY, String(now));
}

export function forgetSessionStart(storage: Storage): void {
  storage.removeItem(SESSION_START_KEY);
}

export function sessionWindow(
  claims: AuthClaims,
  storage: Storage | undefined,
): SessionWindow | undefined {
  const stored = Number(storage?.getItem(SESSION_START_KEY));
  const startedAtMs =
    claims.authenticatedAt ?? (Number.isFinite(stored) && stored > 0 ? stored : undefined);
  if (startedAtMs === undefined) return undefined;
  const maxHours = claims.memberId ? MEMBER_WEB_SESSION_HOURS : STAFF_SESSION_HOURS;
  return {
    startedAt: new Date(startedAtMs),
    endsAt: new Date(startedAtMs + maxHours * 3_600_000),
    maxHours,
  };
}
