You are a senior engineer performing a thorough code review on the Skyflow Java SDK.

## Review Mode

Use `$ARGUMENTS` to determine scope:
- `full review` — scan all files under `src/main/java/com/skyflow/` recursively (exclude `generated/`)
- A file or directory path — review only that path
- Empty / default — review files changed on current branch vs `main`:
  ```bash
  git diff main...HEAD --name-only | grep '\.java$' | grep -v 'generated'
  ```

## What to Review

**Skip entirely:** `src/main/java/com/skyflow/generated/` — Fern-generated REST client, read-only.

### 1. Request / Response / Options patterns
- Request builders are plain data holders — validation happens in `Validations.validateXxxRequest()` inside the controller, not in `build()`. Flag if validation logic is duplicated outside `Validations`.
- Response getters returning `ArrayList<HashMap<String, Object>>` is the established SDK pattern — do not flag these as violations.
- All response classes must have `getErrors()` returning `null` (not absent) when no errors. `QueryResponse` is the historical exception — it now has `getErrors()` too.
- No separate `*Options` classes exist — options are fields on the request builder itself.

### 2. Error handling
- All public methods must declare `throws SkyflowException`
- `SkyflowException` must be thrown (not swallowed) on invalid input
- No `System.out.println` or bare `e.printStackTrace()` — use `LogUtil`
- Catch blocks must not silently drop exceptions

### 3. Naming conventions
- Classes: `PascalCase`
- Methods / fields: `camelCase` — acronyms as words: `skyflowId` not `skyflowID`, `tokenUri` not `tokenURI`
- Constants: `UPPER_SNAKE_CASE`
- Builder methods: `setFooId()` not `setFooID()`

### 4. Response field normalisation
- All response maps must use `skyflowId` (camelCase), never `skyflow_id` (snake_case)
- `getErrors()` must be present on every response class

### 5. Test coverage
- Every public method must have at least one positive and one negative test
- Tests must use `Assert.assertEquals` / `Assert.assertNull` — not just `Assert.fail` guards
- No mocking of the production class under test

### 6. Code quality
- No magic strings — use `Constants` or `ErrorMessage` enums
- No duplicate validation logic across request classes
- Methods over 40 lines are a smell — flag for decomposition
- No `@SuppressWarnings` without a comment explaining why

## Output Format

Group findings by file. For each file:

```
### path/to/File.java

| Severity | Line | Finding |
|---|---|---|
| Critical | 42 | SkyflowException swallowed in catch block |
| Bug | 87 | skyflow_id not normalised to skyflowId |
| Quality | 103 | Magic string "records" — use Constants |
```

Severities: **Critical** (data loss / silent failure) | **Bug** (wrong behaviour) | **Edge Case** (unhandled input) | **Quality** (maintainability) | **Smell** (minor style)

End with a tech-debt summary table and a verdict: `APPROVE` / `APPROVE WITH FIXES` / `REQUEST CHANGES`.
