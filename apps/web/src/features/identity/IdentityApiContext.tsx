import type { IdentityAccessApi } from "@solidariaplus/identity-api";
import { createContext, use, type ReactNode } from "react";

const IdentityApiContext = createContext<IdentityAccessApi | undefined>(undefined);

export function IdentityApiProvider({
  api,
  children,
}: {
  readonly api: IdentityAccessApi;
  readonly children: ReactNode;
}) {
  return <IdentityApiContext value={api}>{children}</IdentityApiContext>;
}

export function useIdentityApi(): IdentityAccessApi {
  const api = use(IdentityApiContext);
  if (!api) throw new Error("useIdentityApi must be used inside an IdentityApiProvider");
  return api;
}
