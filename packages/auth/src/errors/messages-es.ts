import { isAuthError, type AuthErrorCode } from "./auth-error";

export const DEFAULT_ERROR_MESSAGE = "Ocurrió un error inesperado. Intente nuevamente.";

export const ERROR_MESSAGES_ES: Readonly<Record<AuthErrorCode, string>> = {
  invalid_request: "La solicitud de inicio de sesión no es válida. Intente nuevamente.",
  unauthorized_client: "Esta aplicación no está autorizada para iniciar sesión.",
  access_denied: "El acceso fue cancelado o denegado.",
  unsupported_response_type: "El servidor de autenticación no admite este tipo de solicitud.",
  invalid_scope: "Los permisos solicitados no son válidos.",
  server_error: "El servidor de autenticación presentó un error. Intente más tarde.",
  temporarily_unavailable:
    "El servicio de autenticación no está disponible por el momento. Intente más tarde.",
  invalid_client: "La aplicación no pudo identificarse ante el servidor de autenticación.",
  invalid_grant: "Su sesión expiró o ya no es válida. Inicie sesión nuevamente.",
  unsupported_grant_type: "El tipo de autorización no es compatible.",
  AUTH_ACCOUNT_LOCKED:
    "Su cuenta está bloqueada temporalmente por varios intentos fallidos. Intente más tarde.",
  AUTH_ACTIVATION_CODE_INVALID: "El código de activación no es válido o ha expirado.",
  AUTH_MFA_REQUIRED: "Debe completar la verificación en dos pasos para continuar.",
  state_mismatch:
    "La respuesta de inicio de sesión no corresponde a la solicitud. Intente nuevamente.",
  nonce_mismatch: "No se pudo verificar la identidad recibida. Intente nuevamente.",
  invalid_response: "Se recibió una respuesta inválida del servidor de autenticación.",
  network_error: "No se pudo conectar con el servidor. Verifique su conexión a internet.",
  session_expired: "Su sesión terminó. Inicie sesión nuevamente.",
  configuration_error: "La configuración de la aplicación no es válida.",
};

export function messageForCode(code: string): string {
  return (ERROR_MESSAGES_ES as Readonly<Record<string, string>>)[code] ?? DEFAULT_ERROR_MESSAGE;
}

export function toUserMessage(error: unknown): string {
  if (isAuthError(error)) return messageForCode(error.code);
  if (typeof error === "string") return messageForCode(error);
  return DEFAULT_ERROR_MESSAGE;
}
