import { isAuthError } from "@solidariaplus/auth";
import { useEffect, useRef } from "react";
import { useNavigate } from "react-router";
import { FullPageProgress } from "../components/FullPageProgress";
import { useAuth } from "../hooks/useAuth";
import type { SignInErrorState } from "./SignInErrorPage";

export function AuthCallbackPage() {
  const { completeSignIn } = useAuth();
  const navigate = useNavigate();
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    completeSignIn()
      .then(() => navigate("/", { replace: true }))
      .catch((error: unknown) => {
        const state: SignInErrorState = {
          errorCode: isAuthError(error) ? error.code : "invalid_response",
        };
        navigate("/signin-error", { replace: true, state });
      });
  }, [completeSignIn, navigate]);

  return <FullPageProgress message="Verificando tu sesión…" />;
}
