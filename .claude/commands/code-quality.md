---
name: code-quality
description: Quality pipeline — compile, checkstyle, build, tests, coverage check. Pass a class name to target a single test class.
paths:
  - src/**/*.java
  - pom.xml
---

Run the Skyflow Java SDK quality pipeline.

Use `$ARGUMENTS` to target a specific test class (e.g. `BearerTokenTests`). If empty, run the full suite.

> Baseline failures are listed in CLAUDE.md under "Known Pre-existing Test Failures".
> Do not investigate them unless specifically asked. Only report failures **beyond** that baseline.

## Coverage Requirements

**All code written or modified by Claude must have 100% coverage — both instruction and branch.**

**All public interfaces must have 100% coverage — both instruction and branch.** This applies to:
- All classes under `src/main/java/com/skyflow/vault/` (controllers, data, tokens, connection, audit, bin, detect)
- All classes under `src/main/java/com/skyflow/config/`
- All classes under `src/main/java/com/skyflow/serviceaccount/`

Flag any gap as a blocker — **NEEDS FIXES** if coverage is below 100% on Claude-written or public interface code.

---

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
For every public interface class and every class touched by Claude in this session:
- Check for a corresponding test file under `src/test/`
- Check that every public method has at least one positive and one negative test case
- Check that every branch (if/else, switch, try/catch) is covered

List all gaps. Any gap on Claude-written or public interface code is a **blocker**.

### Step 6 — Edge case identification
For any class below 100% coverage, identify missing scenarios:
- Null / empty inputs
- Invalid types / wrong enum values
- Concurrent / reuse scenarios
- Error paths (API rejection, network failure)

Write concrete JUnit 4 test method stubs (not full implementations) for each gap.

### Step 7 — Report

```
| Step             | Status    | Notes                        |
|------------------|-----------|------------------------------|
| Compile          | ✅ / ❌   | ...                          |
| Checkstyle       | ✅ / ❌   | ...                          |
| Build            | ✅ / ❌   | ...                          |
| Tests            | ✅ / ❌   | N passed, M failed           |
| Coverage (100%)  | ✅ / ❌   | list classes with gaps       |
```

Conclude with **READY TO MERGE** or **NEEDS FIXES** and a prioritised fix list.
