import { Box, Typography } from "@mui/material";
import { colors, fontFamilies, fontWeights } from "../theme/tokens";

export interface BrandLogoProps {
  readonly size?: "small" | "medium";
}

export function BrandLogo({ size = "medium" }: BrandLogoProps) {
  const mark = size === "small" ? 26 : 30;
  return (
    <Box sx={{ display: "inline-flex", alignItems: "center", gap: 1.25 }}>
      <svg width={mark} height={mark} viewBox="0 0 30 30" aria-hidden focusable={false}>
        <rect width="30" height="30" rx="7" fill={colors.navy} />
        <path
          d="M15 8.5v13M8.5 15h13"
          stroke={colors.onNavy}
          strokeWidth="3.2"
          strokeLinecap="round"
        />
      </svg>
      <Typography
        component="span"
        sx={{
          fontFamily: fontFamilies.heading,
          fontWeight: fontWeights.bold,
          fontSize: size === "small" ? 17 : 19,
          letterSpacing: "-0.01em",
          color: colors.navy,
          lineHeight: 1,
        }}
      >
        Solidaria
        <Box component="span" sx={{ color: colors.primary }}>
          +
        </Box>
      </Typography>
    </Box>
  );
}
