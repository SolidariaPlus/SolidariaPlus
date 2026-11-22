import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "./App";

describe("App (web)", () => {
  it("renders the title and the main action", () => {
    render(<App />);
    expect(screen.getByRole("heading", { name: /SolidariaPlus/ })).toBeDefined();
    expect(screen.getByRole("button", { name: "Comenzar" })).toBeDefined();
  });
});
