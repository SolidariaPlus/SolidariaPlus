import { DEV_FIXTURES } from "./fixtures";

const name = new URLSearchParams(window.location.search).get("fixture") ?? "login";
const fixture = DEV_FIXTURES[name] ?? DEV_FIXTURES["login"];
const root = document.getElementById("root");
if (root && fixture) Object.assign(root.dataset, fixture);

await import("../main");
