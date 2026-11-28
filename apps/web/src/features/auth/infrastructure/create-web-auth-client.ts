import {
  createAuthClient,
  jsonConfigSource,
  loadRuntimeConfig,
  type AuthClient,
} from "@solidariaplus/auth";
import { WebPlatformAdapter } from "./web-platform-adapter";

export const RUNTIME_CONFIG_PATH = "/runtime-config.json";

export async function createWebAuthClient(): Promise<AuthClient> {
  const config = await loadRuntimeConfig(jsonConfigSource(RUNTIME_CONFIG_PATH));
  const platform = new WebPlatformAdapter(window.location, window.sessionStorage);
  return createAuthClient(config, platform);
}
