# Java SDK Nomenclature Cleanup — Design Spec

**Date:** 2026-05-13
**Reference:** [Skyflow Server-Side SDK: Nomenclature changes](https://skyflow.atlassian.net/wiki/spaces/SDK1/pages/2933162001/Skyflow+Server-Side+SDK+Nomenclature+changes)
**Scope:** Public interface only (`src/main/java/com/skyflow/`)
**Target release:** v3.0.0 (HIGH), v3.1.0 (MEDIUM), v3.1.x (LOW audit)

---

## Summary of changes

| Priority | Change | Files |
|---|---|---|
| HIGH | `clientID` → `clientId` in credentials JSON parsing (with fallback) | `BearerToken.java`, `SignedDataTokens.java` |
| HIGH | `keyID` → `keyId` in credentials JSON parsing (with fallback) | `BearerToken.java`, `SignedDataTokens.java` |
| HIGH | `tokenURI` → `tokenUri` in credentials JSON parsing (with fallback) | `BearerToken.java` |
| MEDIUM | `skyflow_id` → `skyflowId` key in Get and Query response maps | `VaultController.java` |
| MEDIUM | `tokenizedData` as a real field on `QueryResponse` (always present) | `QueryResponse.java` |
| MEDIUM | `getErrors()` added to `QueryResponse` (field was missing entirely) | `QueryResponse.java` |
| LOW | Audit all builder setter/getter names — confirm no `setFooID()` pattern | `VaultConfig.java`, request builders |

---

## Detailed design

### HIGH: Credentials JSON field renames

**Affected:** `BearerToken.java` (method `getBearerTokenFromCredentials`),
`SignedDataTokens.java` (method `generateSignedTokensFromCredentials`)

The credentials JSON file (provided by users) currently uses `clientID`, `keyID`, `tokenURI`.
The new canonical form is `clientId`, `keyId`, `tokenUri` (acronyms treated as words, per Java camelCase convention).

**Strategy:** Try the new key first; fall back to the old key if null. This allows existing credentials files to keep working during migration.

```java
// clientID → clientId
JsonElement clientId = credentials.get("clientId");
if (clientId == null) clientId = credentials.get("clientID");
if (clientId == null) {
    throw new SkyflowException(...MissingClientId...);
}

// keyID → keyId
JsonElement keyId = credentials.get("keyId");
if (keyId == null) keyId = credentials.get("keyID");
if (keyId == null) {
    throw new SkyflowException(...MissingKeyId...);
}

// tokenURI → tokenUri  (BearerToken only)
JsonElement tokenUri = credentials.get("tokenUri");
if (tokenUri == null) tokenUri = credentials.get("tokenURI");
if (tokenUri == null) {
    throw new SkyflowException(...MissingTokenUri...);
}
```

Local variable names and private method parameter names updated to match (`clientId`, `keyId`, `tokenUri`).

---

### MEDIUM: skyflow_id → skyflowId in Get and Query response maps

**Affected:** `VaultController.java` — `getFormattedGetRecord()` and `getFormattedQueryRecord()`

Insert and Update responses already use `skyflowId`. Get and Query currently call `putAll(fieldsOpt.get())` which passes through the raw API field name `skyflow_id`. After the `putAll`, rename the key:

```java
if (record.containsKey("skyflow_id")) {
    record.put("skyflowId", record.remove("skyflow_id"));
}
```

Applied in both `getFormattedGetRecord` and `getFormattedQueryRecord`.

---

### MEDIUM: tokenizedData always present in QueryResponse

**Affected:** `VaultController.java` (`getFormattedQueryRecord`), `QueryResponse.java`

**Why this change is valid:**

The Skyflow API docs state that the Query endpoint "can't return tokens" today. However:

1. The Fern-generated `V1FieldRecords` type explicitly defines a `tokens` field alongside `fields` — meaning the API contract already supports it and it may be populated in future.
2. The spec's cross-SDK requirement is that `tokenizedData` is always present per-record (even as an empty object), so callers don't need to null-check regardless of API version.
3. `getFormattedQueryRecord` currently ignores `record.getTokens()` entirely. The current `toString()` hack papers over this by injecting `tokenizedData: {}` into the serialized JSON string — but a caller doing `queryResponse.getFields().get(0).get("tokenizedData")` still gets `null`.

**Fix:** In `getFormattedQueryRecord`, populate `tokenizedData` from `record.getTokens()` (empty map when absent):

```java
private static synchronized HashMap<String, Object> getFormattedQueryRecord(V1FieldRecords record) {
    HashMap<String, Object> queryRecord = new HashMap<>();
    record.getFields().ifPresent(queryRecord::putAll);
    queryRecord.put("tokenizedData", record.getTokens().orElse(new HashMap<>()));
    return queryRecord;
}
```

Remove the manual `tokenizedData` injection hack from `QueryResponse.toString()`. The `toString()` override simplifies to standard Gson serialization with `serializeNulls`.

---

### MEDIUM: errors always present in QueryResponse

**Affected:** `QueryResponse.java`

`QueryResponse` has no `errors` field or `getErrors()` method today — errors are only referenced in `toString()` as a hardcoded `null`. A caller cannot access errors programmatically.

**Fix:** Add `private final ArrayList<HashMap<String, Object>> errors` (always `null` — not converted to empty list) and a `getErrors()` accessor. Consistent with `GetResponse`, `InsertResponse`, `UpdateResponse` which all have `getErrors()` returning null when no errors.

---

### LOW: Audit builder setter/getter names

**Affected:** `VaultConfig.java`, `InsertRequest`, `UpdateRequest`, `GetRequest`, `DeleteRequest`, `FileUploadRequest`, `QueryRequest`

Confirm all methods follow `setFooId()` / `getFooId()` (title-case `Id`), not `setFooID()` (all-caps `ID`).

From initial review: `setVaultId()`, `setClusterId()` in `VaultConfig` are already correct. Full grep audit required to confirm no remaining violations.

---

## What is NOT in scope

- `UpdateRequest.getData()` map key convention (user passes `skyflow_id` to identify the record to update — this is an input key, not a response key, and is not addressed in the spec)
- Any changes to generated REST client code under `com.skyflow.generated.*`
- `SKYFLOW_CREDENTIALS` environment variable name (stays `ALL_CAPS` per OS convention)
- Validation logic changes (null insert value handling is Python-only per spec)
