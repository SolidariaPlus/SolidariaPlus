// Conventional Commits + clave Jira obligatoria en cada mensaje.
module.exports = {
  extends: ["@commitlint/config-conventional"],
  plugins: [
    {
      rules: {
        "jira-key": ({ header, body }) => {
          const text = `${header ?? ""}\n${body ?? ""}`;
          return [
            /\b[A-Z][A-Z0-9]+-\d+\b/.test(text),
            "el mensaje debe incluir la clave Jira (p.ej. SP-123)",
          ];
        },
      },
    },
  ],
  rules: {
    "jira-key": [2, "always"],
    "header-max-length": [2, "always", 100],
  },
};
