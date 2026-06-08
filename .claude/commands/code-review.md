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
  # REVIEW_BASE_SHA, when set (CI incremental review), is the last commit already
  # reviewed by the bot — diff only lines added since then. Otherwise fall back to
  # the PR base branch.
  BASE="${REVIEW_BASE_SHA:-${GITHUB_BASE_REF:+origin/$GITHUB_BASE_REF}}"
  BASE="${BASE:-main}"
  git diff "$BASE"...HEAD --name-only | grep '\.java$' | grep -v 'generated'
  ```
  **If `GITHUB_ACTIONS` is set:** work from the diff output directly (changed lines only) instead of reading full files:
  ```bash
  git diff "$BASE"...HEAD -- '*.java' | grep -v 'src/main/java/com/skyflow/generated/'
  ```
  Review only added lines (`+` prefix) from the diff. Do not comment on unchanged context lines or pre-existing code.

---

## Step 1 — SDK Pattern Review

Review all files in scope against the rules defined in `CLAUDE.md` (loaded automatically from the project root). Check every rule category: naming conventions, error handling, request/response patterns, string literals, tests, and code quality.

Group findings by file and produce a table:

```
### path/to/File.java

| Severity | Category | Line | Finding |
|----------|----------|------|---------|
| Critical | Security    | 42  | SkyflowException swallowed in catch block |
| High     | Correctness | 87  | skyflow_id not normalised to skyflowId |
| Low      | Pattern     | 103 | Magic string "records" — use Constants |
```

Every finding has **two independent axes** — don't conflate them:

**Severity** — *how serious* (one scale shared by all three steps):

| Severity | Meaning | Blocks merge? |
|---|---|---|
| **Critical** | Data loss, security breach, silent failure | Yes |
| **High** | Wrong behaviour / bug / guaranteed runtime failure | Yes |
| **Medium** | Likely problem, risky or unhandled input, missing safeguard | Yes |
| **Low** | Minor maintainability, naming, style, code smell | No — advisory |
| **Info** | Note / FYI | No — advisory |

**Category** — *what kind*: `Correctness` (a bug), `Edge case`, `Security`, `Pattern`, `Naming`, `Tests`, `Smell`.

A logic bug is **Severity `High`/`Critical` + Category `Correctness`** — never severity "Bug". A magic string is **Severity `Low` + Category `Pattern`** — never severity "Quality". Keep level in the Severity column and kind in the Category column.

---

## Step 2 — Code Smell Analysis

Read the file `.claude/commands/code-smell.md` and follow all of its instructions for the same files in scope. Produce its full output (per-file smell table + smell summary + recommendation).

**If `GITHUB_ACTIONS` is set:** apply that command's **PR / CI mode** — report only smells introduced by added (`+`) lines; do not report whole-file metrics (Long class/method, large parameter list) or any pre-existing debt. Do **not** print code-smell's standalone tables, summary, or recommendation — collect its findings into the single consolidated report defined in **Output (PR / CI mode)** below.

---

## Step 3 — Security Audit

Read the file `.claude/commands/code-security.md` and follow all of its instructions for the same files in scope. Produce its full output (per-finding blocks + summary table + overall risk rating).

**If `GITHUB_ACTIONS` is set:** apply that command's **PR / CI mode** — report only issues introduced by added (`+`) lines; do not raise pre-existing vulnerabilities or whole-project checks the diff does not touch. Do **not** print code-security's standalone per-finding blocks, summary, or risk rating — collect its findings into the single consolidated report defined in **Output (PR / CI mode)** below.

---

## Final Verdict (local mode only)

> Skip this section when `GITHUB_ACTIONS` is set — use **Output (PR / CI mode)** instead.

After all three steps, close with:
1. A tech-debt summary table grouped by category (SDK Patterns / Error Handling / Naming / Tests / Smells / Security)
2. A verdict: `APPROVE` / `APPROVE WITH FIXES` / `REQUEST CHANGES`
3. Remind: run `/code-quality` again after any fixes before merging.

---

## Output (PR / CI mode)

**If `GITHUB_ACTIONS` is set:** read `.claude/includes/code-review-ci.md` and produce **exactly** the consolidated code-review-comment report it specifies — nothing else. Do **not** emit the local-mode **Final Verdict** above. (This file is fetched only in CI; local runs never read it.)

Otherwise (local mode), this section does not apply — use the **Final Verdict (local mode)** section above.
