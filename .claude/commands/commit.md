---
name: commit
description: Stage check + Jira-aware commit — extracts ticket ID from branch name and validates against pr.yml commit-message check.
---

Create a git commit for staged changes on the current branch.

Use `$ARGUMENTS` as the commit message description. If empty, ask the user for a description before proceeding.

## Step 1 — Extract ticket ID from branch name

```bash
git rev-parse --abbrev-ref HEAD
```

Extract the Jira ticket ID using the pattern `[A-Z]{1,10}-[0-9]+`:
- `devesh/SK-1234-fix-foo` → `SK-1234`
- `karthik/GV-770-ext-auth-json-error` → `GV-770`
- `username/SDK-2814-some-fix` → `SDK-2814`

If no ticket ID is found, **stop** and ask the user to provide one before continuing.

## Step 2 — Check what is staged

```bash
git status --short
git diff --cached --stat
```

If nothing is staged, list the unstaged files and ask the user which files to stage. Do not run `git add .` — ask for explicit paths (`.env`, `credentials.json`, and `generated/` must never be staged).

## Step 3 — Assemble and validate the commit message

Build the message as:
```
<ticket-id> <description>
```

If the user provided a Conventional Commits prefix (`feat`, `fix`, `chore`, `docs`, `refactor`, `test`), prepend it:
```
feat: SK-1234 add bulk insert support
fix: GV-770 handle null bearer token on refresh
```

Validate against the `pr.yml` enforced pattern: `(\[?[A-Z]{1,10}-[1-9][0-9]*)|(\[AUTOMATED\])|(Merge)|(Release)`
- Must contain a Jira ID — a bare description without a ticket ID will fail CI.
- If validation fails, report the exact requirement and stop.

## Step 4 — Commit

```bash
git commit -m "<assembled message>"
```

Report the resulting commit SHA and the commit message first line.
