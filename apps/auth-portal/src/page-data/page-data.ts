export interface CsrfToken {
  readonly parameterName: string;
  readonly token: string;
}

interface FormPageData {
  readonly action: string;
  readonly csrf: CsrfToken;
  readonly error: string | undefined;
}

export interface LoginPageData extends FormPageData {
  readonly page: "login";
  readonly activated: boolean;
  readonly activateUrl: string;
}

export interface MfaPageData extends FormPageData {
  readonly page: "mfa";
  readonly mode: "enrol" | "verify";
  readonly secret: string | undefined;
  readonly otpauthUri: string | undefined;
  readonly loginUrl: string;
  readonly enrolmentDeadline: Date | undefined;
}

export interface ActivatePageData extends FormPageData {
  readonly page: "activate";
  readonly loginUrl: string;
  readonly memberNumber: string | undefined;
}

export type PageData = LoginPageData | MfaPageData | ActivatePageData;

export class PageDataError extends Error {
  constructor(detail: string) {
    super(`Invalid page data: ${detail}`);
    this.name = "PageDataError";
  }
}

type Dataset = Readonly<Record<string, string | undefined>>;

export function parsePageData(dataset: Dataset): PageData {
  const page = required(dataset, "page");
  const form: FormPageData = {
    action: required(dataset, "action"),
    csrf: {
      parameterName: required(dataset, "csrfParameter"),
      token: required(dataset, "csrfToken"),
    },
    error: optional(dataset, "error"),
  };
  switch (page) {
    case "login":
      return {
        ...form,
        page,
        activated: dataset["activated"] === "true",
        activateUrl: required(dataset, "activateUrl"),
      };
    case "mfa":
      return parseMfa(dataset, form);
    case "activate":
      return {
        ...form,
        page,
        loginUrl: required(dataset, "loginUrl"),
        memberNumber: optional(dataset, "memberNumber"),
      };
    default:
      throw new PageDataError(`unknown page "${page}"`);
  }
}

function parseMfa(dataset: Dataset, form: FormPageData): MfaPageData {
  const mode = dataset["mode"] === "enrol" ? "enrol" : "verify";
  const secret = optional(dataset, "secret");
  const otpauthUri = optional(dataset, "otpauthUri");
  if (mode === "enrol" && (!secret || !otpauthUri)) {
    throw new PageDataError("enrolment requires secret and otpauthUri");
  }
  if (otpauthUri && !otpauthUri.startsWith("otpauth://")) {
    throw new PageDataError("otpauthUri must use the otpauth scheme");
  }
  const deadline = optional(dataset, "enrolmentDeadline");
  const enrolmentDeadline = deadline ? new Date(deadline) : undefined;
  return {
    ...form,
    page: "mfa",
    mode,
    secret,
    otpauthUri,
    loginUrl: required(dataset, "loginUrl"),
    enrolmentDeadline:
      enrolmentDeadline && !Number.isNaN(enrolmentDeadline.getTime())
        ? enrolmentDeadline
        : undefined,
  };
}

function required(dataset: Dataset, key: string): string {
  const value = dataset[key]?.trim();
  if (!value) throw new PageDataError(`${key} is missing`);
  return value;
}

function optional(dataset: Dataset, key: string): string | undefined {
  const value = dataset[key]?.trim();
  return value ? value : undefined;
}
