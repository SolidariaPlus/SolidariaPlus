import js from "@eslint/js";
import tseslint from "typescript-eslint";

/** Base flat config for every TypeScript package in the monorepo. */
export default tseslint.config(
  { ignores: ["dist/**", "out/**", "build/**", ".expo/**", "coverage/**"] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
);
