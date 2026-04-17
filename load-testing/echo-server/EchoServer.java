/**
 * Skyflow SDK Load Testing - Echo/Mock Server  (v3 SDK)
 *
 * Simulates only the Vault v3 API endpoints that VaultController actually calls:
 *   POST /v2/records/insert     <- bulkInsert() / bulkInsertAsync()
 *   POST /v2/tokens/detokenize  <- bulkDetokenize() / bulkDetokenizeAsync()
 *
 * Usage:
 *   javac EchoServer.java
 *   java EchoServer [port] [wait_time_ms] [error_rate_percent]
 *
 * Examples:
 *   java EchoServer 3015              # defaults: port=3015, wait=0ms, error=0%
 *   java EchoServer 3015 50           # 50 ms simulated latency per request
 *   java EchoServer 3015 50 10        # 50 ms latency + 10 % random 5xx
 *
 * wait_time_ms / expected_response_code can also be passed per-request:
 *   - as JSON fields in the request body  (e.g. {"wait_time_ms":50,...})
 *   - or as query params                  (?wait_time_ms=50&expected_response_code=500)
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EchoServer {

    static int defaultWaitMs = 0;
    static int errorRatePct  = 0;
    static final Random rng  = new Random();

    static final AtomicLong totalRequests = new AtomicLong();
    static final AtomicLong totalErrors   = new AtomicLong();

    public static void main(String[] args) throws IOException {
        int port = 3015;
        if (args.length >= 1) port          = Integer.parseInt(args[0]);
        if (args.length >= 2) defaultWaitMs = Integer.parseInt(args[1]);
        if (args.length >= 3) errorRatePct  = Integer.parseInt(args[2]);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/v2/records/insert",    new InsertHandler());
        server.createContext("/v2/tokens/detokenize", new DetokenizeHandler());
        server.createContext("/metrics",              new MetricsHandler());
        server.createContext("/health",               new HealthHandler());
//        int echoThreads = defaultWaitMs > 0 ? 500 : 100;
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.printf("[EchoServer-v3] port=%d  wait=%dms  error_rate=%d%%%n",
                port, defaultWaitMs, errorRatePct);
    }

    // =========================================================================
    // POST /v2/records/insert
    // Called by VaultController.bulkInsert() / bulkInsertAsync()
    //
    // SDK request body:
    //   {"vaultId":"...","tableName":"...","records":[{"data":{"col":"val"},...}],"upsert":{...}}
    //
    // Expected response (RecordResponseObject per record):
    //   {"records":[{"skyflowID":"uuid","tokens":{"col":"tok"},"tableName":"tbl","httpCode":200}]}
    // =========================================================================
    static class InsertHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            totalRequests.incrementAndGet();
            try {
                String body  = readBody(ex.getRequestBody());
                String query = ex.getRequestURI().getQuery();

                simulateLatency(body, query);

                int code = resolveExpectedCode(body, query);
                if (code != 200) {
                    totalErrors.incrementAndGet();
                    sendJson(ex, code, errorBody(code));
                    return;
                }

                int count = countPattern(body, "\"data\"");
                if (count == 0) count = 1;
                String table = orDefault(extractString(body, "tableName"), "load_test_table");

                StringBuilder sb = new StringBuilder("{\"records\":[");
                for (int i = 0; i < count; i++) {
                    if (i > 0) sb.append(",");
                    String id = uuid();
                    sb.append("{\"skyflowID\":\"").append(id).append("\",")
                      .append("\"tokens\":{\"mock_field\":\"tok-").append(id, 0, 8).append("\"},")
                      .append("\"tableName\":\"").append(table).append("\",")
                      .append("\"httpCode\":200}");
                }
                sb.append("]}");
                // add logs here
                System.out.println("Insert call received with record count: " + count);
                sendJson(ex, 200, sb.toString());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendJson(ex, 500, errorBody(500));
            } catch (IOException e) {
                throw e;
            }
        }
    }

    // =========================================================================
    // POST /v2/tokens/detokenize
    // Called by VaultController.bulkDetokenize() / bulkDetokenizeAsync()
    //
    // SDK request body:
    //   {"vaultId":"...","tokens":["tok1","tok2"],"tokenGroupRedactions":[...]}
    //
    // Expected response (DetokenizeResponseObject per token):
    //   {"response":[{"token":"tok1","value":"plain-val","httpCode":200},...]}
    // =========================================================================
    static class DetokenizeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            totalRequests.incrementAndGet();
            try {
                String body  = readBody(ex.getRequestBody());
                String query = ex.getRequestURI().getQuery();

                simulateLatency(body, query);

                int code = resolveExpectedCode(body, query);
                if (code != 200) {
                    totalErrors.incrementAndGet();
                    sendJson(ex, code, errorBody(code));
                    return;
                }

                // Parse tokens array: "tokens":["tok1","tok2"]
                Pattern p = Pattern.compile("\"tokens\"\\s*:\\s*\\[([^\\]]+)\\]");
                Matcher m = p.matcher(body);
                StringBuilder sb = new StringBuilder("{\"response\":[");
                boolean first = true;

                if (m.find()) {
                    Matcher tm = Pattern.compile("\"([^\"]+)\"").matcher(m.group(1));
                    while (tm.find()) {
                        String tok = tm.group(1);
                        if (!first) sb.append(",");
                        sb.append("{\"token\":\"").append(tok)
                          .append("\",\"value\":\"plain-").append(tok, 0, Math.min(6, tok.length()))
                          .append("\",\"httpCode\":200}");
                        first = false;
                    }
                }
                if (first) {
                    sb.append("{\"token\":\"mock-token\",\"value\":\"mock-plain-value\",\"httpCode\":200}");
                }
                sb.append("]}");
                sendJson(ex, 200, sb.toString());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendJson(ex, 500, errorBody(500));
            } catch (IOException e) {
                throw e;
            }
        }
    }

    // =========================================================================
    // /metrics  — request counters + JVM stats
    // =========================================================================
    static class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            Runtime rt   = Runtime.getRuntime();
            long usedMb  = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            long maxMb   = rt.maxMemory() / (1024 * 1024);
            sendJson(ex, 200, String.format(
                    "{\"total_requests\":%d,\"total_errors\":%d,"
                  + "\"heap_used_mb\":%d,\"heap_max_mb\":%d,\"active_threads\":%d}",
                    totalRequests.get(), totalErrors.get(),
                    usedMb, maxMb, Thread.activeCount()));
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            sendJson(ex, 200, "{\"status\":\"ok\",\"api\":\"v3\"}");
        }
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    static void simulateLatency(String body, String query) throws InterruptedException {
        int ms = defaultWaitMs;
        String qv = extractQueryParam(query, "wait_time_ms");
        String bv = extractLong(body, "wait_time_ms");
        if (qv != null) ms = Integer.parseInt(qv);
        else if (bv != null) ms = Integer.parseInt(bv);
        if (ms > 0) Thread.sleep(ms);
    }

    static int resolveExpectedCode(String body, String query) {
        if (errorRatePct > 0 && rng.nextInt(100) < errorRatePct) return 500;
        String qv = extractQueryParam(query, "expected_response_code");
        String bv = extractLong(body, "expected_response_code");
        if (qv != null) return Integer.parseInt(qv);
        if (bv != null) return Integer.parseInt(bv);
        return 200;
    }

    static String errorBody(int code) {
        return "{\"error\":{\"http_code\":" + code + ",\"message\":\"Simulated server error\"}}";
    }

    static void sendJson(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    static String readBody(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096]; int n;
        while ((n = is.read(tmp)) != -1) buf.write(tmp, 0, n);
        return buf.toString("UTF-8");
    }

    static String extractQueryParam(String query, String key) {
        if (query == null || query.isEmpty()) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    static String extractLong(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    static String extractString(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    static int countPattern(String text, String literal) {
        if (text == null) return 0;
        int count = 0, idx = 0;
        while ((idx = text.indexOf(literal, idx)) != -1) { count++; idx += literal.length(); }
        return count;
    }

    static String uuid() { return UUID.randomUUID().toString(); }

    static String orDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}
