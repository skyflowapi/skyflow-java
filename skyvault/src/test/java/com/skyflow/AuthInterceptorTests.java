package com.skyflow;

import com.skyflow.config.BaseCredentials;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.FakeChain;
import okhttp3.Interceptor;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The auth interceptor installed by skyvault's {@code VaultClient.updateExecutorInHTTP()}.
 *
 * <p>Skyvault's shared OkHttp client carries this one interceptor; the generated {@code ClientOptions}
 * adds no transport retry when a client is supplied. The refresh-and-resend logic lives in
 * {@code BaseVaultClient} and is unit tested there; these tests prove the skyvault client wires it
 * in with {@code this::getToken}, so a token refreshed after a {@code 401} reaches the resend.
 * Mirrors flowvault's {@code AuthInterceptorTests}.
 */
public class AuthInterceptorTests {

    private static VaultConfig config() {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault1");
        config.setClusterId("cluster1");
        config.setEnv(Env.DEV);
        return config;
    }

    private static VaultConfig configWithCredentialsString() {
        Credentials credentials = new Credentials();
        credentials.setCredentialsString("{\"clientID\":\"c\"}");
        VaultConfig config = config();
        config.setCredentials(credentials);
        return config;
    }

    private static String jwtExpiringIn(long seconds) {
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = enc.encodeToString(("{\"exp\":" + (System.currentTimeMillis() / 1000 + seconds) + "}")
                .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }

    /** Skyvault client with mintable credentials and a scripted token source. */
    private static final class MintingClient extends VaultClient {
        final AtomicInteger generations = new AtomicInteger();
        final String fresh;

        MintingClient(String fresh) {
            super(configWithCredentialsString(), null);
            this.fresh = fresh;
            this.finalCredentials = getVaultConfig().getCredentials();
        }

        @Override
        protected String generateBearerToken(BaseCredentials credentials) {
            generations.incrementAndGet();
            return fresh;
        }
    }

    /** updateExecutorInHTTP is private in skyvault; install the client the way setBearerToken does. */
    private static Interceptor authInterceptorOf(VaultClient client) throws Exception {
        Method install = VaultClient.class.getDeclaredMethod("updateExecutorInHTTP");
        install.setAccessible(true);
        install.invoke(client);
        List<Interceptor> interceptors = client.sharedHttpClient.interceptors();
        Assert.assertEquals("skyvault's shared client carries the auth interceptor alone", 1, interceptors.size());
        return interceptors.get(0);
    }

    @Test
    public void testAuthInterceptor_regeneratesOnceAndResendsOn401ForExpiredToken() throws Exception {
        MintingClient client = new MintingClient("fresh-token");
        client.token = jwtExpiringIn(-1);
        FakeChain chain = new FakeChain(401, 200);

        Assert.assertEquals(200, authInterceptorOf(client).intercept(chain).code());
        Assert.assertEquals(2, chain.calls());
        Assert.assertEquals("Bearer fresh-token", chain.lastProceeded().header("Authorization"));
        Assert.assertEquals("the cache must hold the new token for the calls that follow", "fresh-token", client.token);
        Assert.assertEquals(1, client.generations.get());
        Assert.assertTrue("the rejected response must be closed before the resend", chain.bodies().get(0).isClosed());
    }

    @Test
    public void testAuthInterceptor_401ForTokenWithLifeLeftIsReturnedWithoutRegenerating() throws Exception {
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
    public void testAuthInterceptor_401WithStaticTokenIsReturnedAsToday() throws Exception {
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
    public void testAuthInterceptor_401WithApiKeyIsReturnedAsToday() throws Exception {
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
        FakeChain chain = new FakeChain(401, 200);

        Assert.assertEquals(401, authInterceptorOf(client).intercept(chain).code());
        Assert.assertEquals(1, chain.calls());
        Assert.assertEquals("Bearer " + apiKey, chain.lastProceeded().header("Authorization"));
    }

    @Test
    public void testAuthInterceptor_resendThatAlsoGets401IsNotRetriedAgain() throws Exception {
        MintingClient client = new MintingClient("fresh-token");
        client.token = jwtExpiringIn(-1);
        FakeChain chain = new FakeChain(401, 401, 200);

        Assert.assertEquals(401, authInterceptorOf(client).intercept(chain).code());
        Assert.assertEquals(2, chain.calls());
        Assert.assertEquals(1, client.generations.get());
    }

    @Test
    public void testAuthInterceptor_readsTheRefreshedTokenOnTheResend() throws Exception {
        // setBearerToken() is what production code calls; it must install the same interceptor and
        // the resend must carry the token that refresh stored, read back through getToken().
        MintingClient client = new MintingClient("fresh-token");
        client.setBearerToken();
        Assert.assertEquals("fresh-token", client.token);
        client.token = jwtExpiringIn(-1);
        FakeChain chain = new FakeChain(401, 200);

        Interceptor interceptor = client.sharedHttpClient.interceptors().get(0);
        Assert.assertEquals(200, interceptor.intercept(chain).code());
        Assert.assertEquals("Bearer fresh-token", chain.lastProceeded().header("Authorization"));
    }

    @Test
    public void testAuthInterceptor_isTheOnlyInterceptorOnTheSharedClient() throws SkyflowException {
        Credentials credentials = new Credentials();
        credentials.setToken(jwtExpiringIn(3600));
        VaultConfig config = config();
        config.setCredentials(credentials);
        VaultClient client = new VaultClient(config, null);
        client.setBearerToken();

        Assert.assertEquals("no transport retry wraps auth in skyvault; a 401 reaches auth directly",
                1, client.sharedHttpClient.interceptors().size());
    }
}
