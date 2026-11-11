package com.skyflow.utils;

import com.skyflow.generated.rest.core.ObjectMappers;
import com.skyflow.generated.rest.types.ExecuteQueryResponse;
import com.skyflow.generated.rest.types.FileDeleteResponse;
import com.skyflow.generated.rest.types.FileUploadResponse;
import com.skyflow.generated.rest.types.GetTokensFromValuesResponse;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SuccessBodyInterceptorTests {

    private static final String BASE = "https://vault1.example.com";

    /** Runs the interceptor against a chain that answers {@code path} with {@code code} and {@code body}. */
    private static Response run(String path, int code, String body) throws IOException {
        Request request = new Request.Builder().url(BASE + path).build();
        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("status " + code)
                .header("x-request-id", "req-1")
                .body(ResponseBody.create(body, MediaType.parse("application/json")))
                .build();
        Interceptor.Chain chain = Mockito.mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any())).thenReturn(response);
        return new SuccessBodyInterceptor().intercept(chain);
    }

    @Test
    public void testBlankSuccessBodyBecomesAnEmptyJsonObject() throws IOException {
        for (String path : SuccessBodyInterceptor.PATHS) {
            Assert.assertEquals(path, "{}", run(path, 200, "").body().string());
            Assert.assertEquals(path, "{}", run(path, 200, "  \n").body().string());
        }
    }

    @Test
    public void testEmptyJsonObjectParsesForEveryCoveredOperation() throws IOException {
        // the generated client must accept the {} a blank body is turned into
        ObjectMappers.JSON_MAPPER.readValue("{}", ExecuteQueryResponse.class);
        ObjectMappers.JSON_MAPPER.readValue("{}", GetTokensFromValuesResponse.class);
        ObjectMappers.JSON_MAPPER.readValue("{}", FileUploadResponse.class);
        ObjectMappers.JSON_MAPPER.readValue("{}", FileDeleteResponse.class);
    }

    @Test
    public void testJsonObjectSuccessBodyPassesThroughUnchanged() throws IOException {
        Response response = run("/v2/query", 200, "{\"records\":[]}");
        Assert.assertEquals("{\"records\":[]}", response.body().string());
        Assert.assertEquals(200, response.code());
    }

    @Test
    public void testNonJsonSuccessBodyKeepsStatusBodyAndHeaders() {
        try {
            run("/v2/files/delete", 200, "<html>gateway</html>");
            Assert.fail("Should have thrown an exception");
        } catch (UnparseableResponseException e) {
            Assert.assertEquals(200, e.getStatusCode());
            Assert.assertEquals("<html>gateway</html>", e.getBody());
            Assert.assertEquals("req-1", e.getHeaders().get("x-request-id").get(0));
        } catch (IOException e) {
            Assert.fail("Expected UnparseableResponseException, got " + e);
        }
    }

    @Test
    public void testJsonThatIsNotAnObjectIsRejected() throws IOException {
        try {
            run("/v2/records/getTokens", 200, "[1,2]");
            Assert.fail("Should have thrown an exception");
        } catch (UnparseableResponseException e) {
            Assert.assertEquals("[1,2]", e.getBody());
        }
    }

    @Test
    public void testLongNonJsonBodyIsTruncated() throws IOException {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < SuccessBodyInterceptor.MAX_BODY_IN_ERROR + 100; i++) {
            body.append('x');
        }
        try {
            run("/v2/files/upload", 200, body.toString());
            Assert.fail("Should have thrown an exception");
        } catch (UnparseableResponseException e) {
            Assert.assertEquals(SuccessBodyInterceptor.MAX_BODY_IN_ERROR + 3, e.getBody().length());
            Assert.assertTrue(e.getBody().endsWith("..."));
        }
    }

    @Test
    public void testOtherOperationsAndErrorResponsesAreUntouched() throws IOException {
        Assert.assertEquals("", run("/v2/records/insert", 200, "").body().string());
        Assert.assertEquals("not json", run("/v2/records/insert", 200, "not json").body().string());
        Assert.assertEquals("not json", run("/v2/query", 502, "not json").body().string());
    }
}
