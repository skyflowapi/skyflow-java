package com.skyflow.vault.controller;

import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.ApiClientHttpResponse;
import com.skyflow.generated.rest.resources.flowservice.FlowserviceClient;
import com.skyflow.generated.rest.resources.flowservice.RawFlowserviceClient;
import com.skyflow.generated.rest.resources.flowservice.requests.V1FlowDeleteTokenRequest;
import com.skyflow.generated.rest.types.V1DeleteTokenResponseObject;
import com.skyflow.generated.rest.types.V1FlowDeleteTokenResponse;
import com.skyflow.utils.Constants;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDeleteTokensResponseRecord;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Batching and concurrency behaviour for bulkDeleteTokens / bulkDeleteTokensAsync.
 *
 * <p>Batch size and concurrency are user-tunable only through the environment, so these tests swap
 * {@link VaultController#settingResolver} instead of mutating the JVM environment. Every case here
 * drives more than one batch and forces batches to complete out of order, so an implementation that
 * derived the record index from completion order (rather than from the batch's position in the
 * original request) would fail.
 */
public class BulkDeleteTokensBatchingTests {

    private static final int TOTAL_TOKENS = 50;

    private final Function<String, String> originalResolver = VaultController.settingResolver;

    @After
    public void restoreResolver() {
        VaultController.settingResolver = originalResolver;
    }

    private static void useBatching(int batchSize, int concurrency) {
        Map<String, String> settings = new HashMap<>();
        settings.put("DELETE_TOKENS_BATCH_SIZE", String.valueOf(batchSize));
        settings.put("DELETE_TOKENS_CONCURRENCY_LIMIT", String.valueOf(concurrency));
        VaultController.settingResolver = settings::get;
    }

    private static Response okHttp() {
        return new Response.Builder()
                .request(new Request.Builder().url("https://dummy.example.com").build())
                .protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .header(Constants.REQUEST_ID_HEADER_KEY, "req-test-123").build();
    }

    /** Delay derived from the batch's first token so batches finish in a scrambled order. */
    private static void scrambleCompletion(List<String> tokens) throws InterruptedException {
        int first = Integer.parseInt(tokens.get(0).substring(4));
        Thread.sleep(((first * 7919L) % 41) + 3);
    }

    private static List<String> tokens(int count) {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tokens.add("tok-" + i);
        }
        return tokens;
    }

    private static VaultController controllerWith(ApiClient mockApi) throws Exception {
        Credentials creds = new Credentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setClusterId("cluster123");
        config.setEnv(Env.DEV);
        VaultController controller = new VaultController(config, creds);
        Field field = VaultClient.class.getDeclaredField("apiClient");
        field.setAccessible(true);
        field.set(controller, mockApi);
        return controller;
    }

    private static RawFlowserviceClient mockRawFlowservice(ApiClient mockApi) {
        FlowserviceClient mockFlow = Mockito.mock(FlowserviceClient.class);
        RawFlowserviceClient mockRaw = Mockito.mock(RawFlowserviceClient.class);
        when(mockApi.flowservice()).thenReturn(mockFlow);
        when(mockFlow.withRawResponse()).thenReturn(mockRaw);
        return mockRaw;
    }

    /** Echoes each batch back; any token whose number ends in 7 fails with 404. */
    private static ApiClient mockApiEchoingBatches() throws Exception {
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        when(mockRaw.deletetoken(any(), any())).thenAnswer(invocation -> {
            V1FlowDeleteTokenRequest request = invocation.getArgument(0);
            List<String> batchTokens = request.getTokens().get();
            scrambleCompletion(batchTokens);
            List<V1DeleteTokenResponseObject> records = new ArrayList<>();
            for (String token : batchTokens) {
                if (token.endsWith("7")) {
                    records.add(V1DeleteTokenResponseObject.builder()
                            .value(token).error("Token not found").httpCode(404).build());
                } else {
                    records.add(V1DeleteTokenResponseObject.builder()
                            .value(token).httpCode(200).build());
                }
            }
            return new ApiClientHttpResponse<>(
                    V1FlowDeleteTokenResponse.builder().tokens(records).build(), okHttp());
        });
        return mockApi;
    }

    private static void assertIndexesPreserved(BulkDeleteTokensResponse response) {
        Assert.assertEquals(TOTAL_TOKENS, response.getRecords().size());
        for (int i = 0; i < TOTAL_TOKENS; i++) {
            BulkDeleteTokensResponseRecord record = response.getRecords().get(i);
            Assert.assertEquals("index at position " + i, i, record.getIndex());
            Assert.assertEquals("token at position " + i, "tok-" + i, record.getToken());
        }
    }

    @Test
    public void testBulkDeleteTokens_parallelBatchesPreserveIndexOrder() throws Exception {
        useBatching(10, 5);
        VaultController controller = controllerWith(mockApiEchoingBatches());

        BulkDeleteTokensResponse response = controller.bulkDeleteTokens(
                BulkDeleteTokensRequest.builder().tokens(tokens(TOTAL_TOKENS)).build());

        assertIndexesPreserved(response);
        // tok-7, 17, 27, 37, 47 fail; every failure keeps its own index and token
        Assert.assertEquals(TOTAL_TOKENS, response.getSummary().getTotalTokens());
        Assert.assertEquals(5, response.getSummary().getTotalFailed());
        Assert.assertEquals(45, response.getSummary().getTotalDeleted());
        Assert.assertEquals("Token not found", response.getRecords().get(7).getError());
        Assert.assertEquals(Integer.valueOf(404), response.getRecords().get(7).getHttpCode());
        Assert.assertNull(response.getRecords().get(8).getError());
    }

    @Test
    public void testBulkDeleteTokensAsync_parallelBatchesPreserveIndexOrder() throws Exception {
        useBatching(10, 5);
        VaultController controller = controllerWith(mockApiEchoingBatches());

        BulkDeleteTokensResponse response = controller.bulkDeleteTokensAsync(
                        BulkDeleteTokensRequest.builder().tokens(tokens(TOTAL_TOKENS)).build())
                .get(30, TimeUnit.SECONDS);

        assertIndexesPreserved(response);
        Assert.assertEquals(5, response.getSummary().getTotalFailed());
        Assert.assertEquals(45, response.getSummary().getTotalDeleted());
    }

    @Test
    public void testBulkDeleteTokens_maxConcurrencyStillPreservesIndexOrder() throws Exception {
        // batch size 5 => 10 batches, all runnable at once at the max concurrency limit
        useBatching(5, 10);
        VaultController controller = controllerWith(mockApiEchoingBatches());

        BulkDeleteTokensResponse response = controller.bulkDeleteTokens(
                BulkDeleteTokensRequest.builder().tokens(tokens(TOTAL_TOKENS)).build());

        assertIndexesPreserved(response);
        Assert.assertEquals(TOTAL_TOKENS, response.getSummary().getTotalTokens());
    }

    @Test
    public void testBulkDeleteTokens_failedBatchKeepsItsOwnIndexSlice() throws Exception {
        useBatching(10, 5);
        ApiClient mockApi = Mockito.mock(ApiClient.class);
        RawFlowserviceClient mockRaw = mockRawFlowservice(mockApi);
        // the batch starting at tok-20 fails wholesale; the rest succeed
        when(mockRaw.deletetoken(any(), any())).thenAnswer(invocation -> {
            V1FlowDeleteTokenRequest request = invocation.getArgument(0);
            List<String> batchTokens = request.getTokens().get();
            scrambleCompletion(batchTokens);
            if ("tok-20".equals(batchTokens.get(0))) {
                throw new ApiClientApiException("delete failed", 503, "service unavailable");
            }
            List<V1DeleteTokenResponseObject> records = new ArrayList<>();
            for (String token : batchTokens) {
                records.add(V1DeleteTokenResponseObject.builder().value(token).httpCode(200).build());
            }
            return new ApiClientHttpResponse<>(
                    V1FlowDeleteTokenResponse.builder().tokens(records).build(), okHttp());
        });
        VaultController controller = controllerWith(mockApi);

        BulkDeleteTokensResponse response = controller.bulkDeleteTokens(
                BulkDeleteTokensRequest.builder().tokens(tokens(TOTAL_TOKENS)).build());

        // no gaps and no shifting: the failed batch still contributes exactly indexes 20..29
        assertIndexesPreserved(response);
        Assert.assertEquals(10, response.getSummary().getTotalFailed());
        Assert.assertEquals(40, response.getSummary().getTotalDeleted());
        for (int i = 20; i < 30; i++) {
            Assert.assertNotNull("expected error at index " + i, response.getRecords().get(i).getError());
            Assert.assertEquals(Integer.valueOf(503), response.getRecords().get(i).getHttpCode());
            Assert.assertEquals("tok-" + i, response.getRecords().get(i).getToken());
        }
        Assert.assertNull(response.getRecords().get(19).getError());
        Assert.assertNull(response.getRecords().get(30).getError());
    }

    @Test
    public void testBulkDeleteTokens_batchSizeAboveMaxIsCapped() throws Exception {
        // 5000 exceeds MAX_DELETE_TOKENS_BATCH_SIZE (1000) — capped, so 50 tokens stay one batch
        useBatching(5000, 1);
        VaultController controller = controllerWith(mockApiEchoingBatches());

        BulkDeleteTokensResponse response = controller.bulkDeleteTokens(
                BulkDeleteTokensRequest.builder().tokens(tokens(TOTAL_TOKENS)).build());

        assertIndexesPreserved(response);
    }
}
