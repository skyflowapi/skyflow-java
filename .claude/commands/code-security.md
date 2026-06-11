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

You are a security engineer auditing the Skyflow Java SDK for vulnerabilities.

## Audit Scope

Use `$ARGUMENTS` to determine target files. If none provided, run:
```bash
# CI: GITHUB_BASE_REF is set (e.g. "main") — use origin/ prefix
# Local: unset — use main directly
BASE="${GITHUB_BASE_REF:+origin/$GITHUB_BASE_REF}"
BASE="${BASE:-main}"
git diff "$BASE"...HEAD --name-only | grep -E '\.java$|(^|/)pom\.xml$' | grep -v 'generated'
```

**If `GITHUB_ACTIONS` is set (PR review mode):** audit only the code this PR changed. Work from the diff — **note the pathspec includes `pom.xml` so dependency changes are never invisible to the audit:**
```bash
git diff "$BASE"...HEAD -- '*.java' 'pom.xml' | grep -v 'src/main/java/com/skyflow/generated/'
```
Report a finding **only if an added line (`+` prefix) introduces or directly exposes it.** Do not raise pre-existing vulnerabilities in unchanged code, and skip whole-project checks the diff does not touch. **The diff above includes `pom.xml`; whenever a changed `<dependency>` appears in it, you MUST run §6 against those lines — do not treat the audit as `.java`-only.** If the added lines introduce no security issues, state that explicitly rather than listing pre-existing risks. (Local / non-CI runs and explicit file arguments keep full-file auditing.)

## Security Checks

Where a finding maps to an **OWASP Top 10** category (e.g. `A01 — Broken Access Control`, `A06 — Vulnerable and Outdated Components`), tag it with that category in the output — only where it genuinely applies; don't force a mapping.

### 1. Credential and token exposure (Critical)
- Bearer tokens, API keys, and private keys must never appear in logs, error messages, exception messages, or `toString()` output
- `Credentials` fields (`path`, `token`, `apiKey`, `credentialsString`) must not be serialised to logs
- JWT claims must not be logged

### 2. Input validation (High)
- All string inputs from callers must be null/empty checked before use
- File paths passed to `new File(path)` must not allow path traversal (`../`)
- JSON strings parsed with `JsonParser` must be wrapped in try/catch for `JsonSyntaxException`

### 3. Credentials file handling (High)
- Credentials files must only be read from paths provided by the caller — no environment variable path injection without sanitisation
- `FileReader` must be in a try-with-resources or explicitly closed

### 4. HTTP security (Medium)
- All API calls must go over HTTPS — verify `Utils.getBaseURL` enforces this
- Authorization headers must not be logged at any log level
- HTTP timeouts must be configured

### 5. Error information leakage (Medium)
- `SkyflowException` messages must not include raw server response bodies that could contain PII
- Stack traces must not be surfaced to callers — wrap in `SkyflowException`

### 6. Dependency vulnerabilities (Critical)
- Flag any dependency with a known CVE (check `pom.xml` versions). Report at **Critical** severity so it surfaces in the serious-findings table and gets an inline comment on the changed `pom.xml` line.

### 7. Authentication lifecycle (Medium)
- Bearer token caching must check expiry before reuse
- Token refresh must be thread-safe (`synchronized` or equivalent)

## Account for every check

Before writing the report, walk checks **1–7 in order** against the changed lines and account for each one — do not report only the issues that first stand out. The Medium-severity categories (§4 HTTP, §5 error leakage, §7 auth lifecycle) and the dependency check (§6) are missed far more often than credential exposure (§1); give them equal scrutiny.

## Output Format

For each finding:

```
### path/to/File.java : line N

**Severity:** Critical / High / Medium / Low / Info
**Risk:** What an attacker could do
**Trigger:** Input or code path that triggers the vulnerability
**Fix:** Concrete remediation with code example
**CWE:** CWE-NNN
**OWASP:** Relevant OWASP Top 10 category, e.g. `A06 — Vulnerable and Outdated Components` — include only when the finding clearly maps to one; omit otherwise.
```

End with a summary table and overall risk rating.
