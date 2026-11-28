import { hasAnyRole, type AuthClaims } from "@solidariaplus/auth";

export const ROLES = {
  member: "MEMBER",
  teller: "TELLER",
  board: "BOARD",
  accountant: "ACCOUNTANT",
  externalConsultant: "EXTERNAL_CONSULTANT",
} as const;

export const FUNCTIONS = {
  president: "PRESIDENT",
  compliance: "COMPLIANCE",
} as const;

const ROLE_LABELS: Readonly<Record<string, string>> = {
  MEMBER: "Socio",
  TELLER: "Cajero",
  BOARD: "Junta directiva",
  ACCOUNTANT: "Contabilidad",
  EXTERNAL_CONSULTANT: "Consultoría externa",
};

const FUNCTION_LABELS: Readonly<Record<string, string>> = {
  PRESIDENT: "Presidencia",
  COMPLIANCE: "Cumplimiento",
};

export interface AccessRequirement {
  readonly roles?: readonly string[];
  readonly functions?: readonly string[];
}

export const CAN_ISSUE_ACTIVATION_CODES: AccessRequirement = { roles: [ROLES.teller] };

export const CAN_RESET_STAFF_AUTHENTICATOR: AccessRequirement = {
  roles: [ROLES.board],
  functions: [FUNCTIONS.president, FUNCTIONS.compliance],
};

export function satisfies(claims: AuthClaims | undefined, requirement: AccessRequirement): boolean {
  if (!claims) return false;
  const roleOk = !requirement.roles || hasAnyRole(claims, requirement.roles);
  const functionOk =
    !requirement.functions || requirement.functions.some((fn) => claims.functions.includes(fn));
  return roleOk && functionOk;
}

export function roleLabels(claims: AuthClaims | undefined): readonly string[] {
  if (!claims) return [];
  return [
    ...claims.roles.map((role) => ROLE_LABELS[role] ?? role),
    ...claims.functions.map((fn) => FUNCTION_LABELS[fn] ?? fn),
  ];
}
