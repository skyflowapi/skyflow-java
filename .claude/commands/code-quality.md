---
name: code-quality
description: Quality pipeline — compile, checkstyle, build, tests, coverage check. Pass a class name to target a single test class.
paths:
  - src/**/*.java
  - pom.xml
exclude:
  - src/main/java/com/skyflow/generated/**
context: fork
---

## Instructions

@../../../common/.claude/commands/java/code-quality.md

Apply the items below as additions to — and, where they conflict, overrides of — the embedded common baseline above.

## Skyflow additions

- **Test class example:** `$ARGUMENTS` targets a class like `BearerTokenTests`.
- **Baseline failures:** documented in the **Known Pre-existing Test Failures** table in `CLAUDE.md` — do not investigate; report only failures **beyond** that baseline.
- **Coverage scope (100% required):** `src/main/java/com/skyflow/vault/`, `.../config/`, `.../serviceaccount/`. Any gap on Claude-written or public-interface code is a blocker → **NEEDS FIXES**.
- **Checkstyle:** `failsOnError=false` in `pom.xml` — the build won't fail on violations, so scan the output for `[WARN]` checkstyle lines; `generated/` is excluded by pom config.
- **Edge cases:** for any class below 100% coverage, write concrete **JUnit 4** test method stubs (not full implementations) for each missing scenario.
