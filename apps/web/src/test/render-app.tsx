import type { AuthClient } from "@solidariaplus/auth";
import type { IdentityAccessApi } from "@solidariaplus/identity-api";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { AppProviders } from "../app/AppProviders";
import { AppRoutes } from "../app/AppRoutes";
import { createFakeIdentityApi } from "./fake-auth-client";

export interface RenderAppOptions {
  readonly path: string;
  readonly state?: unknown;
  readonly identityApi?: IdentityAccessApi;
}

export function renderApp(
  client: AuthClient,
  { path, state, identityApi = createFakeIdentityApi() }: RenderAppOptions,
) {
  return render(
    <AppProviders authClient={client} identityApi={identityApi} sessionStorage={sessionStorage}>
      <MemoryRouter initialEntries={[{ pathname: path, state }]}>
        <AppRoutes sessionStorage={sessionStorage} />
      </MemoryRouter>
    </AppProviders>,
  );
}
