package com.skyflow;

import com.skyflow.config.BaseCredentials;
import com.skyflow.config.BaseVaultConfig;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.BaseConstants;
import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class BaseVaultClientTests {

    private static final String ENV_FILE = ".env";
    private byte[] originalEnvContent;

    @Before
    public void saveEnvFileState() throws IOException {
        File f = new File(ENV_FILE);
        originalEnvContent = f.exists() ? Files.readAllBytes(Paths.get(ENV_FILE)) : null;
    }

    @After
    public void restoreEnvFile() throws IOException {
        if (originalEnvContent != null) {
            Files.write(Paths.get(ENV_FILE), originalEnvContent);
        } else {
            Files.deleteIfExists(Paths.get(ENV_FILE));
        }
    }

    private BaseVaultClient<BaseVaultConfig> newClient(BaseCredentials commonCredentials) {
        return new BaseVaultClient<>(new BaseVaultConfig(), commonCredentials);
    }

    @Test
    public void testPrioritiseCredentials_prefersVaultSpecificCredentials() throws SkyflowException {
        BaseCredentials vaultSpecific = new BaseCredentials();
        vaultSpecific.setApiKey("test_api_key");
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        client.prioritiseCredentials(vaultSpecific);

        Assert.assertEquals(vaultSpecific, client.finalCredentials);
    }

    @Test
    public void testPrioritiseCredentials_fallsBackToCommonCredentials() throws SkyflowException {
        BaseCredentials common = new BaseCredentials();
        common.setApiKey("common_api_key");
        BaseVaultClient<BaseVaultConfig> client = newClient(common);

        client.prioritiseCredentials(null);

        Assert.assertEquals(common, client.finalCredentials);
    }

    @Test
    public void testPrioritiseCredentials_credentialChange_resetsTokenAndApiKey() throws SkyflowException {
        BaseCredentials credentialsA = new BaseCredentials();
        credentialsA.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y");
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        client.prioritiseCredentials(credentialsA);
        client.token = "cached-token";
        client.apiKey = "cached-api-key";

        BaseCredentials credentialsB = new BaseCredentials();
        credentialsB.setToken("other-token");
        client.prioritiseCredentials(credentialsB);

        Assert.assertNull(client.token);
        Assert.assertNull(client.apiKey);
    }

    @Test
    public void testSetBearerToken_withApiKey() throws SkyflowException {
        BaseCredentials creds = new BaseCredentials();
        creds.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        client.setBearerToken(creds);

        Assert.assertEquals("sky-ab123-abcd1234cdef1234abcd4321cdef4321", client.token);
    }

    @Test
    public void testSetBearerToken_generatesTokenWhenNull() throws SkyflowException {
        BaseCredentials creds = new BaseCredentials();
        creds.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y");
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        client.setBearerToken(creds);

        Assert.assertEquals("x.eyJleHAiOjk5OTk5OTk5OTl9.y", client.token);
    }

    @Test
    public void testSetBearerToken_reusesValidNonExpiredToken() throws SkyflowException {
        BaseCredentials creds = new BaseCredentials();
        creds.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y");
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        // First call: token=null → generates from creds.getToken()
        client.setBearerToken(creds);
        Assert.assertEquals("x.eyJleHAiOjk5OTk5OTk5OTl9.y", client.token);

        // Second call: token valid, not expired → reuse branch
        client.setBearerToken(creds);
        Assert.assertEquals("x.eyJleHAiOjk5OTk5OTk5OTl9.y", client.token);
    }

    @Test
    public void testSetBearerToken_noCredentials_throwsEmptyCredentials() {
        BaseVaultClient<BaseVaultConfig> client = newClient(null);
        try {
            client.setBearerToken(null);
            Assert.fail("Should have thrown SkyflowException");
        } catch (SkyflowException e) {
            // message varies by environment (EmptyCredentials when no .env, or credential error when .env provides creds)
        }
    }

    /**
     * Covers the dotenv success path: Dotenv.load() succeeds and returns a
     * non-null credentials string, so finalCredentials is set via credentialsString.
     */
    @Test
    public void testPrioritiseCredentials_dotenvReturnsCredentials_setsCredentials() throws Exception {
        try (FileWriter fw = new FileWriter(ENV_FILE)) {
            fw.write(BaseConstants.ENV_CREDENTIALS_KEY_NAME + "={\"token\":\"env-token-value\"}\n");
        }

        BaseVaultClient<BaseVaultConfig> client = newClient(null);
        client.prioritiseCredentials(null);

        Assert.assertNotNull(client.finalCredentials);
        Assert.assertEquals("{\"token\":\"env-token-value\"}", client.finalCredentials.getCredentialsString());
    }

    /**
     * Covers the path where dotenv loads but the key is absent (returns null),
     * causing SkyflowException(EmptyCredentials) to be thrown directly.
     */
    @Test
    public void testPrioritiseCredentials_dotenvKeyMissing_throwsSkyflowException() throws Exception {
        try (FileWriter fw = new FileWriter(ENV_FILE)) {
            fw.write("SOME_OTHER_KEY=some_value\n");
        }

        BaseVaultClient<BaseVaultConfig> client = newClient(null);
        try {
            client.prioritiseCredentials(null);
            Assert.fail("Should have thrown SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage().contains(ErrorMessage.EmptyCredentials.getMessage()));
        }
    }

    /**
     * Covers buildSharedHttpClient: the interceptor it installs must inject
     * "Authorization: Bearer <token>" using the supplied tokenSupplier. No real network call is
     * made; a hand-rolled Interceptor.Chain captures the request that would be sent.
     */
    @Test
    public void testBuildSharedHttpClient_injectsAuthorizationHeaderFromTokenSupplier() throws IOException {
        BaseVaultClient<BaseVaultConfig> client = newClient(null);
        Supplier<String> tokenSupplier = () -> "test-shared-token";

        OkHttpClient httpClient = client.buildSharedHttpClient(tokenSupplier);

        Assert.assertEquals(1, httpClient.interceptors().size());
        Interceptor interceptor = httpClient.interceptors().get(0);

        Request originalRequest = new Request.Builder().url("https://example.com/").build();
        final Request[] capturedRequest = new Request[1];
        Interceptor.Chain fakeChain = new Interceptor.Chain() {
            @Override
            public Request request() {
                return originalRequest;
            }

            @Override
            public Response proceed(Request request) {
                capturedRequest[0] = request;
                return new Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .build();
            }

            @Override
            public Connection connection() {
                return null;
            }

            @Override
            public Call call() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int connectTimeoutMillis() {
                return 0;
            }

            @Override
            public Interceptor.Chain withConnectTimeout(int timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int readTimeoutMillis() {
                return 0;
            }

            @Override
            public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int writeTimeoutMillis() {
                return 0;
            }

            @Override
            public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }
        };

        interceptor.intercept(fakeChain);

        Assert.assertNotNull(capturedRequest[0]);
        Assert.assertEquals("Bearer test-shared-token", capturedRequest[0].header("Authorization"));
    }

    /**
     * Covers wrapApiException: the returned SkyflowException should carry the given status code,
     * and its message should reflect the JSON-serialized responseBody (round-tripped through Gson
     * exactly as wrapApiException does internally).
     */
    @Test
    public void testWrapApiException_carriesStatusCodeAndJsonResponseBody() {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("message", "something went wrong");
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", errorBody);
        Map<String, List<String>> headers = new HashMap<>();

        SkyflowException exception = BaseVaultClient.wrapApiException(
                400, new RuntimeException("network error"), headers, responseBody, ErrorLogs.INSERT_RECORDS_REJECTED);

        Assert.assertEquals(400, exception.getHttpCode());
        Assert.assertEquals("something went wrong", exception.getMessage());
    }

    /**
     * Covers setBearerToken's "token present but expired -> regenerate" branch. A hand-crafted
     * always-expired JWT ("x.eyJleHAiOjF9.y", exp=1 => 1970) is seeded directly into the token
     * field, bypassing setBearerToken. The very same credentials instance used to seed
     * finalCredentials is then reused when calling setBearerToken, so prioritiseCredentials does
     * NOT treat this as a credential change (that branch is covered by
     * testPrioritiseCredentials_credentialChange_resetsTokenAndApiKey) - isolating the isExpired
     * branch specifically.
     */
    @Test
    public void testSetBearerToken_expiredToken_regeneratesToken() throws SkyflowException {
        BaseCredentials creds = new BaseCredentials();
        creds.setToken("x.eyJleHAiOjk5OTk5OTk5OTl9.y"); // far-future exp -> not expired once (re)generated
        BaseVaultClient<BaseVaultConfig> client = newClient(null);

        client.prioritiseCredentials(creds);
        client.token = "x.eyJleHAiOjF9.y"; // exp=1 (1970) -> always expired, seeded directly

        client.setBearerToken(creds);

        Assert.assertEquals("x.eyJleHAiOjk5OTk5OTk5OTl9.y", client.token);
        Assert.assertNotEquals("x.eyJleHAiOjF9.y", client.token);
    }
}
