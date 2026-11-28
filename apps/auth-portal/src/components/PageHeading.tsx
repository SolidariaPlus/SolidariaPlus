import { Stack, Typography } from "@mui/material";
import type { ReactNode } from "react";

export interface PageHeadingProps {
  readonly title: string;
  readonly description: ReactNode;
}

export function PageHeading({ title, description }: PageHeadingProps) {
  return (
    <Stack spacing={1}>
      <Typography variant="h1" sx={{ textWrap: "balance" }}>
        {title}
      </Typography>
      <Typography variant="subtitle1">{description}</Typography>
    </Stack>
  );
}
