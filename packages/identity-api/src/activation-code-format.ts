export const ACTIVATION_CODE_PATTERN = /^[A-HJ-NP-Z2-9]{8}$/;

export function normalizeActivationCode(input: string): string {
  return input.toUpperCase().replace(/[^A-Z0-9]/g, "");
}

export function formatActivationCode(code: string): string {
  const normalized = normalizeActivationCode(code);
  return normalized.length > 4 ? `${normalized.slice(0, 4)}-${normalized.slice(4, 8)}` : normalized;
}

export function isActivationCode(input: string): boolean {
  return ACTIVATION_CODE_PATTERN.test(normalizeActivationCode(input));
}
