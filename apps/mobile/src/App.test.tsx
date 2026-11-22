import { render, screen } from "@testing-library/react-native";
import { App } from "./App";

describe("App (mobile)", () => {
  it("muestra el nombre de la app", async () => {
    await render(<App />);
    expect(screen.getByText("SolidariaPlus · Mobile")).toBeTruthy();
  });
});
