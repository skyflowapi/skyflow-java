package com.skyflow;

import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.SkyflowRetryInterceptor;
import com.skyflow.vault.controller.VaultController;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Covers the HTTP timeout / retry configuration and its three-level resolution:
 * vault-level override -> client-wide default -> SDK default.
 */
public class HttpConfigTests {

    // OkHttp's own defaults, applied when we leave a per-attempt timeout unset.
    private static final int OKHTTP_DEFAULT_TIMEOUT_MILLIS = 10_000;
    private static final int SDK_DEFAULT_CALL_TIMEOUT_MILLIS = 60_000;

    private static VaultConfig buildConfig() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault1");
        config.setClusterId("cluster1");
        config.setEnv(Env.DEV);
        return config;
    }

    /** Builds the shared OkHttp client without needing credentials or a live token. */
    private static OkHttpClient httpClientOf(VaultClient client) throws SkyflowException {
        client.updateExecutorInHTTP();
        return client.sharedHttpClient;
    }

    private static int maxRetriesOf(SkyflowRetryInterceptor interceptor) {
        return interceptor.getMaxRetries();
    }

    private static SkyflowRetryInterceptor retryInterceptorOf(OkHttpClient http) {
        for (Interceptor interceptor : http.interceptors()) {
            if (interceptor instanceof SkyflowRetryInterceptor) {
                return (SkyflowRetryInterceptor) interceptor;
            }
        }
        throw new AssertionError("No SkyflowRetryInterceptor installed on the HTTP client");
    }

    // ── SDK defaults (neither level configured) ───────────────────────────────

    @Test
    public void testDefaults_callTimeoutIs60Seconds() throws SkyflowException {
        OkHttpClient http = httpClientOf(new VaultClient(buildConfig(), null));

        Assert.assertEquals(SDK_DEFAULT_CALL_TIMEOUT_MILLIS, http.callTimeoutMillis());
    }

    @Test
    public void testDefaults_retriesAreOff() throws SkyflowException {
        OkHttpClient http = httpClientOf(new VaultClient(buildConfig(), null));

        Assert.assertEquals(0, maxRetriesOf(retryInterceptorOf(http)));
    }

    @Test
    public void testDefaults_perAttemptTimeoutsLeftAtOkHttpDefaults() throws SkyflowException {
        OkHttpClient http = httpClientOf(new VaultClient(buildConfig(), null));

        Assert.assertEquals(OKHTTP_DEFAULT_TIMEOUT_MILLIS, http.connectTimeoutMillis());
        Assert.assertEquals(OKHTTP_DEFAULT_TIMEOUT_MILLIS, http.readTimeoutMillis());
        Assert.assertEquals(OKHTTP_DEFAULT_TIMEOUT_MILLIS, http.writeTimeoutMillis());
    }

    // ── Client-wide values only ───────────────────────────────────────────────

    @Test
    public void testClientWideConfig_appliesWhenVaultLevelUnset() throws SkyflowException {
        VaultClient client = new VaultClient(buildConfig(), null);
        client.setCommonHttpConfig(30, 5, 6, 7, 2, null, null);

        OkHttpClient http = httpClientOf(client);

        Assert.assertEquals(30_000, http.callTimeoutMillis());
        Assert.assertEquals(5_000, http.connectTimeoutMillis());
        Assert.assertEquals(6_000, http.readTimeoutMillis());
        Assert.assertEquals(7_000, http.writeTimeoutMillis());
        Assert.assertEquals(2, maxRetriesOf(retryInterceptorOf(http)));
    }

    // ── Vault-level values only ───────────────────────────────────────────────

    @Test
    public void testVaultLevelConfig_appliesWhenClientWideUnset() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setTimeout(45);
        config.setConnectTimeout(11);
        config.setReadTimeout(12);
        config.setWriteTimeout(13);
        config.setMaxRetries(4);

        OkHttpClient http = httpClientOf(new VaultClient(config, null));

        Assert.assertEquals(45_000, http.callTimeoutMillis());
        Assert.assertEquals(11_000, http.connectTimeoutMillis());
        Assert.assertEquals(12_000, http.readTimeoutMillis());
        Assert.assertEquals(13_000, http.writeTimeoutMillis());
        Assert.assertEquals(4, maxRetriesOf(retryInterceptorOf(http)));
    }

    // ── Precedence: both levels set ───────────────────────────────────────────

    @Test
    public void testPrecedence_vaultLevelBeatsClientWideForEverySetting() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setTimeout(45);
        config.setConnectTimeout(11);
        config.setReadTimeout(12);
        config.setWriteTimeout(13);
        config.setMaxRetries(4);

        VaultClient client = new VaultClient(config, null);
        client.setCommonHttpConfig(30, 5, 6, 7, 2, null, null);

        OkHttpClient http = httpClientOf(client);

        Assert.assertEquals(45_000, http.callTimeoutMillis());
        Assert.assertEquals(11_000, http.connectTimeoutMillis());
        Assert.assertEquals(12_000, http.readTimeoutMillis());
        Assert.assertEquals(13_000, http.writeTimeoutMillis());
        Assert.assertEquals(4, maxRetriesOf(retryInterceptorOf(http)));
    }

    @Test
    public void testPrecedence_settingsResolveIndependently() throws SkyflowException {
        // Vault overrides only the read timeout; everything else falls through to client-wide.
        VaultConfig config = buildConfig();
        config.setReadTimeout(12);

        VaultClient client = new VaultClient(config, null);
        client.setCommonHttpConfig(30, 5, 6, 7, 2, null, null);

        OkHttpClient http = httpClientOf(client);

        Assert.assertEquals(12_000, http.readTimeoutMillis());
        Assert.assertEquals(30_000, http.callTimeoutMillis());
        Assert.assertEquals(5_000, http.connectTimeoutMillis());
        Assert.assertEquals(7_000, http.writeTimeoutMillis());
        Assert.assertEquals(2, maxRetriesOf(retryInterceptorOf(http)));
    }

    @Test
    public void testPrecedence_sdkDefaultUsedWhenBothLevelsNull() throws SkyflowException {
        VaultClient client = new VaultClient(buildConfig(), null);
        client.setCommonHttpConfig(null, null, null, null, null, null, null);

        OkHttpClient http = httpClientOf(client);

        Assert.assertEquals(SDK_DEFAULT_CALL_TIMEOUT_MILLIS, http.callTimeoutMillis());
        Assert.assertEquals(0, maxRetriesOf(retryInterceptorOf(http)));
        Assert.assertEquals(OKHTTP_DEFAULT_TIMEOUT_MILLIS, http.connectTimeoutMillis());
    }

    // ── Cache invalidation ────────────────────────────────────────────────────

    @Test
    public void testSetCommonHttpConfig_rebuildsHttpClientWithNewValues() throws SkyflowException {
        VaultClient client = new VaultClient(buildConfig(), null);
        OkHttpClient first = httpClientOf(client);
        Assert.assertEquals(SDK_DEFAULT_CALL_TIMEOUT_MILLIS, first.callTimeoutMillis());

        client.setCommonHttpConfig(30, null, null, null, 2, null, null);
        OkHttpClient second = httpClientOf(client);

        Assert.assertNotSame(first, second);
        Assert.assertEquals(30_000, second.callTimeoutMillis());
        Assert.assertEquals(2, maxRetriesOf(retryInterceptorOf(second)));
    }

    @Test
    public void testUpdateExecutorInHTTP_reusesCachedClientWhenConfigUnchanged() throws SkyflowException {
        VaultClient client = new VaultClient(buildConfig(), null);

        OkHttpClient first = httpClientOf(client);
        OkHttpClient second = httpClientOf(client);

        Assert.assertSame(first, second);
    }

    // ── Interceptor wiring ────────────────────────────────────────────────────

    @Test
    public void testInterceptors_retryIsOuterSoEachAttemptRereadsTheToken() throws SkyflowException {
        OkHttpClient http = httpClientOf(new VaultClient(buildConfig(), null));

        List<Interceptor> interceptors = http.interceptors();
        Assert.assertEquals(2, interceptors.size());
        Assert.assertTrue("Retry must be registered first so it wraps the auth interceptor",
                interceptors.get(0) instanceof SkyflowRetryInterceptor);
        Assert.assertFalse(interceptors.get(1) instanceof SkyflowRetryInterceptor);
    }

    @Test
    public void testConnectionPool_isConfigured() throws SkyflowException {
        OkHttpClient http = httpClientOf(new VaultClient(buildConfig(), null));

        Assert.assertNotNull(http.connectionPool());
    }

    // ── Boxed-Integer semantics ───────────────────────────────────────────────

    @Test
    public void testExplicitZeroTimeout_isAValueNotAnInheritSignal() throws SkyflowException {
        // Only null means "inherit". An explicit 0 wins over the client-wide value, and OkHttp
        // reads 0 as "no timeout" — so this disables the overall ceiling rather than restoring 60s.
        VaultConfig config = buildConfig();
        config.setTimeout(0);

        VaultClient client = new VaultClient(config, null);
        client.setCommonHttpConfig(30, null, null, null, null, null, null);

        Assert.assertEquals(0, httpClientOf(client).callTimeoutMillis());
    }

    @Test
    public void testExplicitZeroMaxRetries_overridesClientWideRetries() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setMaxRetries(0);

        VaultClient client = new VaultClient(config, null);
        client.setCommonHttpConfig(null, null, null, null, 5, null, null);

        Assert.assertEquals(0, maxRetriesOf(retryInterceptorOf(httpClientOf(client))));
    }

    // ── Retry delays ──────────────────────────────────────────────────────────

    @Test
    public void testDefaults_retryDelaysAre500And2000Millis() throws SkyflowException {
        SkyflowRetryInterceptor retry = retryInterceptorOf(httpClientOf(new VaultClient(buildConfig(), null)));

        Assert.assertEquals(500L, retry.getInitialRetryDelayMillis());
        Assert.assertEquals(2000L, retry.getMaxRetryDelayMillis());
    }

    @Test
    public void testRetryDelays_clientWideValuesApply() throws SkyflowException {
        VaultClient client = new VaultClient(buildConfig(), null);
        client.setCommonHttpConfig(null, null, null, null, 3, 100L, 900L);

        SkyflowRetryInterceptor retry = retryInterceptorOf(httpClientOf(client));

        Assert.assertEquals(100L, retry.getInitialRetryDelayMillis());
        Assert.assertEquals(900L, retry.getMaxRetryDelayMillis());
    }

    @Test
    public void testRetryDelays_vaultLevelBeatsClientWide() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setInitialRetryDelayMillis(250L);
        config.setMaxRetryDelayMillis(4000L);

        VaultClient client = new VaultClient(config, null);
        client.setCommonHttpConfig(null, null, null, null, 3, 100L, 900L);

        SkyflowRetryInterceptor retry = retryInterceptorOf(httpClientOf(client));

        Assert.assertEquals(250L, retry.getInitialRetryDelayMillis());
        Assert.assertEquals(4000L, retry.getMaxRetryDelayMillis());
    }

    @Test
    public void testRetryDelays_resolveIndependentlyOfEachOther() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setMaxRetryDelayMillis(4000L);

        VaultClient client = new VaultClient(config, null);
        client.setCommonHttpConfig(null, null, null, null, 3, 100L, 900L);

        SkyflowRetryInterceptor retry = retryInterceptorOf(httpClientOf(client));

        Assert.assertEquals(100L, retry.getInitialRetryDelayMillis());  // client-wide
        Assert.assertEquals(4000L, retry.getMaxRetryDelayMillis());     // vault
    }

    @Test
    public void testRetryDelays_endToEndThroughTheBuilder() throws SkyflowException {
        Skyflow client = Skyflow.builder()
                .maxRetries(3)
                .initialRetryDelayMillis(100)
                .maxRetryDelayMillis(900)
                .addVaultConfig(buildConfig())
                .build();

        SkyflowRetryInterceptor retry = retryInterceptorOf(httpClientOf(client.vault()));

        Assert.assertEquals(3, retry.getMaxRetries());
        Assert.assertEquals(100L, retry.getInitialRetryDelayMillis());
        Assert.assertEquals(900L, retry.getMaxRetryDelayMillis());
    }

    @Test
    public void testRetryDelays_vaultConfigBeatsBuilderEndToEnd() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setInitialRetryDelayMillis(250L);

        Skyflow client = Skyflow.builder()
                .initialRetryDelayMillis(100)
                .addVaultConfig(config)
                .build();

        Assert.assertEquals(250L,
                retryInterceptorOf(httpClientOf(client.vault())).getInitialRetryDelayMillis());
    }

    @Test
    public void testRetryDelays_survivedUpdateVaultConfig() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(buildConfig());

        VaultConfig update = buildConfig();
        update.setInitialRetryDelayMillis(250L);
        update.setMaxRetryDelayMillis(4000L);

        SkyflowRetryInterceptor retry =
                retryInterceptorOf(httpClientOf(builder.updateVaultConfig(update).build().vault()));

        Assert.assertEquals(250L, retry.getInitialRetryDelayMillis());
        Assert.assertEquals(4000L, retry.getMaxRetryDelayMillis());
    }

    @Test
    public void testRetryDelays_builderMethodsAreFluent() {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder();

        Assert.assertSame(builder, builder.initialRetryDelayMillis(100));
        Assert.assertSame(builder, builder.maxRetryDelayMillis(900));
    }

    // ── Error wrapping ────────────────────────────────────────────────────────

    @Test
    public void testInvalidMaxRetries_wrapsInterceptorIllegalArgumentAsSkyflowException() throws SkyflowException {
        // SkyflowRetryInterceptor rejects negative maxRetries with IllegalArgumentException;
        // updateExecutorInHTTP must translate that (and anything else from client construction)
        // into a SkyflowException rather than letting it escape raw.
        VaultConfig config = buildConfig();
        config.setMaxRetries(-1);
        VaultClient client = new VaultClient(config, null);

        try {
            client.updateExecutorInHTTP();
            Assert.fail("Should have thrown SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    // ── VaultConfig accessors ─────────────────────────────────────────────────

    @Test
    public void testVaultConfig_httpSettingsDefaultToNull() {
        VaultConfig config = new VaultConfig();

        Assert.assertNull(config.getTimeout());
        Assert.assertNull(config.getConnectTimeout());
        Assert.assertNull(config.getReadTimeout());
        Assert.assertNull(config.getWriteTimeout());
        Assert.assertNull(config.getMaxRetries());
    }

    @Test
    public void testVaultConfig_settersRoundTrip() {
        VaultConfig config = new VaultConfig();
        config.setTimeout(45);
        config.setConnectTimeout(11);
        config.setReadTimeout(12);
        config.setWriteTimeout(13);
        config.setMaxRetries(4);

        Assert.assertEquals(Integer.valueOf(45), config.getTimeout());
        Assert.assertEquals(Integer.valueOf(11), config.getConnectTimeout());
        Assert.assertEquals(Integer.valueOf(12), config.getReadTimeout());
        Assert.assertEquals(Integer.valueOf(13), config.getWriteTimeout());
        Assert.assertEquals(Integer.valueOf(4), config.getMaxRetries());
    }

    // ── Skyflow builder wiring ────────────────────────────────────────────────

    @Test
    public void testBuilder_httpConfigSetBeforeAddVaultConfigReachesController() throws SkyflowException {
        Skyflow client = Skyflow.builder()
                .timeout(30)
                .connectTimeout(5)
                .readTimeout(6)
                .writeTimeout(7)
                .maxRetries(2)
                .addVaultConfig(buildConfig())
                .build();

        OkHttpClient http = httpClientOf(client.vault());

        Assert.assertEquals(30_000, http.callTimeoutMillis());
        Assert.assertEquals(5_000, http.connectTimeoutMillis());
        Assert.assertEquals(6_000, http.readTimeoutMillis());
        Assert.assertEquals(7_000, http.writeTimeoutMillis());
        Assert.assertEquals(2, maxRetriesOf(retryInterceptorOf(http)));
    }

    @Test
    public void testBuilder_httpConfigSetAfterAddVaultConfigStillReachesController() throws SkyflowException {
        // Order independence: propagateHttpConfig() must reach controllers already built.
        Skyflow client = Skyflow.builder()
                .addVaultConfig(buildConfig())
                .timeout(30)
                .maxRetries(2)
                .build();

        OkHttpClient http = httpClientOf(client.vault());

        Assert.assertEquals(30_000, http.callTimeoutMillis());
        Assert.assertEquals(2, maxRetriesOf(retryInterceptorOf(http)));
    }

    @Test
    public void testBuilder_vaultConfigOverridesBuilderValueEndToEnd() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setTimeout(45);

        Skyflow client = Skyflow.builder()
                .timeout(30)
                .addVaultConfig(config)
                .build();

        Assert.assertEquals(45_000, httpClientOf(client.vault()).callTimeoutMillis());
    }

    @Test
    public void testBuilder_httpConfigSurvivesUpdateVaultConfig() throws SkyflowException {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .timeout(30)
                .maxRetries(2)
                .addVaultConfig(buildConfig());

        VaultConfig updated = buildConfig();
        updated.setClusterId("cluster2");
        Skyflow client = builder.updateVaultConfig(updated).build();

        OkHttpClient http = httpClientOf(client.vault());

        Assert.assertEquals(30_000, http.callTimeoutMillis());
        Assert.assertEquals(2, maxRetriesOf(retryInterceptorOf(http)));
    }

    @Test
    public void testBuilder_httpConfigAppliesToEveryVault() throws SkyflowException {
        VaultConfig second = new VaultConfig();
        second.setVaultId("vault2");
        second.setClusterId("cluster2");
        second.setEnv(Env.DEV);

        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .addVaultConfig(buildConfig())
                .addVaultConfig(second)
                .timeout(30);

        // vault() resolves the first entry, so drop vault1 to reach the second controller.
        Assert.assertEquals(30_000, httpClientOf(builder.build().vault()).callTimeoutMillis());
        Assert.assertEquals(30_000,
                httpClientOf(builder.removeVaultConfig("vault1").build().vault()).callTimeoutMillis());
    }

    @Test
    public void testBuilder_httpConfigMethodsAreFluent() {
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder();

        Assert.assertSame(builder, builder.timeout(30));
        Assert.assertSame(builder, builder.connectTimeout(5));
        Assert.assertSame(builder, builder.readTimeout(6));
        Assert.assertSame(builder, builder.writeTimeout(7));
        Assert.assertSame(builder, builder.maxRetries(2));
    }

    // ── Precedence through the public API: both levels set ────────────────────
    // Each test sets the same setting on Skyflow.builder() AND on VaultConfig, then asserts the
    // VaultConfig value is what reaches the wire client.

    @Test
    public void testPrecedence_endToEnd_vaultTimeoutBeatsClientLevelTimeout() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setTimeout(45);

        Skyflow client = Skyflow.builder().timeout(30).addVaultConfig(config).build();

        Assert.assertEquals(45_000, httpClientOf(client.vault()).callTimeoutMillis());
    }

    @Test
    public void testPrecedence_endToEnd_vaultConnectTimeoutBeatsClientLevel() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setConnectTimeout(11);

        Skyflow client = Skyflow.builder().connectTimeout(5).addVaultConfig(config).build();

        Assert.assertEquals(11_000, httpClientOf(client.vault()).connectTimeoutMillis());
    }

    @Test
    public void testPrecedence_endToEnd_vaultReadTimeoutBeatsClientLevel() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setReadTimeout(12);

        Skyflow client = Skyflow.builder().readTimeout(6).addVaultConfig(config).build();

        Assert.assertEquals(12_000, httpClientOf(client.vault()).readTimeoutMillis());
    }

    @Test
    public void testPrecedence_endToEnd_vaultWriteTimeoutBeatsClientLevel() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setWriteTimeout(13);

        Skyflow client = Skyflow.builder().writeTimeout(7).addVaultConfig(config).build();

        Assert.assertEquals(13_000, httpClientOf(client.vault()).writeTimeoutMillis());
    }

    @Test
    public void testPrecedence_endToEnd_vaultMaxRetriesBeatsClientLevel() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setMaxRetries(4);

        Skyflow client = Skyflow.builder().maxRetries(2).addVaultConfig(config).build();

        Assert.assertEquals(4, maxRetriesOf(retryInterceptorOf(httpClientOf(client.vault()))));
    }

    @Test
    public void testPrecedence_endToEnd_allFiveSetAtBothLevels() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setTimeout(45);
        config.setConnectTimeout(11);
        config.setReadTimeout(12);
        config.setWriteTimeout(13);
        config.setMaxRetries(4);

        Skyflow client = Skyflow.builder()
                .timeout(30)
                .connectTimeout(5)
                .readTimeout(6)
                .writeTimeout(7)
                .maxRetries(2)
                .addVaultConfig(config)
                .build();

        OkHttpClient http = httpClientOf(client.vault());

        Assert.assertEquals(45_000, http.callTimeoutMillis());
        Assert.assertEquals(11_000, http.connectTimeoutMillis());
        Assert.assertEquals(12_000, http.readTimeoutMillis());
        Assert.assertEquals(13_000, http.writeTimeoutMillis());
        Assert.assertEquals(4, maxRetriesOf(retryInterceptorOf(http)));
    }

    @Test
    public void testPrecedence_endToEnd_partialOverrideTakesEachLevelPerSetting() throws SkyflowException {
        // Vault overrides timeout and maxRetries only; the rest come from the client level.
        VaultConfig config = buildConfig();
        config.setTimeout(45);
        config.setMaxRetries(4);

        Skyflow client = Skyflow.builder()
                .timeout(30)
                .connectTimeout(5)
                .readTimeout(6)
                .writeTimeout(7)
                .maxRetries(2)
                .addVaultConfig(config)
                .build();

        OkHttpClient http = httpClientOf(client.vault());

        Assert.assertEquals(45_000, http.callTimeoutMillis());   // vault
        Assert.assertEquals(4, maxRetriesOf(retryInterceptorOf(http)));  // vault
        Assert.assertEquals(5_000, http.connectTimeoutMillis()); // client
        Assert.assertEquals(6_000, http.readTimeoutMillis());    // client
        Assert.assertEquals(7_000, http.writeTimeoutMillis());   // client
    }

    @Test
    public void testPrecedence_endToEnd_holdsWhenClientLevelIsSetAfterAddVaultConfig() throws SkyflowException {
        // Builder call order must not change who wins.
        VaultConfig config = buildConfig();
        config.setTimeout(45);

        Skyflow client = Skyflow.builder().addVaultConfig(config).timeout(30).build();

        Assert.assertEquals(45_000, httpClientOf(client.vault()).callTimeoutMillis());
    }

    @Test
    public void testPrecedence_endToEnd_appliesPerVaultNotGlobally() throws SkyflowException {
        // vault1 overrides the timeout; vault2 leaves it unset and inherits the client-level value.
        VaultConfig overriding = buildConfig();
        overriding.setTimeout(45);

        VaultConfig inheriting = new VaultConfig();
        inheriting.setVaultId("vault2");
        inheriting.setClusterId("cluster2");
        inheriting.setEnv(Env.DEV);

        Skyflow.SkyflowClientBuilder builder = Skyflow.builder()
                .timeout(30)
                .addVaultConfig(overriding)
                .addVaultConfig(inheriting);

        // vault() resolves the first entry, so drop vault1 to reach the second controller.
        Assert.assertEquals(45_000, httpClientOf(builder.build().vault()).callTimeoutMillis());
        Assert.assertEquals(30_000,
                httpClientOf(builder.removeVaultConfig("vault1").build().vault()).callTimeoutMillis());
    }

    @Test
    public void testPrecedence_endToEnd_vaultStillWinsAfterUpdateVaultConfig() throws SkyflowException {
        VaultConfig config = buildConfig();
        config.setTimeout(45);
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().timeout(30).addVaultConfig(config);

        VaultConfig update = buildConfig();
        update.setTimeout(90);

        Assert.assertEquals(90_000,
                httpClientOf(builder.updateVaultConfig(update).build().vault()).callTimeoutMillis());
    }

    @Test
    public void testPrecedence_endToEnd_updateWithoutHttpSettingsKeepsTheExistingOnes() throws SkyflowException {
        // A null on the incoming config means "leave as is", matching how the base class merges.
        VaultConfig config = buildConfig();
        config.setTimeout(45);
        config.setMaxRetries(4);
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().timeout(30).addVaultConfig(config);

        VaultConfig update = buildConfig();
        update.setClusterId("cluster2");

        OkHttpClient http = httpClientOf(builder.updateVaultConfig(update).build().vault());

        Assert.assertEquals(45_000, http.callTimeoutMillis());
        Assert.assertEquals(4, maxRetriesOf(retryInterceptorOf(http)));
    }

    @Test
    public void testPrecedence_endToEnd_updateVaultConfigCarriesAllFiveHttpSettings() throws SkyflowException {
        // Regression: BaseSkyflow.mergeVaultConfig() carries only env/clusterId/credentials, so
        // without SkyflowClientBuilder.carryHttpOverrides() every value below is silently dropped
        // and the vault keeps running on its original settings.
        VaultConfig config = buildConfig();
        config.setTimeout(45);
        config.setConnectTimeout(11);
        config.setReadTimeout(12);
        config.setWriteTimeout(13);
        config.setMaxRetries(4);
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().addVaultConfig(config);

        VaultConfig update = buildConfig();
        update.setTimeout(90);
        update.setConnectTimeout(21);
        update.setReadTimeout(22);
        update.setWriteTimeout(23);
        update.setMaxRetries(8);

        OkHttpClient http = httpClientOf(builder.updateVaultConfig(update).build().vault());

        Assert.assertEquals(90_000, http.callTimeoutMillis());
        Assert.assertEquals(21_000, http.connectTimeoutMillis());
        Assert.assertEquals(22_000, http.readTimeoutMillis());
        Assert.assertEquals(23_000, http.writeTimeoutMillis());
        Assert.assertEquals(8, maxRetriesOf(retryInterceptorOf(http)));
    }

    @Test
    public void testPrecedence_endToEnd_updateCanIntroduceAVaultOverride() throws SkyflowException {
        // Vault starts with no override (inherits 30), then an update introduces one.
        Skyflow.SkyflowClientBuilder builder = Skyflow.builder().timeout(30).addVaultConfig(buildConfig());
        Assert.assertEquals(30_000, httpClientOf(builder.build().vault()).callTimeoutMillis());

        VaultConfig update = buildConfig();
        update.setTimeout(45);

        Assert.assertEquals(45_000,
                httpClientOf(builder.updateVaultConfig(update).build().vault()).callTimeoutMillis());
    }

    @Test
    public void testController_isAVaultClientSoItInheritsTheHttpConfig() throws SkyflowException {
        Skyflow client = Skyflow.builder().timeout(30).addVaultConfig(buildConfig()).build();

        VaultController controller = client.vault();

        Assert.assertEquals(30_000, httpClientOf(controller).callTimeoutMillis());
    }
}
