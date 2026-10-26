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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
     * always-expired JWT ("<TOKEN_1>", exp=1 => 1970) is seeded directly into the token
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
        client.token = "<TOKEN_1>"; // exp=1 (1970) -> always expired, seeded directly

        client.setBearerToken(creds);

        Assert.assertEquals("x.eyJleHAiOjk5OTk5OTk5OTl9.y", client.token);
        Assert.assertNotEquals("<TOKEN_1>", client.token);
    }

    // ── refresh-and-resend on 401 ────────────────────────────────────────────

    private static String jwtExpiringIn(long seconds) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = enc.encodeToString(("{\"exp\":" + (System.currentTimeMillis() / 1000 + seconds) + "}")
                .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }

    /** A client whose token generation is scripted, so no service-account file is needed. */
    private static final class ScriptedClient extends BaseVaultClient<BaseVaultConfig> {
        final AtomicInteger generations = new AtomicInteger();
        final java.util.List<String> script = new ArrayList<>();
        SkyflowException failure;

        ScriptedClient(BaseCredentials credentials) {
            super(new BaseVaultConfig(), credentials);
            this.finalCredentials = credentials;
        }

        @Override
        protected String generateBearerToken(BaseCredentials credentials) throws SkyflowException {
            int n = generations.getAndIncrement();
            if (failure != null) {
                throw failure;
            }
            return script.get(Math.min(n, script.size() - 1));
        }
    }

    private static BaseCredentials mintableCredentials() {
        BaseCredentials credentials = new BaseCredentials();
        credentials.setCredentialsString("{\"clientID\":\"c\"}");
        return credentials;
    }

    @Test
    public void testRefreshAfter401_expiredSdkTokenIsRegeneratedOnceAndCached() {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        String rejected = jwtExpiringIn(-2);
        client.token = rejected;
        client.script.add("fresh-token");

        Assert.assertEquals("fresh-token", client.refreshBearerTokenAfterUnauthorized(rejected));
        Assert.assertEquals("fresh-token", client.token);
        Assert.assertEquals(1, client.generations.get());
    }

    @Test
    public void testRefreshAfter401_tokenInsideWindowCountsAsExpired() {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        String rejected = jwtExpiringIn(30);
        client.token = rejected;
        client.script.add("fresh-token");

        Assert.assertEquals("fresh-token", client.refreshBearerTokenAfterUnauthorized(rejected));
    }

    @Test
    public void testRefreshAfter401_tokenWithLifeLeftIsNotRetried() {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        String rejected = jwtExpiringIn(600);
        client.token = rejected;
        client.script.add("fresh-token");

        Assert.assertNull("a 401 on a live token is not expiry", client.refreshBearerTokenAfterUnauthorized(rejected));
        Assert.assertEquals("must not spend a mint on a non-expiry 401", 0, client.generations.get());
        Assert.assertEquals(rejected, client.token);
    }

    @Test
    public void testRefreshAfter401_staticTokenIsNeverRegenerated() {
        BaseCredentials credentials = new BaseCredentials();
        String rejected = jwtExpiringIn(-2);
        credentials.setToken(rejected);
        ScriptedClient client = new ScriptedClient(credentials);
        client.token = rejected;
        client.script.add("fresh-token");

        Assert.assertNull(client.refreshBearerTokenAfterUnauthorized(rejected));
        Assert.assertEquals(0, client.generations.get());
    }

    @Test
    public void testRefreshAfter401_apiKeyIsNeverRegenerated() {
        BaseCredentials credentials = new BaseCredentials();
        credentials.setApiKey("sky-test-key");
        ScriptedClient client = new ScriptedClient(credentials);
        client.token = "sky-test-key";
        client.script.add("fresh-token");

        Assert.assertNull(client.refreshBearerTokenAfterUnauthorized("sky-test-key"));
        Assert.assertEquals(0, client.generations.get());
    }

    @Test
    public void testRefreshAfter401_noCredentialsResolvedYet() {
        ScriptedClient client = new ScriptedClient(null);
        Assert.assertNull(client.refreshBearerTokenAfterUnauthorized(jwtExpiringIn(-2)));
    }

    @Test
    public void testRefreshAfter401_generationFailureReturnsNullAndKeepsToken() {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        String rejected = jwtExpiringIn(-2);
        client.token = rejected;
        client.failure = new SkyflowException("auth service down");

        Assert.assertNull(client.refreshBearerTokenAfterUnauthorized(rejected));
        Assert.assertEquals(rejected, client.token);
    }

    @Test
    public void testRefreshAfter401_sameTokenBackMeansStop() {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        String rejected = jwtExpiringIn(-2);
        client.token = rejected;
        client.script.add(rejected);

        Assert.assertNull(client.refreshBearerTokenAfterUnauthorized(rejected));
        Assert.assertEquals(1, client.generations.get());
    }

    @Test
    public void testRefreshAfter401_reusesTokenAnotherRequestAlreadyRegenerated() {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        String rejected = jwtExpiringIn(-2);
        client.token = "already-fresh";
        client.script.add("never-used");

        Assert.assertEquals("already-fresh", client.refreshBearerTokenAfterUnauthorized(rejected));
        Assert.assertEquals(0, client.generations.get());
    }

    @Test
    public void testRefreshAfter401_concurrentRejectionsShareOneGeneration() throws Exception {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        String rejected = jwtExpiringIn(-2);
        client.token = rejected;
        client.script.add("fresh-token");
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        java.util.List<Future<String>> results = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return client.refreshBearerTokenAfterUnauthorized(rejected);
                }));
            }
            start.countDown();
            for (Future<String> result : results) {
                Assert.assertEquals("fresh-token", result.get(10, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        Assert.assertEquals("eight 401s on one token must cost one mint", 1, client.generations.get());
    }

    /** Fake chain that replays scripted status codes and records what was proceeded. */
    private static final class ScriptedChain implements Interceptor.Chain {
        private final Request request = new Request.Builder().url("https://example.com/v1/x").build();
        private final int[] codes;
        final java.util.List<Request> proceeded = new ArrayList<>();

        ScriptedChain(int... codes) {
            this.codes = codes;
        }

        @Override
        public Request request() {
            return request;
        }

        @Override
        public Response proceed(Request request) {
            proceeded.add(request);
            int code = codes[Math.min(proceeded.size() - 1, codes.length - 1)];
            return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(code).message("status " + code)
                    .body(okhttp3.ResponseBody.create(null, new byte[0]))
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
            return this;
        }

        @Override
        public int readTimeoutMillis() {
            return 0;
        }

        @Override
        public Interceptor.Chain withReadTimeout(int timeout, TimeUnit unit) {
            return this;
        }

        @Override
        public int writeTimeoutMillis() {
            return 0;
        }

        @Override
        public Interceptor.Chain withWriteTimeout(int timeout, TimeUnit unit) {
            return this;
        }
    }

    @Test
    public void testAuthInterceptor_401OnExpiredTokenIsResentOnceWithFreshToken() throws IOException {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        client.token = jwtExpiringIn(-2);
        client.script.add("fresh-token");
        ScriptedChain chain = new ScriptedChain(401, 200);

        Response response = client.authInterceptor(() -> client.token).intercept(chain);

        Assert.assertEquals(200, response.code());
        Assert.assertEquals(2, chain.proceeded.size());
        Assert.assertEquals("Bearer fresh-token", chain.proceeded.get(1).header("Authorization"));
        Assert.assertEquals(chain.request().url(), chain.proceeded.get(1).url());
    }

    @Test
    public void testAuthInterceptor_second401IsReturnedNotRetried() throws IOException {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        client.token = jwtExpiringIn(-2);
        client.script.add("fresh-token");
        ScriptedChain chain = new ScriptedChain(401, 401);

        Assert.assertEquals(401, client.authInterceptor(() -> client.token).intercept(chain).code());
        Assert.assertEquals(2, chain.proceeded.size());
        Assert.assertEquals(1, client.generations.get());
    }

    @Test
    public void testAuthInterceptor_401OnStaticTokenIsNotRetried() throws IOException {
        BaseCredentials credentials = new BaseCredentials();
        credentials.setToken(jwtExpiringIn(-2));
        ScriptedClient client = new ScriptedClient(credentials);
        client.token = credentials.getToken();
        ScriptedChain chain = new ScriptedChain(401, 200);

        Assert.assertEquals(401, client.authInterceptor(() -> client.token).intercept(chain).code());
        Assert.assertEquals(1, chain.proceeded.size());
    }

    @Test
    public void testAuthInterceptor_401OnLiveTokenIsNotRetried() throws IOException {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        client.token = jwtExpiringIn(600);
        client.script.add("fresh-token");
        ScriptedChain chain = new ScriptedChain(401, 200);

        Assert.assertEquals(401, client.authInterceptor(() -> client.token).intercept(chain).code());
        Assert.assertEquals(1, chain.proceeded.size());
        Assert.assertEquals(0, client.generations.get());
    }

    @Test
    public void testAuthInterceptor_403IsNotRetried() throws IOException {
        ScriptedClient client = new ScriptedClient(mintableCredentials());
        client.token = jwtExpiringIn(-2);
        client.script.add("fresh-token");
        ScriptedChain chain = new ScriptedChain(403, 200);

        Assert.assertEquals(403, client.authInterceptor(() -> client.token).intercept(chain).code());
        Assert.assertEquals(1, chain.proceeded.size());
        Assert.assertEquals(0, client.generations.get());
    }
}
