import { Box, Typography } from "@mui/material";
import type { ReactNode } from "react";
import { colors, radii } from "../theme/tokens";
import { AlertCircleIcon, AlertTriangleIcon, CheckIcon } from "./icons";

export type NoticeTone = "warning" | "error" | "info" | "success" | "neutral";

const TONES: Record<
  NoticeTone,
  { readonly color: string; readonly background: string; readonly border: string }
> = {
  warning: {
    color: colors.warning,
    background: colors.warningSurface,
    border: colors.warningBorder,
  },
  error: { color: colors.danger, background: colors.dangerSurface, border: colors.dangerBorder },
  info: { color: colors.info, background: colors.infoSurface, border: colors.infoBorder },
  success: { color: colors.success, background: colors.successSurface, border: "#BBF7D0" },
  neutral: { color: colors.text, background: colors.background, border: colors.borderSubtle },
};

export interface NoticeBannerProps {
  readonly tone: NoticeTone;
  readonly title?: string;
  readonly children: ReactNode;
  readonly icon?: boolean;
}

export function NoticeBanner({ tone, title, children, icon = true }: NoticeBannerProps) {
  const palette = TONES[tone];
  const urgent = tone === "error";
  return (
    <Box
      role={urgent ? "alert" : "status"}
      sx={{
        display: "flex",
        gap: 1.25,
        p: 2,
        borderRadius: `${radii.panel}px`,
        border: `1px solid ${palette.border}`,
        bgcolor: palette.background,
        color: palette.color,
        fontSize: 14,
        lineHeight: 1.5,
      }}
    >
      {icon && tone !== "neutral" && (
        <Box component="span" sx={{ display: "inline-flex", pt: "2px", flexShrink: 0 }}>
          {tone === "success" ? (
            <CheckIcon size={18} />
          ) : tone === "warning" ? (
            <AlertTriangleIcon size={18} />
          ) : (
            <AlertCircleIcon size={18} />
          )}
        </Box>
      )}
      <Box sx={{ minWidth: 0 }}>
        {title && (
          <Typography
            variant="body2"
            component="p"
            sx={{ fontWeight: 600, color: tone === "neutral" ? colors.text : "inherit", mb: 0.5 }}
          >
            {title}
          </Typography>
        )}
        <Box sx={{ color: tone === "neutral" ? colors.textMuted : "inherit" }}>{children}</Box>
      </Box>
    </Box>
  );
}
