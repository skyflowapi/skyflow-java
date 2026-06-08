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

When `GITHUB_ACTIONS` is set, your **entire output is the body of a code-review comment** — not a chat reply. The **first character must be the verdict**. Never emit any preamble, planning, narration, acknowledgement, "Now I have…/Let me…" line, restatement of the diff, or per-step headers. Describe the **review outcome**, not the PR.

Merge every finding from Steps 1–3 into one de-duplicated report (same issue flagged by multiple steps → keep once at the highest severity). Emit **exactly** the following, and nothing else.

**Rendering rules (GitHub markdown):** emit each part below as a **top-level block at the left margin**, separated by a blank line. The numbers are labels for you — do **not** reproduce them as a markdown numbered list, and do **not** indent the tables or `<details>` (tables/`<details>` nested inside list items do not render on GitHub). Every table needs a blank line before and after it, and `<details>` needs a blank line after the `</summary>` tag.

**Severity buckets (single source of truth; Category is a separate axis, never a severity):**
- **Blocking** (must fix before merge): `Critical`, `High`, `Medium`.
- **Advisory** (does not block merge): `Low`, `Info`.

1. **Verdict** (first line, nothing above it) — `REQUEST CHANGES` if ≥1 blocking finding; `APPROVE WITH FIXES` if only advisory; `APPROVE` if none. Follow it with **one short clause stating the theme/count only** — e.g. "— 2 error-handling issues on the changed lines." **Never enumerate or restate the individual findings**; the table already lists them.

2. **Blocking-findings table** — `Critical` / `High` / `Medium` only (never `Low` / `Info`). The `Finding` cell is a **terse identifier (≤ ~12 words, a noun phrase)** — no mechanism, no "because…", no fix; the full explanation lives in the inline comment, so never repeat it here. If the **same issue appears at multiple locations**, emit **one row** with the locations comma-separated in `File:Line` (the inline block still gets one entry per location) — do not create near-duplicate rows. Omit the table and write "No blocking findings on the changed lines." if there are none.
   ```
   | File:Line | Severity · Category | Finding |
   |-----------|---------------------|---------|
   | HttpUtility.java:88 | High · Correctness | getMessage() returns null on the no-body error path |
   | VaultClient.java:942, ConnectionClient.java:92 | Medium · Pattern | misleading EmptyCredentials message in generic catch |
   ```

3. **Advisory section (collapsed)** — every advisory finding, one crisp line each; `N` must equal the row count.
   ```
   <details><summary>Advisory (Low / Info) — N items</summary>

   | File:Line | Severity · Category | Finding |
   |-----------|---------------------|---------|
   </details>
   ```

4. **Inline-findings block** — the **very last thing** in your output, wrapped in an **HTML comment** so it never renders even if parsing fails. Its body is a JSON array of **only blocking findings whose line is an added (`+`) line** (never advisory items, never lines outside the diff). Put the **full explanation in `comment`** (this is what renders inline on the code); keep the summary table above terse.

   **Critical:** `comment` must be **plain text** — you may use single backticks for short identifiers, but **never triple backticks / code fences / `\`\`\`` anywhere inside the JSON** (they corrupt extraction). Describe the fix in prose, not a code block.

   Use exactly these sentinels (emit `[]` for the array if there are no inline findings):

   ```
   <!-- ai-review-inline
   [{ "path": "src/main/java/com/skyflow/Foo.java", "line": 42, "severity": "High", "category": "Correctness", "comment": "skyflow_id is not normalised to skyflowId before returning; normalise it in the controller." }]
   -->
   ```

   The workflow extracts this block, strips it from the visible summary, and renders items 1–3 as the review body.
