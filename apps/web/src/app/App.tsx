import type { AuthClient } from "@solidariaplus/auth";
import type { IdentityAccessApi } from "@solidariaplus/identity-api";
import { BrowserRouter } from "react-router";
import { AppProviders } from "./AppProviders";
import { AppRoutes } from "./AppRoutes";

export interface AppProps {
  readonly authClient: AuthClient;
  readonly identityApi: IdentityAccessApi;
  readonly sessionStorage: Storage;
}

export function App({ authClient, identityApi, sessionStorage }: AppProps) {
  return (
    <AppProviders authClient={authClient} identityApi={identityApi} sessionStorage={sessionStorage}>
      <BrowserRouter>
        <AppRoutes sessionStorage={sessionStorage} />
      </BrowserRouter>
    </AppProviders>
  );
}
