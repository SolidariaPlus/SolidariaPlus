import { describe, expect, it } from "vitest";
import { DEV_FIXTURES } from "../dev/fixtures";
import { PageDataError, parsePageData } from "./page-data";

describe("parsePageData", () => {
  it("parses every development fixture", () => {
    for (const fixture of Object.values(DEV_FIXTURES)) {
      expect(() => parsePageData(fixture)).not.toThrow();
    }
  });

  it("reads the login contract", () => {
    const data = parsePageData({
      page: "login",
      action: "/auth/login",
      csrfParameter: "_csrf",
      csrfToken: "t",
      error: "locked",
      activated: "false",
      activateUrl: "/auth/activate",
    });
    expect(data).toMatchObject({
      page: "login",
      action: "/auth/login",
      csrf: { parameterName: "_csrf", token: "t" },
      error: "locked",
      activated: false,
    });
  });

  it("requires the CSRF token", () => {
    expect(() =>
      parsePageData({ page: "login", action: "/login", csrfParameter: "_csrf", activateUrl: "/a" }),
    ).toThrow(PageDataError);
  });

  it("requires the secret and an otpauth URI when enrolling", () => {
    const base = {
      page: "mfa",
      action: "/mfa",
      csrfParameter: "_csrf",
      csrfToken: "t",
      mode: "enrol",
      loginUrl: "/login",
    };
    expect(() => parsePageData(base)).toThrow(PageDataError);
    expect(() =>
      parsePageData({ ...base, secret: "ABC", otpauthUri: "javascript:alert(1)" }),
    ).toThrow(PageDataError);
  });

  it("rejects unknown pages", () => {
    expect(() =>
      parsePageData({ page: "other", action: "/x", csrfParameter: "_csrf", csrfToken: "t" }),
    ).toThrow(PageDataError);
  });
});
