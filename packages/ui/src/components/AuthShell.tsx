import { Box, Typography } from "@mui/material";
import type { ReactNode } from "react";
import { colors, radii, shadows, spacing } from "../theme/tokens";
import { BrandLogo } from "./BrandLogo";

export const PRIVACY_NOTICE =
  "Tus datos personales se tratan conforme a la Ley Orgánica de Protección de Datos Personales del Ecuador.";

export interface AuthShellProps {
  readonly children: ReactNode;
  readonly aside: ReactNode;
}

export function AuthShell({ children, aside }: AuthShellProps) {
  return (
    <Box
      sx={{
        minHeight: "100dvh",
        bgcolor: colors.background,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: { xs: "flex-start", md: "center" },
        px: { xs: 2, sm: 3 },
        py: { xs: 2, sm: 4, md: 6 },
        gap: 2,
      }}
    >
      <Box
        sx={{
          width: "100%",
          maxWidth: spacing.cardMaxWidth,
          display: "grid",
          gridTemplateColumns: { xs: "minmax(0, 1fr)", md: "minmax(0, 1fr) minmax(0, 1fr)" },
          gap: { xs: 2, md: 1.5 },
          p: 1.25,
          bgcolor: colors.surface,
          border: `1px solid ${colors.borderSubtle}`,
          borderRadius: `${radii.card}px`,
          boxShadow: shadows.card,
        }}
      >
        <Box
          component="main"
          sx={{
            px: { xs: 2, sm: 3.5, md: 4.5 },
            pt: { xs: 2.5, md: 5 },
            pb: { xs: 3, md: 5 },
            display: "flex",
            flexDirection: "column",
            gap: 3,
            minWidth: 0,
          }}
        >
          <BrandLogo />
          {children}
        </Box>
        {aside}
      </Box>
      <Typography
        component="footer"
        variant="caption"
        sx={{ textAlign: "center", maxWidth: spacing.cardMaxWidth, color: colors.textMuted }}
      >
        {PRIVACY_NOTICE}
      </Typography>
    </Box>
  );
}
