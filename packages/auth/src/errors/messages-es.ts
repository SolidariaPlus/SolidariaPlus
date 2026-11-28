import { isAuthError, type AuthErrorCode } from "./auth-error.js";

export const DEFAULT_ERROR_MESSAGE = "Ocurrió un error inesperado. Intenta de nuevo.";

const LOCKED_MESSAGE =
  "Por seguridad pausamos el acceso tras 5 intentos fallidos. Vuelve a intentarlo en 15 minutos.";

export const ERROR_MESSAGES_ES: Readonly<Record<AuthErrorCode, string>> = {
  invalid_request: "La solicitud de inicio de sesión no es válida. Intenta de nuevo.",
  unauthorized_client: "Esta aplicación no está autorizada para iniciar sesión.",
  access_denied: "El inicio de sesión fue cancelado o rechazado.",
  unsupported_response_type: "El servidor de autenticación no admite este tipo de solicitud.",
  invalid_scope: "Los permisos solicitados no son válidos.",
  server_error: "El servicio de autenticación tuvo un problema. Intenta más tarde.",
  temporarily_unavailable:
    "El servicio de autenticación no está disponible por el momento. Intenta más tarde.",
  invalid_client: "La aplicación no pudo identificarse ante el servicio de autenticación.",
  invalid_grant: "Tu sesión terminó o ya no es válida. Inicia sesión de nuevo.",
  unsupported_grant_type: "El tipo de autorización no es compatible.",
  AUTH_ACCOUNT_LOCKED: LOCKED_MESSAGE,
  AUTH_ACTIVATION_CODE_INVALID:
    "El código de ventanilla no es válido o ya venció. Pide uno nuevo en la ventanilla de tu caja.",
  AUTH_MFA_REQUIRED: "El código no es correcto. Revisa tu app de autenticación e intenta de nuevo.",
  UNAUTHENTICATED: "Tu sesión no es válida. Inicia sesión de nuevo.",
  MEMBER_NOT_FOUND: "No encontramos un socio con ese número. Revísalo e intenta de nuevo.",
  MEMBER_NOT_ELIGIBLE: "Este socio no puede recibir un código: su membresía no está activa.",
  NOT_FOUND: "No encontramos ese usuario institucional. Revísalo e intenta de nuevo.",
  SAME_PERSON_NOT_ALLOWED:
    "No puedes restablecer tu propia cuenta: lo hace la otra función de la Junta.",
  VALIDATION_FAILED:
    "La contraseña no cumple las reglas: 12 caracteres o más, mayúscula, minúscula, número y símbolo.",
  RATE_LIMITED: "Recibimos demasiados intentos seguidos. Espera un minuto e intenta de nuevo.",
  FORBIDDEN: "No tienes permiso para realizar esta acción.",
  state_mismatch:
    "La respuesta de inicio de sesión no corresponde a tu solicitud. Intenta de nuevo.",
  nonce_mismatch: "No pudimos verificar la identidad recibida. Intenta de nuevo.",
  invalid_response: "Recibimos una respuesta inválida del servicio de autenticación.",
  network_error: "No pudimos conectar con el servidor. Revisa tu conexión a internet.",
  session_expired: "Tu sesión terminó. Inicia sesión de nuevo.",
  configuration_error: "La configuración de la aplicación no es válida.",
  credentials: "El usuario o la contraseña no son correctos.",
  locked: LOCKED_MESSAGE,
};

export function messageForCode(code: string): string {
  return (ERROR_MESSAGES_ES as Readonly<Record<string, string>>)[code] ?? DEFAULT_ERROR_MESSAGE;
}

export function toUserMessage(error: unknown): string {
  if (isAuthError(error)) return messageForCode(error.code);
  if (typeof error === "string") return messageForCode(error);
  return DEFAULT_ERROR_MESSAGE;
}
