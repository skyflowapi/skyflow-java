---
name: code-review
description: Full code review — SDK patterns, naming, test coverage, code smells, and security. Reads code-smell.md and code-security.md inline.
paths:
  - src/main/java/**/*.java
  - src/test/java/**/*.java
---

You are a senior engineer performing a thorough code review on the Skyflow Java SDK.

## Review Mode

Use `$ARGUMENTS` to determine scope:
- `full review` — scan all files under `src/main/java/com/skyflow/` recursively (exclude `generated/`)
- A file or directory path — review only that path
- Empty / default — review files changed on current branch vs `main`:
  ```bash
  git diff main...HEAD --name-only | grep '\.java$' | grep -v 'generated'
  ```

**Skip entirely:** `src/main/java/com/skyflow/generated/` — Fern-generated REST client, read-only.

---

## 1. Request / Response / Options patterns

- Request builders are plain data holders — validation happens in `Validations.validateXxxRequest()` inside the controller, not in `build()`. Flag if validation logic is duplicated outside `Validations`.
- Response getters returning `ArrayList<HashMap<String, Object>>` is the established SDK pattern — do not flag these as violations.
- All response classes must have `getErrors()` returning `null` (not absent) when no errors.
- No separate `*Options` classes exist — options are fields on the request builder itself.
- SDK must not add field-level null/empty validation on top of what the backend enforces. Only structural checks (`table == null`, `values == null`) are permitted.

---

## 2. Error handling

- All public methods must declare `throws SkyflowException`
- `SkyflowException` must be thrown (not swallowed) on invalid input
- No `System.out.println` or bare `e.printStackTrace()` — use `LogUtil`
- Catch blocks must not silently drop exceptions
- `catch (Exception e)` without re-throw or explicit handling is a critical issue

---

## 3. Naming conventions

- Classes: `PascalCase`
- Methods / fields: `camelCase` — acronyms as words: `skyflowId` not `skyflowID`, `tokenUri` not `tokenURI`, `downloadUrl` not `downloadURL`
- Constants: `UPPER_SNAKE_CASE`
- Builder setter methods: `setFooId()` not `setFooID()`
- Deprecated methods must use `@Deprecated(since = "x.x", forRemoval = true)` + `@deprecated` Javadoc with `{@link}` to the replacement

---

## 4. Response field normalisation

- All response maps must use `skyflowId` (camelCase), never `skyflow_id` (snake_case)
- `getErrors()` must be present on every response class

---

## 5. Test coverage

- Every public method must have at least one positive and one negative test
- Tests must use `Assert.assertEquals` / `Assert.assertNull` — not just `Assert.fail` guards
- No mocking of the production class under test
- Reflection-based tests on private methods are acceptable only when no public API exercises the method

---

## 6. Code quality

- No magic strings for API field names — use `Constants` or `ErrorMessage` enums
- No duplicate validation logic across request classes — belongs in `Validations`
- No `@SuppressWarnings` without a comment explaining why
- `LogUtil.printWarningLog` must be used for deprecation warnings, not `System.err`

---

## 7. Code smells

Code smells are structural signals — they may not need immediate fixes but must be flagged. Report them at **Smell** severity.

### Method & class size
- **Long method** — any method over 40 lines. Candidate for decomposition into private helpers.
- **Long class** — any class over 300 lines. May be taking on too many responsibilities.
- **Large parameter list** — more than 4 parameters on a method. Consider a config/options object.

### Responsibility violations
- **Business logic in Request/Response classes** — these are data holders. If a Request/Response class contains conditional logic beyond null-safe getters, flag it.
- **toString() with business logic** — `toString()` should only serialise state. Logic like field renaming, manual JSON construction, or conditional field injection belongs in the controller or formatter methods.
- **Validation outside Validations.java** — any `if (x == null) throw new SkyflowException(...)` outside `src/main/java/com/skyflow/utils/validations/` is misplaced.

### Control flow
- **Deep nesting** — more than 3 levels of `if`/`for`/`try` nesting. Extract inner blocks to named methods.
- **Long if-else chains** — more than 4 branches. Consider a map, switch, or polymorphism.
- **Null checks scattered** — multiple consecutive null guards that could be replaced with `Optional` or early return.

### Data
- **Magic numbers** — literal integers or sizes (e.g. `25`, `3600`, `100`) without a named constant. Use `Constants`.
- **Raw HashMap chains** — `HashMap<String, Object>` passed through more than 2 method boundaries without a typed wrapper or comment explaining why. Flag for awareness; don't require a fix.
- **Temporary field** — a class field that is only set in certain code paths and `null` the rest of the time. Should be a local variable or method parameter instead.

### Dead code
- **Unused private methods** — private methods with no callers.
- **Unused imports** — any `import` not referenced in the file.
- **Unreachable code** — code after `return`/`throw` in the same branch.
- **Commented-out code** — blocks of commented code without explanation. Remove or add a TODO with a ticket reference.

### Comments
- **Explains what, not why** — a comment that restates what the code does (`// get the vault ID`) is noise. Only flag comments that explain the *what* without adding *why*.
- **Stale comment** — a comment that contradicts the current code (e.g. references a removed parameter or old method name).

---

## Output Format

Group findings by file. For each file:

```
### path/to/File.java

| Severity   | Line | Finding                                                    |
|------------|------|------------------------------------------------------------|
| Critical   | 42   | SkyflowException swallowed in catch block                  |
| Bug        | 87   | skyflow_id not normalised to skyflowId                     |
| Quality    | 103  | Magic string "records" — use Constants                     |
| Smell      | 210  | toString() renames map keys — move to formatter method     |
| Smell      | 315  | Method is 58 lines — candidate for decomposition           |
```

**Severities:**
| Level | Meaning |
|---|---|
| **Critical** | Data loss, silent failure, security risk — must fix before merge |
| **Bug** | Wrong behaviour, incorrect output — must fix before merge |
| **Edge Case** | Unhandled input that will cause runtime failure — fix before merge |
| **Quality** | Maintainability issue, naming violation, missing pattern — fix before merge |
| **Smell** | Structural signal, technical debt — flag and track, fix when in the area |

End with:
1. A tech-debt summary table grouped by category (Error handling / Naming / Smells / Tests)
2. A verdict: `APPROVE` / `APPROVE WITH FIXES` / `REQUEST CHANGES`
