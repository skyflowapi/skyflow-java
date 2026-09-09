package com.skyflow.utils.validations;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.UpdateType;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertRequestRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.ColumnRedactions;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetRequestRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateRequestRecord;
import com.skyflow.vault.data.UpsertOptions;

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
    public void testValidateVaultConfiguration_nullRequest() {
        try {
            Validations.validateVaultConfiguration(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.NullVaultConfig.getMessage(), e.getMessage());
        }
    }

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

    @Test
    public void testValidateInsertRequest_emptyTokensMapThrows() {
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").tokens(new HashMap<>()).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_nullKeyInTokensThrows() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put(null, "tok-abc");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").tokens(tokens).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyKeyInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_blankKeyInTokensThrows() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("   ", "tok-abc");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").tokens(tokens).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyKeyInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_nullValueInTokensThrows() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", null);
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").tokens(tokens).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyValueInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_blankValueInTokensThrows() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "   ");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName("table1").tokens(tokens).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyValueInTokens.getMessage(), e.getMessage());
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
    public void testValidateBulkInsertRequest_overMaxBulkDataSizeRecordsThrows() {
        // Constants.MAX_BULK_DATA_SIZE is a hard ceiling; batching splits the payload but
        // does not lift it.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_BULK_DATA_SIZE + 1; i++) {
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
    public void testValidateBulkInsertRequest_exactlyMaxBulkDataSizeRecordsIsValid() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_BULK_DATA_SIZE; i++) {
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
    public void testValidateBulkDetokenizeRequest_overMaxBulkDataSizeTokensThrows() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_BULK_DATA_SIZE + 1; i++) {
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
    public void testValidateBulkDeleteTokensRequest_overMaxBulkDataSizeTokensThrows() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_BULK_DATA_SIZE + 1; i++) {
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
    public void testValidateBulkTokenizeRequest_overMaxBulkDataSizeRecordsThrows() {
        ArrayList<BulkTokenizeRequestRecord> records = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_BULK_DATA_SIZE + 1; i++) {
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

    // ── validateQueryRequest ──────────────────────────────────────────────────

    @Test
    public void testValidateQueryRequest_nullRequestThrows() {
        try {
            Validations.validateQueryRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.QueryRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateQueryRequest_nullQueryThrows() {
        QueryRequest request = QueryRequest.builder().build();
        try {
            Validations.validateQueryRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.QueryKeyError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateQueryRequest_blankQueryThrows() {
        QueryRequest request = QueryRequest.builder().query("   ").build();
        try {
            Validations.validateQueryRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyQuery.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateQueryRequest_validRequestDoesNotThrow() {
        QueryRequest request = QueryRequest.builder().query("SELECT * FROM table1").build();
        try {
            Validations.validateQueryRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // ── validateDeleteRequest ─────────────────────────────────────────────────

    @Test
    public void testValidateDeleteRequest_nullRequestThrows() {
        try {
            Validations.validateDeleteRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.DeleteRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteRequest_nullTableThrows() {
        DeleteRequest request = DeleteRequest.builder().skyflowIds(Collections.singletonList("id1")).build();
        try {
            Validations.validateDeleteRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TableKeyError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteRequest_blankTableThrows() {
        DeleteRequest request = DeleteRequest.builder().tableName("   ").skyflowIds(Collections.singletonList("id1")).build();
        try {
            Validations.validateDeleteRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyTable.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteRequest_neitherIdsNorUniqueValuesThrows() {
        DeleteRequest request = DeleteRequest.builder().tableName("table1").build();
        try {
            Validations.validateDeleteRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.IdsOrUniqueValuesKeyError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteRequest_bothIdsAndUniqueValuesThrows() {
        Map<String, Object> uniqueValue = new HashMap<>();
        uniqueValue.put("email", "john@example.com");
        DeleteRequest request = DeleteRequest.builder()
                .tableName("table1")
                .skyflowIds(Collections.singletonList("id1"))
                .uniqueValues(Collections.singletonList(uniqueValue))
                .build();
        try {
            Validations.validateDeleteRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.BothIdsAndUniqueValuesSpecified.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteRequest_blankIdInIdsThrows() {
        DeleteRequest request = DeleteRequest.builder().tableName("table1").skyflowIds(Arrays.asList("id1", "  ")).build();
        try {
            Validations.validateDeleteRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyIdInIds.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteRequest_emptyUniqueValueInUniqueValuesThrows() {
        DeleteRequest request = DeleteRequest.builder()
                .tableName("table1")
                .uniqueValues(Collections.singletonList(new HashMap<>()))
                .build();
        try {
            Validations.validateDeleteRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyUniqueValueInUniqueValues.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteRequest_validWithIdsDoesNotThrow() {
        DeleteRequest request = DeleteRequest.builder().tableName("table1").skyflowIds(Collections.singletonList("id1")).build();
        try {
            Validations.validateDeleteRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateDeleteRequest_validWithUniqueValuesDoesNotThrow() {
        Map<String, Object> uniqueValue = new HashMap<>();
        uniqueValue.put("email", "john@example.com");
        DeleteRequest request = DeleteRequest.builder()
                .tableName("table1")
                .uniqueValues(Collections.singletonList(uniqueValue))
                .build();
        try {
            Validations.validateDeleteRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // ── validateUpdateRequest ─────────────────────────────────────────────────

    @Test
    public void testValidateUpdateRequest_nullRequestThrows() {
        try {
            Validations.validateUpdateRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.UpdateRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_nullTableNameThrows() {
        UpdateRequestRecord record = UpdateRequestRecord.builder().skyflowId("sky1").build();
        UpdateRequest request = UpdateRequest.builder().records(Collections.singletonList(record)).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TableKeyError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_blankTableNameThrows() {
        UpdateRequestRecord record = UpdateRequestRecord.builder().skyflowId("sky1").build();
        UpdateRequest request = UpdateRequest.builder().tableName("  ").records(Collections.singletonList(record)).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyTable.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_nullRecordsThrows() {
        UpdateRequest request = UpdateRequest.builder().tableName("table1").build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.RecordsKeyError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_emptyRecordsThrows() {
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(new ArrayList<>()).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyRecords.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_nullRecordInListThrows() {
        List<UpdateRequestRecord> records = new ArrayList<>();
        records.add(null);
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(records).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.UpdateRecordNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_nullSkyflowIdThrows() {
        UpdateRequestRecord record = UpdateRequestRecord.builder().build();
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(Collections.singletonList(record)).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.RecordSkyflowIdKeyError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_blankSkyflowIdThrows() {
        UpdateRequestRecord record = UpdateRequestRecord.builder().skyflowId("  ").build();
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(Collections.singletonList(record)).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptySkyflowIdInRecord.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_blankKeyInDataThrows() {
        Map<String, Object> data = new HashMap<>();
        data.put("  ", "value1");
        UpdateRequestRecord record = UpdateRequestRecord.builder().skyflowId("sky1").data(data).build();
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(Collections.singletonList(record)).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyKeyInRecords.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_blankValueInDataThrows() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "  ");
        UpdateRequestRecord record = UpdateRequestRecord.builder().skyflowId("sky1").data(data).build();
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(Collections.singletonList(record)).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyValueInValues.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_emptyTokensMapThrows() {
        UpdateRequestRecord record = UpdateRequestRecord.builder().skyflowId("sky1").tokens(new HashMap<>()).build();
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(Collections.singletonList(record)).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_blankKeyInTokensThrows() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("  ", "tok-abc");
        UpdateRequestRecord record = UpdateRequestRecord.builder().skyflowId("sky1").tokens(tokens).build();
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(Collections.singletonList(record)).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyKeyInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_blankValueInTokensThrows() {
        Map<String, Object> tokens = new HashMap<>();
        tokens.put("name", "  ");
        UpdateRequestRecord record = UpdateRequestRecord.builder().skyflowId("sky1").tokens(tokens).build();
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(Collections.singletonList(record)).build();
        try {
            Validations.validateUpdateRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyValueInTokens.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateUpdateRequest_validMinimalRequestDoesNotThrow() {
        UpdateRequestRecord record = UpdateRequestRecord.builder().skyflowId("sky1").build();
        UpdateRequest request = UpdateRequest.builder().tableName("table1").records(Collections.singletonList(record)).build();
        try {
            Validations.validateUpdateRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateUpdateRequest_validWithReplaceUpdateTypeAndRecordTableNameDoesNotThrow() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "jane");
        UpdateRequestRecord record = UpdateRequestRecord.builder()
                .skyflowId("sky1")
                .data(data)
                .tableName("table2")
                .build();
        UpdateRequest request = UpdateRequest.builder()
                .tableName("table1")
                .records(Collections.singletonList(record))
                .updateType(UpdateType.REPLACE)
                .build();
        try {
            Validations.validateUpdateRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // ── validateGetRequest ────────────────────────────────────────────────────

    @Test
    public void testValidateGetRequest_nullRequestThrows() {
        try {
            Validations.validateGetRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.GetRequestNull.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_bothSingleTableFieldsAndRecordsThrows() {
        GetRequestRecord nestedRecord = GetRequestRecord.builder().tableName("table2").skyflowIds(Collections.singletonList("id1")).build();
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Collections.singletonList("id1")))
                .records(Collections.singletonList(nestedRecord))
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.BothSingleTableFieldsAndRecordsSpecified.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_nullRecordInRecordsListThrows() {
        List<GetRequestRecord> records = new ArrayList<>();
        records.add(null);
        GetRequest request = GetRequest.builder().records(records).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.NullGetRecordRequest.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_nullTableThrows() {
        GetRequest request = GetRequest.builder().skyflowIds(new ArrayList<>(Collections.singletonList("id1"))).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TableKeyError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_blankTableThrows() {
        GetRequest request = GetRequest.builder().tableName("  ").skyflowIds(new ArrayList<>(Collections.singletonList("id1"))).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyTable.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_neitherIdsNorUniqueValuesThrows() {
        GetRequest request = GetRequest.builder().tableName("table1").build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.IdsOrUniqueValuesKeyError.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_bothIdsAndUniqueValuesThrows() {
        Map<String, Object> uniqueValue = new HashMap<>();
        uniqueValue.put("email", "john@example.com");
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Collections.singletonList("id1")))
                .uniqueValues(Collections.singletonList(uniqueValue))
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.BothIdsAndUniqueValuesSpecified.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_blankIdInIdsThrows() {
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Arrays.asList("id1", "  ")))
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyIdInIds.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_emptyUniqueValueInUniqueValuesThrows() {
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .uniqueValues(Collections.singletonList(new HashMap<>()))
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyUniqueValueInUniqueValues.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_emptyFieldsThrows() {
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Collections.singletonList("id1")))
                .columns(new ArrayList<>())
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyFields.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_blankFieldInFieldsThrows() {
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Collections.singletonList("id1")))
                .columns(new ArrayList<>(Arrays.asList("name", "  ")))
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.EmptyFieldInFields.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_nullColumnRedactionInListThrows() {
        List<ColumnRedactions> columnRedactions = new ArrayList<>();
        columnRedactions.add(null);
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Collections.singletonList("id1")))
                .columnRedactions(columnRedactions)
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.NullColumnRedactions.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_blankColumnNameInColumnRedactionThrows() {
        ColumnRedactions redaction = ColumnRedactions.builder().redaction("MASKED").build();
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Collections.singletonList("id1")))
                .columnRedactions(Collections.singletonList(redaction))
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.NullColumnNameInColumnRedaction.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_blankRedactionInColumnRedactionThrows() {
        ColumnRedactions redaction = ColumnRedactions.builder().columnName("email").build();
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Collections.singletonList("id1")))
                .columnRedactions(Collections.singletonList(redaction))
                .build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.NullRedactionInColumnRedaction.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testValidateGetRequest_validSingleTableWithIdsDoesNotThrow() {
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .skyflowIds(new ArrayList<>(Collections.singletonList("id1")))
                .build();
        try {
            Validations.validateGetRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateGetRequest_validSingleTableWithUniqueValuesDoesNotThrow() {
        Map<String, Object> uniqueValue = new HashMap<>();
        uniqueValue.put("email", "john@example.com");
        GetRequest request = GetRequest.builder()
                .tableName("table1")
                .uniqueValues(Collections.singletonList(uniqueValue))
                .build();
        try {
            Validations.validateGetRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateGetRequest_validMultiTableRecordsModeDoesNotThrow() {
        GetRequestRecord nestedRecord1 = GetRequestRecord.builder()
                .tableName("table1")
                .skyflowIds(Collections.singletonList("id1"))
                .build();
        GetRequestRecord nestedRecord2 = GetRequestRecord.builder()
                .tableName("table2")
                .skyflowIds(Collections.singletonList("id2"))
                .build();
        GetRequest request = GetRequest.builder()
                .records(Arrays.asList(nestedRecord1, nestedRecord2))
                .build();
        try {
            Validations.validateGetRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateGetRequest_multiTableRecordMissingTableThrows() {
        GetRequestRecord nestedRecord = GetRequestRecord.builder().skyflowIds(Collections.singletonList("id1")).build();
        GetRequest request = GetRequest.builder().records(Collections.singletonList(nestedRecord)).build();
        try {
            Validations.validateGetRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorMessage.TableKeyError.getMessage(), e.getMessage());
        }
    }
}
