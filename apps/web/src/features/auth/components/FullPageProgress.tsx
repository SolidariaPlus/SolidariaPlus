import { Box, CircularProgress, Typography } from "@mui/material";
import { BrandLogo } from "@solidariaplus/ui";

export function FullPageProgress({ message }: { readonly message: string }) {
  return (
    <Box
      component="main"
      sx={{ minHeight: "100dvh", display: "grid", placeItems: "center", px: 3 }}
    >
      <Box sx={{ display: "grid", justifyItems: "center", gap: 2.5 }}>
        <BrandLogo />
        <CircularProgress size={28} aria-hidden />
        <Typography variant="body2" color="text.secondary" role="status">
          {message}
        </Typography>
      </Box>
    </Box>
  );
}
