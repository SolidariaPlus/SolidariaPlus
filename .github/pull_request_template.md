<!--
The PR title must follow Conventional Commits with the Jira key at the start of the subject,
e.g. "feat(credit): KAN-123 add loan simulator". With squash merge the title becomes the commit
on the target branch, and GitHub for Jira uses it to link deployments (AR-02 / AR-03).
The branch must be the one Jira creates: feature/kan-123-<summary>.
-->

## Jira

KAN-XXX

## Description

<!-- What changes and why -->

## Type of change

- [ ] feat
- [ ] fix
- [ ] refactor / chore
- [ ] docs
- [ ] infra

## Checklist

- [ ] The branch, the commits, and the PR title include the Jira key.
- [ ] Tests added/updated and passing
- [ ] Updated contracts (`contracts/openapi`, `contracts/asyncapi`) if applicable.
- [ ] No secrets, credentials, or real personal data (RNF-SEG-05, RNF-PRIV-04)
- [ ] README / updated documentation, if applicable
