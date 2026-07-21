package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.RetryInterceptor;
import java.lang.reflect.Field;
import java.time.Duration;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies the HTTP timeout/retry config is wired onto the shared OkHttpClient and that the
 * timeout precedence (vault-level > client-wide > SDK default) resolves correctly. Uses an API-key
 * credential so {@code setBearerToken()} builds the client without a network call.
 */
public class VaultClientHttpConfigTests {

    private static final int DEFAULT_CALL_TIMEOUT_MS = 60000; // SDK default = 60s

    private VaultConfig apiKeyConfig() {
        VaultConfig cfg = new VaultConfig();
        cfg.setVaultId("vault123");
        cfg.setClusterId("cluster123");
        cfg.setEnv(Env.PROD);
        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        cfg.setCredentials(credentials);
        return cfg;
    }

    private OkHttpClient sharedClient(VaultClient client) throws Exception {
        Field field = VaultClient.class.getDeclaredField("sharedHttpClient");
        field.setAccessible(true);
        return (OkHttpClient) field.get(client);
    }

    @Test
    public void defaultTimeoutAppliedWhenNothingConfigured() throws Exception {
        VaultConfig cfg = apiKeyConfig();
        VaultClient client = new VaultClient(cfg, cfg.getCredentials());
        client.setBearerToken();

        Assert.assertEquals(DEFAULT_CALL_TIMEOUT_MS, sharedClient(client).callTimeoutMillis());
    }

    @Test
    public void vaultLevelTimeoutOverridesDefault() throws Exception {
        VaultConfig cfg = apiKeyConfig();
        cfg.setTimeout(15); // seconds
        VaultClient client = new VaultClient(cfg, cfg.getCredentials());
        client.setBearerToken();

        Assert.assertEquals(15000, sharedClient(client).callTimeoutMillis());
    }

    @Test
    public void clientLevelTimeoutUsedWhenNoVaultOverride() throws Exception {
        VaultConfig cfg = apiKeyConfig();
        VaultClient client = new VaultClient(cfg, cfg.getCredentials());
        client.setCommonHttpConfig(45, null, null, null); // client-wide 45s, no vault override
        client.setBearerToken();

        Assert.assertEquals(45000, sharedClient(client).callTimeoutMillis());
    }

    @Test
    public void vaultLevelTimeoutOverridesClientLevel() throws Exception {
        VaultConfig cfg = apiKeyConfig();
        cfg.setTimeout(15); // vault-level
        VaultClient client = new VaultClient(cfg, cfg.getCredentials());
        client.setCommonHttpConfig(45, null, null, null); // client-wide 45s — should lose to vault's 15s
        client.setBearerToken();

        Assert.assertEquals(15000, sharedClient(client).callTimeoutMillis());
    }

    @Test
    public void callTimeoutIsBoundedNotZero() throws Exception {
        // Regression guard: the real bug was callTimeout == 0 (unbounded). It must never be 0 by default.
        VaultConfig cfg = apiKeyConfig();
        VaultClient client = new VaultClient(cfg, cfg.getCredentials());
        client.setBearerToken();

        Assert.assertTrue(sharedClient(client).callTimeoutMillis() > 0);
    }

    @Test
    public void vaultConfigStoresHttpFieldsAndDefaultsToNull() throws SkyflowException {
        VaultConfig cfg = new VaultConfig();
        Assert.assertNull(cfg.getTimeout());
        Assert.assertNull(cfg.getMaxRetries());
        Assert.assertNull(cfg.getInitialRetryDelay());
        Assert.assertNull(cfg.getMaxRetryDelay());

        cfg.setTimeout(30);
        cfg.setMaxRetries(2);
        cfg.setInitialRetryDelay(250L);
        cfg.setMaxRetryDelay(1500L);

        Assert.assertEquals(Integer.valueOf(30), cfg.getTimeout());
        Assert.assertEquals(Integer.valueOf(2), cfg.getMaxRetries());
        Assert.assertEquals(Long.valueOf(250L), cfg.getInitialRetryDelay());
        Assert.assertEquals(Long.valueOf(1500L), cfg.getMaxRetryDelay());
    }

    private RetryInterceptor retryInterceptor(VaultClient client) throws Exception {
        for (Interceptor interceptor : sharedClient(client).interceptors()) {
            if (interceptor instanceof RetryInterceptor) {
                return (RetryInterceptor) interceptor;
            }
        }
        throw new AssertionError("Fern RetryInterceptor was not attached to the shared client");
    }

    private int intField(Object obj, String name) throws Exception {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(obj);
    }

    // Fern stores the delays as java.time.Duration; compare in millis.
    private long durationFieldMillis(Object obj, String name) throws Exception {
        Field field = obj.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return ((Duration) field.get(obj)).toMillis();
    }

    @Test
    public void defaultRetryConfigAppliedToInterceptor() throws Exception {
        VaultConfig cfg = apiKeyConfig();
        VaultClient client = new VaultClient(cfg, cfg.getCredentials());
        client.setBearerToken();

        RetryInterceptor ri = retryInterceptor(client);
        Assert.assertEquals(0, intField(ri, "maxRetries"));                 // SDK default: retries off (opt-in)
        Assert.assertEquals(500L, durationFieldMillis(ri, "initialRetryDelay"));
        Assert.assertEquals(2000L, durationFieldMillis(ri, "maxRetryDelay"));
    }

    @Test
    public void vaultLevelRetryConfigOverridesDefault() throws Exception {
        VaultConfig cfg = apiKeyConfig();
        cfg.setMaxRetries(1);
        cfg.setInitialRetryDelay(250L);
        cfg.setMaxRetryDelay(1200L);
        VaultClient client = new VaultClient(cfg, cfg.getCredentials());
        client.setBearerToken();

        RetryInterceptor ri = retryInterceptor(client);
        Assert.assertEquals(1, intField(ri, "maxRetries"));
        Assert.assertEquals(250L, durationFieldMillis(ri, "initialRetryDelay"));
        Assert.assertEquals(1200L, durationFieldMillis(ri, "maxRetryDelay"));
    }

    @Test
    public void clientLevelRetryConfigUsedWhenNoVaultOverride() throws Exception {
        VaultConfig cfg = apiKeyConfig();
        VaultClient client = new VaultClient(cfg, cfg.getCredentials());
        client.setCommonHttpConfig(null, 2, 300L, 1500L); // client-wide retry config, no vault override
        client.setBearerToken();

        RetryInterceptor ri = retryInterceptor(client);
        Assert.assertEquals(2, intField(ri, "maxRetries"));
        Assert.assertEquals(300L, durationFieldMillis(ri, "initialRetryDelay"));
        Assert.assertEquals(1500L, durationFieldMillis(ri, "maxRetryDelay"));
    }
}
