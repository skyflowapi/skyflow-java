---
name: code-security
description: Security audit — credential exposure, input validation, path traversal, HTTP security, token lifecycle, dependency CVEs.
paths:
  - src/main/java/com/skyflow/**/*.java
  - pom.xml
exclude:
  - src/main/java/com/skyflow/generated/**
context: fork
---

## Instructions

@../../../common/.claude/commands/java/code-security.md

Apply the items below as additions to — and, where they conflict, overrides of — the embedded common baseline above.

## Skyflow additions

- **Scope override (CI-aware):** for the default empty-`$ARGUMENTS` case, derive the base from the environment and include `pom.xml`:
  ```bash
  BASE="${GITHUB_BASE_REF:+origin/$GITHUB_BASE_REF}"
  BASE="${BASE:-main}"
  git diff "$BASE"...HEAD --name-only | grep -E '\.java$|(^|/)pom\.xml$' | grep -v 'generated'
  ```
  When `GITHUB_ACTIONS` is set, audit only added (`+`) lines, and run the dependency-CVE check (§6) against any changed `<dependency>` line.
- **Concrete bindings for the generic checks:**
  - §1 — `Credentials` fields (`path`, `token`, `apiKey`, `credentialsString`) and JWT claims must never be logged.
  - §2 — `JsonParser` calls wrapped in try/catch for `JsonSyntaxException`.
  - §3 — credentials files read only from caller-provided paths; `FileReader` in try-with-resources.
  - §4 — verify `Utils.getBaseURL` enforces HTTPS.
  - §5 — `SkyflowException` messages must not include raw server response bodies / PII; wrap stack traces in `SkyflowException`.
  - §7 — Bearer token caching checks expiry before reuse; refresh is thread-safe (`synchronized` or equivalent).
