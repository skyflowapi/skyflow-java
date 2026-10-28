package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.RetryInterceptor;
import com.skyflow.utils.FakeChain;
import com.skyflow.utils.SkyflowRetryInterceptor;
import okhttp3.Interceptor;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The auth interceptor installed by {@link VaultClient#updateExecutorInHTTP()}.
 *
 * <p>It is a lambda, so the only way to reach it is to pull it back off the built OkHttpClient and
 * drive it with a fake chain. The property that matters is that it reads {@code this.token} on every
 * request rather than capturing it once — that is why retry is registered outside auth, so a replayed
 * attempt picks up a refreshed token instead of resending an expired one.
 */
public class AuthInterceptorTests {

    private static VaultConfig config() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault1");
        config.setClusterId("cluster1");
        config.setEnv(Env.DEV);
        return config;
    }

    private static Interceptor authInterceptorOf(VaultClient client) throws SkyflowException {
        client.updateExecutorInHTTP();
        List<Interceptor> interceptors = client.sharedHttpClient.interceptors();
        for (Interceptor interceptor : interceptors) {
            if (!(interceptor instanceof SkyflowRetryInterceptor) && !(interceptor instanceof RetryInterceptor)) {
                return interceptor;
            }
        }
        throw new AssertionError("no auth interceptor installed");
    }

    @Test
    public void testAuthInterceptor_addsBearerAuthorizationHeader() throws SkyflowException, IOException {
        // Loaded from dummy-non-secrets/ (Gitleaks-excluded) rather than a string literal,
        // since an API-key-shaped literal would itself trip the scanner.
        String apiKey = new String(Files.readAllBytes(
                Paths.get("./src/test/resources/dummy-non-secrets/dummy-api-key.txt")), StandardCharsets.UTF_8).trim();
        Credentials credentials = new Credentials();
        credentials.setApiKey(apiKey);
        VaultConfig config = config();
        config.setCredentials(credentials);
        VaultClient client = new VaultClient(config, null);
        client.setBearerToken();

        FakeChain chain = new FakeChain(200);
        authInterceptorOf(client).intercept(chain);

        Assert.assertEquals("Bearer " + apiKey, chain.lastProceeded().header("Authorization"));
    }

    @Test
    public void testAuthInterceptor_readsTheTokenFreshOnEveryRequest() throws SkyflowException, IOException {
        VaultClient client = new VaultClient(config(), null);
        Interceptor auth = authInterceptorOf(client);

        client.token = "first-token";
        FakeChain first = new FakeChain(200);
        auth.intercept(first);

        client.token = "second-token";
        FakeChain second = new FakeChain(200);
        auth.intercept(second);

        Assert.assertEquals("Bearer first-token", first.lastProceeded().header("Authorization"));
        Assert.assertEquals("A captured token would resend the stale value after a refresh",
                "Bearer second-token", second.lastProceeded().header("Authorization"));
    }

    @Test
    public void testAuthInterceptor_leavesTheRestOfTheRequestAlone() throws SkyflowException, IOException {
        VaultClient client = new VaultClient(config(), null);
        client.token = "t";
        FakeChain chain = new FakeChain(200);

        authInterceptorOf(client).intercept(chain);

        Assert.assertEquals(chain.request().url(), chain.lastProceeded().url());
        Assert.assertEquals(chain.request().method(), chain.lastProceeded().method());
    }

    @Test
    public void testAuthInterceptor_returnsTheChainResponseUntouched() throws SkyflowException, IOException {
        VaultClient client = new VaultClient(config(), null);
        client.token = "t";
        FakeChain chain = new FakeChain(503);

        Assert.assertEquals(503, authInterceptorOf(client).intercept(chain).code());
        Assert.assertEquals("auth must not retry - that is the outer interceptor's job", 1, chain.calls());
    }

    // ── refresh-and-resend on 401, wired through the flowvault client ────────

    private static String jwtExpiringIn(long seconds) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = enc.encodeToString(("{\"exp\":" + (System.currentTimeMillis() / 1000 + seconds) + "}")
                .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }

    /** Flowvault client with mintable credentials and a scripted token source. */
    private static final class MintingClient extends VaultClient {
        final AtomicInteger generations = new AtomicInteger();
        final String fresh;

        MintingClient(String fresh) throws SkyflowException {
            super(configWithCredentialsString(), null);
            this.fresh = fresh;
            this.finalCredentials = getVaultConfig().getCredentials();
        }

        @Override
        protected String generateBearerToken(com.skyflow.config.BaseCredentials credentials) {
            generations.incrementAndGet();
            return fresh;
        }
    }

    private static VaultConfig configWithCredentialsString() {
        Credentials credentials = new Credentials();
        credentials.setCredentialsString("{\"clientID\":\"c\"}");
        VaultConfig config = config();
        config.setCredentials(credentials);
        return config;
    }

    @Test
    public void testAuthInterceptor_regeneratesOnceAndResendsOn401ForExpiredToken() throws SkyflowException, IOException {
        MintingClient client = new MintingClient("fresh-token");
        client.token = jwtExpiringIn(-1);
        FakeChain chain = new FakeChain(401, 200);

        Assert.assertEquals(200, authInterceptorOf(client).intercept(chain).code());
        Assert.assertEquals(2, chain.calls());
        Assert.assertEquals("Bearer fresh-token", chain.lastProceeded().header("Authorization"));
        Assert.assertEquals("the cache must hold the new token for the batches that follow", "fresh-token", client.token);
        Assert.assertEquals(1, client.generations.get());
        Assert.assertTrue("the rejected response must be closed before the resend", chain.bodies().get(0).isClosed());
    }

    @Test
    public void testAuthInterceptor_401ForTokenWithLifeLeftIsReturnedWithoutRegenerating() throws SkyflowException, IOException {
        MintingClient client = new MintingClient("fresh-token");
        String live = jwtExpiringIn(900);
        client.token = live;
        FakeChain chain = new FakeChain(401, 200);

        Assert.assertEquals(401, authInterceptorOf(client).intercept(chain).code());
        Assert.assertEquals(1, chain.calls());
        Assert.assertEquals(0, client.generations.get());
        Assert.assertEquals(live, client.token);
    }

    @Test
    public void testAuthInterceptor_401WithStaticTokenIsReturnedAsToday() throws SkyflowException, IOException {
        Credentials credentials = new Credentials();
        credentials.setToken(jwtExpiringIn(-1));
        VaultConfig config = config();
        config.setCredentials(credentials);
        VaultClient client = new VaultClient(config, null);
        client.setBearerToken();
        FakeChain chain = new FakeChain(401, 200);

        Assert.assertEquals(401, authInterceptorOf(client).intercept(chain).code());
        Assert.assertEquals(1, chain.calls());
    }

    @Test
    public void testAuthInterceptor_resendThatAlsoGets401IsNotRetriedAgain() throws SkyflowException, IOException {
        MintingClient client = new MintingClient("fresh-token");
        client.token = jwtExpiringIn(-1);
        FakeChain chain = new FakeChain(401, 401, 200);

        Assert.assertEquals(401, authInterceptorOf(client).intercept(chain).code());
        Assert.assertEquals(2, chain.calls());
        Assert.assertEquals(1, client.generations.get());
    }

    @Test
    public void testAuthInterceptor_sitsInsideTheTransportRetryInterceptor() throws SkyflowException {
        VaultClient client = new VaultClient(config(), null);
        client.updateExecutorInHTTP();
        List<Interceptor> interceptors = client.sharedHttpClient.interceptors();

        Assert.assertEquals(2, interceptors.size());
        Assert.assertTrue("transport retries must wrap auth so each attempt re-reads the token",
                interceptors.get(0) instanceof SkyflowRetryInterceptor);
    }
}
