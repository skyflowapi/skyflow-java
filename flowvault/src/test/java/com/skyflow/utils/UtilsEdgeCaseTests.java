package com.skyflow.utils;

import com.skyflow.config.VaultConfig;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ObjectMappers;
import com.skyflow.generated.rest.resources.files.requests.FileDeleteRequest;
import com.skyflow.generated.rest.resources.files.requests.FileUploadRequest;
import com.skyflow.generated.rest.types.DeleteTokenResponse;
import com.skyflow.generated.rest.types.DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.FileDeleteResponse;
import com.skyflow.generated.rest.types.FileUploadResponseObject;
import com.skyflow.generated.rest.types.GetRequestData;
import com.skyflow.vault.data.BulkDeleteTokensResponseRecord;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeResponseRecord;
import com.skyflow.vault.data.ColumnRedactions;
import com.skyflow.vault.data.DeleteFilesColumnResult;
import com.skyflow.vault.data.DeleteFilesRequest;
import com.skyflow.vault.data.DeleteFilesRequestRecord;
import com.skyflow.vault.data.DeleteFilesResponse;
import com.skyflow.vault.data.DeleteFilesResponseRecord;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetRequestRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateRequestRecord;
import com.skyflow.vault.data.UploadFilesRequest;
import com.skyflow.vault.data.UploadFilesRequestColumn;
import com.skyflow.vault.data.UploadFilesRequestRecord;
import com.skyflow.vault.data.UpsertOptions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

/** Edge-case branches of the request builders, response formatters and error-body helpers in {@link Utils}. */
public class UtilsEdgeCaseTests {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final Map<String, List<String>> HEADERS =
            Collections.singletonMap(Constants.REQUEST_ID_HEADER_KEY, Collections.singletonList("req-1"));

    private static VaultConfig config() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        return config;
    }

    private static Map<String, Object> uniqueValue() {
        Map<String, Object> unique = new HashMap<>();
        unique.put("email", "a@b.com");
        return unique;
    }

    // ── request builders ─────────────────────────────────────────────────────

    @Test
    public void testGetInsertRequestBody_upsertWithoutUniqueColumnsIsNotSent() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "john");
        for (List<String> uniqueColumns : Arrays.asList(null, Collections.<String>emptyList())) {
            InsertRequest request = InsertRequest.builder().tableName("table1")
                    .records(Collections.singletonList(InsertRequestRecord.builder().data(data).build()))
                    .upsert(UpsertOptions.builder().uniqueColumns(uniqueColumns).build())
                    .build();
            Assert.assertFalse(Utils.getInsertRequestBody(request, config()).getUpsert().isPresent());
        }
    }

    @Test
    public void testGetGetRequestBody_sendsUniqueValuesAtRequestAndRecordLevel() {
        GetRequest single = GetRequest.builder().tableName("table1")
                .uniqueValues(Collections.singletonList(uniqueValue())).build();
        Assert.assertEquals(1, Utils.getGetRequestBody(single, config()).getUniqueValues().get().size());

        GetRequestRecord record = GetRequestRecord.builder().tableName("table2")
                .uniqueValues(Collections.singletonList(uniqueValue()))
                .columnRedactions(Collections.singletonList(
                        ColumnRedactions.builder().columnName("email").redaction("MASKED").build()))
                .build();
        GetRequest multi = GetRequest.builder().records(Collections.singletonList(record)).build();
        GetRequestData sent = Utils.getGetRequestBody(multi, config()).getRecords().get().get(0);
        Assert.assertEquals(1, sent.getUniqueValues().get().size());
        Assert.assertEquals(1, sent.getColumnRedactions().get().size());
    }

    @Test
    public void testGetUpdateRequestBody_omitsBlankRequestTableName() {
        UpdateRequestRecord record = UpdateRequestRecord.builder()
                .skyflowId("sky-1").data(new HashMap<>()).tableName("table2").build();
        UpdateRequest request = UpdateRequest.builder().records(Collections.singletonList(record)).build();

        com.skyflow.generated.rest.resources.records.requests.UpdateRequest body =
                Utils.getUpdateRequestBody(request, config());

        Assert.assertNull(body.getTableName());
        Assert.assertEquals("table2", body.getRecords().get(0).getTableName().get());
    }

    @Test
    public void testGetUploadFilesRequestBody_columnWithoutResolvableNameSendsNoFileName() {
        UploadFilesRequest request = UploadFilesRequest.builder().records(Collections.singletonList(
                UploadFilesRequestRecord.builder().tableName("onboarding").columns(Collections.singletonList(
                        UploadFilesRequestColumn.builder().column("photo").base64("cA==").build())).build())).build();

        FileUploadRequest body = Utils.getUploadFilesRequestBody(request, "vault123");

        Assert.assertFalse(body.getRecords().get(0).getColumns().get(0).getFileName().isPresent());
    }

    @Test
    public void testResolveUploadFileName_base64TakesPrecedenceOverFileObject() throws Exception {
        UploadFilesRequestColumn column = UploadFilesRequestColumn.builder().column("photo")
                .base64("cA==").fileObject(folder.newFile("photo.png")).build();
        Assert.assertNull(Utils.resolveUploadFileName(column));
        // a column with no source at all has no name either
        Assert.assertNull(Utils.resolveUploadFileName(UploadFilesRequestColumn.builder().column("photo").build()));
    }

    @Test
    public void testGetDeleteFilesRequestBody_emptyUniqueValuesAreNotSent() {
        DeleteFilesRequest request = DeleteFilesRequest.builder().records(Collections.singletonList(
                DeleteFilesRequestRecord.builder().tableName("onboarding").skyflowId("sky-1")
                        .uniqueValues(new ArrayList<>()).columns(Collections.singletonList("photo")).build())).build();

        FileDeleteRequest body = Utils.getDeleteFilesRequestBody(request, "vault123");

        Assert.assertEquals("sky-1", body.getRecords().get(0).getSkyflowId().get());
        Assert.assertFalse(body.getRecords().get(0).getUniqueValues().isPresent());
    }

    // ── error bodies ─────────────────────────────────────────────────────────

    @Test
    public void testErrorBodyAsMap_nonObjectBodiesYieldNull() {
        Assert.assertNull(Utils.errorBodyAsMap(new ApiClientApiException("x", 500, Arrays.asList("a", "b"))));
        // Jackson cannot serialise a bare Object, which surfaces as IllegalArgumentException
        Assert.assertNull(Utils.errorBodyAsMap(new ApiClientApiException("x", 500, new Object())));
        Assert.assertEquals("plain text", Utils.errorBody(new ApiClientApiException("x", 500, "plain text")));
    }

    @Test
    public void testUploadFilesErrorBody_recordsWithoutErrorsFallBackToRawBody() {
        Map<String, Object> blank = new HashMap<>();
        blank.put("error", "");
        Map<String, Object> missing = new HashMap<>();
        missing.put("tableName", "onboarding");
        Map<String, Object> body = new HashMap<>();
        body.put("records", Arrays.asList(blank, missing));

        Assert.assertSame(body, Utils.uploadFilesErrorBody(new ApiClientApiException("x", 400, body)));
    }

    @Test
    public void testHandleBulkDeleteTokensBatchException_withoutBatchStillReportsBodyRecords() {
        Map<String, Object> token = new HashMap<>();
        token.put("value", "tok-1");
        token.put("error", "not found");
        token.put("httpCode", 404);
        Map<String, Object> body = new HashMap<>();
        body.put("tokens", Collections.singletonList(token));
        CompletionException ex = new CompletionException(new ApiClientApiException("x", 404, body));

        List<BulkDeleteTokensResponseRecord> records = Utils.handleBulkDeleteTokensBatchException(ex, null, 0, 10);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals(0, records.get(0).getIndex());
    }

    @Test
    public void testHandleBulkTokenizeBatchException_emptyResponseInBodySynthesisesRecords() {
        Map<String, Object> body = new HashMap<>();
        body.put("response", new ArrayList<>());
        CompletionException ex = new CompletionException(new ApiClientApiException("x", 500, body));
        List<BulkTokenizeRequestRecord> batch = Collections.singletonList(BulkTokenizeRequestRecord.builder()
                .value("v").tokenGroupNames(Collections.singletonList("g")).build());

        List<BulkTokenizeResponseRecord> records = Utils.handleBulkTokenizeBatchException(ex, batch, 0);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals(Integer.valueOf(500), records.get(0).getHttpCode());
    }

    // ── response formatters ──────────────────────────────────────────────────

    @Test
    public void testFormatters_nullResponseYieldsNoRecords() {
        // unary formatters return an empty response; batch formatters return null, which callers skip
        Assert.assertTrue(Utils.formatInsertResponse(null, HEADERS).getRecords().isEmpty());
        Assert.assertTrue(Utils.formatGetResponse(null, HEADERS).getRecords().isEmpty());
        Assert.assertTrue(Utils.formatDeleteResponse(null, HEADERS).getRecords().isEmpty());
        Assert.assertTrue(Utils.formatUpdateResponse(null, HEADERS).getRecords().isEmpty());
        Assert.assertNull(Utils.formatBulkInsertResponse(null, 0, 10, HEADERS));
        Assert.assertTrue(Utils.formatDetokenizeResponse(null, HEADERS).getRecords().isEmpty());
        Assert.assertNull(Utils.formatBulkDetokenizeResponse(null, 0, 10, HEADERS));
        Assert.assertNull(Utils.formatBulkDeleteTokensResponse(null, null, 0, 10, HEADERS));
        Assert.assertTrue(Utils.formatGetTokensResponse(null, HEADERS).getRecords().isEmpty());
        Assert.assertNull(Utils.formatBulkTokenizeResponse(null, new ArrayList<>(), 0, HEADERS));
        Assert.assertTrue(Utils.formatDeleteFilesResponse(null, HEADERS).getRecords().isEmpty());
    }

    @Test
    public void testFormatBulkDeleteTokensResponse_withoutBatchRequestKeepsReturnedTokens() {
        DeleteTokenResponse response = DeleteTokenResponse.builder()
                .tokens(Collections.singletonList(DeleteTokenResponseObject.builder().value("tok-1").build())).build();

        Assert.assertEquals(1, Utils.formatBulkDeleteTokensResponse(response, null, 0, 10, HEADERS).getRecords().size());
    }

    @Test
    public void testFormatDeleteFilesResponse_omittedHttpCodeAndNullColumnStatus() throws Exception {
        FileDeleteResponse response = ObjectMappers.JSON_MAPPER.readValue("{\"records\":["
                + "{\"skyflowID\":\"sky-1\",\"tableName\":\"onboarding\",\"data\":{\"photo\":null}},"
                + "{\"skyflowID\":\"sky-2\",\"tableName\":\"onboarding\",\"error\":\"record locked\"}]}",
                FileDeleteResponse.class);

        DeleteFilesResponse formatted = Utils.formatDeleteFilesResponse(response, HEADERS);

        DeleteFilesResponseRecord deleted = formatted.getRecords().get(0);
        Assert.assertEquals(200, deleted.getHttpCode());
        List<DeleteFilesColumnResult> columns = deleted.getColumns();
        Assert.assertEquals("photo", columns.get(0).getColumn());
        Assert.assertNull(columns.get(0).getStatus());
        Assert.assertEquals(500, formatted.getRecords().get(1).getHttpCode());
        Assert.assertEquals("req-1", formatted.getRecords().get(1).getRequestId());
    }

    // ── uploadFiles response pairing ─────────────────────────────────────────

    private static FileUploadResponseObject returned(String skyflowId, String tableName) throws Exception {
        return ObjectMappers.JSON_MAPPER.readValue(
                "{\"skyflowID\":\"" + skyflowId + "\",\"tableName\":\"" + tableName + "\",\"httpCode\":200}",
                FileUploadResponseObject.class);
    }

    private static UploadFilesRequestRecord requested(String skyflowId, String tableName) {
        return UploadFilesRequestRecord.builder().tableName(tableName).skyflowId(skyflowId)
                .columns(Collections.singletonList(UploadFilesRequestColumn.builder().column("c").base64("cA==")
                        .fileName("c.txt").build())).build();
    }

    @Test
    public void testUploadResponseMatchesRequest() throws Exception {
        Assert.assertTrue(Utils.uploadResponseMatchesRequest(requested(null, "t"), returned("sky-new", "t")));
        Assert.assertTrue(Utils.uploadResponseMatchesRequest(requested("sky-1", "t"), returned("sky-1", "t")));
        Assert.assertFalse(Utils.uploadResponseMatchesRequest(requested("sky-1", "t"), returned("sky-1", "other")));
        Assert.assertFalse(Utils.uploadResponseMatchesRequest(requested(null, "t"), returned("sky-new", "other")));
        Assert.assertFalse(Utils.uploadResponseMatchesRequest(requested("sky-1", "t"), returned("sky-2", "t")));
    }

    @Test
    public void testSkippedUploadColumns_withoutRecordIsEmpty() {
        Assert.assertTrue(Utils.skippedUploadColumns(null, "error").isEmpty());
        Assert.assertTrue(Utils.skippedUploadColumns(
                UploadFilesRequestRecord.builder().tableName("t").build(), "error").isEmpty());
    }
}
