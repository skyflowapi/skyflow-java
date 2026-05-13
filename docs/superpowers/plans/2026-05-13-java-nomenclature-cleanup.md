# Java SDK Nomenclature Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename credential JSON field keys (`clientID`→`clientId`, `keyID`→`keyId`, `tokenURI`→`tokenUri`) with fallback support, normalise `skyflow_id`→`skyflowId` in Get and Query responses, and add `getErrors()` to `QueryResponse`.

**Architecture:** Three independent, targeted changes to existing files — no new files, no new abstractions. Each change is a surgical edit to one method or class, verified by unit tests that already exist or that we add inline.

**Tech Stack:** Java 11+, JUnit 4, Mockito/PowerMock, Maven (`mvn test`)

**Design spec:** `docs/superpowers/specs/2026-05-13-java-nomenclature-cleanup-design.md`

---

## File Map

| File | Change |
|---|---|
| `src/main/java/com/skyflow/serviceaccount/util/BearerToken.java` | Fallback lookup for `clientId`/`keyId`/`tokenUri` in `getBearerTokenFromCredentials` |
| `src/main/java/com/skyflow/serviceaccount/util/SignedDataTokens.java` | Fallback lookup for `clientId`/`keyId` in `generateSignedTokensFromCredentials` |
| `src/main/java/com/skyflow/vault/controller/VaultController.java` | Rename `skyflow_id`→`skyflowId` in `getFormattedGetRecord` and `getFormattedQueryRecord` |
| `src/main/java/com/skyflow/vault/data/QueryResponse.java` | Add `errors` field and `getErrors()` accessor |
| `src/test/java/com/skyflow/serviceaccount/util/BearerTokenTests.java` | Add tests for new-form keys, old-form fallback, and missing-key errors |
| `src/test/java/com/skyflow/serviceaccount/util/SignedDataTokensTests.java` | Add tests for new-form keys, old-form fallback, and missing-key errors |
| `src/test/java/com/skyflow/vault/data/QueryResponseTest.java` | New file — tests for `getErrors()` always returning null |

---

## Task 1: Credential field renames in BearerToken — new key form

**Files:**
- Modify: `src/main/java/com/skyflow/serviceaccount/util/BearerToken.java:92-145`
- Modify: `src/test/java/com/skyflow/serviceaccount/util/BearerTokenTests.java`

### Background
`getBearerTokenFromCredentials` parses a `JsonObject` representing the credentials file. It currently looks up `clientID`, `keyID`, and `tokenURI`. We need it to accept `clientId`, `keyId`, `tokenUri` (new canonical form) while still accepting the old keys as a fallback.

The test at line 228 of `BearerTokenTests.java` currently uses the old keys — we need a parallel test using the new keys.

- [ ] **Step 1: Write a failing test for new-form credential keys**

Add this test to `BearerTokenTests.java`. It uses a credentials string with `clientId`, `keyId`, `tokenUri` (new form) and expects a `SkyflowException` with the `InvalidTokenUri` message (because the URI value is invalid — not because the keys are unrecognised). This confirms the new keys are read successfully.

```java
@Test
public void testBearerTokenWithNewFormCredentialKeys() {
    try {
        String credentialsString = "{\"privateKey\": \"-----BEGIN PRIVATE KEY-----\\ncHJpdmF0ZV9rZXlfdmFsdWU=\\n-----END PRIVATE KEY-----\", "
                + "\"clientId\": \"client_id_value\", \"keyId\": \"key_id_value\", \"tokenUri\": \"invalid_token_uri\"}";
        BearerToken bearerToken = BearerToken.builder().setCredentials(credentialsString).build();
        bearerToken.getBearerToken();
        Assert.fail(EXCEPTION_NOT_THROWN);
    } catch (SkyflowException e) {
        Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        Assert.assertEquals(ErrorMessage.InvalidTokenUri.getMessage(), e.getMessage());
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```bash
mvn test -pl . -Dtest=BearerTokenTests#testBearerTokenWithNewFormCredentialKeys -q
```

Expected: FAIL — the test throws `MissingClientId` (because `clientId` is not found, only `clientID` is looked up).

- [ ] **Step 3: Update `getBearerTokenFromCredentials` in `BearerToken.java`**

Replace the three field lookups (lines 102–118) with fallback logic:

```java
JsonElement clientId = credentials.get("clientId");
if (clientId == null) clientId = credentials.get("clientID");
if (clientId == null) {
    LogUtil.printErrorLog(ErrorLogs.CLIENT_ID_IS_REQUIRED.getLog());
    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingClientId.getMessage());
}

JsonElement keyId = credentials.get("keyId");
if (keyId == null) keyId = credentials.get("keyID");
if (keyId == null) {
    LogUtil.printErrorLog(ErrorLogs.KEY_ID_IS_REQUIRED.getLog());
    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingKeyId.getMessage());
}

JsonElement tokenUri = credentials.get("tokenUri");
if (tokenUri == null) tokenUri = credentials.get("tokenURI");
if (tokenUri == null) {
    LogUtil.printErrorLog(ErrorLogs.TOKEN_URI_IS_REQUIRED.getLog());
    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingTokenUri.getMessage());
}
```

Also update the `getSignedToken` call on line 121 to use the renamed variables:

```java
String signedUserJWT = getSignedToken(
        clientId.getAsString(), keyId.getAsString(), tokenUri.getAsString(), pvtKey, context
);
String basePath = Utils.getBaseURL(tokenUri.getAsString());
```

Also update the private method signature at line 147–148 to use idiomatic parameter names (internal only, no public impact):

```java
private static String getSignedToken(
        String clientId, String keyId, String tokenUri, PrivateKey pvtKey, Object context
) {
    final Date createdDate = new Date();
    final Date expirationDate = new Date(createdDate.getTime() + (3600 * 1000));
    io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
            .claim("iss", clientId)
            .claim("key", keyId)
            .claim("aud", tokenUri)
            .claim("sub", clientId)
            .expiration(expirationDate);
    if (context != null) {
        builder.claim("ctx", context);
    }
    return builder.signWith(pvtKey, Jwts.SIG.RS256).compact();
}
```

- [ ] **Step 4: Run the new test to confirm it passes**

```bash
mvn test -pl . -Dtest=BearerTokenTests#testBearerTokenWithNewFormCredentialKeys -q
```

Expected: PASS — `clientId` is found, execution reaches `InvalidTokenUri`.

- [ ] **Step 5: Run the full BearerToken test suite to confirm no regressions**

```bash
mvn test -pl . -Dtest=BearerTokenTests -q
```

Expected: All existing tests pass (old-form keys `clientID`/`keyID`/`tokenURI` still work via fallback).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/skyflow/serviceaccount/util/BearerToken.java \
        src/test/java/com/skyflow/serviceaccount/util/BearerTokenTests.java
git commit -m "feat: accept clientId/keyId/tokenUri in BearerToken with fallback to old form"
```

---

## Task 2: Credential field renames in SignedDataTokens — new key form

**Files:**
- Modify: `src/main/java/com/skyflow/serviceaccount/util/SignedDataTokens.java:92-122`
- Modify: `src/test/java/com/skyflow/serviceaccount/util/SignedDataTokensTests.java`

### Background
`generateSignedTokensFromCredentials` parses a credentials `JsonObject` and looks up `clientID` and `keyID`. Same rename as Task 1, but no `tokenURI` (SignedDataTokens does not need it).

- [ ] **Step 1: Check what the existing SignedDataTokens test uses for credential keys**

```bash
grep -n "clientID\|keyID\|clientId\|keyId" src/test/java/com/skyflow/serviceaccount/util/SignedDataTokensTests.java
```

Note the line number of any credentials string that uses `clientID`/`keyID` — you will add a parallel test using the new keys.

- [ ] **Step 2: Write a failing test for new-form keys**

Add this test to `SignedDataTokensTests.java`. It expects the token generation to fail at the private key parsing stage (not at the field-lookup stage), confirming `clientId` and `keyId` are successfully read:

```java
@Test
public void testSignedDataTokensWithNewFormCredentialKeys() {
    try {
        String credentialsString = "{\"privateKey\": \"-----BEGIN PRIVATE KEY-----\\ncHJpdmF0ZV9rZXlfdmFsdWU=\\n-----END PRIVATE KEY-----\", "
                + "\"clientId\": \"client_id_value\", \"keyId\": \"key_id_value\"}";
        ArrayList<String> dataTokens = new ArrayList<>();
        dataTokens.add("test-token");
        SignedDataTokens signedDataTokens = SignedDataTokens.builder()
                .setCredentials(credentialsString)
                .setDataTokens(dataTokens)
                .build();
        signedDataTokens.getSignedDataTokens();
        Assert.fail(EXCEPTION_NOT_THROWN);
    } catch (SkyflowException e) {
        // Should fail past field lookup — at private key parsing, not at MissingClientId
        Assert.assertNotEquals(ErrorMessage.MissingClientId.getMessage(), e.getMessage());
        Assert.assertNotEquals(ErrorMessage.MissingKeyId.getMessage(), e.getMessage());
    }
}
```

- [ ] **Step 3: Run the test to confirm it fails**

```bash
mvn test -pl . -Dtest=SignedDataTokensTests#testSignedDataTokensWithNewFormCredentialKeys -q
```

Expected: FAIL — throws `MissingClientId` because `clientId` is not yet recognised.

- [ ] **Step 4: Update `generateSignedTokensFromCredentials` in `SignedDataTokens.java`**

Replace the `clientID` and `keyID` lookups (lines 103–113) with fallback logic:

```java
JsonElement clientId = credentials.get("clientId");
if (clientId == null) clientId = credentials.get("clientID");
if (clientId == null) {
    LogUtil.printErrorLog(ErrorLogs.CLIENT_ID_IS_REQUIRED.getLog());
    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingClientId.getMessage());
}

JsonElement keyId = credentials.get("keyId");
if (keyId == null) keyId = credentials.get("keyID");
if (keyId == null) {
    LogUtil.printErrorLog(ErrorLogs.KEY_ID_IS_REQUIRED.getLog());
    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MissingKeyId.getMessage());
}
```

Update the `getSignedToken` call on line 115 to use the renamed variables:

```java
signedDataTokens = getSignedToken(
        clientId.getAsString(), keyId.getAsString(), pvtKey, dataTokens, timeToLive, context);
```

Update the private method signature at line 124–125 to use idiomatic parameter names:

```java
private static List<SignedDataTokenResponse> getSignedToken(
        String clientId, String keyId, PrivateKey pvtKey,
        ArrayList<String> dataTokens, Integer timeToLive, Object context
) {
```

And update the JWT claims inside `getSignedToken` (lines 142–143):

```java
.claim("key", keyId)
.claim("sub", clientId)
```

- [ ] **Step 5: Run the new test to confirm it passes**

```bash
mvn test -pl . -Dtest=SignedDataTokensTests#testSignedDataTokensWithNewFormCredentialKeys -q
```

Expected: PASS — `clientId` and `keyId` are found; exception is from private key parsing, not from missing fields.

- [ ] **Step 6: Run the full SignedDataTokens test suite**

```bash
mvn test -pl . -Dtest=SignedDataTokensTests -q
```

Expected: All existing tests pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/skyflow/serviceaccount/util/SignedDataTokens.java \
        src/test/java/com/skyflow/serviceaccount/util/SignedDataTokensTests.java
git commit -m "feat: accept clientId/keyId in SignedDataTokens with fallback to old form"
```

---

## Task 3: Normalise `skyflow_id` → `skyflowId` in Get and Query responses

**Files:**
- Modify: `src/main/java/com/skyflow/vault/controller/VaultController.java:121-152`
- Modify: `src/test/java/com/skyflow/vault/controller/VaultControllerTests.java`

### Background
`getFormattedGetRecord` and `getFormattedQueryRecord` call `putAll(fieldsOpt.get())` which passes through the raw API map — including the `skyflow_id` snake_case key from the wire format. Insert and Update responses already use `skyflowId`. This inconsistency means callers must know which operation produced the response in order to read the record ID.

The test suite does not currently test the contents of Get or Query responses (no existing tests for `skyflowId` in these paths), so we add new unit tests.

Because the actual vault API is not called in unit tests (no mock infrastructure for it in `VaultControllerTests`), we test the formatter methods indirectly by verifying the behaviour of the public `get()` and `query()` methods throw the right validation errors — and we test the formatters directly via reflection, or we add a thin package-private helper.

The simplest approach: add package-private unit tests for the two static formatter methods directly.

- [ ] **Step 1: Write failing tests for the formatter methods**

Add these tests to `VaultControllerTests.java`:

```java
import com.skyflow.generated.rest.types.V1FieldRecords;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;

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

    Assert.assertFalse("skyflow_id (snake_case) should not be present", result.containsKey("skyflow_id"));
    Assert.assertEquals("skyflowId should be present", "abc-123", result.get("skyflowId"));
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

    Assert.assertFalse("skyflow_id (snake_case) should not be present", result.containsKey("skyflow_id"));
    Assert.assertEquals("skyflowId should be present", "xyz-456", result.get("skyflowId"));
    Assert.assertEquals("other fields should be preserved", "test@example.com", result.get("email"));
}
```

- [ ] **Step 2: Run the tests to confirm they fail**

```bash
mvn test -pl . -Dtest=VaultControllerTests#testGetFormattedGetRecordNormalisesSkyflowId+testGetFormattedQueryRecordNormalisesSkyflowId -q
```

Expected: FAIL — `skyflow_id` is present in the result, `skyflowId` is absent.

- [ ] **Step 3: Update `getFormattedGetRecord` in `VaultController.java`**

After the `putAll` block (after line 131), add the key rename:

```java
private static synchronized HashMap<String, Object> getFormattedGetRecord(V1FieldRecords record) {
    HashMap<String, Object> getRecord = new HashMap<>();

    Optional<Map<String, Object>> fieldsOpt = record.getFields();
    Optional<Map<String, Object>> tokensOpt = record.getTokens();

    if (fieldsOpt.isPresent()) {
        getRecord.putAll(fieldsOpt.get());
    } else if (tokensOpt.isPresent()) {
        getRecord.putAll(tokensOpt.get());
    }

    if (getRecord.containsKey("skyflow_id")) {
        getRecord.put("skyflowId", getRecord.remove("skyflow_id"));
    }

    return getRecord;
}
```

- [ ] **Step 4: Update `getFormattedQueryRecord` in `VaultController.java`**

After the `putAll` block (after line 150), add the key rename:

```java
private static synchronized HashMap<String, Object> getFormattedQueryRecord(V1FieldRecords record) {
    HashMap<String, Object> queryRecord = new HashMap<>();
    Optional<Map<String, Object>> fieldsOpt = record.getFields();
    if (fieldsOpt.isPresent()) {
        queryRecord.putAll(fieldsOpt.get());
    }

    if (queryRecord.containsKey("skyflow_id")) {
        queryRecord.put("skyflowId", queryRecord.remove("skyflow_id"));
    }

    return queryRecord;
}
```

- [ ] **Step 5: Run the new tests to confirm they pass**

```bash
mvn test -pl . -Dtest=VaultControllerTests#testGetFormattedGetRecordNormalisesSkyflowId+testGetFormattedQueryRecordNormalisesSkyflowId -q
```

Expected: PASS.

- [ ] **Step 6: Run the full VaultController test suite**

```bash
mvn test -pl . -Dtest=VaultControllerTests -q
```

Expected: All existing tests pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/skyflow/vault/controller/VaultController.java \
        src/test/java/com/skyflow/vault/controller/VaultControllerTests.java
git commit -m "feat: normalise skyflow_id to skyflowId in Get and Query response maps"
```

---

## Task 4: Add `getErrors()` to `QueryResponse`

**Files:**
- Modify: `src/main/java/com/skyflow/vault/data/QueryResponse.java`
- Create: `src/test/java/com/skyflow/vault/data/QueryResponseTest.java`

### Background
`QueryResponse` is the only response class without a `getErrors()` method. The field is referenced in `toString()` as a hardcoded `null` literal but is not accessible programmatically. We add the field and accessor to match the pattern in `GetResponse`, `InsertResponse`, and `UpdateResponse` (all return `null` when no errors).

- [ ] **Step 1: Write a failing test**

Create `src/test/java/com/skyflow/vault/data/QueryResponseTest.java`:

```java
package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

public class QueryResponseTest {

    @Test
    public void testGetErrorsReturnsNull() {
        ArrayList<HashMap<String, Object>> fields = new ArrayList<>();
        HashMap<String, Object> record = new HashMap<>();
        record.put("skyflowId", "abc-123");
        fields.add(record);

        QueryResponse response = new QueryResponse(fields);

        Assert.assertNull("getErrors() should return null when no errors", response.getErrors());
    }

    @Test
    public void testGetErrorsIsPresentInToString() {
        QueryResponse response = new QueryResponse(new ArrayList<>());
        String json = response.toString();
        Assert.assertTrue("toString() should include errors field", json.contains("\"errors\""));
    }
}
```

- [ ] **Step 2: Run the tests to confirm they fail**

```bash
mvn test -pl . -Dtest=QueryResponseTest -q
```

Expected: FAIL — compile error: `getErrors()` method does not exist on `QueryResponse`.

- [ ] **Step 3: Update `QueryResponse.java`**

Add the `errors` field and accessor. The `toString()` no longer needs to manually inject `errors` since `serializeNulls` will include it automatically:

```java
package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;

public class QueryResponse {
    private final ArrayList<HashMap<String, Object>> fields;
    private final ArrayList<HashMap<String, Object>> errors;

    public QueryResponse(ArrayList<HashMap<String, Object>> fields) {
        this.fields = fields;
        this.errors = null;
    }

    public ArrayList<HashMap<String, Object>> getFields() {
        return fields;
    }

    public ArrayList<HashMap<String, Object>> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        JsonObject responseObject = gson.toJsonTree(this).getAsJsonObject();
        // tokenizedData is intentionally not surfaced — Query API cannot return tokens
        JsonArray fieldsArray = responseObject.get("fields").getAsJsonArray();
        for (JsonElement fieldElement : fieldsArray) {
            fieldElement.getAsJsonObject().add("tokenizedData", new JsonObject());
        }
        return responseObject.toString();
    }
}
```

- [ ] **Step 4: Run the new tests to confirm they pass**

```bash
mvn test -pl . -Dtest=QueryResponseTest -q
```

Expected: PASS.

- [ ] **Step 5: Run the full test suite to confirm no regressions**

```bash
mvn test -q
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/skyflow/vault/data/QueryResponse.java \
        src/test/java/com/skyflow/vault/data/QueryResponseTest.java
git commit -m "feat: add getErrors() accessor to QueryResponse"
```

---

## Task 5: LOW audit — verify no `setFooID` / `getFooID` violations

**Files:**
- Read-only audit (no changes expected)

### Background
The spec requires confirming that all builder setter/getter methods use `setFooId()` / `getFooId()` (title-case `Id`), not `setFooID()`. Initial review of `VaultConfig` already shows `setVaultId()` and `setClusterId()` are correct. This task confirms nothing was missed.

- [ ] **Step 1: Run the grep audit**

```bash
grep -rn "set[A-Za-z]*ID\b\|get[A-Za-z]*ID\b" \
  src/main/java/com/skyflow/config/ \
  src/main/java/com/skyflow/vault/data/ \
  src/main/java/com/skyflow/serviceaccount/ \
  --include="*.java"
```

Expected output: **no results** — all methods already use title-case `Id`.

- [ ] **Step 2: If violations are found, rename them**

For each violation (e.g. `setVaultID` → `setVaultId`), use your editor's rename refactor across all callers, then run:

```bash
mvn test -q
```

Expected: All tests pass.

- [ ] **Step 3: Commit (only if changes were made)**

```bash
git add -p
git commit -m "fix: rename setFooID/getFooID to setFooId/getFooId per Java convention"
```

If no violations were found, record the result:

```bash
git commit --allow-empty -m "chore: audit confirms no setFooID/getFooID violations in public API"
```

---

## Final verification

- [ ] **Run the complete test suite one last time**

```bash
mvn test -q
```

Expected: All tests pass with no failures or errors.
