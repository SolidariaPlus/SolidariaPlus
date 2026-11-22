module.exports = {
  extends: ["@commitlint/config-conventional"],
  plugins: [
    {
      rules: {
        "jira-key": ({ header, body }) => {
          const text = `${header ?? ""}\n${body ?? ""}`;
          return [/\bKAN-\d+\b/.test(text), "the message must include the Jira key (e.g. KAN-123)"];
        },
      },
    },
  ],
  rules: {
    "jira-key": [2, "always"],
    "header-max-length": [2, "always", 100],
    "subject-case": [0],
  },
};
