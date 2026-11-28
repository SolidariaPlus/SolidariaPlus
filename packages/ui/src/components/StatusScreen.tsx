import { Stack, Typography } from "@mui/material";
import type { ReactNode } from "react";
import { AsidePanel, type AsidePanelProps } from "./AsidePanel";
import { AuthShell } from "./AuthShell";
import { StatusBadge, type StatusTone } from "./StatusBadge";

export interface StatusScreenProps {
  readonly tone: StatusTone;
  readonly title: string;
  readonly description: ReactNode;
  readonly action?: ReactNode;
  readonly aside: AsidePanelProps;
  readonly children?: ReactNode;
}

export function StatusScreen({
  tone,
  title,
  description,
  action,
  aside,
  children,
}: StatusScreenProps) {
  return (
    <AuthShell aside={<AsidePanel {...aside} />}>
      <Stack spacing={2.5} sx={{ my: { md: "auto" }, maxWidth: 420 }}>
        <StatusBadge tone={tone} />
        <Stack spacing={1}>
          <Typography variant="h1">{title}</Typography>
          <Typography variant="subtitle1">{description}</Typography>
        </Stack>
        {children}
        {action}
      </Stack>
    </AuthShell>
  );
}
