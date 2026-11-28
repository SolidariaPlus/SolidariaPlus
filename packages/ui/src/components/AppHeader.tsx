import { Box, Button, Chip, Stack } from "@mui/material";
import { colors, spacing } from "../theme/tokens";
import { BrandLogo } from "./BrandLogo";

export interface AppHeaderProps {
  readonly roleLabels: readonly string[];
  readonly onSignOut: () => void;
}

export function AppHeader({ roleLabels, onSignOut }: AppHeaderProps) {
  return (
    <Box
      component="header"
      sx={{
        bgcolor: colors.surface,
        borderBottom: `1px solid ${colors.borderSubtle}`,
        position: "sticky",
        top: 0,
        zIndex: 10,
      }}
    >
      <Box
        sx={{
          maxWidth: spacing.contentMaxWidth,
          mx: "auto",
          px: { xs: 2, sm: 3, md: 6 },
          minHeight: 60,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: 2,
        }}
      >
        <BrandLogo size="small" />
        <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
          {roleLabels.length > 0 && (
            <Stack
              direction="row"
              spacing={0.75}
              aria-label="Tus roles"
              sx={{ display: { xs: "none", sm: "flex" } }}
            >
              {roleLabels.map((label) => (
                <Chip key={label} label={label} size="small" />
              ))}
            </Stack>
          )}
          <Button variant="outlined" size="small" onClick={onSignOut}>
            Cerrar sesión
          </Button>
        </Stack>
      </Box>
    </Box>
  );
}
