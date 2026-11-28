import { Box } from "@mui/material";
import { colors } from "../theme/tokens";
import { AlertCircleIcon, CheckIcon } from "./icons";

export type StatusTone = "success" | "info" | "warning";

const TONES: Record<StatusTone, { readonly color: string; readonly background: string }> = {
  success: { color: colors.success, background: colors.successSurface },
  info: { color: colors.info, background: colors.infoSurface },
  warning: { color: colors.warning, background: colors.warningSurface },
};

export interface StatusBadgeProps {
  readonly tone: StatusTone;
}

export function StatusBadge({ tone }: StatusBadgeProps) {
  const { color, background } = TONES[tone];
  return (
    <Box
      aria-hidden
      sx={{
        width: 44,
        height: 44,
        borderRadius: "50%",
        display: "grid",
        placeItems: "center",
        color,
        bgcolor: background,
      }}
    >
      {tone === "success" ? <CheckIcon size={22} /> : <AlertCircleIcon size={22} />}
    </Box>
  );
}
