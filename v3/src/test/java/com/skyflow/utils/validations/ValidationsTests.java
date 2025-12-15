package com.skyflow.utils.validations;

import com.skyflow.config.VaultConfig;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.TokenGroupRedactions;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationsTests {

    @Test
    public void validateInsertRequest_nullRecords_throws() {
        InsertRequest request = InsertRequest.builder()
                .table("tbl")
                .records(null)
                .build();
        assertSkyflowException(() -> Validations.validateInsertRequest(request),
                ErrorMessage.RecordsKeyError.getMessage());
    }

    @Test
    public void validateInsertRequest_emptyRecords_throws() {
        InsertRequest request = InsertRequest.builder()
                .table("tbl")
                .records(new ArrayList<>())
                .build();
        assertSkyflowException(() -> Validations.validateInsertRequest(request),
                ErrorMessage.EmptyRecords.getMessage());
    }

    @Test
    public void validateInsertRequest_nullRecordEntry_throws() {
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(null);
        InsertRequest request = InsertRequest.builder()
                .table("tbl")
                .records(records)
                .build();
        assertSkyflowException(() -> Validations.validateInsertRequest(request),
                ErrorMessage.InvalidRecord.getMessage());
    }

    @Test
    public void validateInsertRequest_tableSpecifiedBoth_throws() {
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table("recordTable").build());
        InsertRequest request = InsertRequest.builder()
                .table("requestTable")
                .records(records)
                .build();
        assertSkyflowException(() -> Validations.validateInsertRequest(request),
                ErrorMessage.TableSpecifiedInRequestAndRecordObject.getMessage());
    }

    @Test
    public void validateInsertRequest_tableMissing_throws() {
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table(null).build());
        InsertRequest request = InsertRequest.builder()
                .table(null)
                .records(records)
                .build();
        assertSkyflowException(() -> Validations.validateInsertRequest(request),
                ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage());
    }

    @Test
    public void validateInsertRequest_upsertAtRecordLevel_throws() {
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder()
                .table(null)
                .upsert(Collections.singletonList("key"))
                .build());
        InsertRequest request = InsertRequest.builder()
                .table("requestTable")
                .records(records)
                .build();
        assertSkyflowException(() -> Validations.validateInsertRequest(request),
                ErrorMessage.UpsertTableRequestAtRecordLevel.getMessage());
    }

    @Test
    public void validateInsertRequest_upsertAtRequestLevelWithoutTable_throws() {
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table("recordTable").build());
        InsertRequest request = InsertRequest.builder()
                .table(null)
                .upsert(Collections.singletonList("key"))
                .records(records)
                .build();
        assertSkyflowException(() -> Validations.validateInsertRequest(request),
                ErrorMessage.UpsertTableRequestAtRequestLevel.getMessage());
    }

    @Test
    public void validateInsertRequest_emptyUpsert_throws() {
        ArrayList<InsertRecord> records = new ArrayList<>();
        // avoid table conflict so we reach the empty upsert validation
        records.add(InsertRecord.builder().table(null).build());
        InsertRequest request = InsertRequest.builder()
                .table("requestTable")
                .upsert(new ArrayList<>())
                .records(records)
                .build();
        assertSkyflowException(() -> Validations.validateInsertRequest(request),
                ErrorMessage.EmptyUpsertValues.getMessage());
    }

    @Test
    public void validateInsertRequest_emptyKey_throws() {
        Map<String, Object> data = new HashMap<>();
        data.put("", "value");
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table(null).data(data).build());
        InsertRequest request = InsertRequest.builder()
                .table("tbl")
                .records(records)
                .build();
        assertSkyflowException(() -> Validations.validateInsertRequest(request),
                ErrorMessage.EmptyKeyInRecords.getMessage());
    }

    @Test
    public void validateInsertRequest_emptyValue_throws() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "   ");
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table(null).data(data).build());
        InsertRequest request = InsertRequest.builder()
                .table("tbl")
                .records(records)
                .build();
        assertSkyflowException(() -> Validations.validateInsertRequest(request),
                ErrorMessage.EmptyValueInValues.getMessage());
    }

    @Test
    public void validateInsertRequest_valid_passes() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        ArrayList<InsertRecord> records = new ArrayList<>();
        records.add(InsertRecord.builder().table(null).data(data).build());
        InsertRequest request = InsertRequest.builder()
                .table("tbl")
                .records(records)
                .build();
        try {
            Validations.validateInsertRequest(request);
        } catch (Exception e) {
            Assert.fail("Should not throw for valid request: " + e.getMessage());
        }
    }

    @Test
    public void validateDetokenizeRequest_nullRequest_throws() {
        assertSkyflowException(() -> Validations.validateDetokenizeRequest(null),
                ErrorMessage.DetokenizeRequestNull.getMessage());
    }

    @Test
    public void validateDetokenizeRequest_emptyTokens_throws() {
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(new ArrayList<>())
                .build();
        assertSkyflowException(() -> Validations.validateDetokenizeRequest(request),
                ErrorMessage.EmptyDetokenizeData.getMessage());
    }

    @Test
    public void validateDetokenizeRequest_nullToken_throws() {
        List<String> tokens = new ArrayList<>();
        tokens.add(null);
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .build();
        assertSkyflowException(() -> Validations.validateDetokenizeRequest(request),
                ErrorMessage.EmptyTokenInDetokenizeData.getMessage());
    }

    @Test
    public void validateDetokenizeRequest_nullGroup_throws() {
        List<String> tokens = Collections.singletonList("tok");
        List<TokenGroupRedactions> groups = new ArrayList<>();
        groups.add(null);
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(groups)
                .build();
        assertSkyflowException(() -> Validations.validateDetokenizeRequest(request),
                ErrorMessage.NullTokenGroupRedactions.getMessage());
    }

    @Test
    public void validateDetokenizeRequest_emptyGroupName_throws() {
        List<String> tokens = Collections.singletonList("tok");
        List<TokenGroupRedactions> groups = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("").redaction("MASK").build()
        );
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(groups)
                .build();
        assertSkyflowException(() -> Validations.validateDetokenizeRequest(request),
                ErrorMessage.NullTokenGroupNameInTokenGroup.getMessage());
    }

    @Test
    public void validateDetokenizeRequest_emptyRedaction_throws() {
        List<String> tokens = Collections.singletonList("tok");
        List<TokenGroupRedactions> groups = Collections.singletonList(
                TokenGroupRedactions.builder().tokenGroupName("grp").redaction(" ").build()
        );
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(groups)
                .build();
        assertSkyflowException(() -> Validations.validateDetokenizeRequest(request),
                ErrorMessage.NullRedactionInTokenGroup.getMessage());
    }

    @Test
    public void validateDetokenizeRequest_tokensSizeExceed_throws() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 10001; i++) {
            tokens.add("tok" + i);
        }
        DetokenizeRequest request = DetokenizeRequest.builder()
                .tokens(tokens)
                .build();
        assertSkyflowException(() -> Validations.validateDetokenizeRequest(request),
                ErrorMessage.TokensSizeExceedError.getMessage());
    }

    @Test
    public void validateVaultConfig_nullVaultId_throws() {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultURL("https://vault.example.com");
        cfg.setVaultId(null);
        assertSkyflowException(() -> Validations.validateVaultConfiguration(cfg),
                ErrorMessage.InvalidVaultId.getMessage());
    }

    @Test
    public void validateVaultConfig_emptyVaultId_throws() {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultURL("https://vault.example.com");
        cfg.setVaultId("   ");
        assertSkyflowException(() -> Validations.validateVaultConfiguration(cfg),
                ErrorMessage.EmptyVaultId.getMessage());
    }

    @Test
    public void validateVaultConfig_emptyVaultURL_throws() {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("vault");
        cfg.setVaultURL("   ");
        assertSkyflowException(() -> Validations.validateVaultConfiguration(cfg),
                ErrorMessage.EmptyVaultUrl.getMessage());
    }

    @Test
    public void validateVaultConfig_invalidVaultURL_throws() {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("vault");
        cfg.setVaultURL("http://bad");
        assertSkyflowException(() -> Validations.validateVaultConfiguration(cfg),
                ErrorMessage.InvalidVaultUrlFormat.getMessage());
    }

    @Test
    public void validateVaultConfig_missingBothVaultUrlAndCluster_throws() {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("vault");
        cfg.setVaultURL(null);
        cfg.setClusterId(null);
        assertSkyflowException(() -> Validations.validateVaultConfiguration(cfg),
                ErrorMessage.EitherVaultUrlOrClusterIdRequired.getMessage());
    }

    @Test
    public void validateVaultConfig_emptyClusterId_throws() {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("vault");
        cfg.setVaultURL(null);
        cfg.setClusterId("  ");
        assertSkyflowException(() -> Validations.validateVaultConfiguration(cfg),
                ErrorMessage.EmptyClusterId.getMessage());
    }

    @Test
    public void validateVaultConfig_valid_passes() {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("vault");
        cfg.setVaultURL("https://vault.example.com");
        try {
            Validations.validateVaultConfiguration(cfg);
        } catch (Exception e) {
            Assert.fail("Should not throw for valid config: " + e.getMessage());
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private void assertSkyflowException(ThrowingRunnable runnable, String expectedMessage) {
        try {
            runnable.run();
            Assert.fail("Expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(expectedMessage, e.getMessage());
        } catch (Exception e) {
            Assert.fail("Unexpected exception type: " + e.getClass().getSimpleName());
        }
    }
}
