import { ThemeProvider } from "@mui/material/styles";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactElement } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { solidariaTheme } from "../theme/mui-theme";
import { AuthShell, PRIVACY_NOTICE } from "./AuthShell";
import { AsidePanel } from "./AsidePanel";
import { CopyableCode } from "./CopyableCode";
import { NoticeBanner } from "./NoticeBanner";
import { PasswordChecklist } from "./PasswordChecklist";
import { PasswordField } from "./PasswordField";

afterEach(cleanup);

function renderThemed(element: ReactElement) {
  return render(<ThemeProvider theme={solidariaTheme}>{element}</ThemeProvider>);
}

describe("PasswordField", () => {
  it("toggles visibility with an accessible pressed state", () => {
    renderThemed(<PasswordField label="Contraseña" name="password" />);
    const input = screen.getByLabelText("Contraseña");
    expect(input.getAttribute("type")).toBe("password");

    const toggle = screen.getByRole("button", { name: "Mostrar contraseña" });
    fireEvent.click(toggle);

    expect(input.getAttribute("type")).toBe("text");
    expect(
      screen.getByRole("button", { name: "Ocultar contraseña" }).getAttribute("aria-pressed"),
    ).toBe("true");
  });

  it("links helper text through aria-describedby", () => {
    renderThemed(<PasswordField label="Contraseña" helperText="Después de 5 intentos" />);
    const input = screen.getByLabelText("Contraseña");
    const describedBy = input.getAttribute("aria-describedby") ?? "";
    expect(document.getElementById(describedBy)?.textContent).toBe("Después de 5 intentos");
  });
});

describe("PasswordChecklist", () => {
  it("announces each rule state in text, not only with color", () => {
    renderThemed(
      <PasswordChecklist
        rules={[
          { id: "length", label: "12 caracteres o más", satisfied: true },
          { id: "symbol", label: "Un símbolo", satisfied: false },
        ]}
      />,
    );
    expect(screen.getByText("12 caracteres o más").textContent).toContain("(cumplido)");
    expect(screen.getByText("Un símbolo").textContent).toContain("(pendiente)");
  });
});

describe("CopyableCode", () => {
  it("copies the raw value and confirms", async () => {
    const onCopy = vi.fn(async () => undefined);
    renderThemed(<CopyableCode value="K8M4P9X2" display="K8M4-P9X2" onCopy={onCopy} />);

    fireEvent.click(screen.getByRole("button", { name: "Copiar código" }));

    expect(onCopy).toHaveBeenCalledWith("K8M4P9X2");
    await waitFor(() => expect(screen.getByRole("button", { name: "¡Copiado!" })).toBeDefined());
  });
});

describe("AuthShell and AsidePanel", () => {
  it("renders the privacy notice and a labelled aside", () => {
    renderThemed(
      <AuthShell aside={<AsidePanel title="Antes de entrar" items={["Uno", "Dos"]} />}>
        <h1>Inicia sesión</h1>
      </AuthShell>,
    );
    expect(screen.getByText(PRIVACY_NOTICE)).toBeDefined();
    expect(screen.getByRole("complementary", { name: "Antes de entrar" })).toBeDefined();
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
  });
});

describe("NoticeBanner", () => {
  it("uses alert for errors and status otherwise", () => {
    renderThemed(
      <>
        <NoticeBanner tone="error">Falló</NoticeBanner>
        <NoticeBanner tone="info">Info</NoticeBanner>
      </>,
    );
    expect(screen.getByRole("alert").textContent).toContain("Falló");
    expect(screen.getByRole("status").textContent).toContain("Info");
  });
});
