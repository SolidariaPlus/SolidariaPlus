import react from "@vitejs/plugin-react";
import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";

const DEV_ORIGIN_HOST = "localhost:5173";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "");
  const identityAccessService = env["IDENTITY_ACCESS_URL"] ?? "http://localhost:8097";
  return {
    plugins: [react()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        "/auth": {
          target: identityAccessService,
          changeOrigin: false,
          headers: {
            "X-Forwarded-Host": DEV_ORIGIN_HOST,
            "X-Forwarded-Proto": "http",
          },
        },
      },
    },
    test: {
      environment: "jsdom",
      testTimeout: 15_000,
    },
  };
});
