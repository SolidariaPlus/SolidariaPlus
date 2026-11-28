import { SESSION_DURATION_ASIDE, StatusScreen } from "@solidariaplus/ui";
import { SignInButton } from "../components/SignInButton";

export function SignedOutPage() {
  return (
    <StatusScreen
      tone="success"
      title="Cerraste sesión"
      description="Si usas una computadora compartida, cierra también esta ventana."
      action={<SignInButton label="Volver a iniciar sesión" />}
      aside={SESSION_DURATION_ASIDE}
    />
  );
}

export function SessionExpiredPage() {
  return (
    <StatusScreen
      tone="info"
      title="Tu sesión terminó"
      description="Por seguridad, las sesiones del personal duran hasta 10 horas. Ingresa de nuevo para continuar."
      action={<SignInButton label="Iniciar sesión de nuevo" />}
      aside={SESSION_DURATION_ASIDE}
    />
  );
}
