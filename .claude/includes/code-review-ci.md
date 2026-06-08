# Code Review — PR / CI Output Spec

> Loaded by `/code-review` (`.claude/commands/code-review.md`) **only when `GITHUB_ACTIONS` is set**.
> This is a plain include, not a slash command — it has no frontmatter and is not in `commands/`,
> so it cannot be invoked directly. It defines the consolidated review-comment format the CI
> workflow extracts and posts.

When `GITHUB_ACTIONS` is set, your **entire output is the body of a code-review comment** — not a chat reply. The **first character must be the verdict**. Never emit any preamble, planning, narration, acknowledgement, "Now I have…/Let me…" line, restatement of the diff, or per-step headers. Describe the **review outcome**, not the PR.

Merge every finding from Steps 1–3 into one de-duplicated report (same issue flagged by multiple steps → keep once at the highest severity). Emit **exactly** the following, and nothing else.

**Rendering rules (GitHub markdown):** emit each part below as a **top-level block at the left margin**, separated by a blank line. The numbers are labels for you — do **not** reproduce them as a markdown numbered list, and do **not** indent the tables or `<details>` (tables/`<details>` nested inside list items do not render on GitHub). Every table needs a blank line before and after it, and `<details>` needs a blank line after the `</summary>` tag.

**Severity buckets (single source of truth; Category is a separate axis, never a severity):**
- **Blocking** (must fix before merge): `Critical`, `High`, `Medium`.
- **Advisory** (does not block merge): `Low`, `Info`.

1. **Verdict** — emit **exactly one** verdict line, as the very first line and nowhere else: `REQUEST CHANGES` if ≥1 blocking finding; `APPROVE WITH FIXES` if only advisory; `APPROVE` if none. Follow it with **one short clause naming the count + theme(s)** — e.g. `REQUEST CHANGES — 5 blocking findings in ReviewProbe.java (error handling, naming, magic string).` **Never** write a second verdict line, repeat/rephrase the verdict, or enumerate the individual findings (the table lists them).

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
