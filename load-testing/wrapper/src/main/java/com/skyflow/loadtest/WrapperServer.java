package com.skyflow.loadtest;

/**
 * Skyflow SDK Load Testing - Wrapper Server  (v3 SDK)
 *
 * HTTP server that wraps the Skyflow Java v3 SDK.
 * K6 hits this server → this server calls the SDK → SDK hits EchoServer.
 *
 * Configuration (environment variables):
 *   VAULT_ID       Skyflow vault ID          (default: mock-vault-id)
 *   VAULT_URL      Echo server base URL      (default: http://localhost:3015)
 *   WRAPPER_PORT   Port this server listens  (default: 8080)
 *   API_KEY        Static API key for auth   (default: mock-api-key)
 *
 * Endpoints exposed to K6:
 *   POST /insert        -> skyflow.vault().bulkInsert()
 *   POST /detokenize    -> skyflow.vault().bulkDetokenize()
 *   GET  /health        -> liveness check
 *   GET  /metrics       -> JVM + SDK call counters
 *
 * Optional request body fields (all endpoints):
 *   { "table": "my_table", "num_records": 3, "token": "tok-abc" }
 *
 * Build:
 *   # 1. Install v3 SDK to local Maven repo
 *   mvn install -f v3/pom.xml -DskipTests
 *   # 2. Build wrapper fat jar
 *   mvn package -f load-testing/wrapper/pom.xml
 *
 * Run:
 *   VAULT_URL=http://localhost:3015 \
 *   java -jar load-testing/wrapper/target/skyflow-load-test-wrapper-1.0.0.jar
 */

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class WrapperServer {

    // -- Configuration -------------------------------------------------------
    static final String VAULT_ID     = env("VAULT_ID",      "mock-vault-id");
    static final String VAULT_URL    = env("VAULT_URL",     "http://localhost:3015");
    static final int    PORT         = Integer.parseInt(env("WRAPPER_PORT", "8080"));
//    static final String API_KEY      = env("API_KEY",       "mock-api-key");
    static final String TOKEN        = "Token";
    // Default test data (overridable per-request)
    static final String DEFAULT_TABLE = "load_test_table";
    static final String DEFAULT_TOKEN = "mock-token-0000-0000-0000-000000000001";

    // -- Counters ------------------------------------------------------------
    static final AtomicLong reqTotal   = new AtomicLong();
    static final AtomicLong reqSuccess = new AtomicLong();
    static final AtomicLong reqError   = new AtomicLong();

    // -- Shared SDK client ---------------------------------------------------
    static Skyflow skyflowClient;

    public static void main(String[] args) throws Exception {
        Credentials credentials = new Credentials();
        // Using apiKey avoids JWT generation — the SDK passes it directly as Bearer token.
        credentials.setToken(TOKEN);

        VaultConfig config = new VaultConfig();
        config.setVaultId(VAULT_ID);
        config.setVaultURL(VAULT_URL);
        config.setCredentials(credentials);

        skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)   // suppress SDK info logs during load test
                .addVaultConfig(config)
                .build();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/insert",     new InsertHandler());
        server.createContext("/detokenize", new DetokenizeHandler());
        server.createContext("/health",     new HealthHandler());
        server.createContext("/metrics",    new MetricsHandler());
        server.setExecutor(Executors.newFixedThreadPool(200));
        server.start();

        System.out.printf("[WrapperServer-v3] port=%d  vault=%s  echo=%s%n",
                PORT, VAULT_ID, VAULT_URL);
    }

    public static String getTOKEN() {
        return TOKEN;
    }

    // =========================================================================
    // POST /insert  ->  skyflow.vault().bulkInsert()
    // =========================================================================
    static class InsertHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            reqTotal.incrementAndGet();
            JSONObject params  = parseBody(ex);
            String table       = str(params, "table",       DEFAULT_TABLE);
            int    numRecords  = (int) longVal(params, "num_records", 1);

            ArrayList<InsertRecord> records = new ArrayList<>();
            for (int i = 0; i < numRecords; i++) {
                Map<String, Object> data = new HashMap<>();
                data.put("mock_field", "load-test-" + i + "-" + System.currentTimeMillis());
                InsertRecord rec = InsertRecord.builder()
//                        .table(table)
                        .data(data)
                        .build();
                records.add(rec);
            }

            InsertRequest request = InsertRequest.builder()
                    .table(table)
                    .records(records)
                    .build();

            try {
                com.skyflow.vault.data.InsertResponse response =
                        skyflowClient.vault().bulkInsert(request);
                reqSuccess.incrementAndGet();
                sendJson(ex, 200, toJson(response));
            } catch (SkyflowException e) {
                reqError.incrementAndGet();
                sendJson(ex, 500, errorJson(e.getMessage()));
            }
        }
    }

    // =========================================================================
    // POST /detokenize  ->  skyflow.vault().bulkDetokenize()
    // =========================================================================
    static class DetokenizeHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            reqTotal.incrementAndGet();
            JSONObject params = parseBody(ex);
            String token      = str(params, "token", DEFAULT_TOKEN);
            int    numTokens  = (int) longVal(params, "num_tokens", 1);

            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < numTokens; i++) {
                tokens.add(token + "-" + i);
            }

            DetokenizeRequest request = DetokenizeRequest.builder()
                    .tokens(tokens)
                    .build();

            try {
                com.skyflow.vault.data.DetokenizeResponse response =
                        skyflowClient.vault().bulkDetokenize(request);
                reqSuccess.incrementAndGet();
                sendJson(ex, 200, toJson(response));
            } catch (SkyflowException e) {
                reqError.incrementAndGet();
                sendJson(ex, 500, errorJson(e.getMessage()));
            }
        }
    }

    // =========================================================================
    // Utility handlers
    // =========================================================================

    static class HealthHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            sendJson(ex, 200,
                    "{\"status\":\"ok\",\"sdk\":\"v3\",\"vault_id\":\"" + VAULT_ID
                    + "\",\"vault_url\":\"" + VAULT_URL + "\"}");
        }
    }

    /**
     * GET /metrics — JVM heap, GC stats, thread count, and SDK call counters.
     * Poll this during load tests to detect memory leaks or thread pool saturation.
     */
    static class MetricsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            Runtime rt    = Runtime.getRuntime();
            long usedMb   = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            long totalMb  = rt.totalMemory() / (1024 * 1024);
            long maxMb    = rt.maxMemory()   / (1024 * 1024);
            int  threads  = Thread.activeCount();

            long gcCount = 0, gcTimeMs = 0;
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            for (GarbageCollectorMXBean gc : gcBeans) {
                gcCount  += gc.getCollectionCount();
                gcTimeMs += gc.getCollectionTime();
            }

            String body = String.format(
                    "{\"sdk_calls\":{\"total\":%d,\"success\":%d,\"error\":%d},"
                  + "\"jvm\":{\"heap_used_mb\":%d,\"heap_total_mb\":%d,\"heap_max_mb\":%d,"
                  + "\"active_threads\":%d,\"gc_count\":%d,\"gc_time_ms\":%d}}",
                    reqTotal.get(), reqSuccess.get(), reqError.get(),
                    usedMb, totalMb, maxMb, threads, gcCount, gcTimeMs);
            sendJson(ex, 200, body);
        }
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    static void sendJson(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    static JSONObject parseBody(HttpExchange ex) {
        try {
            String raw = readBody(ex.getRequestBody());
            if (raw == null || raw.trim().isEmpty()) return new JSONObject();
            return (JSONObject) new JSONParser().parse(raw);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    static String readBody(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] data = new byte[4096]; int n;
        while ((n = is.read(data)) != -1) buf.write(data, 0, n);
        return buf.toString("UTF-8");
    }

    static String str(JSONObject obj, String key, String def) {
        if (obj == null || !obj.containsKey(key)) return def;
        Object v = obj.get(key);
        return v != null ? v.toString() : def;
    }

    static long longVal(JSONObject obj, String key, long def) {
        if (obj == null || !obj.containsKey(key)) return def;
        Object v = obj.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return def; }
    }

    static String errorJson(String msg) {
        String safe = msg == null ? "unknown error" : msg.replace("\"", "'");
        return "{\"error\":\"" + safe + "\"}";
    }

    /** Minimal toString for v3 response objects (they have their own toString via Gson). */
    static String toJson(Object obj) {
        return obj != null ? obj.toString() : "{}";
    }

    static String env(String key, String def) {
        String v = System.getenv(key);
        return (v != null && !v.isEmpty()) ? v : def;
    }
}
