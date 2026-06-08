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
  BASE="${GITHUB_BASE_REF:+origin/$GITHUB_BASE_REF}"
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

| Severity | Line | Finding |
|----------|------|---------|
| Critical | 42   | SkyflowException swallowed in catch block |
| Bug      | 87   | skyflow_id not normalised to skyflowId |
| Quality  | 103  | Magic string "records" — use Constants |
```

**Severities:**

| Level | Meaning | Blocks merge? |
|---|---|---|
| **Critical** | Data loss, silent failure, security risk | Yes |
| **Bug** | Wrong behaviour, incorrect output | Yes |
| **Edge Case** | Unhandled input that will cause runtime failure | Yes |
| **Quality** | Maintainability issue, naming violation, missing pattern | No — advisory |

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

When `GITHUB_ACTIONS` is set, **do not** print the three steps' standalone tables/summaries/verdicts. Merge every finding from Steps 1–3 into a single de-duplicated report (if the same issue is flagged by more than one step, keep it once with the highest severity). Emit **exactly** the following, and nothing else.

**Severity buckets (single source of truth for this mode — every finding is exactly one):**
- **Blocking** (must fix before merge): `Critical`, `Bug`, `Edge Case`, `High`, `Medium`.
- **Advisory** (does not block merge): `Quality`, `Smell`, `Low`, `Info`.

1. **One-line verdict** — `REQUEST CHANGES` if there is **≥1 blocking** finding; `APPROVE WITH FIXES` if there are only advisory findings; `APPROVE` if there are none. Add a one-sentence rationale.

2. **Blocking-findings table** — blocking severities only. A `Quality` / `Smell` / `Low` / `Info` finding must **never** appear here. Omit the table and say "No blocking findings on the changed lines." if there are none.
   ```
   | File:Line | Severity | Category | Finding |
   |-----------|----------|----------|---------|
   ```

3. **Advisory section (collapsed)** — every advisory finding (and only advisory ones). The count `N` must match the row count.
   ```
   <details><summary>Advisory (Quality / Smell / Low / Info) — N items</summary>

   | File:Line | Severity | Finding |
   |-----------|----------|---------|
   </details>
   ```

4. **An inline-findings block** — a fenced ```` ```json:inline ```` block whose body is a JSON array of the findings that should be posted as inline review comments. Include **only blocking findings (step 2) whose line is an added (`+`) line in the diff** — never non-blocking items, never lines outside the diff. Each entry:
   ```json:inline
   [{ "path": "src/main/java/com/skyflow/Foo.java", "line": 42, "severity": "Bug", "comment": "skyflow_id not normalised to skyflowId" }]
   ```
   Emit `[]` if there are none. The workflow parses this block to attach inline comments and renders items 1–3 as the review summary; keep it as the **last** thing in the output.

Be concise: no preamble, no restating the diff, no per-step headers.
