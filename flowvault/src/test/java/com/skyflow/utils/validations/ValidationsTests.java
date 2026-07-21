package com.skyflow.utils.validations;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkInsertRecord;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkTokenGroupRedactions;
import com.skyflow.vault.data.BulkTokenizeRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.DeleteTokensRequest;
import com.skyflow.vault.data.DetokenizeData;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.TokenGroupRedactions;
import com.skyflow.vault.data.TokenizeRecord;
import com.skyflow.vault.data.TokenizeRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationsTests {
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";

    // ── validateTokenizeRequest ──────────────────────────────────────────────

    @Test
    public void testValidateTokenizeRequest_nullRequest() {
        try {
            Validations.validateTokenizeRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_nullData() {
        TokenizeRequest request = TokenizeRequest.builder().data(null).build();
        try {
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_emptyData() {
        TokenizeRequest request = TokenizeRequest.builder().data(new ArrayList<>()).build();
        try {
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_largeValidListDoesNotThrow() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            records.add(TokenizeRecord.builder()
                    .value("value" + i)
                    .tokenGroupNames(Collections.singletonList("group1"))
                    .build());
        }
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateTokenizeRequest_nullRecordInList() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(null);
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_nullValue() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value(null).tokenGroupNames(Collections.singletonList("group1")).build());
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_blankStringValue() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("   ").tokenGroupNames(Collections.singletonList("group1")).build());
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_nullTokenGroupNames() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("value1").tokenGroupNames(null).build());
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_emptyTokenGroupNames() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder().value("value1").tokenGroupNames(new ArrayList<>()).build());
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_blankTokenGroupNameAtIndex() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder()
                .value("value1")
                .tokenGroupNames(Arrays.asList("group1", "   "))
                .build());
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateTokenizeRequest_validRequest() {
        ArrayList<TokenizeRecord> records = new ArrayList<>();
        records.add(TokenizeRecord.builder()
                .value("value1")
                .tokenGroupNames(Collections.singletonList("group1"))
                .build());
        TokenizeRequest request = TokenizeRequest.builder().data(records).build();
        try {
            Validations.validateTokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    // ── validateDeleteTokensRequest ──────────────────────────────────────────

    @Test
    public void testValidateDeleteTokensRequest_nullRequest() {
        try {
            Validations.validateDeleteTokensRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequest_nullTokens() {
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(null).build();
        try {
            Validations.validateDeleteTokensRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequest_emptyTokens() {
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(new ArrayList<>()).build();
        try {
            Validations.validateDeleteTokensRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequest_largeValidListDoesNotThrow() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tokens.add("token" + i);
        }
        DeleteTokensRequest request = DeleteTokensRequest.builder().tokens(tokens).build();
        try {
            Validations.validateDeleteTokensRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateDeleteTokensRequest_blankTokenAtIndex() {
        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(Arrays.asList("token1", "   "))
                .build();
        try {
            Validations.validateDeleteTokensRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDeleteTokensRequest_validRequest() {
        DeleteTokensRequest request = DeleteTokensRequest.builder()
                .tokens(Collections.singletonList("token1"))
                .build();
        try {
            Validations.validateDeleteTokensRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

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
        config.setVaultURL("   ");
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
        config.setVaultURL("http://not-https.example.com");
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
        config.setVaultURL("https://myvault.example.com");
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
    public void testValidateInsertRequest_nullRecords() {
        InsertRequest request = InsertRequest.builder().table("table1").records(null).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_emptyRecords() {
        InsertRequest request = InsertRequest.builder().table("table1").records(new ArrayList<>()).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_nullRecordInList() {
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(null);
        InsertRequest request = InsertRequest.builder().table("table1").records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_tableSpecifiedAtBothPlaces() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table("recordTable").data(data).build());
        InsertRequest request = InsertRequest.builder().table("requestTable").records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_tableMissingAtBothPlaces() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().data(data).build());
        InsertRequest request = InsertRequest.builder().records(records).build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_upsertAtRecordLevelWhenTableAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().data(data).upsert(Collections.singletonList("email")).build());
        InsertRequest request = InsertRequest.builder()
                .table("table1")
                .records(records)
                .build();
        try {
            Validations.validateInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateInsertRequest_emptyUpsertAtRequestLevel() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().data(data).build());
        InsertRequest request = InsertRequest.builder()
                .table("table1")
                .records(records)
                .upsert(new ArrayList<>())
                .build();
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
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table("table1").data(data).build());
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
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table("table1").data(data).build());
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
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table("table1").data(data).build());
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
    public void testValidateDetokenizeRequest_nullDetokenizeData() {
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(null).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_emptyDetokenizeData() {
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(new ArrayList<>()).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_nullEntryInDetokenizeData() {
        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(null);
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(data).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_blankTokenInDetokenizeData() {
        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("   "));
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(data).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateDetokenizeRequest_nullTokenGroupRedactionInList() {
        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        List<TokenGroupRedactions> groupRedactions = new ArrayList<>();
        groupRedactions.add(null);
        DetokenizeRequest request = DetokenizeRequest.builder()
                .detokenizeData(data)
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
        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("   ").redaction("MASKED").build());
        DetokenizeRequest request = DetokenizeRequest.builder()
                .detokenizeData(data)
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
        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("   ").build());
        DetokenizeRequest request = DetokenizeRequest.builder()
                .detokenizeData(data)
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
        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        DetokenizeRequest request = DetokenizeRequest.builder().detokenizeData(data).build();
        try {
            Validations.validateDetokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateDetokenizeRequest_validRequestWithTokenGroupRedactions() {
        ArrayList<DetokenizeData> data = new ArrayList<>();
        data.add(new DetokenizeData("token1"));
        List<TokenGroupRedactions> groupRedactions = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASKED").build());
        DetokenizeRequest request = DetokenizeRequest.builder()
                .detokenizeData(data)
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
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(null);
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_tableSpecifiedAtBothPlaces() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(BulkInsertRecord.builder().table("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().table("table1").records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_tableNotSpecifiedAnywhere() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(BulkInsertRecord.builder().data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkInsertRequest_emptyKeyInData() {
        Map<String, Object> data = new HashMap<>();
        data.put("", "john");
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(BulkInsertRecord.builder().table("table1").data(data).build());
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
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(BulkInsertRecord.builder().table("table1").data(data).build());
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
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        records.add(BulkInsertRecord.builder().table("table1").data(data).build());
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkInsertRequest_validRequestOver10000Records() {
        // Bulk operations rely on batching, not a hard cap — must not throw for large lists.
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        ArrayList<BulkInsertRecord> records = new ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            records.add(BulkInsertRecord.builder().table("table1").data(data).build());
        }
        BulkInsertRequest request = BulkInsertRequest.builder().records(records).build();
        try {
            Validations.validateBulkInsertRequest(request);
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
        List<BulkTokenGroupRedactions> groupRedactions = new ArrayList<>();
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
        List<BulkTokenGroupRedactions> groupRedactions = Collections.singletonList(
                BulkTokenGroupRedactions.builder().redaction("MASKED").build());
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
        List<BulkTokenGroupRedactions> groupRedactions = Collections.singletonList(
                BulkTokenGroupRedactions.builder().tokenGroupName("group1").build());
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
        List<BulkTokenGroupRedactions> groupRedactions = Collections.singletonList(
                BulkTokenGroupRedactions.builder().tokenGroupName("group1").redaction("MASKED").build());
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
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(new ArrayList<>()).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_nullRecordInList() {
        ArrayList<BulkTokenizeRecord> data = new ArrayList<>();
        data.add(null);
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_emptyValue() {
        ArrayList<BulkTokenizeRecord> data = new ArrayList<>();
        data.add(BulkTokenizeRecord.builder().value("   ").build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_emptyGroupNameInList() {
        ArrayList<BulkTokenizeRecord> data = new ArrayList<>();
        data.add(BulkTokenizeRecord.builder().value("value1").tokenGroupNames(Arrays.asList("group1", "  ")).build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_validRequestWithTokenGroupNames() {
        ArrayList<BulkTokenizeRecord> data = new ArrayList<>();
        data.add(BulkTokenizeRecord.builder().value("value1").tokenGroupNames(Collections.singletonList("group1")).build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidateBulkTokenizeRequest_validRequestWithoutTokenGroupNames() {
        // Unlike the non-bulk tokenize validator, tokenGroupNames is optional for bulk requests.
        ArrayList<BulkTokenizeRecord> data = new ArrayList<>();
        data.add(BulkTokenizeRecord.builder().value("value1").build());
        BulkTokenizeRequest request = BulkTokenizeRequest.builder().data(data).build();
        try {
            Validations.validateBulkTokenizeRequest(request);
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
