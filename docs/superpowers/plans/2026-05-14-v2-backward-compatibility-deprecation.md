# V2 Backward Compatibility — Deprecation Warnings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore backward compatibility for v2 customers by keeping old public interface forms alongside new ones, and emit deprecation signals when the old forms are used.

**Architecture:** Three independent changes. (1) The `skyflow_id` response key was removed — it must be restored alongside `skyflowId` so both exist in the map simultaneously; a WARN log is emitted per response build when the old key is present. (2) The credential field fallback (`clientID`/`keyID`/`tokenURI`) already works silently — add WARN logs when the old form triggers the fallback path. (3) `downloadURL` method names on `GetRequest` and `DetokenizeRequest` violate the acronym-as-word rule — keep the old `@Deprecated` methods alongside new `downloadUrl` methods. All runtime deprecation messages use `LogUtil.printWarningLog()`; method-level deprecation uses the standard Java `@Deprecated` annotation.

**Tech Stack:** Java 11+, JUnit 4, Maven (`mvn test`)

**Design context:** `docs/superpowers/specs/2026-05-13-java-nomenclature-cleanup-design.md`

---

## Breaking-Change Summary

| Change | Status | Fix needed |
|---|---|---|
| `skyflow_id` removed from Get/Query response maps | **BREAKING** | Keep both `skyflow_id` + `skyflowId`; emit WARN |
| `clientID`/`keyID`/`tokenURI` replaced in BearerToken | Not breaking (fallback exists) | Add WARN log on old-form fallback |
| `clientID`/`keyID` replaced in SignedDataTokens | Not breaking (fallback exists) | Add WARN log on old-form fallback |
| `getErrors()` added to QueryResponse | Not breaking (additive) | No change needed |
| `downloadURL` → `downloadUrl` in GetRequest & DetokenizeRequest | **BREAKING** | Keep `@Deprecated` old methods; add new `downloadUrl` methods |

---

## File Map

| File | Change |
|---|---|
| `src/main/java/com/skyflow/logs/InfoLogs.java` | Add 5 deprecation warning log entries (4 existing + 1 for downloadURL) |
| `src/main/java/com/skyflow/vault/data/GetRequest.java` | Add `getDownloadUrl()` + builder `downloadUrl()`; mark old `getDownloadURL()` as `@Deprecated` |
| `src/main/java/com/skyflow/vault/tokens/DetokenizeRequest.java` | Add `getDownloadUrl()` + builder `downloadUrl()`; mark old `getDownloadURL()` as `@Deprecated` |
| `src/main/java/com/skyflow/vault/controller/VaultController.java` | Keep `skyflow_id` key alongside `skyflowId`; emit WARN per record |
| `src/main/java/com/skyflow/vault/data/GetResponse.java` | Add `@deprecated` Javadoc on `getData()` for `skyflow_id` key |
| `src/main/java/com/skyflow/vault/data/QueryResponse.java` | Add `@deprecated` Javadoc on `getFields()` for `skyflow_id` key |
| `src/main/java/com/skyflow/serviceaccount/util/BearerToken.java` | Add WARN log when `clientID`/`keyID`/`tokenURI` fallback fires |
| `src/main/java/com/skyflow/serviceaccount/util/SignedDataTokens.java` | Add WARN log when `clientID`/`keyID` fallback fires |
| `src/test/java/com/skyflow/vault/controller/VaultControllerTests.java` | Update existing tests: assert BOTH `skyflow_id` and `skyflowId` present |
| `src/test/java/com/skyflow/serviceaccount/util/BearerTokenTests.java` | Existing tests unchanged (old-form already passes); add comment |
| `src/test/java/com/skyflow/serviceaccount/util/SignedDataTokensTests.java` | Existing tests unchanged |

---

## Task 1: Add deprecation log entries to InfoLogs

**Files:**
- Modify: `src/main/java/com/skyflow/logs/InfoLogs.java`

### Background
`InfoLogs` is an enum that holds all INFO-level log message strings. Deprecation warnings use `LogUtil.printWarningLog(String)` which takes a plain string — but following the codebase convention of centralising messages in enums, we add the deprecation messages here. They will be passed as `InfoLogs.DEPRECATED_SKYFLOW_ID_KEY.getLog()` etc.

- [ ] **Step 1: Add deprecation entries to InfoLogs enum**

Open `src/main/java/com/skyflow/logs/InfoLogs.java`. Find the last enum entry before the blank line and constructor (around line 98). Add these four entries in a new section:

```java
    // Deprecation warnings — v2 backward compat, to be removed in v3
    DEPRECATED_SKYFLOW_ID_KEY("[DEPRECATED] Response key 'skyflow_id' is deprecated and will be removed in an upcoming release. Use 'skyflowId' instead."),
    DEPRECATED_CREDENTIAL_CLIENT_ID("[DEPRECATED] Credential field 'clientID' is deprecated and will be removed in an upcoming release. Use 'clientId' instead."),
    DEPRECATED_CREDENTIAL_KEY_ID("[DEPRECATED] Credential field 'keyID' is deprecated and will be removed in an upcoming release. Use 'keyId' instead."),
    DEPRECATED_CREDENTIAL_TOKEN_URI("[DEPRECATED] Credential field 'tokenURI' is deprecated and will be removed in an upcoming release. Use 'tokenUri' instead.");
```

Note: The last existing entry before your addition ends with a comma. Your last entry (`DEPRECATED_CREDENTIAL_TOKEN_URI`) ends with a semicolon `;` to close the enum constant list — verify you are replacing the existing semicolon, not duplicating it.

- [ ] **Step 2: Verify compilation**

```bash
mvn compile -q 2>&1 | tail -10
```

Expected: no output (clean compile).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/skyflow/logs/InfoLogs.java
git commit -m "chore: add deprecation warning log entries to InfoLogs"
```

---

## Task 2: Restore `skyflow_id` key in Get/Query response maps

**Files:**
- Modify: `src/main/java/com/skyflow/vault/controller/VaultController.java:121-165`
- Modify: `src/test/java/com/skyflow/vault/controller/VaultControllerTests.java`

### Background
`getFormattedGetRecord` and `getFormattedQueryRecord` currently do:
```java
if (getRecord.containsKey("skyflow_id")) {
    getRecord.put("skyflowId", getRecord.remove("skyflow_id")); // BREAKS customers using skyflow_id
}
```
The `remove` deletes `skyflow_id` from the map. We must change this to a **copy** (not a move): put `skyflowId` AND keep `skyflow_id`. Emit one WARN log per record that contains the old key.

The existing tests `testGetFormattedGetRecordNormalisesSkyflowId` and `testGetFormattedQueryRecordNormalisesSkyflowId` assert `skyflow_id` is **absent** — those assertions must be flipped.

- [ ] **Step 1: Update the existing tests to assert BOTH keys present**

In `src/test/java/com/skyflow/vault/controller/VaultControllerTests.java`, find `testGetFormattedGetRecordNormalisesSkyflowId` and `testGetFormattedQueryRecordNormalisesSkyflowId` and update their assertions:

```java
@Test
public void testGetFormattedGetRecordNormalisesSkyflowId() throws Exception {
    Map<String, Object> fields = new HashMap<>();
    fields.put("skyflow_id", "abc-123");
    fields.put("name", "John");
    V1FieldRecords record = V1FieldRecords.builder().fields(fields).build();

    Method method = VaultController.class.getDeclaredMethod("getFormattedGetRecord", V1FieldRecords.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    HashMap<String, Object> result = (HashMap<String, Object>) method.invoke(null, record);

    // Both keys must be present — skyflow_id kept for v2 backward compat, skyflowId is the new form
    Assert.assertEquals("skyflowId should be present (new form)", "abc-123", result.get("skyflowId"));
    Assert.assertEquals("skyflow_id should still be present (v2 deprecated form)", "abc-123", result.get("skyflow_id"));
    Assert.assertEquals("other fields should be preserved", "John", result.get("name"));
}

@Test
public void testGetFormattedQueryRecordNormalisesSkyflowId() throws Exception {
    Map<String, Object> fields = new HashMap<>();
    fields.put("skyflow_id", "xyz-456");
    fields.put("email", "test@example.com");
    V1FieldRecords record = V1FieldRecords.builder().fields(fields).build();

    Method method = VaultController.class.getDeclaredMethod("getFormattedQueryRecord", V1FieldRecords.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    HashMap<String, Object> result = (HashMap<String, Object>) method.invoke(null, record);

    // Both keys must be present — skyflow_id kept for v2 backward compat, skyflowId is the new form
    Assert.assertEquals("skyflowId should be present (new form)", "xyz-456", result.get("skyflowId"));
    Assert.assertEquals("skyflow_id should still be present (v2 deprecated form)", "xyz-456", result.get("skyflow_id"));
    Assert.assertEquals("other fields should be preserved", "test@example.com", result.get("email"));
}

@Test
public void testGetFormattedGetRecordNormalisesSkyflowIdInTokensBranch() throws Exception {
    Map<String, Object> tokens = new HashMap<>();
    tokens.put("skyflow_id", "tok-789");
    tokens.put("card_number", "tok-card-abc");
    V1FieldRecords record = V1FieldRecords.builder().tokens(tokens).build();

    Method method = VaultController.class.getDeclaredMethod("getFormattedGetRecord", V1FieldRecords.class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    HashMap<String, Object> result = (HashMap<String, Object>) method.invoke(null, record);

    // Both keys must be present in tokens branch too
    Assert.assertEquals("skyflowId should be present (new form)", "tok-789", result.get("skyflowId"));
    Assert.assertEquals("skyflow_id should still be present (v2 deprecated form)", "tok-789", result.get("skyflow_id"));
    Assert.assertEquals("other token fields should be preserved", "tok-card-abc", result.get("card_number"));
}
```

- [ ] **Step 2: Run the tests to confirm they fail (skyflow_id is still being removed)**

```bash
mvn test -pl . -Dtest=VaultControllerTests#testGetFormattedGetRecordNormalisesSkyflowId+testGetFormattedQueryRecordNormalisesSkyflowId+testGetFormattedGetRecordNormalisesSkyflowIdInTokensBranch -q 2>&1 | tail -20
```

Expected: FAIL — assertions on `skyflow_id` being present fail because it is currently removed.

- [ ] **Step 3: Update `getFormattedGetRecord` in VaultController.java**

Change the rename block from a **move** to a **copy** and add a WARN log. The import `com.skyflow.logs.InfoLogs` is already present.

Replace:
```java
        if (getRecord.containsKey("skyflow_id")) {
            getRecord.put("skyflowId", getRecord.remove("skyflow_id"));
        }
```

With:
```java
        if (getRecord.containsKey("skyflow_id")) {
            getRecord.put("skyflowId", getRecord.get("skyflow_id"));
            LogUtil.printWarningLog(InfoLogs.DEPRECATED_SKYFLOW_ID_KEY.getLog());
        }
```

- [ ] **Step 4: Update `getFormattedQueryRecord` in VaultController.java**

Replace:
```java
        if (queryRecord.containsKey("skyflow_id")) {
            queryRecord.put("skyflowId", queryRecord.remove("skyflow_id"));
        }
```

With:
```java
        if (queryRecord.containsKey("skyflow_id")) {
            queryRecord.put("skyflowId", queryRecord.get("skyflow_id"));
            LogUtil.printWarningLog(InfoLogs.DEPRECATED_SKYFLOW_ID_KEY.getLog());
        }
```

- [ ] **Step 5: Run the tests to confirm they pass**

```bash
mvn test -pl . -Dtest=VaultControllerTests -q 2>&1 | tail -20
```

Expected: all 11 tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/skyflow/vault/controller/VaultController.java \
        src/test/java/com/skyflow/vault/controller/VaultControllerTests.java
git commit -m "fix: restore skyflow_id key in Get/Query responses for v2 backward compat

Both skyflow_id (deprecated, v2) and skyflowId (new form) are now present
in response maps simultaneously. WARN log emitted per record to signal
migration path to callers."
```

---

## Task 3: Add deprecation Javadoc to GetResponse and QueryResponse

**Files:**
- Modify: `src/main/java/com/skyflow/vault/data/GetResponse.java`
- Modify: `src/main/java/com/skyflow/vault/data/QueryResponse.java`

### Background
Customers reading `getData()` or `getFields()` should see a compiler-visible signal that `skyflow_id` is a deprecated key in the returned map, with guidance to migrate to `skyflowId`.

- [ ] **Step 1: Add Javadoc to `GetResponse.getData()`**

In `src/main/java/com/skyflow/vault/data/GetResponse.java`, add Javadoc above `getData()`:

```java
    /**
     * Returns the list of record maps from the Get response. Each map contains all
     * field name/value pairs for the record.
     *
     * <p><b>Deprecation notice:</b> The {@code skyflow_id} key in each record map is
     * deprecated and will be removed in an upcoming release. Use {@code skyflowId} instead.
     * Both keys are present simultaneously in v2 for backward compatibility.</p>
     */
    public ArrayList<HashMap<String, Object>> getData() {
        return data;
    }
```

- [ ] **Step 2: Add Javadoc to `QueryResponse.getFields()`**

In `src/main/java/com/skyflow/vault/data/QueryResponse.java`, add Javadoc above `getFields()`:

```java
    /**
     * Returns the list of record maps from the Query response. Each map contains all
     * field name/value pairs for the record.
     *
     * <p><b>Deprecation notice:</b> The {@code skyflow_id} key in each record map is
     * deprecated and will be removed in an upcoming release. Use {@code skyflowId} instead.
     * Both keys are present simultaneously in v2 for backward compatibility.</p>
     */
    public ArrayList<HashMap<String, Object>> getFields() {
        return fields;
    }
```

- [ ] **Step 3: Verify compilation**

```bash
mvn compile -q 2>&1 | tail -10
```

Expected: no output.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/skyflow/vault/data/GetResponse.java \
        src/main/java/com/skyflow/vault/data/QueryResponse.java
git commit -m "docs: add deprecation Javadoc for skyflow_id key in GetResponse and QueryResponse"
```

---

## Task 4: Add deprecation WARN logs in BearerToken for old credential field names

**Files:**
- Modify: `src/main/java/com/skyflow/serviceaccount/util/BearerToken.java:102-127`

### Background
`getBearerTokenFromCredentials` already has fallback: tries `clientId` first, then `clientID`. When the fallback triggers (new form returns null, old form returns non-null), we emit a WARN log so users know to migrate their credentials file.

- [ ] **Step 1: Add WARN logs to the three fallback paths in `BearerToken.java`**

Current code (lines 102–127):
```java
            // Accept both new-form keys (clientId/keyId/tokenUri) and legacy all-caps form for migration
            JsonElement clientId = credentials.get("clientId");
            if (clientId == null) {
                clientId = credentials.get("clientID");
            }
            if (clientId == null) { ... throw ... }

            JsonElement keyId = credentials.get("keyId");
            if (keyId == null) {
                keyId = credentials.get("keyID");
            }
            if (keyId == null) { ... throw ... }

            JsonElement tokenUri = credentials.get("tokenUri");
            if (tokenUri == null) {
                tokenUri = credentials.get("tokenURI");
            }
            if (tokenUri == null) { ... throw ... }
```

Replace with — adding a WARN log inside each fallback `if` block:

```java
            // Accept both new-form keys (clientId/keyId/tokenUri) and legacy all-caps form for migration
            JsonElement clientId = credentials.get("clientId");
            if (clientId == null) {
                clientId = credentials.get("clientID");
                if (clientId != null) {
                    LogUtil.printWarningLog(InfoLogs.DEPRECATED_CREDENTIAL_CLIENT_ID.getLog());
                }
            }
            if (clientId == null) {
                LogUtil.printErrorLog(ErrorLogs.CLIENT_ID_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingClientId.getMessage());
            }

            JsonElement keyId = credentials.get("keyId");
            if (keyId == null) {
                keyId = credentials.get("keyID");
                if (keyId != null) {
                    LogUtil.printWarningLog(InfoLogs.DEPRECATED_CREDENTIAL_KEY_ID.getLog());
                }
            }
            if (keyId == null) {
                LogUtil.printErrorLog(ErrorLogs.KEY_ID_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingKeyId.getMessage());
            }

            JsonElement tokenUri = credentials.get("tokenUri");
            if (tokenUri == null) {
                tokenUri = credentials.get("tokenURI");
                if (tokenUri != null) {
                    LogUtil.printWarningLog(InfoLogs.DEPRECATED_CREDENTIAL_TOKEN_URI.getLog());
                }
            }
            if (tokenUri == null) {
                LogUtil.printErrorLog(ErrorLogs.TOKEN_URI_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingTokenUri.getMessage());
            }
```

- [ ] **Step 2: Verify compilation and run BearerToken tests**

```bash
mvn compile -q 2>&1 | tail -5
mvn test -pl . -Dtest=BearerTokenTests -q 2>&1 | tail -10
```

Expected: clean compile, 17/17 tests pass. The existing test `testBearerTokenWithOldFormCredentialKeys` (which uses `clientID`/`keyID`/`tokenURI`) continues to pass — now with a WARN log emitted at runtime.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/skyflow/serviceaccount/util/BearerToken.java
git commit -m "feat: emit deprecation WARN log when legacy clientID/keyID/tokenURI credential fields are used in BearerToken"
```

---

## Task 5: Add deprecation WARN logs in SignedDataTokens for old credential field names

**Files:**
- Modify: `src/main/java/com/skyflow/serviceaccount/util/SignedDataTokens.java:103-122`

### Background
Same as Task 4 but for `SignedDataTokens`. Only `clientId`/`keyId` — no `tokenUri` in this class.

- [ ] **Step 1: Add WARN logs to the two fallback paths in `SignedDataTokens.java`**

Current code (lines 103–122):
```java
            // Accept both new-form keys (clientId/keyId) and legacy all-caps form for migration
            JsonElement clientId = credentials.get("clientId");
            if (clientId == null) {
                clientId = credentials.get("clientID");
            }
            if (clientId == null) { ... throw ... }

            JsonElement keyId = credentials.get("keyId");
            if (keyId == null) {
                keyId = credentials.get("keyID");
            }
            if (keyId == null) { ... throw ... }
```

Replace with:

```java
            // Accept both new-form keys (clientId/keyId) and legacy all-caps form for migration
            JsonElement clientId = credentials.get("clientId");
            if (clientId == null) {
                clientId = credentials.get("clientID");
                if (clientId != null) {
                    LogUtil.printWarningLog(InfoLogs.DEPRECATED_CREDENTIAL_CLIENT_ID.getLog());
                }
            }
            if (clientId == null) {
                LogUtil.printErrorLog(ErrorLogs.CLIENT_ID_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingClientId.getMessage());
            }

            JsonElement keyId = credentials.get("keyId");
            if (keyId == null) {
                keyId = credentials.get("keyID");
                if (keyId != null) {
                    LogUtil.printWarningLog(InfoLogs.DEPRECATED_CREDENTIAL_KEY_ID.getLog());
                }
            }
            if (keyId == null) {
                LogUtil.printErrorLog(ErrorLogs.KEY_ID_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingKeyId.getMessage());
            }
```

Also verify that `InfoLogs` is already imported in `SignedDataTokens.java`. If not, add:
```java
import com.skyflow.logs.InfoLogs;
```

- [ ] **Step 2: Verify compilation and run SignedDataTokens tests**

```bash
mvn compile -q 2>&1 | tail -5
mvn test -pl . -Dtest=SignedDataTokensTests -q 2>&1 | tail -10
```

Expected: clean compile, 15/15 tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/skyflow/serviceaccount/util/SignedDataTokens.java
git commit -m "feat: emit deprecation WARN log when legacy clientID/keyID credential fields are used in SignedDataTokens"
```

---

## Task 6: Deprecate `downloadURL` → `downloadUrl` in GetRequest and DetokenizeRequest

**Files:**
- Modify: `src/main/java/com/skyflow/vault/data/GetRequest.java`
- Modify: `src/main/java/com/skyflow/vault/tokens/DetokenizeRequest.java`
- Modify: `src/main/java/com/skyflow/logs/InfoLogs.java` (add one entry)

### Background
`getDownloadURL()` and builder `.downloadURL()` use all-caps `URL`, violating the same acronym-as-word rule as `clientID`/`tokenURI`. Since these are Java method names (not map keys), we use the standard `@Deprecated` annotation + Javadoc, which gives callers a **compile-time warning** in their IDE. No runtime `LogUtil` log is needed — the annotation is the industry standard signal for method deprecation. Keep the old methods as delegates to the new ones so existing code compiles without changes.

- [ ] **Step 1: Add `DEPRECATED_DOWNLOAD_URL` to InfoLogs.java**

Open `src/main/java/com/skyflow/logs/InfoLogs.java` and add one entry to the deprecation section:

```java
    DEPRECATED_DOWNLOAD_URL("[DEPRECATED] Method 'downloadURL()' is deprecated and will be removed in an upcoming release. Use 'downloadUrl()' instead."),
```

- [ ] **Step 2: Write failing tests for new `downloadUrl` methods**

Add these tests to `src/test/java/com/skyflow/vault/controller/VaultControllerTests.java` (or a new `GetRequestTest.java`):

```java
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.tokens.DetokenizeRequest;

@Test
public void testGetRequestDownloadUrlNewForm() {
    GetRequest request = GetRequest.builder()
            .table("test_table")
            .downloadUrl(true)
            .build();
    Assert.assertTrue("downloadUrl(true) should be set", request.getDownloadUrl());
}

@Test
public void testGetRequestDownloadURLOldFormStillWorks() {
    GetRequest request = GetRequest.builder()
            .table("test_table")
            .downloadURL(true)
            .build();
    // Old method delegates to new — both accessors return the same value
    Assert.assertTrue("old downloadURL() should still work", request.getDownloadURL());
    Assert.assertTrue("new getDownloadUrl() should also return same value", request.getDownloadUrl());
}

@Test
public void testDetokenizeRequestDownloadUrlNewForm() {
    DetokenizeRequest request = DetokenizeRequest.builder()
            .downloadUrl(true)
            .build();
    Assert.assertTrue("downloadUrl(true) should be set", request.getDownloadUrl());
}
```

- [ ] **Step 3: Run tests to confirm they fail**

```bash
mvn test -pl . -Dtest=VaultControllerTests#testGetRequestDownloadUrlNewForm+testGetRequestDownloadURLOldFormStillWorks+testDetokenizeRequestDownloadUrlNewForm -q 2>&1 | tail -10
```

Expected: compile error — `downloadUrl()` and `getDownloadUrl()` methods do not exist yet.

- [ ] **Step 4: Update `GetRequest.java`**

In `src/main/java/com/skyflow/vault/data/GetRequest.java`:

**On the request class** — add new getter, mark old one `@Deprecated`:
```java
    /**
     * @deprecated Use {@link #getDownloadUrl()} instead.
     */
    @Deprecated
    public Boolean getDownloadURL() {
        return getDownloadUrl();
    }

    public Boolean getDownloadUrl() {
        return this.builder.downloadUrl;
    }
```

**On the builder** — rename the field and add both builder methods:
```java
    public static final class GetRequestBuilder {
        // ... other fields ...
        private Boolean downloadUrl;  // renamed from downloadURL

        /**
         * @deprecated Use {@link #downloadUrl(Boolean)} instead.
         */
        @Deprecated
        public GetRequestBuilder downloadURL(Boolean downloadURL) {
            return downloadUrl(downloadURL);
        }

        public GetRequestBuilder downloadUrl(Boolean downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }
    }
```

Also update the `getDownloadURL()` accessor in the request body (the non-builder getter) to delegate:
```java
    public Boolean getDownloadURL() {
        return getDownloadUrl();
    }

    public Boolean getDownloadUrl() {
        return this.builder.downloadUrl;
    }
```

Note: the existing `getDownloadURL()` in the request class (not the builder) currently reads `this.builder.downloadURL`. After renaming the field to `downloadUrl`, update the reference accordingly.

- [ ] **Step 5: Update `DetokenizeRequest.java`**

Apply the identical pattern in `src/main/java/com/skyflow/vault/tokens/DetokenizeRequest.java`:

**On the request class:**
```java
    /**
     * @deprecated Use {@link #getDownloadUrl()} instead.
     */
    @Deprecated
    public Boolean getDownloadURL() {
        return getDownloadUrl();
    }

    public Boolean getDownloadUrl() {
        return this.builder.downloadUrl;
    }
```

**On the builder:**
```java
        private Boolean downloadUrl;  // renamed from downloadURL

        /**
         * @deprecated Use {@link #downloadUrl(Boolean)} instead.
         */
        @Deprecated
        public DetokenizeRequestBuilder downloadURL(Boolean downloadURL) {
            return downloadUrl(downloadURL);
        }

        public DetokenizeRequestBuilder downloadUrl(Boolean downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }
```

- [ ] **Step 6: Run the new tests to confirm they pass**

```bash
mvn test -pl . -Dtest=VaultControllerTests#testGetRequestDownloadUrlNewForm+testGetRequestDownloadURLOldFormStillWorks+testDetokenizeRequestDownloadUrlNewForm -q 2>&1 | tail -10
```

Expected: all 3 pass.

- [ ] **Step 7: Run full suite to confirm no regressions**

```bash
mvn test -q 2>&1 | grep -E "Tests run|FAIL|ERROR" | tail -5
```

Expected: baseline only (no new failures).

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/skyflow/vault/data/GetRequest.java \
        src/main/java/com/skyflow/vault/tokens/DetokenizeRequest.java \
        src/main/java/com/skyflow/logs/InfoLogs.java \
        src/test/java/com/skyflow/vault/controller/VaultControllerTests.java
git commit -m "feat: deprecate downloadURL in favour of downloadUrl in GetRequest and DetokenizeRequest

Old downloadURL() methods kept as @Deprecated delegates for v2 backward
compat. New downloadUrl() methods follow the acronym-as-word convention
consistent with skyflowId, clientId, tokenUri."
```

---

## Task 8: Final verification — full test suite

- [ ] **Step 1: Run full test suite**

```bash
mvn test -q 2>&1 | grep -E "Tests run|FAIL|ERROR" | tail -10
```

Expected baseline: 374 tests, ~5 failures, ~4 errors (all pre-existing — see `CLAUDE.md` for the list). No new failures.

- [ ] **Step 2: Verify both keys appear in a sample response**

Manually verify by running a grep to confirm the implementation is correct:

```bash
grep -n "skyflow_id\|skyflowId\|DEPRECATED" src/main/java/com/skyflow/vault/controller/VaultController.java
```

Expected output includes lines like:
```
getRecord.put("skyflowId", getRecord.get("skyflow_id"));
LogUtil.printWarningLog(InfoLogs.DEPRECATED_SKYFLOW_ID_KEY.getLog());
```

and no `getRecord.remove("skyflow_id")`.

- [ ] **Step 3: Commit (if any final cleanup needed)**

```bash
git commit --allow-empty -m "chore: v2 backward compat + deprecation warnings complete"
```
