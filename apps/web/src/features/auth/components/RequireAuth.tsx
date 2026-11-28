import { Box, CircularProgress } from "@mui/material";
import type { ReactNode } from "react";
import { Navigate } from "react-router";
import { satisfies, type AccessRequirement } from "../access/roles";
import { useAuth } from "../hooks/useAuth";

export interface RequireAuthProps {
  readonly children: ReactNode;
  readonly requirement?: AccessRequirement;
}

export function RequireAuth({ children, requirement }: RequireAuthProps) {
  const { status, claims, sessionEnd } = useAuth();

  if (status === "restoring") {
    return (
      <Box sx={{ minHeight: "100dvh", display: "grid", placeItems: "center" }}>
        <CircularProgress aria-label="Cargando tu sesión" />
      </Box>
    );
  }
  if (status === "anonymous") {
    if (sessionEnd === "signed-out") return <Navigate to="/signed-out" replace />;
    if (sessionEnd === "expired") return <Navigate to="/session-expired" replace />;
    return <Navigate to="/signin" replace />;
  }
  if (requirement && !satisfies(claims, requirement)) return <Navigate to="/forbidden" replace />;
  return children;
}
