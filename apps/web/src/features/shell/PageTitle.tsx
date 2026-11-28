import { Stack, Typography } from "@mui/material";
import { useEffect, useRef, type ReactNode } from "react";

export interface PageTitleProps {
  readonly title: string;
  readonly description?: ReactNode;
}

export function PageTitle({ title, description }: PageTitleProps) {
  const heading = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    document.title = `${title} · Solidaria+`;
    heading.current?.focus({ preventScroll: true });
  }, [title]);

  return (
    <Stack spacing={1} sx={{ mb: 3, maxWidth: 640 }}>
      <Typography
        variant="h1"
        ref={heading}
        tabIndex={-1}
        sx={{ outline: "none", textWrap: "balance" }}
      >
        {title}
      </Typography>
      {description && <Typography variant="subtitle1">{description}</Typography>}
    </Stack>
  );
}
