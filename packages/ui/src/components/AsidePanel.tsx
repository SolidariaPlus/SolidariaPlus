import { Box, Stack, Typography } from "@mui/material";
import type { ReactNode } from "react";
import { colors, radii } from "../theme/tokens";
import { PlusPattern } from "./PlusPattern";

export interface AsidePanelProps {
  readonly title: string;
  readonly items: readonly ReactNode[];
  readonly footnote?: ReactNode;
}

export function AsidePanel({ title, items, footnote }: AsidePanelProps) {
  return (
    <Box
      component="aside"
      aria-label={title}
      sx={{
        position: "relative",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        bgcolor: colors.navy,
        color: colors.onNavy,
        borderRadius: `${radii.panel}px`,
        px: { xs: 3, md: 3.5 },
        pt: { xs: 3, md: 0 },
        pb: 3,
        overflow: "hidden",
      }}
    >
      <Box sx={{ display: { xs: "none", md: "block" }, ml: -1.5, mb: 3 }}>
        <PlusPattern />
      </Box>
      <Stack spacing={2.5} sx={{ flexGrow: 1 }}>
        <Typography variant="h2" component="h2" sx={{ color: colors.onNavy }}>
          {title}
        </Typography>
        <Box component="ul" sx={{ m: 0, p: 0, listStyle: "none", display: "grid", gap: 2 }}>
          {items.map((item, index) => (
            <Box
              component="li"
              key={index}
              sx={{
                display: "grid",
                gridTemplateColumns: "16px 1fr",
                gap: 1.25,
                fontSize: 14,
                lineHeight: 1.55,
                color: colors.onNavyMuted,
              }}
            >
              <Box
                component="span"
                aria-hidden
                sx={{ color: colors.navyAccent, fontWeight: 700, lineHeight: 1.45 }}
              >
                +
              </Box>
              <span>{item}</span>
            </Box>
          ))}
        </Box>
      </Stack>
      {footnote && (
        <Typography
          variant="body2"
          sx={{
            mt: 4,
            pt: 2,
            borderTop: `1px solid rgba(255, 255, 255, 0.18)`,
            color: colors.onNavyMuted,
            fontSize: 13,
          }}
        >
          {footnote}
        </Typography>
      )}
    </Box>
  );
}
