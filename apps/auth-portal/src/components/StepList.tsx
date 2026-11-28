import { Box, Stack, Typography } from "@mui/material";
import { colors } from "@solidariaplus/ui/tokens";
import type { ReactNode } from "react";

export interface Step {
  readonly title: string;
  readonly description?: ReactNode;
  readonly content?: ReactNode;
}

export function StepList({ steps }: { readonly steps: readonly Step[] }) {
  return (
    <Box component="ol" sx={{ m: 0, p: 0, listStyle: "none", display: "grid", gap: 2.5 }}>
      {steps.map((step, index) => (
        <Box
          component="li"
          key={step.title}
          sx={{ display: "grid", gridTemplateColumns: "28px minmax(0, 1fr)", gap: 1.5 }}
        >
          <Box
            aria-hidden
            sx={{
              width: 24,
              height: 24,
              mt: "1px",
              borderRadius: "50%",
              display: "grid",
              placeItems: "center",
              fontSize: 12,
              fontWeight: 700,
              color: colors.info,
              bgcolor: colors.infoSurface,
            }}
          >
            {index + 1}
          </Box>
          <Stack spacing={1.25} sx={{ minWidth: 0 }}>
            <Box>
              <Typography variant="body2" component="h2" sx={{ fontWeight: 600 }}>
                {step.title}
              </Typography>
              {step.description && (
                <Typography variant="caption" component="p" sx={{ mt: 0.25 }}>
                  {step.description}
                </Typography>
              )}
            </Box>
            {step.content}
          </Stack>
        </Box>
      ))}
    </Box>
  );
}
