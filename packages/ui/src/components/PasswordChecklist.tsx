import { Box, Typography } from "@mui/material";
import { colors, radii } from "../theme/tokens";
import { CheckIcon, CircleIcon } from "./icons";

export interface ChecklistRule {
  readonly id: string;
  readonly label: string;
  readonly satisfied: boolean;
}

export interface PasswordChecklistProps {
  readonly rules: readonly ChecklistRule[];
  readonly title?: string;
}

export function PasswordChecklist({
  rules,
  title = "Tu contraseña necesita",
}: PasswordChecklistProps) {
  return (
    <Box
      sx={{
        p: 2,
        borderRadius: `${radii.panel}px`,
        bgcolor: colors.background,
        border: `1px solid ${colors.borderSubtle}`,
      }}
    >
      <Typography variant="body2" sx={{ fontWeight: 600, mb: 1.25 }}>
        {title}
      </Typography>
      <Box
        component="ul"
        sx={{
          m: 0,
          p: 0,
          listStyle: "none",
          display: "grid",
          gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" },
          columnGap: 2,
          rowGap: 0.75,
        }}
      >
        {rules.map((rule) => (
          <Box
            component="li"
            key={rule.id}
            sx={{
              display: "flex",
              alignItems: "flex-start",
              gap: 1,
              fontSize: 13,
              lineHeight: 1.45,
              color: rule.satisfied ? colors.success : colors.textMuted,
              transition: "color 150ms ease",
            }}
          >
            <Box component="span" sx={{ display: "inline-flex", pt: "1px" }}>
              {rule.satisfied ? <CheckIcon size={15} /> : <CircleIcon size={15} />}
            </Box>
            <span>
              {rule.label}
              <Box component="span" sx={visuallyHidden}>
                {rule.satisfied ? " (cumplido)" : " (pendiente)"}
              </Box>
            </span>
          </Box>
        ))}
      </Box>
    </Box>
  );
}

const visuallyHidden = {
  position: "absolute",
  width: 1,
  height: 1,
  overflow: "hidden",
  clip: "rect(0 0 0 0)",
  whiteSpace: "nowrap",
} as const;
