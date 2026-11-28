import { ERROR_MESSAGES_ES, AuthError } from "@solidariaplus/auth";
import { act, cleanup, fireEvent, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";
import { claimsFor, createFakeAuthClient, createFakeIdentityApi } from "../test/fake-auth-client";
import { renderApp } from "../test/render-app";

afterEach(() => {
  cleanup();
  sessionStorage.clear();
});

describe("sign-in flow", () => {
  it("sends anonymous visitors straight to the authorization server", async () => {
    const client = createFakeAuthClient();
    renderApp(client, { path: "/" });

    await waitFor(() => expect(client.signIn).toHaveBeenCalledOnce());
    expect(screen.getByRole("status").textContent).toContain("inicio de sesión seguro");
  });

  it("completes the callback and opens the home page (B6)", async () => {
    const client = createFakeAuthClient();
    renderApp(client, { path: "/callback" });

    expect(await screen.findByRole("heading", { level: 1, name: "Tu inicio" })).toBeDefined();
    expect(client.completeSignIn).toHaveBeenCalledOnce();
  });

  it("shows A7 with the Spanish message when the callback fails", async () => {
    const client = createFakeAuthClient();
    client.completeSignIn.mockRejectedValueOnce(new AuthError("access_denied"));
    renderApp(client, { path: "/callback" });

    expect(
      await screen.findByRole("heading", { name: "No pudimos completar el inicio de sesión" }),
    ).toBeDefined();
    expect(screen.getByText(ERROR_MESSAGES_ES.access_denied)).toBeDefined();
    expect(screen.getByRole("complementary", { name: "Qué puedes hacer" })).toBeDefined();

    fireEvent.click(screen.getByRole("button", { name: "Intentar de nuevo" }));
    await waitFor(() => expect(client.signIn).toHaveBeenCalledOnce());
  });
});

describe("home (B6)", () => {
  it("shows the teller card, the role chip and the session window", async () => {
    renderApp(createFakeAuthClient(claimsFor()), { path: "/" });

    expect(await screen.findByRole("heading", { name: "Tu inicio" })).toBeDefined();
    expect(screen.getByText("Cajero")).toBeDefined();
    expect(
      screen.getByText(/Sesión iniciada hoy a las 08:02\. Se cerrará a las 18:02/),
    ).toBeDefined();
    expect(screen.getByRole("link", { name: "Emitir código" }).getAttribute("href")).toBe(
      "/activation-codes/new",
    );
    expect(screen.queryByRole("link", { name: "Restablecer" })).toBeNull();
    expect(screen.getByText("Tus módulos aparecerán aquí")).toBeDefined();
  });

  it("shows the reset card only to the President or the Compliance delegate", async () => {
    renderApp(createFakeAuthClient(claimsFor({ roles: ["BOARD"], functions: ["PRESIDENT"] })), {
      path: "/",
    });

    expect(await screen.findByRole("link", { name: "Restablecer" })).toBeDefined();
    expect(screen.queryByRole("link", { name: "Emitir código" })).toBeNull();
    expect(screen.getByText("Presidencia")).toBeDefined();
  });
});

describe("sign out (B7 → A6) and expiry (B8)", () => {
  it("signs out of every device and lands on A6", async () => {
    const client = createFakeAuthClient(claimsFor());
    renderApp(client, { path: "/" });

    fireEvent.click(await screen.findByRole("button", { name: "Cerrar sesión" }));
    const dialog = await screen.findByRole("dialog", { name: "¿Cerrar sesión?" });
    fireEvent.click(within(dialog).getByRole("checkbox"));
    fireEvent.click(within(dialog).getByRole("button", { name: "Cerrar sesión" }));

    await waitFor(() => expect(client.signOut).toHaveBeenCalledWith("all"));
    expect(await screen.findByRole("heading", { name: "Cerraste sesión" })).toBeDefined();
  });

  it("shows B8 when the session ends without a sign-out", async () => {
    const client = createFakeAuthClient(claimsFor());
    renderApp(client, { path: "/" });
    await screen.findByRole("heading", { name: "Tu inicio" });

    act(() => client.expire());

    expect(await screen.findByRole("heading", { name: "Tu sesión terminó" })).toBeDefined();
    expect(screen.getByRole("button", { name: "Iniciar sesión de nuevo" })).toBeDefined();
  });
});

describe("guards (B9)", () => {
  it("blocks a member from the teller screen", async () => {
    renderApp(createFakeAuthClient(claimsFor({ roles: ["MEMBER"], memberId: "m-1" })), {
      path: "/activation-codes/new",
    });

    expect(
      await screen.findByRole("heading", { name: "No tienes acceso a esta sección" }),
    ).toBeDefined();
  });

  it("blocks a board member without the required function", async () => {
    renderApp(createFakeAuthClient(claimsFor({ roles: ["BOARD"], functions: [] })), {
      path: "/staff/authenticator-resets",
    });

    expect(
      await screen.findByRole("heading", { name: "No tienes acceso a esta sección" }),
    ).toBeDefined();
  });
});

describe("issue activation code (B10)", () => {
  it("issues the code and shows it once with its expiry", async () => {
    const api = createFakeIdentityApi();
    renderApp(createFakeAuthClient(claimsFor()), {
      path: "/activation-codes/new",
      identityApi: api,
    });

    fireEvent.change(await screen.findByLabelText("Número de socio"), {
      target: { value: "001458" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Emitir código" }));

    expect(await screen.findByText("K8M4-P9X2")).toBeDefined();
    expect(api.issueActivationCode).toHaveBeenCalledWith("001458");
    expect(screen.getByText(/Vence el 01\/12\/2026 a las 10:15, en 72/)).toBeDefined();
    expect(screen.getByText(/Este código no se volverá a mostrar/)).toBeDefined();
  });

  it("explains an unknown member next to the field", async () => {
    const api = createFakeIdentityApi();
    api.issueActivationCode.mockRejectedValueOnce(new AuthError("MEMBER_NOT_FOUND"));
    renderApp(createFakeAuthClient(claimsFor()), {
      path: "/activation-codes/new",
      identityApi: api,
    });

    fireEvent.change(await screen.findByLabelText("Número de socio"), {
      target: { value: "9999" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Emitir código" }));

    expect(await screen.findByText(ERROR_MESSAGES_ES.MEMBER_NOT_FOUND)).toBeDefined();
  });
});

describe("reset second factor (B11)", () => {
  it("asks for confirmation before resetting", async () => {
    const api = createFakeIdentityApi();
    renderApp(createFakeAuthClient(claimsFor({ roles: ["BOARD"], functions: ["COMPLIANCE"] })), {
      path: "/staff/authenticator-resets",
      identityApi: api,
    });

    fireEvent.change(await screen.findByLabelText("Usuario institucional de la persona"), {
      target: { value: "COOP\\jvaldez" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Restablecer segundo factor" }));

    const dialog = await screen.findByRole("dialog", {
      name: "¿Restablecer el segundo factor de COOP\\jvaldez?",
    });
    expect(api.resetStaffAuthenticator).not.toHaveBeenCalled();
    fireEvent.click(within(dialog).getByRole("button", { name: "Sí, restablecer" }));

    await waitFor(() => expect(api.resetStaffAuthenticator).toHaveBeenCalledWith("COOP\\jvaldez"));
    expect(await screen.findByText(/Cerramos 2 sesiones abiertas/)).toBeDefined();
  });

  it("shows SAME_PERSON_NOT_ALLOWED next to the field", async () => {
    const api = createFakeIdentityApi();
    api.resetStaffAuthenticator.mockRejectedValueOnce(new AuthError("SAME_PERSON_NOT_ALLOWED"));
    renderApp(createFakeAuthClient(claimsFor({ roles: ["BOARD"], functions: ["PRESIDENT"] })), {
      path: "/staff/authenticator-resets",
      identityApi: api,
    });

    fireEvent.change(await screen.findByLabelText("Usuario institucional de la persona"), {
      target: { value: "COOP\\me" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Restablecer segundo factor" }));
    fireEvent.click(await screen.findByRole("button", { name: "Sí, restablecer" }));

    expect(await screen.findAllByText(ERROR_MESSAGES_ES.SAME_PERSON_NOT_ALLOWED)).not.toHaveLength(
      0,
    );
  });
});
