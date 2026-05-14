Run the Skyflow Java SDK quality pipeline.

Use `$ARGUMENTS` to target a specific test class (e.g. `BearerTokenTests`). If empty, run the full suite.

## Known Pre-existing Failures (not regressions)

Before reporting failures, check against this baseline:
- `HttpUtilityTests` — ALL tests fail (JDK 21 + PowerMock `InaccessibleObject` incompatibility)
- `TokenTests#testExpiredTokenForIsExpiredToken` — needs live credentials
- `VaultClientTests#testSetBearerTokenWithEnvCredentials` — needs `SKYFLOW_CREDENTIALS` env var
- `ConnectionClientTests#testSetBearerTokenWithEnvCredentials` — needs `SKYFLOW_CREDENTIALS` env var

Baseline: 374 tests, ~5 failures, ~4 errors. Only report failures **beyond** this baseline.

## Pipeline

### Step 1 — Compile
```bash
mvn compile -q 2>&1 | tail -20
```
Expected: no output (clean compile). Report any errors.

### Step 2 — Checkstyle
```bash
mvn checkstyle:check -q 2>&1 | tail -20
```
Note: `failsOnError=false` in pom.xml means the build will not fail even if violations exist — check the output for `[WARN]` checkstyle lines. Violations are excluded from `generated/` by pom config.

### Step 3 — Build
```bash
mvn package -DskipTests -q 2>&1 | tail -20
```
Expected: BUILD SUCCESS.

### Step 4 — Tests
If `$ARGUMENTS` is set:
```bash
mvn test -Dtest=$ARGUMENTS -q 2>&1 | tail -40
```
Otherwise:
```bash
mvn test -q 2>&1 | tail -40
```
Report: tests run, failures, errors. Flag any pre-existing failures separately from new ones.

### Step 5 — Coverage analysis
Flag any public interface class (`src/main/java/com/skyflow/vault/`, `src/main/java/com/skyflow/config/`, `src/main/java/com/skyflow/serviceaccount/`) that has no corresponding test file under `src/test/`.

For classes that do have tests, check whether each public method has at least one positive and one negative test case. List any gaps.

### Step 6 — Edge case identification
For any test class below complete coverage, identify missing scenarios:
- Null / empty inputs
- Invalid types / wrong enum values
- Concurrent / reuse scenarios
- Error paths (API rejection, network failure)

Write concrete JUnit 4 test method stubs (not full implementations) for each gap.

### Step 7 — Report

```
| Step | Status | Notes |
|---|---|---|
| Compile | ✅ / ❌ | ... |
| Checkstyle | ✅ / ❌ | ... |
| Build | ✅ / ❌ | ... |
| Tests | ✅ / ❌ | N passed, M failed |
| Coverage gaps | ... | list classes |
```

Conclude with **READY TO MERGE** or **NEEDS FIXES** and a prioritised fix list.
