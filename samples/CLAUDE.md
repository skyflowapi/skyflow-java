---
name: skyflow-java-samples
description: Samples project context — file placement, structure, and rules for Skyflow Java SDK sample files.
paths:
  - "**/*.java"
  - pom.xml
---

# Skyflow Java SDK — Samples

Standalone Maven project demonstrating SDK features. Compile with:
```bash
cd samples && mvn compile -q
```

## File Placement

| Feature | Package | Directory |
|---|---|---|
| Vault ops (insert/get/update/delete/query/tokenize/detokenize) | `com.example.vault` | `samples/src/main/java/com/example/vault/` |
| Service account auth | `com.example.serviceaccount` | `samples/src/main/java/com/example/serviceaccount/` |
| Connection | `com.example.connection` | `samples/src/main/java/com/example/connection/` |
| Detect | `com.example.detect` | `samples/src/main/java/com/example/detect/` |
| Audit event operations | `com.example.audit` | `samples/src/main/java/com/example/audit/` |
| BIN lookup | `com.example.bin` | `samples/src/main/java/com/example/bin/` |

File name: `<FeatureName>Example.java`

## Deprecated Samples

Deprecated examples (v1-era or superseded APIs) live in:
```
samples/src/main/java/com/example/vault/deprecated/
```
Do not update deprecated samples — they are kept for reference only. New samples go in the parent package, not in `deprecated/`.

## Rules

- Vault IDs / cluster IDs: `"<YOUR_VAULT_ID>"`, `"<YOUR_CLUSTER_ID>"`
- Credential values: `"<YOUR_API_KEY>"`, `"<YOUR_CREDENTIALS_STRING>"`
- Credentials file path: `"credentials.json"` (relative, never absolute)
- Always catch `SkyflowException` and print `e.getMessage()`
- No separate `*Options` classes — use request builder methods directly
- Keep under 80 lines
- Imports from `com.skyflow.*` only — never from `com.skyflow.generated.*`
