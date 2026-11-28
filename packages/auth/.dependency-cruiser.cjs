module.exports = {
  forbidden: [
    {
      name: "no-circular",
      severity: "error",
      from: {},
      to: { circular: true },
    },
    {
      name: "no-ui-frameworks",
      severity: "error",
      from: { path: "^src" },
      to: { path: "(^|/)node_modules/(react|react-dom|react-native|electron|expo)(/|$)" },
    },
    {
      name: "no-browser-storage-modules",
      severity: "error",
      from: { path: "^src" },
      to: { path: "(^|/)node_modules/(js-cookie|localforage|idb-keyval)(/|$)" },
    },
    {
      name: "core-does-not-depend-on-http",
      severity: "error",
      from: { path: "^src/(pkce|claims|errors|crypto|config|oauth)/" },
      to: { path: "^src/http/" },
    },
    {
      name: "only-index-uses-auth-client",
      severity: "error",
      from: { path: "^src/(?!index\.ts$|auth-client\.test\.ts$)" },
      to: { path: "^src/auth-client\.ts$" },
    },
    {
      name: "no-test-imports-in-production",
      severity: "error",
      from: { path: "^src", pathNot: "\.test\.ts$" },
      to: { path: "\.test\.ts$" },
    },
  ],
  options: {
    doNotFollow: { path: "node_modules" },
    tsPreCompilationDeps: true,
    tsConfig: { fileName: "tsconfig.json" },
    exclude: { path: "(^dist/|node_modules)" },
  },
};
