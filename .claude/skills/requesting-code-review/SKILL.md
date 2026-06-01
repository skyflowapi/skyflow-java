---
name: requesting-code-review
description: Use when completing tasks, implementing major features, or before merging to verify work meets requirements
paths:
  - src/main/java/**/*.java
  - src/test/java/**/*.java
exclude:
  - src/main/java/com/skyflow/generated/**
context: fork
---

# Requesting Code Review

**Core principle:** Review early, review often. Review after each task — catch issues before they compound.

## When to Request Review

**Mandatory:**
- After each task in subagent-driven development
- After completing a major feature
- Before merge to main

**Optional but valuable:**
- When stuck (fresh perspective)
- Before refactoring (baseline check)
- After fixing a complex bug

## How to Request

**1. Pick the right command:**

| Change type | Command |
|---|---|
| SDK logic, patterns, naming, tests | `/code-review` — SDK checks + smell + security |
| Structural debt only | `/code-smell` — standalone smell analysis |
| Auth, credentials, tokens, HTTP | `/code-security` — standalone security audit |
| Compile + tests + 100% coverage | `/code-quality` — run after fixing review findings, before `/commit` |

For security-sensitive changes, run both:
```bash
/code-review src/main/java/com/skyflow/
/code-security src/main/java/com/skyflow/
```

All review rules, severity definitions, output format, and post-review steps are defined in `.claude/commands/code-review.md` — that file is the single source of truth.
