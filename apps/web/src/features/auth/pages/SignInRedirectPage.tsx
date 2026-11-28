import { isAuthError } from "@solidariaplus/auth";
import { useEffect, useRef } from "react";
import { useNavigate } from "react-router";
import { FullPageProgress } from "../components/FullPageProgress";
import { useAuth } from "../hooks/useAuth";
import type { SignInErrorState } from "./SignInErrorPage";

export function SignInRedirectPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    signIn().catch((error: unknown) => {
      const state: SignInErrorState = {
        errorCode: isAuthError(error) ? error.code : "network_error",
      };
      navigate("/signin-error", { replace: true, state });
    });
  }, [signIn, navigate]);

  return <FullPageProgress message="Te llevamos al inicio de sesión seguro de Solidaria+…" />;
}
