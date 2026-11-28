import "@fontsource/inter/400.css";
import "@fontsource/inter/500.css";
import "@fontsource/inter/600.css";
import "@fontsource/plus-jakarta-sans/600.css";
import "@fontsource/plus-jakarta-sans/700.css";
import { createIdentityAccessApi } from "@solidariaplus/identity-api";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./app/App";
import { createWebAuthClient } from "./features/auth/infrastructure/create-web-auth-client";

const container = document.getElementById("root");
if (!container) throw new Error("Root element #root not found");

const authClient = await createWebAuthClient();
const identityApi = createIdentityAccessApi({
  baseUrl: authClient.config.issuer,
  fetch: authClient.fetch,
});

createRoot(container).render(
  <StrictMode>
    <App authClient={authClient} identityApi={identityApi} sessionStorage={window.sessionStorage} />
  </StrictMode>,
);
