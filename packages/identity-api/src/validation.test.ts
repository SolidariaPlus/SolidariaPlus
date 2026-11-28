import { describe, expect, it } from "vitest";
import {
  formatActivationCode,
  isActivationCode,
  normalizeActivationCode,
} from "./activation-code-format.js";
import { evaluatePassword, isPasswordAccepted } from "./password-policy.js";

describe("activation code format", () => {
  it("normalizes case and separators", () => {
    expect(normalizeActivationCode("k8m4-p9x2")).toBe("K8M4P9X2");
    expect(formatActivationCode("k8m4p9x2")).toBe("K8M4-P9X2");
  });

  it("accepts only the MS-17 alphabet", () => {
    expect(isActivationCode("K8M4-P9X2")).toBe(true);
    expect(isActivationCode("K8M4-P9X")).toBe(false);
    expect(isActivationCode("O1IL-P9X2")).toBe(false);
  });
});

describe("password policy", () => {
  it("mirrors PasswordPolicy.java", () => {
    const rules = evaluatePassword("Ahorro2026familia", "Ahorro2026familia");
    expect(rules.filter((rule) => !rule.satisfied).map((rule) => rule.id)).toEqual(["symbol"]);
    expect(isPasswordAccepted(rules)).toBe(false);
    expect(isPasswordAccepted(evaluatePassword("Ahorro2026*familia", "Ahorro2026*familia"))).toBe(
      true,
    );
  });

  it("requires both passwords to match", () => {
    const rules = evaluatePassword("Ahorro2026*familia", "Ahorro2026*famili");
    expect(rules.find((rule) => rule.id === "match")?.satisfied).toBe(false);
  });
});
