import { AuthError, type FetchLike } from "@solidariaplus/auth";
import {
  MEMBER_NUMBER_PATTERN,
  type AuthenticatorReset,
  type IssuedActivationCode,
} from "./models.js";
import {
  asRecord,
  readJson,
  requireDate,
  requireNumber,
  requireString,
} from "./response-parsing.js";

export interface ActivationCodeIssuer {
  issueActivationCode(memberNumber: string): Promise<IssuedActivationCode>;
}

export interface StaffAuthenticatorResetter {
  resetStaffAuthenticator(directoryAccount: string): Promise<AuthenticatorReset>;
}

export type IdentityAccessApi = ActivationCodeIssuer & StaffAuthenticatorResetter;

export interface IdentityAccessApiOptions {
  readonly baseUrl: string;
  readonly fetch: FetchLike;
}

export function createIdentityAccessApi(options: IdentityAccessApiOptions): IdentityAccessApi {
  const baseUrl = options.baseUrl.replace(/\/+$/, "");

  const post = async (path: string, body?: unknown): Promise<unknown> => {
    let response: Response;
    try {
      response = await options.fetch(`${baseUrl}${path}`, {
        method: "POST",
        headers: {
          Accept: "application/json",
          ...(body === undefined ? {} : { "Content-Type": "application/json" }),
        },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch (cause) {
      if (cause instanceof AuthError) throw cause;
      throw new AuthError("network_error", cause instanceof Error ? cause.message : undefined);
    }
    const payload = await readJson(response);
    if (response.status === 403) throw new AuthError("FORBIDDEN", undefined, 403);
    if (!response.ok) throw AuthError.fromResponseBody(payload, response.status);
    return payload;
  };

  return {
    async issueActivationCode(memberNumber) {
      const normalized = memberNumber.trim();
      if (!MEMBER_NUMBER_PATTERN.test(normalized)) {
        throw new AuthError("MEMBER_NOT_FOUND", "The member number must have 1 to 10 digits");
      }
      const record = asRecord(
        await post(`/members/by-number/${encodeURIComponent(normalized)}/activation-codes`),
      );
      return {
        activationCodeId: requireString(record, "activationCodeId"),
        memberId: requireString(record, "memberId"),
        memberNumber: requireString(record, "memberNumber"),
        code: requireString(record, "code"),
        issuedAt: requireDate(record, "issuedAt"),
        expiresAt: requireDate(record, "expiresAt"),
      };
    },

    async resetStaffAuthenticator(directoryAccount) {
      const record = asRecord(
        await post("/staff/authenticator-resets", { directoryAccount: directoryAccount.trim() }),
      );
      return {
        staffSubject: requireString(record, "staffSub"),
        resetAt: requireDate(record, "resetAt"),
        resetBySubject: requireString(record, "resetBySub"),
        enrolmentDeadline: requireDate(record, "enrolmentDeadline"),
        sessionsRevoked: requireNumber(record, "sessionsRevoked"),
      };
    },
  };
}
