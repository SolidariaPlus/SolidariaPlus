import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "./App";

describe("App (desktop renderer)", () => {
  it("renders the desktop app title", () => {
    render(<App />);
    expect(screen.getByRole("heading", { name: /Desktop/ })).toBeDefined();
  });
});
