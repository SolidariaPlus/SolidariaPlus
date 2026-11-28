import { Box, LinearProgress, Typography } from "@mui/material";
import { useEffect, useState } from "react";

export const TOTP_STEP_SECONDS = 30;

export function secondsUntilNextCode(nowMs: number): number {
  const elapsed = Math.floor(nowMs / 1000) % TOTP_STEP_SECONDS;
  return TOTP_STEP_SECONDS - elapsed;
}

export interface TotpCountdownProps {
  readonly clock?: () => number;
}

export function TotpCountdown({ clock = Date.now }: TotpCountdownProps) {
  const [remaining, setRemaining] = useState(() => secondsUntilNextCode(clock()));

  useEffect(() => {
    const timer = setInterval(() => setRemaining(secondsUntilNextCode(clock())), 1000);
    return () => clearInterval(timer);
  }, [clock]);

  return (
    <Box sx={{ mt: -1 }}>
      <LinearProgress
        variant="determinate"
        value={(remaining / TOTP_STEP_SECONDS) * 100}
        aria-hidden
        sx={{
          "& .MuiLinearProgress-bar": {
            transitionDuration: "1s",
            transitionTimingFunction: "linear",
          },
        }}
      />
      <Typography variant="caption" component="p" sx={{ mt: 1 }}>
        Tu app muestra un código nuevo cada 30 segundos. El actual cambia en{" "}
        <Box component="span" sx={{ fontVariantNumeric: "tabular-nums", fontWeight: 600 }}>
          {remaining} s
        </Box>
        .
      </Typography>
    </Box>
  );
}
