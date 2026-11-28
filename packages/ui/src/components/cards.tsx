import { Box, Button, Stack, Typography } from "@mui/material";
import type { ElementType, ReactNode } from "react";
import { colors, radii } from "../theme/tokens";

export interface SurfaceCardProps {
  readonly children: ReactNode;
  readonly dashed?: boolean;
}

export function SurfaceCard({ children, dashed = false }: SurfaceCardProps) {
  return (
    <Box
      sx={{
        p: { xs: 2.5, sm: 3 },
        borderRadius: `${radii.panel}px`,
        bgcolor: dashed ? "transparent" : colors.surface,
        border: dashed ? `1.5px dashed ${colors.border}` : `1px solid ${colors.borderSubtle}`,
        height: "100%",
      }}
    >
      {children}
    </Box>
  );
}

export interface ActionCardProps {
  readonly title: string;
  readonly description: ReactNode;
  readonly actionLabel: string;
  readonly actionComponent?: ElementType;
  readonly actionTo?: string;
  readonly onAction?: () => void;
}

export function ActionCard({
  title,
  description,
  actionLabel,
  actionComponent,
  actionTo,
  onAction,
}: ActionCardProps) {
  return (
    <SurfaceCard>
      <Stack spacing={1.5} sx={{ height: "100%", alignItems: "flex-start" }}>
        <Typography variant="h3" component="h2">
          {title}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ flexGrow: 1 }}>
          {description}
        </Typography>
        <Button
          variant="contained"
          onClick={onAction}
          {...(actionComponent ? { component: actionComponent, to: actionTo } : {})}
        >
          {actionLabel}
        </Button>
      </Stack>
    </SurfaceCard>
  );
}

export interface PlaceholderCardProps {
  readonly title: string;
  readonly description: ReactNode;
}

export function PlaceholderCard({ title, description }: PlaceholderCardProps) {
  return (
    <SurfaceCard dashed>
      <Stack spacing={1} sx={{ height: "100%", justifyContent: "center" }}>
        <Typography variant="h3" component="h2">
          {title}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {description}
        </Typography>
      </Stack>
    </SurfaceCard>
  );
}
