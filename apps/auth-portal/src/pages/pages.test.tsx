import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { ERROR_MESSAGES_ES } from "@solidariaplus/auth";
import { afterEach, describe, expect, it, vi } from "vitest";
import { DEV_FIXTURES } from "../dev/fixtures";
import { parsePageData } from "../page-data/page-data";
import { PortalApp } from "../PortalApp";
import { qrPath } from "../components/TotpQrCode";
import { secondsUntilNextCode } from "../components/TotpCountdown";
import { groupSecret } from "./MfaEnrolPage";

afterEach(cleanup);

function renderFixture(name: string, overrides: Record<string, string> = {}) {
  const fixture = DEV_FIXTURES[name];
  if (!fixture) throw new Error(`Unknown fixture ${name}`);
  const view = render(<PortalApp data={parsePageData({ ...fixture, ...overrides })} />);
  const form = view.container.querySelector("form");
  return { ...view, form };
}

function submit(form: HTMLFormElement | null): boolean {
  if (!form) throw new Error("No form rendered");
  const event = new Event("submit", { bubbles: true, cancelable: true });
  act(() => {
    form.dispatchEvent(event);
  });
  return !event.defaultPrevented;
}

describe("LoginPage (A1)", () => {
  it("renders a native POST form with the CSRF token and autocomplete hints", () => {
    const { form } = renderFixture("login");

    expect(screen.getByRole("heading", { level: 1, name: "Inicia sesión" })).toBeDefined();
    expect(form?.getAttribute("method")).toBe("post");
    expect(form?.getAttribute("action")).toBe("#login");
    const csrf = form?.querySelector<HTMLInputElement>('input[name="_csrf"]');
    expect(csrf?.value).toBe("dev-token");
    expect(
      screen.getByLabelText("Número de socio o usuario institucional").getAttribute("autocomplete"),
    ).toBe("username");
    expect(screen.getByLabelText("Contraseña").getAttribute("autocomplete")).toBe(
      "current-password",
    );
    expect(screen.getByRole("link", { name: /Activa tu cuenta/ }).getAttribute("href")).toBe(
      "?fixture=activate",
    );
  });

  it("blocks the submit and explains empty fields", () => {
    const { form } = renderFixture("login");

    expect(submit(form)).toBe(false);
    expect(
      screen.getByText("Escribe tu número de socio o tu usuario institucional."),
    ).toBeDefined();
    expect(screen.getByText("Escribe tu contraseña.")).toBeDefined();
  });

  it("lets a complete form submit natively", () => {
    const { form } = renderFixture("login");
    fireEvent.change(screen.getByLabelText("Número de socio o usuario institucional"), {
      target: { value: "1001" },
    });
    fireEvent.change(screen.getByLabelText("Contraseña"), { target: { value: "secret" } });

    expect(submit(form)).toBe(true);
  });

  it("shows the server error in Spanish", () => {
    renderFixture("login-locked");
    expect(screen.getByRole("alert").textContent).toContain(ERROR_MESSAGES_ES.locked);
  });
});

describe("Account activated (A5)", () => {
  it("confirms the activation and reveals the form on demand", () => {
    renderFixture("activated");

    expect(screen.getByRole("heading", { name: "Tu cuenta está activa" })).toBeDefined();
    fireEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));

    expect(screen.getByRole("heading", { name: "Inicia sesión" })).toBeDefined();
  });
});

describe("MfaVerifyPage (A2)", () => {
  it("keeps only digits, allows paste and validates length", () => {
    const { form } = renderFixture("mfa-verify");
    const input = screen.getByLabelText("Código de 6 dígitos");

    expect(input.getAttribute("autocomplete")).toBe("one-time-code");
    expect(input.getAttribute("inputmode")).toBe("numeric");
    fireEvent.change(input, { target: { value: "12 34" } });
    expect((input as HTMLInputElement).value).toBe("1234");
    expect(submit(form)).toBe(false);
    expect(screen.getByText("El código tiene 6 dígitos.")).toBeDefined();

    fireEvent.change(input, { target: { value: "123456" } });
    expect(submit(form)).toBe(true);
  });

  it("counts down to the next 30-second window", () => {
    expect(secondsUntilNextCode(0)).toBe(30);
    expect(secondsUntilNextCode(12_000)).toBe(18);
    expect(secondsUntilNextCode(29_999)).toBe(1);
  });
});

describe("MfaEnrolPage (A3)", () => {
  it("renders the QR, the deep link, the grouped key and the deadline", () => {
    renderFixture("mfa-enrol");

    expect(screen.getByRole("img", { name: /Código QR/ })).toBeDefined();
    expect(
      screen.getByRole("link", { name: "Abrir en mi app de autenticación" }).getAttribute("href"),
    ).toMatch(/^otpauth:\/\/totp\//);
    expect(screen.getByText("JBSW Y3DP EHPK 3PXP JBSW Y3DP EHPK 3PXP")).toBeDefined();
    expect(screen.getByRole("status").textContent).toContain("Tu segundo factor fue restablecido");
  });

  it("builds a square QR path and groups secrets by four", () => {
    const { size, path } = qrPath("otpauth://totp/x?secret=ABC");
    expect(size).toBeGreaterThan(20);
    expect(path.length).toBeGreaterThan(0);
    expect(groupSecret("ABCDEFGHIJ")).toBe("ABCD EFGH IJ");
  });
});

describe("ActivatePage (A4)", () => {
  it("submits the normalized code and never posts the confirmation", () => {
    const { form } = renderFixture("activate");
    fireEvent.change(screen.getByLabelText("Número de socio"), { target: { value: "1001" } });
    fireEvent.change(screen.getByLabelText("Código de ventanilla"), {
      target: { value: "k8m4-p9x2" },
    });
    fireEvent.change(screen.getByLabelText("Nueva contraseña"), {
      target: { value: "Ahorro2026*familia" },
    });
    fireEvent.change(screen.getByLabelText("Repite la contraseña"), {
      target: { value: "Ahorro2026*familia" },
    });

    expect((screen.getByLabelText("Código de ventanilla") as HTMLInputElement).value).toBe(
      "K8M4-P9X2",
    );
    expect(submit(form)).toBe(true);
    const formData = new FormData(form ?? undefined);
    expect(formData.get("activationCode")).toBe("K8M4P9X2");
    expect(formData.get("memberNumber")).toBe("1001");
    expect(formData.get("newPassword")).toBe("Ahorro2026*familia");
    expect([...formData.keys()].sort()).toEqual(
      ["_csrf", "activationCode", "memberNumber", "newPassword"].sort(),
    );
  });

  it("blocks weak or mismatched passwords and updates the checklist", () => {
    const { form } = renderFixture("activate");
    fireEvent.change(screen.getByLabelText("Nueva contraseña"), {
      target: { value: "Ahorro2026familia" },
    });

    expect(screen.getByText("Un símbolo, como * / ? ,").textContent).toContain("(pendiente)");
    expect(screen.getByText("12 caracteres o más").textContent).toContain("(cumplido)");
    expect(submit(form)).toBe(false);
    expect(
      screen.getByText("Tu contraseña aún no cumple todas las reglas de abajo."),
    ).toBeDefined();
    expect(screen.getByText("Las dos contraseñas deben coincidir.")).toBeDefined();
  });

  it("keeps the member number and explains an invalid code", () => {
    renderFixture("activate-error");
    expect((screen.getByLabelText("Número de socio") as HTMLInputElement).value).toBe("1001");
    expect(screen.getByRole("alert").textContent).toContain(
      ERROR_MESSAGES_ES.AUTH_ACTIVATION_CODE_INVALID,
    );
  });
});

vi.stubGlobal("requestAnimationFrame", (callback: FrameRequestCallback) => {
  callback(0);
  return 0;
});
