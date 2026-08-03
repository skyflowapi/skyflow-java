package com.skyflow.vault.controller;

import com.skyflow.VaultClient;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Batch size and concurrency resolution for all four bulk operations.
 *
 * <p>These settings are user-tunable only through the environment, so the tests swap
 * {@link VaultController#settingResolver} rather than mutating the JVM environment. Each operation
 * has its own near-identical resolver, so every case runs against all four to stop one drifting.
 */
public class BatchConfigResolutionTests {

    private final Function<String, String> originalResolver = VaultController.settingResolver;

    @After
    public void restoreResolver() {
        VaultController.settingResolver = originalResolver;
    }

    /** Operation name, its env-var prefix, and its default/max constants. */
    private static final String[][] OPS = {
            {"Insert", "INSERT"},
            {"Detokenize", "DETOKENIZE"},
            {"Tokenize", "TOKENIZE"},
            {"DeleteTokens", "DELETE_TOKENS"},
    };

    private static void useSettings(Map<String, String> settings) {
        VaultController.settingResolver = settings::get;
    }

    private static Map<String, String> settings(String prefix, String batchSize, String concurrency) {
        Map<String, String> map = new HashMap<>();
        if (batchSize != null) {
            map.put(prefix + "_BATCH_SIZE", batchSize);
        }
        if (concurrency != null) {
            map.put(prefix + "_CONCURRENCY_LIMIT", concurrency);
        }
        return map;
    }

    private static VaultController controller() throws SkyflowException {
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault1");
        config.setClusterId("cluster1");
        config.setEnv(Env.DEV);
        return new VaultController(config, null);
    }

    /** Invokes the private configure&lt;Op&gt;ConcurrencyAndBatchSize and reads the result. */
    private static int[] resolve(String op, int totalRequests) throws Exception {
        Method method = VaultController.class.getDeclaredMethod(
                "configure" + op + "ConcurrencyAndBatchSize", int.class);
        method.setAccessible(true);
        Object cfg;
        try {
            cfg = method.invoke(controller(), totalRequests);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
        Field batchSize = cfg.getClass().getDeclaredField("batchSize");
        Field concurrency = cfg.getClass().getDeclaredField("concurrencyLimit");
        batchSize.setAccessible(true);
        concurrency.setAccessible(true);
        return new int[] {(int) batchSize.get(cfg), (int) concurrency.get(cfg)};
    }

    private static int defaultBatchSize(String prefix) {
        switch (prefix) {
            case "INSERT": return Constants.INSERT_BATCH_SIZE;
            case "DETOKENIZE": return Constants.DETOKENIZE_BATCH_SIZE;
            case "TOKENIZE": return Constants.TOKENIZE_BATCH_SIZE;
            default: return Constants.DELETE_TOKENS_BATCH_SIZE;
        }
    }

    private static int maxBatchSize(String prefix) {
        switch (prefix) {
            case "INSERT": return Constants.MAX_INSERT_BATCH_SIZE;
            case "DETOKENIZE": return Constants.MAX_DETOKENIZE_BATCH_SIZE;
            case "TOKENIZE": return Constants.MAX_TOKENIZE_BATCH_SIZE;
            default: return Constants.MAX_DELETE_TOKENS_BATCH_SIZE;
        }
    }

    private static int maxConcurrency(String prefix) {
        switch (prefix) {
            case "INSERT": return Constants.MAX_INSERT_CONCURRENCY_LIMIT;
            case "DETOKENIZE": return Constants.MAX_DETOKENIZE_CONCURRENCY_LIMIT;
            case "TOKENIZE": return Constants.MAX_TOKENIZE_CONCURRENCY_LIMIT;
            default: return Constants.MAX_DELETE_TOKENS_CONCURRENCY_LIMIT;
        }
    }

    // ── batch size ────────────────────────────────────────────────────────────

    @Test
    public void testBatchSize_defaultsWhenNothingIsConfigured() throws Exception {
        for (String[] op : OPS) {
            useSettings(new HashMap<>());
            Assert.assertEquals(op[0], defaultBatchSize(op[1]), resolve(op[0], 10_000)[0]);
        }
    }

    @Test
    public void testBatchSize_honoursAValidSetting() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "200", null));
            Assert.assertEquals(op[0], 200, resolve(op[0], 10_000)[0]);
        }
    }

    @Test
    public void testBatchSize_isCappedAtTheMaximum() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "999999", null));
            Assert.assertEquals(op[0], maxBatchSize(op[1]), resolve(op[0], 10_000)[0]);
        }
    }

    @Test
    public void testBatchSize_exactlyAtTheMaximumIsAccepted() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], String.valueOf(maxBatchSize(op[1])), null));
            Assert.assertEquals(op[0], maxBatchSize(op[1]), resolve(op[0], 10_000)[0]);
        }
    }

    @Test
    public void testBatchSize_zeroFallsBackToTheDefault() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "0", null));
            Assert.assertEquals(op[0], defaultBatchSize(op[1]), resolve(op[0], 10_000)[0]);
        }
    }

    @Test
    public void testBatchSize_negativeFallsBackToTheDefault() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "-5", null));
            Assert.assertEquals(op[0], defaultBatchSize(op[1]), resolve(op[0], 10_000)[0]);
        }
    }

    @Test
    public void testBatchSize_nonNumericFallsBackToTheDefault() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "not-a-number", null));
            Assert.assertEquals(op[0], defaultBatchSize(op[1]), resolve(op[0], 10_000)[0]);
        }
    }

    @Test
    public void testBatchSize_emptyStringFallsBackToTheDefault() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "", null));
            Assert.assertEquals(op[0], defaultBatchSize(op[1]), resolve(op[0], 10_000)[0]);
        }
    }

    // ── concurrency limit ─────────────────────────────────────────────────────

    @Test
    public void testConcurrency_defaultsToOneWhenNothingIsConfigured() throws Exception {
        for (String[] op : OPS) {
            useSettings(new HashMap<>());
            Assert.assertEquals(op[0], 1, resolve(op[0], 10_000)[1]);
        }
    }

    @Test
    public void testConcurrency_honoursAValidSetting() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "1", "5"));
            Assert.assertEquals(op[0], 5, resolve(op[0], 10_000)[1]);
        }
    }

    @Test
    public void testConcurrency_isCappedAtTheMaximum() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "1", "999"));
            Assert.assertEquals(op[0], maxConcurrency(op[1]), resolve(op[0], 10_000)[1]);
        }
    }

    @Test
    public void testConcurrency_zeroFallsBackToTheDefault() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "1", "0"));
            Assert.assertEquals(op[0], 1, resolve(op[0], 10_000)[1]);
        }
    }

    @Test
    public void testConcurrency_negativeFallsBackToTheDefault() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "1", "-3"));
            Assert.assertEquals(op[0], 1, resolve(op[0], 10_000)[1]);
        }
    }

    @Test
    public void testConcurrency_nonNumericFallsBackToTheDefault() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "1", "lots"));
            Assert.assertEquals(op[0], 1, resolve(op[0], 10_000)[1]);
        }
    }

    // ── concurrency is further capped by how many batches there actually are ──

    @Test
    public void testConcurrency_neverExceedsTheNumberOfBatches() throws Exception {
        for (String[] op : OPS) {
            // 25 records at batch size 10 is 3 batches, so 10 threads would leave 7 idle.
            useSettings(settings(op[1], "10", "10"));
            Assert.assertEquals(op[0], 3, resolve(op[0], 25)[1]);
        }
    }

    @Test
    public void testConcurrency_singleBatchUsesASingleThread() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "100", "10"));
            Assert.assertEquals(op[0], 1, resolve(op[0], 5)[1]);
        }
    }

    @Test
    public void testConcurrency_exactBatchMultipleDoesNotRoundUp() throws Exception {
        for (String[] op : OPS) {
            useSettings(settings(op[1], "10", "10"));
            Assert.assertEquals(op[0], 2, resolve(op[0], 20)[1]);
        }
    }

    @Test
    public void testBatchSizeAndConcurrency_resolveIndependently() throws Exception {
        for (String[] op : OPS) {
            // batch size invalid (falls back), concurrency valid
            useSettings(settings(op[1], "oops", "4"));
            int[] cfg = resolve(op[0], 10_000);
            Assert.assertEquals(op[0], defaultBatchSize(op[1]), cfg[0]);
            Assert.assertEquals(op[0], 4, cfg[1]);
        }
    }
}
