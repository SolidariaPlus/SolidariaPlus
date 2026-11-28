export type PasswordRuleId = "length" | "uppercase" | "lowercase" | "digit" | "symbol" | "match";

export interface PasswordRule {
  readonly id: PasswordRuleId;
  readonly label: string;
  readonly satisfied: boolean;
}

export const MIN_PASSWORD_LENGTH = 12;

export function evaluatePassword(password: string, confirmation: string): readonly PasswordRule[] {
  return [
    {
      id: "length",
      label: `${MIN_PASSWORD_LENGTH} caracteres o más`,
      satisfied: password.length >= MIN_PASSWORD_LENGTH,
    },
    { id: "uppercase", label: "Una letra mayúscula", satisfied: /[A-Z]/.test(password) },
    { id: "lowercase", label: "Una letra minúscula", satisfied: /[a-z]/.test(password) },
    { id: "digit", label: "Un número", satisfied: /\d/.test(password) },
    {
      id: "symbol",
      label: "Un símbolo, como * / ? ,",
      satisfied: /[^A-Za-z\d]/.test(password),
    },
    {
      id: "match",
      label: "Las dos contraseñas coinciden",
      satisfied: password.length > 0 && password === confirmation,
    },
  ];
}

export function isPasswordAccepted(rules: readonly PasswordRule[]): boolean {
  return rules.every((rule) => rule.satisfied);
}
