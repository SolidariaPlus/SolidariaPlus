import type { FetchLike } from "@solidariaplus/auth";
import { describe, expect, it, vi } from "vitest";
import { createIdentityAccessApi } from "./identity-access-api.js";

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

const issued = {
  activationCodeId: "c-1",
  memberId: "0190-uuid",
  memberNumber: "001458",
  code: "K8M4P9X2",
  issuedAt: "2026-11-28T15:15:00Z",
  expiresAt: "2026-12-01T15:15:00Z",
};

describe("IdentityAccessApi.issueActivationCode", () => {
  it("posts to the member-number endpoint and parses dates", async () => {
    const send = vi.fn<FetchLike>(async () => json(issued, 201));
    const api = createIdentityAccessApi({ baseUrl: "https://app.test/auth/", fetch: send });

    const result = await api.issueActivationCode(" 001458 ");

    expect(send.mock.calls[0]?.[0]).toBe(
      "https://app.test/auth/members/by-number/001458/activation-codes",
    );
    expect(send.mock.calls[0]?.[1]?.method).toBe("POST");
    expect(result.code).toBe("K8M4P9X2");
    expect(result.expiresAt.toISOString()).toBe("2026-12-01T15:15:00.000Z");
  });

  it("rejects malformed member numbers without calling the API", async () => {
    const send = vi.fn<FetchLike>();
    const api = createIdentityAccessApi({ baseUrl: "https://app.test/auth", fetch: send });

    await expect(api.issueActivationCode("12a")).rejects.toMatchObject({
      code: "MEMBER_NOT_FOUND",
    });
    expect(send).not.toHaveBeenCalled();
  });

  it("maps problem details to AuthError", async () => {
    const send = vi.fn<FetchLike>(async () =>
      json({ code: "MEMBER_NOT_ELIGIBLE", detail: "disabled" }, 422),
    );
    const api = createIdentityAccessApi({ baseUrl: "https://app.test/auth", fetch: send });

    await expect(api.issueActivationCode("1001")).rejects.toMatchObject({
      code: "MEMBER_NOT_ELIGIBLE",
      status: 422,
    });
  });

  it("maps 403 to FORBIDDEN", async () => {
    const send = vi.fn<FetchLike>(async () => new Response(null, { status: 403 }));
    const api = createIdentityAccessApi({ baseUrl: "https://app.test/auth", fetch: send });

    await expect(api.issueActivationCode("1001")).rejects.toMatchObject({ code: "FORBIDDEN" });
  });

  it("maps network failures to network_error", async () => {
    const send = vi.fn<FetchLike>(async () => Promise.reject(new TypeError("offline")));
    const api = createIdentityAccessApi({ baseUrl: "https://app.test/auth", fetch: send });

    await expect(api.issueActivationCode("1001")).rejects.toMatchObject({
      code: "network_error",
    });
  });
});

describe("IdentityAccessApi.resetStaffAuthenticator", () => {
  it("sends the directory account in the body", async () => {
    const send = vi.fn<FetchLike>(async () =>
      json(
        {
          staffSub: "s-1",
          resetAt: "2026-11-28T15:15:00Z",
          resetBySub: "b-1",
          enrolmentDeadline: "2026-11-29T15:15:00Z",
          sessionsRevoked: 2,
        },
        201,
      ),
    );
    const api = createIdentityAccessApi({ baseUrl: "https://app.test/auth", fetch: send });

    const result = await api.resetStaffAuthenticator(" COOP\\jvaldez ");

    const init = send.mock.calls[0]?.[1];
    expect(send.mock.calls[0]?.[0]).toBe("https://app.test/auth/staff/authenticator-resets");
    expect(JSON.parse(init?.body as string)).toEqual({ directoryAccount: "COOP\\jvaldez" });
    expect(result.sessionsRevoked).toBe(2);
    expect(result.enrolmentDeadline.toISOString()).toBe("2026-11-29T15:15:00.000Z");
  });

  it("surfaces SAME_PERSON_NOT_ALLOWED", async () => {
    const send = vi.fn<FetchLike>(async () => json({ code: "SAME_PERSON_NOT_ALLOWED" }, 422));
    const api = createIdentityAccessApi({ baseUrl: "https://app.test/auth", fetch: send });

    await expect(api.resetStaffAuthenticator("COOP\\me")).rejects.toMatchObject({
      code: "SAME_PERSON_NOT_ALLOWED",
    });
  });
});
