module.exports = {
  forbidden: [
    { name: "no-circular", severity: "error", from: {}, to: { circular: true } },
    {
      name: "no-ui-frameworks",
      severity: "error",
      from: { path: "^src" },
      to: { path: "(^|/)node_modules/(react|react-dom|react-native|electron|expo|@mui)(/|$)" },
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
