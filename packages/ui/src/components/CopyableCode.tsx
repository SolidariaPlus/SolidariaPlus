import { Box, Button } from "@mui/material";
import { useEffect, useState } from "react";
import { colors, fontFamilies, radii } from "../theme/tokens";

export interface CopyableCodeProps {
  readonly value: string;
  readonly display?: string;
  readonly copyLabel?: string;
  readonly size?: "large" | "compact";
  readonly onCopy?: (value: string) => Promise<void>;
}

const COPIED_FEEDBACK_MS = 2000;

export function CopyableCode({
  value,
  display,
  copyLabel = "Copiar código",
  size = "large",
  onCopy = (text) => navigator.clipboard.writeText(text),
}: CopyableCodeProps) {
  const [state, setState] = useState<"idle" | "copied" | "failed">("idle");

  useEffect(() => {
    if (state === "idle") return;
    const timer = setTimeout(() => setState("idle"), COPIED_FEEDBACK_MS);
    return () => clearTimeout(timer);
  }, [state]);

  const copy = () => {
    onCopy(value).then(
      () => setState("copied"),
      () => setState("failed"),
    );
  };

  const large = size === "large";
  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        flexWrap: "wrap",
        gap: 2,
        px: large ? 2.5 : 1.5,
        py: large ? 2 : 1,
        borderRadius: `${radii.panel}px`,
        bgcolor: colors.background,
        border: `1px solid ${colors.borderSubtle}`,
      }}
    >
      <Box
        component="span"
        sx={{
          fontFamily: large ? fontFamilies.heading : fontFamilies.mono,
          fontSize: large ? { xs: 30, sm: 40 } : 14,
          fontWeight: large ? 700 : 500,
          letterSpacing: large ? "0.16em" : "0.08em",
          color: large ? colors.navy : colors.text,
          wordBreak: "break-all",
        }}
      >
        {display ?? value}
      </Box>
      <Button variant={large ? "outlined" : "text"} size="small" onClick={copy} aria-live="polite">
        {state === "copied" ? "¡Copiado!" : state === "failed" ? "No se pudo copiar" : copyLabel}
      </Button>
    </Box>
  );
}
