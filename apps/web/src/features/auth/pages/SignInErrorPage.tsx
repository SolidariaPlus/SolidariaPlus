import { toUserMessage } from "@solidariaplus/auth";
import { StatusScreen } from "@solidariaplus/ui";
import { useLocation } from "react-router";
import { SignInButton } from "../components/SignInButton";

export interface SignInErrorState {
  readonly errorCode?: string;
}

const WHAT_TO_DO = {
  title: "Qué puedes hacer",
  items: [
    "Vuelve a intentarlo: cada intento usa un enlace nuevo y seguro.",
    "Si cerraste la ventana de inicio de sesión, empieza otra vez desde aquí.",
    "Si el problema sigue, acércate a la ventanilla de tu caja.",
  ],
  footnote: "Solidaria+ nunca te pedirá tu contraseña por teléfono, mensaje ni correo.",
};

export function SignInErrorPage() {
  const location = useLocation();
  const code = (location.state as SignInErrorState | null)?.errorCode;

  return (
    <StatusScreen
      tone="warning"
      title="No pudimos completar el inicio de sesión"
      description={toUserMessage(code ?? "invalid_response")}
      action={<SignInButton label="Intentar de nuevo" />}
      aside={WHAT_TO_DO}
    />
  );
}
