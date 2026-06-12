---
name: code-smell
description: Structural smell analysis + spell check — long methods, dead code, misplaced validation, deep nesting, magic numbers. Does not check patterns or security.
paths:
  - src/main/java/**/*.java
  - src/test/java/**/*.java
  - .claude/**/*.md
  - docs/**/*.md
exclude:
  - src/main/java/com/skyflow/generated/**
context: fork
---

## Instructions

@../../../common/.claude/commands/java/code-smell.md

Apply the items below as additions to — and, where they conflict, overrides of — the embedded common baseline above.

## Skyflow additions

- **Scope override (CI-aware):** for the default empty-`$ARGUMENTS` case, derive the base from the environment:
  ```bash
  BASE="${GITHUB_BASE_REF:+origin/$GITHUB_BASE_REF}"
  BASE="${BASE:-main}"
  git diff "$BASE"...HEAD --name-only | grep '\.java$' | grep -v 'generated'
  ```
- **PR / CI mode:** when `GITHUB_ACTIONS` is set, report a smell only if an added (`+`) line introduces it; never flag whole-file metrics (Long class/method, large parameter list) or pre-existing debt unless the diff itself creates them; if the added lines introduce no smells, state **"No new smells introduced by this PR."**
- **Threshold & rule overrides:**
  - Long class: flag at **300 lines** (not the common 400).
  - Misplaced validation: any `if (x == null) throw new SkyflowException(...)` outside `src/main/java/com/skyflow/utils/validations/` belongs in `Validations.validateXxxRequest()`.
  - Magic numbers: extract to `Constants` (e.g. `Constants.MAX_QUERY_RECORDS`).
