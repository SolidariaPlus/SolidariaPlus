import js from "@eslint/js";
import tseslint from "typescript-eslint";

/** Configuración base (flat config) para todo paquete TypeScript del monorepo. */
export default tseslint.config(
  { ignores: ["dist/**", "out/**", "build/**", ".expo/**", "coverage/**"] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
);
