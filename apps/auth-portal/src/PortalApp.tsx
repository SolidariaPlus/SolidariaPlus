import { CssBaseline, ThemeProvider } from "@mui/material";
import { solidariaTheme } from "@solidariaplus/ui/theme";
import type { PageData } from "./page-data/page-data";
import { ActivatePage } from "./pages/ActivatePage";
import { LoginPage } from "./pages/LoginPage";
import { MfaEnrolPage } from "./pages/MfaEnrolPage";
import { MfaVerifyPage } from "./pages/MfaVerifyPage";

export function PortalPage({ data }: { readonly data: PageData }) {
  switch (data.page) {
    case "login":
      return <LoginPage data={data} />;
    case "activate":
      return <ActivatePage data={data} />;
    case "mfa":
      return data.mode === "enrol" ? <MfaEnrolPage data={data} /> : <MfaVerifyPage data={data} />;
  }
}

export function PortalApp({ data }: { readonly data: PageData }) {
  return (
    <ThemeProvider theme={solidariaTheme}>
      <CssBaseline />
      <PortalPage data={data} />
    </ThemeProvider>
  );
}
