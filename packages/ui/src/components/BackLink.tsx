import { Link } from "@mui/material";
import type { ElementType, MouseEventHandler, ReactNode } from "react";
import { colors } from "../theme/tokens";
import { ChevronLeftIcon } from "./icons";

export interface BackLinkProps {
  readonly children: ReactNode;
  readonly href?: string;
  readonly to?: string;
  readonly component?: ElementType;
  readonly onClick?: MouseEventHandler<HTMLAnchorElement>;
}

export function BackLink({ children, href, to, component, onClick }: BackLinkProps) {
  return (
    <Link
      href={href}
      onClick={onClick}
      underline="hover"
      {...(component ? { component, to } : {})}
      sx={{
        alignSelf: "flex-start",
        display: "inline-flex",
        alignItems: "center",
        gap: 0.5,
        minHeight: 32,
        fontSize: 14,
        fontWeight: 500,
        color: colors.textMuted,
        "&:hover": { color: colors.text },
      }}
    >
      <ChevronLeftIcon size={16} />
      {children}
    </Link>
  );
}
