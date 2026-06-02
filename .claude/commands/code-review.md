---
name: code-review
description: Full code review — SDK patterns, naming, test coverage, then runs /code-smell and /code-security.
paths:
  - src/main/java/**/*.java
  - src/test/java/**/*.java
exclude:
  - src/main/java/com/skyflow/generated/**
context: fork
---

You are a senior engineer performing a thorough code review on the Skyflow Java SDK.

## Pre-requisite

If `GITHUB_ACTIONS` environment variable is set, skip this step (CI runs compile/test in a separate job).

Otherwise, confirm `/code-quality` has been run and passed (compile, tests, 100% coverage). If it has not been run, run it now before proceeding with the review.

## Scope

Use `$ARGUMENTS` to determine scope:
- `full review` — scan all files under `src/main/java/com/skyflow/` recursively (exclude `generated/`)
- A file or directory path — review only that path
- Empty / default — review files changed on current PR/branch vs base:
  ```bash
  # CI: GITHUB_BASE_REF is set (e.g. "main") — use origin/ prefix
  # Local: unset — use main directly
  BASE="${GITHUB_BASE_REF:+origin/$GITHUB_BASE_REF}"
  BASE="${BASE:-main}"
  git diff "$BASE"...HEAD --name-only | grep '\.java$' | grep -v 'generated'
  ```

---

## Step 1 — SDK Pattern Review

Review all files in scope against the rules defined in `CLAUDE.md` (loaded automatically from the project root). Check every rule category: naming conventions, error handling, request/response patterns, string literals, tests, and code quality.

Group findings by file and produce a table:

```
### path/to/File.java

| Severity | Line | Finding |
|----------|------|---------|
| Critical | 42   | SkyflowException swallowed in catch block |
| Bug      | 87   | skyflow_id not normalised to skyflowId |
| Quality  | 103  | Magic string "records" — use Constants |
```

**Severities:**

| Level | Meaning |
|---|---|
| **Critical** | Data loss, silent failure, security risk — must fix before merge |
| **Bug** | Wrong behaviour, incorrect output — must fix before merge |
| **Edge Case** | Unhandled input that will cause runtime failure — fix before merge |
| **Quality** | Maintainability issue, naming violation, missing pattern — fix before merge |

---

## Step 2 — Code Smell Analysis

Read the file `.claude/commands/code-smell.md` and follow all of its instructions for the same files in scope. Produce its full output (per-file smell table + smell summary + recommendation).

---

## Step 3 — Security Audit

Read the file `.claude/commands/code-security.md` and follow all of its instructions for the same files in scope. Produce its full output (per-finding blocks + summary table + overall risk rating).

---

## Final Verdict

After all three steps, close with:
1. A tech-debt summary table grouped by category (SDK Patterns / Error Handling / Naming / Tests / Smells / Security)
2. A verdict: `APPROVE` / `APPROVE WITH FIXES` / `REQUEST CHANGES`
3. Remind: run `/code-quality` again after any fixes before merging.
