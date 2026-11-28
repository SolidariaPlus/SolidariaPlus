import { Box, Link } from "@mui/material";
import { AppHeader } from "@solidariaplus/ui";
import { colors, spacing } from "@solidariaplus/ui/tokens";
import { useState, type ReactNode } from "react";
import { roleLabels } from "../auth/access/roles";
import { useAuth } from "../auth/hooks/useAuth";
import { SignOutDialog } from "./SignOutDialog";

export const MAIN_CONTENT_ID = "main-content";

export function AppShell({ children }: { readonly children: ReactNode }) {
  const { claims } = useAuth();
  const [signingOut, setSigningOut] = useState(false);

  return (
    <Box sx={{ minHeight: "100dvh", bgcolor: colors.background }}>
      <Link
        href={`#${MAIN_CONTENT_ID}`}
        sx={{
          position: "absolute",
          left: 16,
          top: -48,
          zIndex: 20,
          px: 2,
          py: 1,
          bgcolor: colors.surface,
          "&:focus": { top: 12 },
        }}
      >
        Saltar al contenido
      </Link>
      <AppHeader roleLabels={roleLabels(claims)} onSignOut={() => setSigningOut(true)} />
      <Box
        component="main"
        id={MAIN_CONTENT_ID}
        tabIndex={-1}
        sx={{
          maxWidth: spacing.contentMaxWidth,
          mx: "auto",
          px: { xs: 2, sm: 3, md: 6 },
          py: { xs: 3, md: 5 },
          outline: "none",
        }}
      >
        {children}
      </Box>
      <SignOutDialog open={signingOut} onClose={() => setSigningOut(false)} />
    </Box>
  );
}
