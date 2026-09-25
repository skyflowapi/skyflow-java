package com.example.otel.benchmark;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A stand-in for the vault that speaks just enough HTTP/1.1 for the benchmark.
 *
 * <h2>Why a mock at all</h2>
 * The customer's claim is about the SDK, not about Skyflow's servers or the internet in between.
 * Against a real vault, network and server time are two orders of magnitude larger than anything
 * the SDK does and they move from run to run, so a difference of a few hundred microseconds is
 * unmeasurable no matter how many samples you collect. Against a loopback server that answers in
 * tens of microseconds, SDK overhead stops being noise and becomes most of the signal — the
 * harshest possible test for the SDK, which is what you want when answering a complaint rather
 * than marketing a result. Set {@code MOCK_LATENCY_MS} to watch that fixed overhead shrink as a
 * proportion of the total as the backend gets slower, which is the other half of the argument.
 *
 * <h2>Why this is hand-rolled rather than {@code com.sun.net.httpserver}</h2>
 * The JDK's built-in server sends response headers and response body as separate TCP segments.
 * Interacting with Nagle's algorithm on one side and delayed ACK on the other, that produces a
 * dead-on 40 ms stall between {@code responseHeadersEnd} and {@code responseBodyEnd} on a
 * significant share of keep-alive requests. A 40 ms artefact in a benchmark whose subject is a
 * sub-millisecond difference does not just add noise — it swamps the measurement and lands on
 * whichever client happens to trigger it, which is exactly how a benchmark ends up "proving"
 * something about the wrong component.
 *
 * <p>This server therefore does two things the JDK's does not: it sets {@code TCP_NODELAY} on
 * every accepted socket, and it writes status line, headers and body in a single
 * {@code write()}. Responses then leave in one segment and the measured latency is the server's
 * actual work.
 *
 * <p>Endpoints: {@code POST /v2/records/insert}, {@code POST /v2/tokens/detokenize},
 * {@code GET /healthz}. Authorization is accepted but never verified; this server holds no data.
 */
public final class MockVaultServer {

    private static final Gson GSON = new Gson();
    private static final byte[] CRLF = {'\r', '\n'};

    /** Prints the first few requests and the handler's own time. See {@code MOCK_DEBUG}. */
    private static final boolean DEBUG = Boolean.parseBoolean(String.valueOf(System.getenv("MOCK_DEBUG")));
    private static final int DEBUG_REQUESTS = 6;

    private final ServerSocket serverSocket;
    private final ExecutorService workers;
    private final long latencyMs;
    private final AtomicLong requests = new AtomicLong();
    private volatile boolean running = true;

    private MockVaultServer(ServerSocket serverSocket, ExecutorService workers, long latencyMs) {
        this.serverSocket = serverSocket;
        this.workers = workers;
        this.latencyMs = latencyMs;
    }

    public static void main(String[] args) throws Exception {
        int port = intEnv("MOCK_PORT", 3015);
        int threads = intEnv("MOCK_THREADS", Runtime.getRuntime().availableProcessors() * 4);
        long latencyMs = intEnv("MOCK_LATENCY_MS", 0);

        MockVaultServer mock = start(port, threads, latencyMs);
        System.out.printf(Locale.US,
                "mock vault listening on http://127.0.0.1:%d (threads=%d, latency=%dms)%n",
                mock.port(), threads, latencyMs);
        System.out.println("point the benchmark at it with TARGET_URL=http://127.0.0.1:" + mock.port());
        Runtime.getRuntime().addShutdownHook(new Thread(mock::stop));
        Thread.currentThread().join();
    }

    public static MockVaultServer start(int port, int threads, long latencyMs) throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new java.net.InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 1024);

        ExecutorService workers = Executors.newFixedThreadPool(threads);
        MockVaultServer mock = new MockVaultServer(serverSocket, workers, latencyMs);

        Thread acceptor = new Thread(mock::acceptLoop, "mock-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
        return mock;
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public long requestCount() {
        return requests.get();
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // Closing the listener is how the accept loop is woken; failure here means it is
            // already closed, which is the desired state anyway.
        }
        workers.shutdownNow();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                // The reason this class exists. Without it the kernel holds back the small
                // response segment waiting for an ACK, and the client sees a 40 ms stall.
                socket.setTcpNoDelay(true);
                workers.submit(() -> serve(socket));
            } catch (IOException e) {
                if (running) {
                    System.err.println("mock accept failed: " + e.getMessage());
                }
            }
        }
    }

    /** One connection, kept alive until the peer closes it or sends something unparseable. */
    private void serve(Socket socket) {
        try (Socket open = socket;
             InputStream in = new java.io.BufferedInputStream(socket.getInputStream(), 16384);
             OutputStream out = socket.getOutputStream()) {
            while (running) {
                Request request = Request.read(in);
                if (request == null) {
                    return;
                }
                long arrived = System.nanoTime();
                long n = requests.incrementAndGet();
                if (DEBUG && n <= DEBUG_REQUESTS) {
                    System.out.printf("req#%d %s %s%s%n", n, request.method, request.path, request.headerDump);
                }

                if (latencyMs > 0) {
                    // Holding the worker thread is what a backend doing real work would do, so
                    // the mock's behaviour under concurrency stays representative.
                    TimeUnit.MILLISECONDS.sleep(latencyMs);
                }

                String body = dispatch(request);
                writeResponse(out, 200, body);

                if (DEBUG && n <= DEBUG_REQUESTS) {
                    System.out.printf("req#%d handled in %.3fms (server side)%n",
                            n, (System.nanoTime() - arrived) / 1e6);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // A client closing a pooled connection is routine; nothing here is worth reporting.
        }
    }

    private String dispatch(Request request) {
        if (request.path.startsWith("/healthz")) {
            return "{\"status\":\"ok\"}";
        }
        JsonObject payload = request.body.isEmpty()
                ? new JsonObject()
                : JsonParser.parseString(request.body).getAsJsonObject();
        if (request.path.startsWith("/v2/records/insert")) {
            return GSON.toJson(insert(payload));
        }
        if (request.path.startsWith("/v2/tokens/detokenize")) {
            return GSON.toJson(detokenize(payload));
        }
        return "{\"error\":\"unknown endpoint\"}";
    }

    /**
     * Mirrors {@code V1InsertResponse}: one record out per record in, each carrying a skyflowID
     * and a token for every column sent, so the SDK's response mapping does the same amount of
     * work it would against a real vault.
     */
    private static JsonObject insert(JsonObject request) {
        JsonArray out = new JsonArray();
        JsonArray records = request.has("records") ? request.getAsJsonArray("records") : new JsonArray();
        for (JsonElement element : records) {
            JsonObject record = element.getAsJsonObject();
            JsonObject tokens = new JsonObject();
            if (record.has("data") && record.get("data").isJsonObject()) {
                for (Map.Entry<String, JsonElement> field : record.getAsJsonObject("data").entrySet()) {
                    tokens.addProperty(field.getKey(), UUID.randomUUID().toString());
                }
            }
            JsonObject responseRecord = new JsonObject();
            responseRecord.addProperty("skyflowID", UUID.randomUUID().toString());
            responseRecord.add("tokens", tokens);
            if (record.has("tableName")) {
                responseRecord.add("tableName", record.get("tableName"));
            }
            out.add(responseRecord);
        }
        JsonObject response = new JsonObject();
        response.add("records", out);
        return response;
    }

    /** Mirrors {@code V1FlowDetokenizeResponse}: one entry per requested token. */
    private static JsonObject detokenize(JsonObject request) {
        JsonArray out = new JsonArray();
        JsonArray tokens = request.has("tokens") ? request.getAsJsonArray("tokens") : new JsonArray();
        for (JsonElement token : tokens) {
            JsonObject entry = new JsonObject();
            entry.add("token", token);
            entry.addProperty("value", "detokenized-" + token.getAsString());
            entry.addProperty("httpCode", 200);
            out.add(entry);
        }
        JsonObject response = new JsonObject();
        response.add("response", out);
        return response;
    }

    /**
     * Status line, headers and body in one write, so the whole response leaves in one segment.
     * Splitting this back into three writes reintroduces the 40 ms stall described on the class.
     */
    private static void writeResponse(OutputStream out, int status, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bodyBytes.length + 256);
        buffer.write(("HTTP/1.1 " + status + " OK").getBytes(StandardCharsets.US_ASCII));
        buffer.write(CRLF);
        buffer.write("Content-Type: application/json".getBytes(StandardCharsets.US_ASCII));
        buffer.write(CRLF);
        buffer.write(("Content-Length: " + bodyBytes.length).getBytes(StandardCharsets.US_ASCII));
        buffer.write(CRLF);
        buffer.write("Connection: keep-alive".getBytes(StandardCharsets.US_ASCII));
        buffer.write(CRLF);
        buffer.write(("x-request-id: " + UUID.randomUUID()).getBytes(StandardCharsets.US_ASCII));
        buffer.write(CRLF);
        buffer.write(CRLF);
        buffer.write(bodyBytes);
        buffer.writeTo(out);
        out.flush();
    }

    // ── Minimal request parsing ──────────────────────────────────────────────

    private static final class Request {
        final String method;
        final String path;
        final String body;
        final String headerDump;

        private Request(String method, String path, String body, String headerDump) {
            this.method = method;
            this.path = path;
            this.body = body;
            this.headerDump = headerDump;
        }

        /**
         * Reads one request. Only Content-Length bodies are supported: the SDK and the benchmark's
         * baseline both send a byte array of known length, so chunked encoding never arrives, and
         * accepting it would mean writing a parser nothing exercises.
         *
         * @return null at end of stream, which is a peer closing a pooled connection
         */
        static Request read(InputStream in) throws IOException {
            String requestLine = readLine(in);
            if (requestLine == null || requestLine.isEmpty()) {
                return null;
            }
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                return null;
            }

            int contentLength = 0;
            StringBuilder dump = DEBUG ? new StringBuilder() : null;
            String header;
            while ((header = readLine(in)) != null && !header.isEmpty()) {
                if (dump != null) {
                    dump.append("\n    ").append(header);
                }
                int colon = header.indexOf(':');
                if (colon > 0 && "content-length".equalsIgnoreCase(header.substring(0, colon).trim())) {
                    contentLength = Integer.parseInt(header.substring(colon + 1).trim());
                }
            }

            String body = "";
            if (contentLength > 0) {
                byte[] bytes = new byte[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int chunk = in.read(bytes, read, contentLength - read);
                    if (chunk < 0) {
                        return null;
                    }
                    read += chunk;
                }
                body = new String(bytes, StandardCharsets.UTF_8);
            }
            return new Request(parts[0], parts[1], body, dump == null ? "" : dump.toString());
        }

        private static String readLine(InputStream in) throws IOException {
            ByteArrayOutputStream line = new ByteArrayOutputStream(128);
            int c;
            while ((c = in.read()) != -1) {
                if (c == '\n') {
                    byte[] bytes = line.toByteArray();
                    int length = bytes.length > 0 && bytes[bytes.length - 1] == '\r'
                            ? bytes.length - 1
                            : bytes.length;
                    return new String(bytes, 0, length, StandardCharsets.US_ASCII);
                }
                line.write(c);
            }
            return line.size() == 0 ? null : line.toString("US-ASCII");
        }
    }

    private static int intEnv(String key, int fallback) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? fallback : Integer.parseInt(value.trim());
    }
}
