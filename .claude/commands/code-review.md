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

## Instructions

@../../../common/.claude/commands/java/code-review.md

Apply the items below as additions to — and, where they conflict, overrides of — the embedded common baseline above.

## Skyflow additions

- **Pre-requisite (local runs only):** unless `GITHUB_ACTIONS` is set, confirm `/code-quality` has passed before reviewing.
- **Scope override (CI-aware):** for the default empty-`$ARGUMENTS` case, replace the common scope command with the base-aware one and include `pom.xml`:
  ```bash
  # REVIEW_BASE_SHA = last commit already reviewed by the bot (incremental CI review)
  BASE="${REVIEW_BASE_SHA:-${GITHUB_BASE_REF:+origin/$GITHUB_BASE_REF}}"
  BASE="${BASE:-main}"
  git diff "$BASE"...HEAD --name-only | grep -E '\.java$|(^|/)pom\.xml$' | grep -v 'generated'
  ```
  When `GITHUB_ACTIONS` is set, work from `git diff "$BASE"...HEAD -- '*.java' 'pom.xml'` and review only added (`+`) lines — never unchanged context.
- **SDK Pattern Review:** in addition to the generic Java rules, review every file against the rules in `CLAUDE.md` (naming, error handling, request/response patterns, string literals, tests). Report SDK-rule violations under Category `Pattern`.
- **Steps 2 & 3 (chaining):** run this repo's `.claude/commands/code-smell.md` then `.claude/commands/code-security.md` over the same scope — these supersede the common file's generic "if the project has a command" pointers.
- **CI output:** if `GITHUB_ACTIONS` is set, read `.claude/includes/code-review-ci.md` and emit **exactly** the consolidated report it specifies — skip the common file's local-mode final verdict.
