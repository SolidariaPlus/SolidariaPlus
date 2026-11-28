const common = {
  csrfParameter: "_csrf",
  csrfToken: "dev-token",
};

export const DEV_FIXTURES: Readonly<Record<string, Readonly<Record<string, string>>>> = {
  login: { ...common, page: "login", action: "#login", activateUrl: "?fixture=activate" },
  "login-error": {
    ...common,
    page: "login",
    action: "#login",
    activateUrl: "?fixture=activate",
    error: "credentials",
  },
  "login-locked": {
    ...common,
    page: "login",
    action: "#login",
    activateUrl: "?fixture=activate",
    error: "locked",
  },
  activated: {
    ...common,
    page: "login",
    action: "#login",
    activateUrl: "?fixture=activate",
    activated: "true",
  },
  "mfa-verify": {
    ...common,
    page: "mfa",
    action: "#mfa",
    mode: "verify",
    loginUrl: "?fixture=login",
  },
  "mfa-enrol": {
    ...common,
    page: "mfa",
    action: "#mfa",
    mode: "enrol",
    loginUrl: "?fixture=login",
    secret: "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP",
    otpauthUri:
      "otpauth://totp/Solidaria%2B?secret=JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP&issuer=Solidaria%2B",
    enrolmentDeadline: "2026-11-29T15:15:00Z",
  },
  activate: { ...common, page: "activate", action: "#activate", loginUrl: "?fixture=login" },
  "activate-error": {
    ...common,
    page: "activate",
    action: "#activate",
    loginUrl: "?fixture=login",
    error: "AUTH_ACTIVATION_CODE_INVALID",
    memberNumber: "1001",
  },
};
