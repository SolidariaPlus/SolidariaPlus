import { AuthError } from "@solidariaplus/auth";

export type JsonRecord = Readonly<Record<string, unknown>>;

export function asRecord(value: unknown): JsonRecord {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    throw invalid("the response is not an object");
  }
  return value as JsonRecord;
}

export function requireString(record: JsonRecord, key: string): string {
  const value = record[key];
  if (typeof value !== "string" || value.length === 0) throw invalid(`${key} is missing`);
  return value;
}

export function requireDate(record: JsonRecord, key: string): Date {
  const date = new Date(requireString(record, key));
  if (Number.isNaN(date.getTime())) throw invalid(`${key} is not a date`);
  return date;
}

export function requireNumber(record: JsonRecord, key: string): number {
  const value = record[key];
  if (typeof value !== "number" || !Number.isFinite(value)) throw invalid(`${key} is not a number`);
  return value;
}

export async function readJson(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return undefined;
  }
}

function invalid(detail: string): AuthError {
  return new AuthError("invalid_response", `Invalid identity-access response: ${detail}`);
}
