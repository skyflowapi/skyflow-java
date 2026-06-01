---
name: code-smell
description: Structural smell analysis + spell check — long methods, dead code, misplaced validation, deep nesting, magic numbers. Does not check patterns or security.
paths:
  - src/main/java/**/*.java
exclude:
  - src/main/java/com/skyflow/generated/**
---

You are a senior engineer performing a code smell analysis on the Skyflow Java SDK.

## Scope

Use `$ARGUMENTS` to determine scope:
- A file or directory path — analyse only that path
- Empty / default — analyse files changed on current branch vs `main`:
  ```bash
  git diff main...HEAD --name-only | grep '\.java$' | grep -v 'generated'
  ```

**Skip entirely:** `src/main/java/com/skyflow/generated/` — Fern-generated REST client, read-only.

---

## Spell check

Before analysing smells, run cspell on the files in scope:

```bash
npx cspell --no-progress "src/**/*.java" ".claude/**/*.md" "CLAUDE.md" "docs/**/*.md" 2>&1 | grep "Unknown word"
```

Report any spelling violations at **Smell** severity in the per-file table. The word list is in `.cspell.json` — add legitimate project-specific terms there rather than fixing them as typos.

---

## What Are Code Smells

Code smells are structural signals — they do not necessarily mean the code is broken, but they indicate areas of technical debt, reduced readability, or future maintenance risk. All findings are reported at **Smell** severity and do not block merge unless they indicate a design violation.

---

## Smell Catalogue

### Method & Class Size

**Long method** — any method over 40 lines.
Signal: the method is doing too much. Candidate for decomposition into named private helpers.

**Long class** — any class over 300 lines.
Signal: the class may be taking on too many responsibilities. Check if it can be split by concern.

**Large parameter list** — more than 4 parameters on a method.
Signal: consider a config/options object or a builder to group related parameters.

---

### Responsibility Violations

**Business logic in Request/Response classes**
Request and Response classes are data holders — they carry data, nothing more. Flag any conditional logic, field transformation, or computation beyond null-safe getters.
Example of a violation: a Response class that renames map keys in `toString()` instead of letting the controller do it.

**toString() with business logic**
`toString()` should only serialise state for debugging. Logic like field renaming, manual JSON construction, conditional field injection, or iteration belongs in the controller or formatter methods.

**Validation outside `Validations.java`**
Any `if (x == null) throw new SkyflowException(...)` outside `src/main/java/com/skyflow/utils/validations/` is misplaced validation. All request validation must live in `Validations.validateXxxRequest()`.

---

### Control Flow

**Deep nesting** — more than 3 levels of `if` / `for` / `try` nesting.
Signal: extract inner blocks to named private methods. Deep nesting hides the happy path.

**Long if-else chains** — more than 4 branches on the same condition.
Signal: consider a `Map`, `switch`, or polymorphism.

**Null checks scattered**
Multiple consecutive null guards that could be replaced with `Optional` or an early return guard clause.

---

### Data

**Magic numbers**
Literal integers or sizes (e.g. `25`, `3600`, `100`) without a named constant. Use `Constants`.

**Raw HashMap chains**
`HashMap<String, Object>` passed through more than 2 method boundaries without a typed wrapper or explanatory comment. Flag for awareness — do not require an immediate fix.

**Temporary field**
A class field that is only set in certain code paths and is `null` the rest of the time. Should be a local variable or method parameter instead.

---

### Dead Code

**Unused private methods** — private methods with no callers.

**Unused imports** — any `import` not referenced in the file.

**Unreachable code** — code after `return` / `throw` in the same branch.

**Commented-out code** — blocks of commented code without explanation. Remove entirely or add a `// TODO: [ticket]` with context.

---

### Comments

**Explains what, not why**
A comment that restates what the code does (`// get the vault ID`) adds no value. Only flag comments that explain the *what* without explaining *why*.

**Stale comment**
A comment that contradicts the current code — e.g. references a removed parameter, an old method name, or a behaviour that has changed.

---

## Output Format

Group findings by file:

```
### path/to/File.java

| Smell                     | Line | Detail                                                    |
|---------------------------|------|-----------------------------------------------------------|
| Long method               | 42   | processInsertResponse() is 67 lines — decompose          |
| Business logic in Response| 88   | toString() renames skyflow_id — move to formatter         |
| Magic number              | 103  | Literal 25 — extract to Constants.MAX_QUERY_RECORDS       |
| Stale comment             | 210  | References removed tokenizedData field                    |
| Dead code                 | 315  | Private method buildHeaders() has no callers              |
```

End with a **Smell Summary** table:

```
| Category              | Count | Files affected         |
|-----------------------|-------|------------------------|
| Long methods          | 2     | VaultController.java   |
| Business logic in DTO | 1     | QueryResponse.java     |
| Magic numbers         | 3     | Validations.java       |
| Dead code             | 2     | Utils.java             |
```

Close with a recommendation: **CLEAN** / **MINOR DEBT** / **SIGNIFICANT DEBT** and a one-sentence summary.
