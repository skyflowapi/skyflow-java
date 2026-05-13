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
| MEDIUM | `getErrors()` added to `QueryResponse` (field was missing entirely) | `QueryResponse.java` |
| LOW | Audit all builder setter/getter names — confirm no `setFooID()` pattern | `VaultConfig.java`, request builders |

---

## Detailed design

### HIGH: Credentials JSON field renames (`clientID` → `clientId`, `keyID` → `keyId`, `tokenURI` → `tokenUri`)

**Affected:** `BearerToken.java` (`getBearerTokenFromCredentials`), `SignedDataTokens.java` (`generateSignedTokensFromCredentials`)

**Why this change is needed:**

Java's naming convention treats acronyms as ordinary word components in camelCase identifiers — `Id` not `ID`, `Uri` not `URI`. The current field names `clientID`, `keyID`, `tokenURI` violate this by capitalising the acronym in full. This is inconsistent with the rest of the SDK (e.g. `setVaultId()`, `setClusterId()`) and breaks the "principle of least surprise" for Java developers who expect `clientId`.

These field names are defined in the credentials JSON file that users create and pass to the SDK (either as a file path or as a credentials string). They are therefore part of the SDK's public contract — a change forces users to update their credentials files. This is a breaking change, which is why it is gated to the v3.0.0 major release.

**Why a fallback is used instead of a hard cut:**

A hard cut would silently break all existing integrations the moment users upgrade to v3. The try-new-first fallback gives users a transition window: credentials files with the old keys continue to work, and users can migrate at their own pace. The fallback can be removed in a future major version once the old form is fully deprecated.

**Implementation strategy:** Try the new key first; fall back to the old key if null; throw if both are absent.

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

// tokenURI → tokenUri  (BearerToken only — SignedDataTokens does not use tokenURI)
JsonElement tokenUri = credentials.get("tokenUri");
if (tokenUri == null) tokenUri = credentials.get("tokenURI");
if (tokenUri == null) {
    throw new SkyflowException(...MissingTokenUri...);
}
```

Local variable names and private method parameter names are also updated to the new form (`clientId`, `keyId`, `tokenUri`) for internal consistency, though this has no effect on the public interface.

---

### MEDIUM: `skyflow_id` → `skyflowId` in Get and Query response maps

**Affected:** `VaultController.java` — `getFormattedGetRecord()` and `getFormattedQueryRecord()`

**Why this change is needed:**

The Skyflow REST API returns records with a `skyflow_id` field in snake_case — this is the wire format. The Java SDK is responsible for translating the wire format into language-idiomatic representations before handing data to callers. Java is a camelCase language, and the SDK already normalises `skyflow_id` to `skyflowId` in Insert and Update responses:

- `getFormattedBatchInsertRecord`: `insertRecord.put("skyflowId", recordObject.get("skyflow_id").getAsString())`
- `getFormattedBulkInsertRecord`: `insertRecord.put("skyflowId", record.getSkyflowId().get())`
- `getFormattedUpdateRecord`: `updateTokens.put("skyflowId", skyflowId)`

However, `getFormattedGetRecord` and `getFormattedQueryRecord` call `putAll(fieldsOpt.get())` which passes the raw API map directly through — including `skyflow_id` in snake_case. This inconsistency means that developers who write `record.get("skyflowId")` after a Get or Query call get `null`, while the same code works after an Insert or Update. It forces callers to know which operation produced the response just to read a single field.

**Implementation:** After `putAll`, check for the raw API key and rename it:

```java
if (record.containsKey("skyflow_id")) {
    record.put("skyflowId", record.remove("skyflow_id"));
}
```

Applied in both `getFormattedGetRecord` and `getFormattedQueryRecord`.

---

### MEDIUM: `getErrors()` added to `QueryResponse`

**Affected:** `QueryResponse.java`

**Why this change is needed:**

All other response types in the SDK (`GetResponse`, `InsertResponse`, `UpdateResponse`, `FileUploadResponse`) expose a `getErrors()` method. `QueryResponse` is the only one that does not — the `errors` field is referenced only inside `toString()` as a hardcoded literal `null`:

```java
responseObject.add("errors", null);
```

A caller who writes `queryResponse.getErrors()` gets a compile error because the method does not exist. This breaks the consistency contract that callers rely on when writing generic response-handling code across different vault operations.

**Fix:** Add `private final ArrayList<HashMap<String, Object>> errors` as a constructor field (always `null` — consistent with other response types that pass `null` when there are no errors) and expose it via `getErrors()`. The field will always be `null` for QueryResponse since the Query API does not currently model partial-error responses the same way batch insert does. This is kept as `null` rather than an empty list to stay consistent with the existing pattern across other response classes.

---

### LOW: Audit builder setter/getter names

**Affected:** `VaultConfig.java`, `InsertRequest`, `UpdateRequest`, `GetRequest`, `DeleteRequest`, `FileUploadRequest`, `QueryRequest`

**Why this change is needed:**

The same acronym-casing rule that applies to credentials fields applies to all Java method names. Any setter or getter using `ID` (all-caps) as a suffix — e.g. `setVaultID()`, `getSkyflowID()` — is non-idiomatic and inconsistent with Java convention. The spec item 15 calls out this as a verification task.

From initial review, `setVaultId()` and `setClusterId()` in `VaultConfig` are already correct. A full grep audit across all request builder classes is required to confirm there are no remaining `setFooID()` / `getFooID()` methods that were missed.

**Outcome:** If any violations are found, rename them to `setFooId()` / `getFooId()`. If none are found, this item is closed as verified-clean.

---

## What is NOT in scope

- **`tokenizedData` in QueryResponse:** The Skyflow Query API explicitly cannot return tokens. The existing `toString()` hack that injects `tokenizedData: {}` is a minor inconsistency between string output and programmatic access, but since callers have no reason to access tokenized data from a query result, this is not worth fixing now.

- **`UpdateRequest.getData()` map key**: Users currently pass `skyflow_id` (snake_case) in the data map to identify the record to update. This is an *input* key consumed by the SDK internally (`updateRequest.getData().remove("skyflow_id")`), not a response field surfaced to callers. The spec does not address this and changing it would require a separate design decision.
- **Generated REST client code** under `com.skyflow.generated.*`: These files are auto-generated by Fern from the API definition. Manual edits would be overwritten on the next regeneration.
- **`SKYFLOW_CREDENTIALS` environment variable name**: Stays `ALL_CAPS` per OS and shell convention. Only the parsed field names within the JSON value change.
- **Validation logic for null/None insert values**: The spec marks this as Python-only (item 12). Java already throws on invalid input at the API boundary.
