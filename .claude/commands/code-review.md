---
name: code-review
description: Full code review — SDK patterns, naming, test coverage, then runs /code-smell and /code-security.
paths:
  - src/main/java/**/*.java
  - src/test/java/**/*.java
---

You are a senior engineer performing a thorough code review on the Skyflow Java SDK.

## Scope

Use `$ARGUMENTS` to determine scope:
- `full review` — scan all files under `src/main/java/com/skyflow/` recursively (exclude `generated/`)
- A file or directory path — review only that path
- Empty / default — review files changed on current branch vs `main`:
  ```bash
  git diff main...HEAD --name-only | grep '\.java$' | grep -v 'generated'
  ```

**Skip entirely:** `src/main/java/com/skyflow/generated/` — Fern-generated REST client, read-only.

---

## Step 1 — SDK Pattern Review

Check the files in scope against the rules below.

### 1. Request / Response / Options patterns

- Request builders are plain data holders — validation happens in `Validations.validateXxxRequest()` inside the controller, not in `build()`. Flag if validation logic is duplicated outside `Validations`.
- Response getters returning `ArrayList<HashMap<String, Object>>` is the established SDK pattern — do not flag these as violations.
- All response classes must have `getErrors()` returning `null` (not absent) when no errors.
- No separate `*Options` classes exist — options are fields on the request builder itself.
- SDK must not add field-level null/empty validation on top of what the backend enforces. Only structural checks (`table == null`, `values == null`) are permitted.

### 2. Error handling

- All public methods must declare `throws SkyflowException`
- `SkyflowException` must be thrown (not swallowed) on invalid input
- No `System.out.println` or bare `e.printStackTrace()` — use `LogUtil`
- Catch blocks must not silently drop exceptions
- `catch (Exception e)` without re-throw or explicit handling is a critical issue

### 3. Naming conventions and response field normalisation

Follow the conventions in CLAUDE.md under "Naming Conventions". Key enforcement points:
- Acronyms as words: `skyflowId`, `tokenUri`, `clientId` — never uppercase abbreviations
- Builder setters: `setFooId()` not `setFooID()`; constants: `UPPER_SNAKE_CASE`; classes: `PascalCase`
- Response maps: `skyflowId` (camelCase) only — never `skyflow_id`; `getErrors()` must be present on every response class
- Deprecated methods: `@Deprecated(since = "x.x", forRemoval = true)` + `@deprecated` Javadoc with `{@link}` to replacement

### 5. Test coverage

- Every public method must have at least one positive and one negative test
- Tests must use `Assert.assertEquals` / `Assert.assertNull` — not just `Assert.fail` guards
- No mocking of the production class under test
- Reflection-based tests on private methods are acceptable only when no public API exercises the method

### 6. Code quality

- No magic strings for API field names — use `Constants` or `ErrorMessage` enums
- No duplicate validation logic across request classes — belongs in `Validations`
- No `@SuppressWarnings` without a comment explaining why
- `LogUtil.printWarningLog` must be used for deprecation warnings, not `System.err`

### Output for Step 1

Group findings by file:

```
### path/to/File.java

| Severity   | Line | Finding                                                    |
|------------|------|------------------------------------------------------------|
| Critical   | 42   | SkyflowException swallowed in catch block                  |
| Bug        | 87   | skyflow_id not normalised to skyflowId                     |
| Quality    | 103  | Magic string "records" — use Constants                     |
```

**Severities:**
| Level | Meaning |
|---|---|
| **Critical** | Data loss, silent failure, security risk — must fix before merge |
| **Bug** | Wrong behaviour, incorrect output — must fix before merge |
| **Edge Case** | Unhandled input that will cause runtime failure — fix before merge |
| **Quality** | Maintainability issue, naming violation, missing pattern — fix before merge |

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
