import { CssBaseline, ThemeProvider } from "@mui/material";
import type { AuthClient } from "@solidariaplus/auth";
import type { IdentityAccessApi } from "@solidariaplus/identity-api";
import { solidariaTheme } from "@solidariaplus/ui/theme";
import type { ReactNode } from "react";
import { AuthProvider } from "../features/auth/context/AuthContext";
import { IdentityApiProvider } from "../features/identity/IdentityApiContext";

export interface AppProvidersProps {
  readonly authClient: AuthClient;
  readonly identityApi: IdentityAccessApi;
  readonly sessionStorage?: Storage;
  readonly children: ReactNode;
}

export function AppProviders({
  authClient,
  identityApi,
  sessionStorage,
  children,
}: AppProvidersProps) {
  return (
    <ThemeProvider theme={solidariaTheme}>
      <CssBaseline />
      <AuthProvider client={authClient} sessionStorage={sessionStorage}>
        <IdentityApiProvider api={identityApi}>{children}</IdentityApiProvider>
      </AuthProvider>
    </ThemeProvider>
  );
}
