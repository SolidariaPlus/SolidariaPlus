import reactHooks from "eslint-plugin-react-hooks";
import globals from "globals";
import tseslint from "typescript-eslint";
import base from "./base.js";

/** Configuración para paquetes React (web, desktop renderer, ui, mobile). */
export default tseslint.config(...base, {
  files: ["**/*.{ts,tsx}"],
  languageOptions: {
    globals: { ...globals.browser },
  },
  plugins: { "react-hooks": reactHooks },
  rules: {
    ...reactHooks.configs.recommended.rules,
  },
});
