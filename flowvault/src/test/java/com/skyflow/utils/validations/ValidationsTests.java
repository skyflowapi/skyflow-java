package com.skyflow.utils.validations;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkInsertRequestRecord;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.TokenizeRequestRecord;
import com.skyflow.vault.data.TokenizeRequest;
import com.skyflow.vault.data.UpsertOptions;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationsTests {
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";

    // Tests for validateTokenizeRequest / validateDeleteTokensRequest were removed:
    // those unary validators no longer exist (bulk-only module).

    // ── validateCredentials ───────────────────────────────────────────────────

    @Test
    public void testValidateCredentials_validToken() {
        Credentials credentials = new Credentials();
        credentials.setToken("some-token");
        try {
            Validations.validateCredentials(credentials);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateCredentials_validApiKey() {
        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        try {
            Validations.validateCredentials(credentials);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateCredentials_invalidApiKeyFormat() {
        Credentials credentials = new Credentials();
        credentials.setApiKey("not-a-valid-api-key");
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_noAuthMeansPassed() {
        Credentials credentials = new Credentials();
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_multipleAuthMeansPassed() {
        Credentials credentials = new Credentials();
        credentials.setToken("some-token");
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_emptyRoles() {
        Credentials credentials = new Credentials();
        credentials.setToken("some-token");
        credentials.setRoles(new ArrayList<>());
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_blankEntryInRoles() {
        Credentials credentials = new Credentials();
        credentials.setToken("some-token");
        ArrayList<String> roles = new ArrayList<>();
        roles.add("validRole");
        roles.add("   ");
        credentials.setRoles(roles);
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_emptyPath() {
        Credentials credentials = new Credentials();
        credentials.setPath("");
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_emptyCredentialsString() {
        Credentials credentials = new Credentials();
        credentials.setCredentialsString("");
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_emptyToken() {
        Credentials credentials = new Credentials();
        credentials.setToken("");
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_emptyStringContext() {
        Credentials credentials = new Credentials();
        credentials.setToken("some-token");
        credentials.setContext("");
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_emptyMapContext() {
        Credentials credentials = new Credentials();
        credentials.setToken("some-token");
        credentials.setContext(new HashMap<>());
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_invalidMapKeyContext() {
        Credentials credentials = new Credentials();
        credentials.setToken("some-token");
        Map<String, Object> context = new HashMap<>();
        context.put("invalid-key!", "value");
        credentials.setContext(context);
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateCredentials_invalidContextType() throws Exception {
        Credentials credentials = new Credentials();
        credentials.setToken("some-token");
        Field contextField = credentials.getClass().getSuperclass().getDeclaredField("context");
        contextField.setAccessible(true);
        contextField.set(credentials, Integer.valueOf(5));
        try {
            Validations.validateCredentials(credentials);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── validateVaultConfiguration ────────────────────────────────────────────

    @Test
    public void testValidateVaultConfiguration_nullVaultId() {
        VaultConfig config = new VaultConfig();
        config.setClusterId("cluster1");
        try {
            Validations.validateVaultConfiguration(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateVaultConfiguration_emptyVaultId() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("   ");
        config.setClusterId("cluster1");
        try {
            Validations.validateVaultConfiguration(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateVaultConfiguration_neitherVaultUrlNorClusterId() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        try {
            Validations.validateVaultConfiguration(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateVaultConfiguration_emptyClusterId() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setClusterId("   ");
        try {
            Validations.validateVaultConfiguration(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateVaultConfiguration_emptyVaultUrl() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setClusterId("cluster1");
        config.setVaultUrl("   ");
        try {
            Validations.validateVaultConfiguration(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateVaultConfiguration_invalidVaultUrlFormat() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setVaultUrl("http://not-https.example.com");
        try {
            Validations.validateVaultConfiguration(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateVaultConfiguration_validWithClusterId() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setClusterId("cluster1");
        config.setEnv(Env.DEV);
        try {
            Validations.validateVaultConfiguration(config);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateVaultConfiguration_validWithVaultUrl() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setVaultUrl("https://myvault.example.com");
        try {
            Validations.validateVaultConfiguration(config);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateVaultConfiguration_delegatesToCredentialValidation() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setClusterId("cluster1");
        Credentials credentials = new Credentials();
        credentials.setApiKey("not-a-valid-api-key");
        config.setCredentials(credentials);
        try {
            Validations.validateVaultConfiguration(config);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ── validateInsertRequest ─────────────────────────────────────────────────

    @Test
    public void testValidateInsertRequest_nullRequest() {
        try {
            Validations.validateInsertRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_nullRecords() {
        InsertRequest request = InsertRequest.builder().records(null).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_emptyRecords() {
        InsertRequest request = InsertRequest.builder().records(new ArrayList<>()).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_nullRecordInList() {
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(null);
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // NOTE: a table name may now be supplied at either the record level or the request level.
    // EmptyTable is thrown only when it is missing from both. Specifying it at both levels is
    // currently accepted (no conflict error) — that decision is deliberately deferred. Likewise
    // the old "upsert present at record/request level requires table at the other level" checks are
    // gone, since upsert is no longer coupled to where the table is specified — it's just an
    // UpsertOptions object validated the same way at either level (see the upsert tests below).

    @Test
    public void testValidateInsertRequest_missingTableNameInRecordThrowsEmptyTable() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_emptyTableNameInRecordThrowsEmptyTable() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("   ").data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_requestLevelTableNameSatisfiesRecordsWithoutOne() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().data(data).build());
        InsertRequest request = InsertRequest.builder().tableName("table1").records(records).build();
        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateInsertRequest_emptyUpsertAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").data(data).build());
        UpsertOptions upsert = UpsertOptions.builder().uniqueColumns(new ArrayList<>()).build();
        InsertRequest request = InsertRequest.builder()
                .records(records)
                .upsert(upsert)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_emptyUpsertAtRecordLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        UpsertOptions upsert = UpsertOptions.builder().uniqueColumns(new ArrayList<>()).build();
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").data(data).upsert(upsert).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_emptyOrNullKeyInData() {
        Map<String, Object> data = new HashMap<>();
        data.put("", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_emptyOrNullValueInData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_validRequest() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateInsertRequest_validRequestWithUpsertAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().data(data).build());
        UpsertOptions upsert = UpsertOptions.builder()
                .uniqueColumns(Collections.singletonList("email"))
                .updateType("UPDATE")
                .build();
        InsertRequest request = InsertRequest.builder()
                .tableName("table1")
                .records(records)
                .upsert(upsert)
                .build();
        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateInsertRequest_validRequestWithUpsertAtRecordLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        UpsertOptions upsert = UpsertOptions.builder()
                .uniqueColumns(Collections.singletonList("email"))
                .updateType("REPLACE")
                .build();
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").data(data).upsert(upsert).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateInsertRequest_validRequestWithTokens() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").tokens(tokens).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // ── validateDetokenizeRequest ─────────────────────────────────────────────

    @Test
    public void testValidateDetokenizeRequest_nullRequest() {
        try {
            Validations.validateDetokenizeRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_nullTokens() {
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(null).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_emptyTokens() {
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(new ArrayList<>()).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_nullTokenInList() {
        List<String> tokens = new ArrayList<>();
        tokens.add(null);
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_blankTokenInList() {
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(Collections.singletonList("   "))
                .build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_nullTokenGroupRedactionInList() {
        List<TokenGroupRedactions> groupRedactions = new ArrayList<>();
        groupRedactions.add(null);
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .tokenGroupRedactions(groupRedactions)
                .build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_blankTokenGroupNameInRedaction() {
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("   ").redaction("MASKED").build());
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .tokenGroupRedactions(groupRedactions)
                .build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_blankRedactionInGroup() {
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("   ").build());
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .tokenGroupRedactions(groupRedactions)
                .build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_validRequest() {
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();
        try {
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateDetokenizeRequest_validRequestWithTokenGroupRedactions() {
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASKED").build());
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .tokenGroupRedactions(groupRedactions)
                .build();
        try {
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // ── validateBulkInsertRequest ──────────────────────────────────────────────

    @Test
    public void testValidateBulkInsertRequest_nullRequest() {
        try {
            Validations.validateBulkInsertRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_nullRecords() {
        BulkInsertRequest request = BulkInsertRequest.builder().records(null).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_emptyRecords() {
        BulkInsertRequest request = BulkInsertRequest.builder().records(new ArrayList<>()).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_nullRecordInList() {
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(null);
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // Removed: testValidateBulkInsertRequest_tableSpecifiedAtBothPlaces — bulk insert now
    // delegates to validateInsertRequest, which deliberately accepts a table name at both
    // the request and record level (TableSpecifiedInRequestAndRecordObject is no longer thrown).

    // Removed: testValidateBulkInsertRequest_upsertAtRecordLevelWhenTableAtRequestLevel and
    // testValidateBulkInsertRequest_upsertAtRequestLevelWhenNoTableAtRequestLevel — upsert is
    // now an UpsertOptions object accepted at either level, so UpsertTableRequestAtRecordLevel /
    // UpsertTableRequestAtRequestLevel are no longer thrown for insert.

    @Test
    public void testValidateBulkInsertRequest_tableNotSpecifiedAnywhereThrowsEmptyTable() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_requestLevelTableNameSatisfiesRecordsWithoutOne() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().tableName("table1").records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkInsertRequest_tableNameAtBothLevelsThrows() {
        // Table name must live at exactly one level, never both.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().tableName("table1").records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TableSpecifiedInRequestAndRecordObject.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_emptyUpsertAtRecordLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder()
                .tableName("table1")
                .data(data)
                .upsert(UpsertOptions.builder().updateType("UPDATE").build())
                .build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyUpsertValues.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_emptyUpsertAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder()
                .records(records)
                .upsert(UpsertOptions.builder().uniqueColumns(new ArrayList<>()).build())
                .build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyUpsertValues.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_emptyKeyInData() {
        Map<String, Object> data = new HashMap<>();
        data.put("", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_emptyValueInData() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_validRequest() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkInsertRequest_validRequestWithUpsertAndTokens() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "tok-abc");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder()
                .tableName("table1")
                .data(data)
                .tokens(tokens)
                .upsert(UpsertOptions.builder()
                        .updateType("REPLACE")
                        .uniqueColumns(Collections.singletonList("email"))
                        .build())
                .build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkInsertRequest_over10000RecordsThrows() {
        // Constants.MAX_BULK_DATA_SIZE is a hard ceiling; batching splits the payload but
        // does not lift it.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        }
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.RecordSizeExceedError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_exactly10000RecordsIsValid() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            records.add(BulkInsertRequestRecord.builder().tableName("table1").data(data).build());
        }
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkDetokenizeRequest_over10000TokensThrows() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            tokens.add("token-" + i);
        }
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().tokens(tokens).build();
        try {
            Validations.validateBulkDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TokensSizeExceedError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDeleteTokensRequest_over10000TokensThrows() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            tokens.add("token-" + i);
        }
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(tokens).build();
        try {
            Validations.validateBulkDeleteTokensRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.DeleteTokensSizeExceedError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_over10000RecordsThrows() {
        ArrayList<BulkTokenizeRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            records.add(BulkTokenizeRequestRecord.builder()
                    .value("value-" + i)
                    .tokenGroupNames(Collections.singletonList("group"))
                    .build());
        }
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(records).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TokenizeDataSizeExceedError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_unrecognizedUpdateTypeThrows() {
        // Previously this was silently dropped during mapping; it is now rejected up front.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().data(data).build());
        InsertRequest request = InsertRequest.builder()
                .tableName("table1")
                .records(records)
                .upsert(UpsertOptions.builder()
                        .uniqueColumns(Collections.singletonList("email"))
                        .updateType("MERGE")
                        .build())
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.InvalidUpsertUpdateType.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_updateTypeWithStrayWhitespaceThrows() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().data(data).build());
        InsertRequest request = InsertRequest.builder()
                .tableName("table1")
                .records(records)
                .upsert(UpsertOptions.builder()
                        .uniqueColumns(Collections.singletonList("email"))
                        .updateType("update ")
                        .build())
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.InvalidUpsertUpdateType.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_nullUpdateTypeIsValid() {
        // updateType is optional; only a non-null unrecognized value is rejected.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().data(data).build());
        InsertRequest request = InsertRequest.builder()
                .tableName("table1")
                .records(records)
                .upsert(UpsertOptions.builder().uniqueColumns(Collections.singletonList("email")).build())
                .build();
        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkInsertRequest_plainInsertRequestRecordRejected() {
        // `records` is inherited as List<InsertRequestRecord>, so a plain unary record is
        // accepted by the compiler. Bulk must reject it, otherwise getRecordsToRetry() would
        // hit a ClassCastException later.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.InvalidRecord.getMessage(), e.getMessage());
        }
    }

    // ── validateBulkInsertRequest: table-name presence rule ────────────────────
    //
    // The rule is: a table name must be present at the request level OR on EVERY record.
    // "Present" means non-null and non-blank at both levels (Validations trims before testing,
    // and Utils.hasText applies the same test when mapping to the wire), so a blank record-level
    // name is treated as absent and satisfied by the request-level one.

    @Test
    public void testValidateBulkInsertRequest_requestLevelTableNameSatisfiesBlankRecordTableNames() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("   ").data(data).build());
        records.add(BulkInsertRequestRecord.builder().tableName("").data(data).build());
        records.add(BulkInsertRequestRecord.builder().data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().tableName("cards").records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkInsertRequest_noRequestLevelTableNameButEveryRecordHasOneIsValid() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("cards").data(data).build());
        records.add(BulkInsertRequestRecord.builder().tableName("accounts").data(data).build());
        records.add(BulkInsertRequestRecord.builder().tableName("people").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkInsertRequest_noRequestLevelTableNameAndOneRecordMissingThrowsEmptyTable() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("cards").data(data).build());
        records.add(BulkInsertRequestRecord.builder().data(data).build());
        records.add(BulkInsertRequestRecord.builder().tableName("people").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_noRequestLevelTableNameAndOneRecordBlankThrowsEmptyTable() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().tableName("cards").data(data).build());
        records.add(BulkInsertRequestRecord.builder().tableName("   ").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_blankRequestLevelTableNameDoesNotSatisfyRecords() {
        // A blank request-level table name is treated as absent too, so records must supply one.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(BulkInsertRequestRecord.builder().data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().tableName("   ").records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_upsertAtRequestLevelWithTableAtRecordLevelThrows() {
        // upsert must sit at the same level as the table name.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").data(data).build());
        InsertRequest request = InsertRequest.builder()
                .records(records)
                .upsert(UpsertOptions.builder().uniqueColumns(Collections.singletonList("email")).build())
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.UpsertTableRequestAtRequestLevel.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_upsertAtRecordLevelWithTableAtRequestLevelThrows() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder()
                .data(data)
                .upsert(UpsertOptions.builder().uniqueColumns(Collections.singletonList("email")).build())
                .build());
        InsertRequest request = InsertRequest.builder().tableName("table1").records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.UpsertTableRequestAtRecordLevel.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_upsertOnSomeRecordsOnlyIsValid() {
        // Upsert is optional per record; it only has to sit at the same level as the table name.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder()
                .tableName("table1")
                .data(data)
                .upsert(UpsertOptions.builder().uniqueColumns(Collections.singletonList("email")).build())
                .build());
        records.add(InsertRequestRecord.builder().tableName("table1").data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateInsertRequest_upsertOnEveryRecordWithTableOnEveryRecordIsValid() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            records.add(InsertRequestRecord.builder()
                    .tableName("table1")
                    .data(data)
                    .upsert(UpsertOptions.builder().uniqueColumns(Collections.singletonList("email")).build())
                    .build());
        }
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateInsertRequest_noUpsertAnywhereIsValid() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().data(data).build());
        InsertRequest request = InsertRequest.builder().tableName("table1").records(records).build();
        try {
            Validations.validateInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // ── validateBulkDetokenizeRequest ──────────────────────────────────────────

    @Test
    public void testValidateBulkDetokenizeRequest_nullRequest() {
        try {
            Validations.validateBulkDetokenizeRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDetokenizeRequest_nullTokens() {
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().tokens(null).build();
        try {
            Validations.validateBulkDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDetokenizeRequest_emptyTokens() {
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().tokens(new ArrayList<>()).build();
        try {
            Validations.validateBulkDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDetokenizeRequest_nullTokenInList() {
        List<String> tokens = new ArrayList<>();
        tokens.add(null);
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder().tokens(tokens).build();
        try {
            Validations.validateBulkDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDetokenizeRequest_nullRedactionGroupObject() {
        List<TokenGroupRedactions> groupRedactions = new ArrayList<>();
        groupRedactions.add(null);
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .tokenGroupRedactions(groupRedactions)
                .build();
        try {
            Validations.validateBulkDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDetokenizeRequest_nullGroupNameInRedaction() {
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(
                TokenGroupRedactions.builder().redaction("MASKED").build());
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .tokenGroupRedactions(groupRedactions)
                .build();
        try {
            Validations.validateBulkDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDetokenizeRequest_nullRedactionInGroup() {
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").build());
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .tokenGroupRedactions(groupRedactions)
                .build();
        try {
            Validations.validateBulkDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDetokenizeRequest_validRequest() {
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();
        try {
            Validations.validateBulkDetokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkDetokenizeRequest_validRequestWithRedactions() {
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASKED").build());
        BulkDetokenizeRequest request = BulkDetokenizeRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .tokenGroupRedactions(groupRedactions)
                .build();
        try {
            Validations.validateBulkDetokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // ── validateBulkDeleteTokensRequest ────────────────────────────────────────

    @Test
    public void testValidateBulkDeleteTokensRequest_nullRequest() {
        try {
            Validations.validateBulkDeleteTokensRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDeleteTokensRequest_emptyTokens() {
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder().tokens(new ArrayList<>()).build();
        try {
            Validations.validateBulkDeleteTokensRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDeleteTokensRequest_emptyTokenInList() {
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder()
                .tokens(Arrays.asList("token1", "   "))
                .build();
        try {
            Validations.validateBulkDeleteTokensRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkDeleteTokensRequest_validRequest() {
        BulkDeleteTokensRequest request = BulkDeleteTokensRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();
        try {
            Validations.validateBulkDeleteTokensRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // ── validateBulkTokenizeRequest ────────────────────────────────────────────

    @Test
    public void testValidateBulkTokenizeRequest_nullRequest() {
        try {
            Validations.validateBulkTokenizeRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_emptyData() {
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(new ArrayList<>()).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_nullRecordInList() {
        ArrayList<BulkTokenizeRequestRecord> data = new ArrayList<>();
        data.add(null);
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_emptyValue() {
        ArrayList<BulkTokenizeRequestRecord> data = new ArrayList<>();
        data.add(BulkTokenizeRequestRecord.builder().value("   ").build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_emptyGroupNameInList() {
        ArrayList<BulkTokenizeRequestRecord> data = new ArrayList<>();
        data.add(BulkTokenizeRequestRecord.builder().value("value1").tokenGroupNames(Arrays.asList("group1", "  ")).build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_validRequestWithTokenGroupNames() {
        ArrayList<BulkTokenizeRequestRecord> data = new ArrayList<>();
        data.add(BulkTokenizeRequestRecord.builder().value("value1").tokenGroupNames(Collections.singletonList("group1")).build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_validRequestWithoutTokenGroupNames() {
        // Unlike the non-bulk tokenize validator, tokenGroupNames is optional for bulk requests.
        ArrayList<BulkTokenizeRequestRecord> data = new ArrayList<>();
        data.add(BulkTokenizeRequestRecord.builder().value("value1").build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_nonSequentialIndexesAccepted() {
        // indexes only need to be present and unique - gaps and ordering are the caller's business
        ArrayList<BulkTokenizeRequestRecord> data = new ArrayList<>();
        data.add(BulkTokenizeRequestRecord.builder().value("value1").build());
        data.add(BulkTokenizeRequestRecord.builder().value("value2").build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().records(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // Tests for validateQueryRequest / validateGetRequest were removed:
    // those unary validators no longer exist (bulk-only module).

}
