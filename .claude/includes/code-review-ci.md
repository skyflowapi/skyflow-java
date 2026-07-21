# Code Review — PR / CI Output Spec

> Loaded by `/code-review` (`.claude/commands/code-review.md`) **only when `GITHUB_ACTIONS` is set**.
> This is a plain include, not a slash command — it has no frontmatter and is not in `commands/`,
> so it cannot be invoked directly. It defines the consolidated review-comment format the CI
> workflow extracts and posts.

When `GITHUB_ACTIONS` is set, your **entire output is the body of a code-review comment** — not a chat reply. The **first line must be the findings-count summary** (item 1 below). Never emit any preamble, planning, narration, acknowledgement, "Now I have…/Let me…" line, restatement of the diff, or per-step headers.

**Do NOT emit any verdict** — no `APPROVE`, `APPROVE WITH FIXES`, `REQUEST CHANGES`, or any pass/fail statement. CI reviews are advisory; merge gating is handled by GitHub branch protection, not by this comment.

Merge every finding from Steps 1–3 into one de-duplicated report (same issue flagged by multiple steps → keep once at the highest severity).

**If the changed lines introduce no findings at all, output exactly `<!-- ai-review-empty -->` and nothing else** — the workflow posts no comment in that case. Otherwise emit **exactly** the following, and nothing else.

**Rendering rules (GitHub markdown):** emit each part below as a **top-level block at the left margin**, separated by a blank line. The numbers are labels for you — do **not** reproduce them as a markdown numbered list, and do **not** indent the tables or `<details>` (tables/`<details>` nested inside list items do not render on GitHub). Every table needs a blank line before and after it, and `<details>` needs a blank line after the `</summary>` tag.

**Severity (a separate axis from Category; used for the tables, not for any verdict):**
- **Blocking-worth** (serious): `Critical`, `High`, `Medium`.
- **Advisory** (minor): `Low`, `Info`.

1. **Findings summary (first line)** — a single line counting findings by type on the changed lines, in this exact style (omit any type whose count is 0; if every count is 0 you should have emitted the empty marker instead):

   `**Findings on the changed lines:** 2 bugs, 2 security, 3 smells, 1 other`

   Count by Category:
   - **bugs** = `Correctness` + `Edge case`
   - **security** = `Security`
   - **smells** = `Smell`
   - **other** = `Pattern` + `Naming` + `Tests`

   Do not restate individual findings on this line — the tables below list them.

2. **Serious-findings table** — `Critical` / `High` / `Medium` only (never `Low` / `Info`). The `Finding` cell is a **terse identifier (≤ ~12 words, a noun phrase)** — no mechanism, no "because…", no fix; the full explanation lives in the inline comment, so never repeat it here. If the **same issue appears at multiple locations**, emit **one row** with the locations comma-separated in `File:Line` (the inline block still gets one entry per location) — do not create near-duplicate rows. Omit the table and write "No serious findings on the changed lines." if there are none.
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

4. **Inline-findings block** — the **very last thing** in your output, wrapped in an **HTML comment** so it never renders even if parsing fails. Its body is a JSON array of **only serious (Critical/High/Medium) findings whose line is an added (`+`) line** (never advisory items, never lines outside the diff). Put the **full explanation in `comment`** (this is what renders inline on the code); keep the summary table above terse.

   **Critical:** `comment` must be **plain text** — you may use single backticks for short identifiers, but **never triple backticks / code fences / `\`\`\`` anywhere inside the JSON** (they corrupt extraction). Describe the fix in prose, not a code block.

   Use exactly these sentinels (emit `[]` for the array if there are no inline findings):

   ```
   <!-- ai-review-inline
   [{ "path": "src/main/java/com/skyflow/Foo.java", "line": 42, "severity": "High", "category": "Correctness", "comment": "skyflow_id is not normalised to skyflowId before returning; normalise it in the controller." }]
   -->
   ```

   The workflow extracts this block, strips it from the visible summary, and renders items 1–3 as the review body.
