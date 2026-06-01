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

Before starting the review, confirm `/code-quality` has been run and passed (compile, tests, 100% coverage). If it has not been run, run it now before proceeding with the review.

## Scope

Use `$ARGUMENTS` to determine scope:
- `full review` — scan all files under `src/main/java/com/skyflow/` recursively (exclude `generated/`)
- A file or directory path — review only that path
- Empty / default — review files changed on current branch vs `main`:
  ```bash
  git diff main...HEAD --name-only | grep '\.java$' | grep -v 'generated'
  ```

---

## Step 1 — SDK Pattern Review

Read the file `.claude/commands/code-patterns.md` and follow all of its instructions for the same files in scope. Produce its full output (per-file findings table + severity key).

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
