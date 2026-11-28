import type { AuthClaims, AuthClient, SignOutScope } from "@solidariaplus/auth";
import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  useSyncExternalStore,
  type ReactNode,
} from "react";
import { forgetSessionStart, rememberSessionStart } from "../session/session-start";

export type AuthStatus = "restoring" | "authenticated" | "anonymous";

export type SessionEnd = "signed-out" | "expired";

export interface AuthContextValue {
  readonly status: AuthStatus;
  readonly claims: AuthClaims | undefined;
  readonly sessionEnd: SessionEnd | undefined;
  readonly activationUrl: string;
  signIn(): Promise<void>;
  completeSignIn(): Promise<AuthClaims>;
  signOut(scope?: SignOutScope): Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export interface AuthProviderProps {
  readonly client: AuthClient;
  readonly sessionStorage?: Storage;
  readonly clock?: () => number;
  readonly children: ReactNode;
}

export function AuthProvider({
  client,
  sessionStorage,
  clock = Date.now,
  children,
}: AuthProviderProps) {
  const subscribe = useCallback((onChange: () => void) => client.subscribe(onChange), [client]);
  const getClaims = useCallback(() => client.getClaims(), [client]);
  const claims = useSyncExternalStore(subscribe, getClaims);
  const [restoring, setRestoring] = useState(() => !client.isAuthenticated());
  const [sessionEnd, setSessionEnd] = useState<SessionEnd>();
  const signingOut = useRef(false);

  useEffect(() => {
    let wasAuthenticated = client.isAuthenticated();
    return client.subscribe((next) => {
      if (wasAuthenticated && !next) {
        setSessionEnd(signingOut.current ? "signed-out" : "expired");
        if (sessionStorage) forgetSessionStart(sessionStorage);
      }
      if (next) setSessionEnd(undefined);
      wasAuthenticated = next !== undefined;
    });
  }, [client, sessionStorage]);

  useEffect(() => {
    if (client.isAuthenticated()) return;
    let active = true;
    client
      .restoreSession()
      .catch(() => undefined)
      .finally(() => {
        if (active) setRestoring(false);
      });
    return () => {
      active = false;
    };
  }, [client]);

  const signIn = useCallback(() => client.signIn(), [client]);

  const completeSignIn = useCallback(async () => {
    const result = await client.completeSignIn();
    if (sessionStorage) rememberSessionStart(sessionStorage, clock());
    return result;
  }, [client, sessionStorage, clock]);

  const signOut = useCallback(
    async (scope?: SignOutScope) => {
      signingOut.current = true;
      try {
        await client.signOut(scope);
      } finally {
        signingOut.current = false;
      }
    },
    [client],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      status: claims ? "authenticated" : restoring ? "restoring" : "anonymous",
      claims,
      sessionEnd,
      activationUrl: client.config.endpoints.activation,
      signIn,
      completeSignIn,
      signOut,
    }),
    [claims, restoring, sessionEnd, client, signIn, completeSignIn, signOut],
  );

  return <AuthContext value={value}>{children}</AuthContext>;
}
