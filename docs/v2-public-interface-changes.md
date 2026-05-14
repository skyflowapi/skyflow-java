# Skyflow Java SDK — Public Interface Changes & Deprecation Notice

**Audience:** Product Managers, Technical Program Managers, Customer Success  
**SDK:** skyflow-java  
**Affected versions:** v2.x (current) → upcoming release

---

## Overview

As part of aligning the Skyflow Java SDK with cross-language naming standards, a set of public-facing field names and response keys are being updated. All changes are designed to be **non-breaking for existing customers** — old forms continue to work alongside new ones, with deprecation warnings logged at runtime to guide migration.

A future release will remove the deprecated forms entirely. No removal date is set yet.

---

## What Is Changing

### 1. Credentials file field names

When customers authenticate using a service account credentials JSON file, the field names inside that file are changing to follow Java naming conventions (lowercase acronyms).

| Old field name (deprecated) | New field name | Used in |
|---|---|---|
| `clientID` | `clientId` | `credentials.json` |
| `keyID` | `keyId` | `credentials.json` |
| `tokenURI` | `tokenUri` | `credentials.json` |

**Customer impact:** Customers with existing credentials files using the old field names (`clientID`, `keyID`, `tokenURI`) will continue to work without any changes. A deprecation warning will appear in their application logs recommending they update to the new field names.

**Example of the warning customers will see in their logs:**
```
[DEPRECATED] Credential field 'clientID' is deprecated and will be removed in an upcoming release. Use 'clientId' instead.
```

---

### 2. Response field key in Get and Query operations

When customers retrieve records from a vault using the **Get** or **Query** operations, each record includes a `skyflow_id` field identifying the record. This key name is changing to follow Java camelCase conventions.

| Old key (deprecated) | New key | Affected operations |
|---|---|---|
| `skyflow_id` | `skyflowId` | Get, Query |

**Customer impact:** Both `skyflow_id` and `skyflowId` will be present in response records simultaneously. Customers accessing `skyflow_id` today continue to receive the correct value. A deprecation warning will be logged once per record to prompt migration.

**Example of the warning customers will see in their logs:**
```
[DEPRECATED] Response key 'skyflow_id' is deprecated and will be removed in an upcoming release. Use 'skyflowId' instead.
```

> **Note:** Insert and Update operations already return `skyflowId` (camelCase) and are unaffected.

---

### 3. `downloadURL` method names in Get and Detokenize operations

Two method names used when configuring Get and Detokenize requests are changing to follow the same naming convention as the other fields above (`URL` → `Url`).

| Old method (deprecated) | New method | Used in |
|---|---|---|
| `.downloadURL(true)` builder method | `.downloadUrl(true)` | `GetRequest.builder()`, `DetokenizeRequest.builder()` |
| `.getDownloadURL()` | `.getDownloadUrl()` | `GetRequest`, `DetokenizeRequest` |

**Customer impact:** Existing code using `.downloadURL()` or `.getDownloadURL()` continues to compile and work. IDEs that support Java `@Deprecated` annotation will show a visual strikethrough on the old method name as a migration hint. No runtime behavior changes.

**Example of the IDE/compiler warning customers will see:**
```
[DEPRECATED] Method 'downloadURL()' is deprecated and will be removed in an upcoming release. Use 'downloadUrl()' instead.
```

---

---

## Behaviour Change: Insert and Update field value validation removed

**Affected operations:** Insert, Update

Previously the Java SDK threw an error if a record field value was `null` or an empty string `""`. This validation was inconsistent with both the Skyflow API spec (which accepts `additionalProperties: Any type`) and with other SDKs (Node has no such validation).

**The validation has been removed.** Field values of `null`, `""`, or any type are now passed through to the backend unchanged. The backend is the authoritative source for field-level validation.

| Scenario | Before | After |
|---|---|---|
| `{"name": null}` | SDK throws `SkyflowException` | Passed to BE ✓ |
| `{"name": ""}` | SDK throws `SkyflowException` | Passed to BE ✓ |
| `records: []` (empty array) | SDK throws `SkyflowException` | Passed to BE ✓ |

**This is a non-breaking change** — code that was previously failing will now succeed.

---

## What Is NOT Changing

- The Java method names customers call (e.g. `.insert()`, `.get()`, `.query()`)
- The request builder APIs (e.g. `InsertRequest.builder()`)
- Any vault configuration APIs (`VaultConfig`, `Credentials` setters)
- Authentication behaviour — credentials files still work identically
- Any connection, detect, audit, or tokenize interfaces

---

## Deprecation Strategy

| Phase | What happens | Timeline |
|---|---|---|
| **Now (v2.x)** | Old forms still work. Deprecation `[DEPRECATED]` warning logged at WARN level when old form is used. New forms also accepted. | Current |
| **Upcoming release** | Old forms removed. Only new forms accepted. Customers who have not migrated will see errors. | TBD |

Customers can suppress deprecation warnings by updating to the new field names at any time — no other code changes are required.

---

## Customer Migration Guide

### Get / Detokenize `downloadURL` → `downloadUrl`

```java
// Before (deprecated — still compiles in v2, removed in upcoming release)
GetRequest request = GetRequest.builder()
    .table("persons")
    .ids(ids)
    .downloadURL(true)    // ← deprecated
    .build();

// After
GetRequest request = GetRequest.builder()
    .table("persons")
    .ids(ids)
    .downloadUrl(true)    // ← new form
    .build();
```

### Credentials file

Update `credentials.json`:

```json
// Before (deprecated)
{
  "clientID": "...",
  "keyID": "...",
  "tokenURI": "...",
  "privateKey": "..."
}

// After (new — no other changes needed)
{
  "clientId": "...",
  "keyId": "...",
  "tokenUri": "...",
  "privateKey": "..."
}
```

### Get / Query response access

```java
// Before (deprecated — still works in v2, removed in upcoming release)
String id = record.get("skyflow_id").toString();

// After (new form — works in current and all future versions)
String id = record.get("skyflowId").toString();
```

---

## How to Check If Your Integration Is Affected

Set log level to `WARN` or higher. If you see any `[DEPRECATED]` entries in your application logs after upgrading, your integration is using an old form and should be updated before the next major release.

```java
Skyflow client = Skyflow.builder()
    .setLogLevel(LogLevel.WARN)
    ...
    .build();
```

---

## Questions

For technical questions, contact the SDK team. For release timeline questions, contact your Skyflow account representative.
