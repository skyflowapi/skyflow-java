---
name: requesting-code-review
description: Use when completing tasks, implementing major features, or before merging to verify work meets requirements
paths:
  - src/main/java/**/*.java
  - src/test/java/**/*.java
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
/code-review src/main/java/com/skyflow/serviceaccount/
/code-security src/main/java/com/skyflow/serviceaccount/
```

**2. Fork context — dispatch a subagent reviewer:**

The commands above run in the current session and share your context. For an independent second opinion (no confirmation bias, preserved main context window), dispatch a fresh subagent:

```
Agent tool (general-purpose):
  description: "SDK code review"
  prompt: |
    You are a senior engineer reviewing the Skyflow Java SDK.

    Read CLAUDE.md for project conventions, then read and follow
    .claude/commands/code-review.md for the full review process
    including all rules, output format, and act-on-feedback guidance.

    Git range to review:
      Base: {BASE_SHA}
      Head: {HEAD_SHA}

    Run:
      git diff --stat {BASE_SHA}..{HEAD_SHA}
      git diff {BASE_SHA}..{HEAD_SHA}

    Description of what was implemented:
      {DESCRIPTION}
```

Get the SHAs:
```bash
BASE_SHA=$(git merge-base main HEAD)   # branch vs main
HEAD_SHA=$(git rev-parse HEAD)
```

All review rules, severity definitions, output format, and post-review steps are defined in `.claude/commands/code-review.md` — that file is the single source of truth.
