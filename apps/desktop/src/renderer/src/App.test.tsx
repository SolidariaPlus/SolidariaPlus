import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "./App";

describe("App (desktop renderer)", () => {
  it("muestra el título de la app de escritorio", () => {
    render(<App />);
    expect(screen.getByRole("heading", { name: /Desktop/ })).toBeDefined();
  });
});
