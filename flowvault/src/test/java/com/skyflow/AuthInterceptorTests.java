package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.RetryInterceptor;
import com.skyflow.utils.FakeChain;
import okhttp3.Interceptor;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * The auth interceptor installed by {@link VaultClient#updateExecutorInHTTP()}.
 *
 * <p>It is a lambda, so the only way to reach it is to pull it back off the built OkHttpClient and
 * drive it with a fake chain. The property that matters is that it reads {@code this.token} on every
 * request rather than capturing it once — that is why retry is registered outside auth, so a replayed
 * attempt picks up a refreshed token instead of resending an expired one.
 */
public class AuthInterceptorTests {

    private static final String API_KEY = "sky-ab123-abcd1234cdef1234abcd4321cdef4321";

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
            if (!(interceptor instanceof RetryInterceptor)) {
                return interceptor;
            }
        }
        throw new AssertionError("no auth interceptor installed");
    }

    @Test
    public void testAuthInterceptor_addsBearerAuthorizationHeader() throws SkyflowException, IOException {
        Credentials credentials = new Credentials();
        credentials.setApiKey(API_KEY);
        VaultConfig config = config();
        config.setCredentials(credentials);
        VaultClient client = new VaultClient(config, null);
        client.setBearerToken();

        FakeChain chain = new FakeChain(200);
        authInterceptorOf(client).intercept(chain);

        Assert.assertEquals("Bearer " + API_KEY, chain.lastProceeded().header("Authorization"));
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
}
