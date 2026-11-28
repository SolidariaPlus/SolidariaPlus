import { describe, expect, it } from "vitest";
import { solidariaTheme } from "./mui-theme";
import { colors, fontFamilies } from "./tokens";

describe("solidariaTheme", () => {
  it("maps the Figma palette and typography", () => {
    expect(solidariaTheme.palette.primary.main).toBe(colors.primary);
    expect(solidariaTheme.palette.secondary.main).toBe(colors.navy);
    expect(solidariaTheme.palette.text.primary).toBe(colors.text);
    expect(solidariaTheme.typography.fontFamily).toBe(fontFamilies.body);
    expect(solidariaTheme.typography.h1.fontFamily).toBe(fontFamilies.heading);
    expect(solidariaTheme.typography.button.textTransform).toBe("none");
    expect(solidariaTheme.shape.borderRadius).toBe(8);
  });
});
