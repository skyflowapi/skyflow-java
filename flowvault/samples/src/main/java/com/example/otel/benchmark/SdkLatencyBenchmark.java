package com.example.otel.benchmark;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.controller.VaultController;
import com.skyflow.vault.data.DetokenizeOptions;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.InsertOptions;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.InsertResponseRecord;
import com.skyflow.vault.data.Token;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;

import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Measures, from outside the Skyflow SDK, how much latency the SDK adds on top of the vault API.
 *
 * <h2>What this answers</h2>
 * "The Java SDK is slower than calling the API directly" is a claim about a difference, so it can
 * only be settled by measuring both sides under identical conditions. This harness runs, in one
 * JVM, against one server, with one payload:
 *
 * <ul>
 *   <li><b>sdk</b> — {@code vault.insert(...)} / {@code vault.detokenize(...)}
 *   <li><b>raw</b> — the same JSON POSTed to the same endpoint with a bare OkHttp client
 * </ul>
 *
 * The two paths are interleaved iteration by iteration, so JIT state, GC pauses, CPU frequency
 * and server-side jitter land on both roughly equally instead of favouring whichever ran first.
 * The difference between their distributions is the SDK's cost. Everything else — connection
 * reuse, payload size, TLS, server work — is held constant by construction.
 *
 * <h2>Why the raw baseline also uses OkHttp</h2>
 * The SDK's transport is OkHttp 4.x with a 10-connection pool. A baseline built on
 * HttpURLConnection would measure "OkHttp vs HttpURLConnection" as much as "SDK vs no SDK", and
 * any difference found could be argued either way. Using the same client, the same pool settings
 * and the same timeouts leaves exactly one variable: the SDK's own code — validation, request
 * mapping, Jackson serialisation and deserialisation, the retry and auth interceptors, and
 * response mapping.
 *
 * <h2>Why nothing inside the SDK was touched</h2>
 * Instrumentation compiled into the SDK would measure a build the customer does not run, and
 * would invite the obvious objection. Everything here is sample-application code against the
 * SDK's public surface. The one probe that reaches inside a call is
 * {@link com.skyflow.vault.data.RequestInterceptor}, a public SDK hook that fires immediately
 * before the HTTP request is issued; it splits an SDK call into the work done before the wire and
 * everything from the wire onwards, at the cost of one {@code System.nanoTime()}.
 *
 * <h2>Why the instrumentation does not distort the result</h2>
 * No measured window contains an OpenTelemetry call. Windows record {@code nanoTime} longs only;
 * spans are materialised afterwards with explicit start/end timestamps (see {@link EpochClock}).
 * The run begins with a self-check that reports what that materialisation costs, so the figure is
 * on the table rather than assumed away.
 *
 * <h2>Running it</h2>
 * <pre>
 *   # terminal 1
 *   java -cp ... com.example.otel.MockVaultServer
 *
 *   # terminal 2
 *   TARGET_URL=http://127.0.0.1:3015 java -cp ... com.example.otel.SdkLatencyBenchmark insert
 * </pre>
 *
 * Env: TARGET_URL (required), VAULT_ID, CLUSTER_ID, API_KEY, TABLE, COLUMN, ROWS, BATCH_SIZE,
 * VUS, ITERATIONS, WARMUP_ITERATIONS, MODE (interleaved|sdk|raw), CSV_DIR, LOG_SPANS.
 *
 * <p>Note {@code TARGET_URL} rather than {@code VAULT_URL}: the SDK reads {@code VAULT_URL} from
 * the environment ahead of its own config and requires https, which rules out a loopback mock.
 */
public final class SdkLatencyBenchmark {

    // Span names. The summary table is keyed off these, and they are what shows up in a trace
    // viewer if LOG_SPANS is on or the exporter is later pointed at a collector.
    static final String SPAN_SDK = "skyflow.sdk.call";
    static final String SPAN_SDK_PREPARE = "skyflow.sdk.prepare";
    static final String SPAN_SDK_TRANSPORT = "skyflow.sdk.transport_and_decode";
    static final String SPAN_RAW = "skyflow.raw.call";
    static final String SPAN_RAW_CONNECT = "skyflow.raw.acquire_connection";
    static final String SPAN_RAW_SERVER = "skyflow.raw.server_wait";
    static final String SPAN_RAW_READ = "skyflow.raw.read_response";

    private static final AttributeKey<String> ATTR_OPERATION = Telemetry.OPERATION;
    private static final AttributeKey<String> ATTR_PATH = Telemetry.PATH;
    private static final AttributeKey<Long> ATTR_PAYLOAD_BYTES = AttributeKey.longKey("skyflow.request.body_bytes");

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static void main(String[] args) throws Exception {
        String operation = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "insert";
        if (!"insert".equals(operation) && !"detokenize".equals(operation)) {
            System.err.println("Usage: SdkLatencyBenchmark <insert|detokenize>");
            System.exit(2);
        }

        Config config = Config.fromEnvironment(operation);
        config.print();

        Telemetry telemetry = Telemetry.create("skyflow-sdk-latency-benchmark",
                config.logSpans, config.iterations * 8 + 1024);
        try {
            reportInstrumentationCost(telemetry);

            Skyflow skyflow = buildSkyflowClient(config);
            // Building the client wipes the root JUL handlers (see restoreConsoleLogging), which
            // is what OpenTelemetry's console exporters write through. Restore them here or the
            // metric histograms are collected and then thrown away unseen.
            Telemetry.restoreConsoleLogging();
            VaultController vault = skyflow.vault(config.vaultId);
            OkHttpClient rawClient = buildRawClient();

            Runner runner = new Runner(config, telemetry, vault, rawClient);

            // Warmup exists to take JIT compilation, class loading, connection-pool fill and the
            // SDK's lazy credential resolution out of the measured sample. Without it the first
            // few hundred SDK iterations carry one-time costs that have nothing to do with
            // steady-state latency, and they land entirely on whichever path ran first.
            if (config.warmupIterations > 0) {
                System.out.printf("warmup: %d iterations per path...%n", config.warmupIterations);
                runner.run(config.warmupIterations, true);
                telemetry.recorder().reset();
            }

            System.out.printf("measuring: %d iterations, vus=%d, mode=%s%n",
                    config.iterations, config.vus, config.mode);
            long startNanos = System.nanoTime();
            Outcome outcome = runner.run(config.iterations, false);
            double elapsedSeconds = (System.nanoTime() - startNanos) / 1e9;

            telemetry.flushMetrics();
            Report report = new Report(config, telemetry.recorder(), outcome, elapsedSeconds);
            report.printToConsole();
            report.writeCsv(config.csvDir);
        } finally {
            telemetry.close();
        }
    }

    // ── Configuration ────────────────────────────────────────────────────────

    static final class Config {
        final String operation;
        final String vaultUrl;
        final String vaultId;
        final String clusterId;
        final String apiKey;
        final String table;
        final String column;
        final int rows;
        final int batchSize;
        final int vus;
        final int iterations;
        final int warmupIterations;
        final String mode;
        final Path csvDir;
        final boolean logSpans;

        private Config(String operation) {
            this.operation = operation;
            String url = env("TARGET_URL", null);
            if (url == null) {
                throw new IllegalStateException(
                        "TARGET_URL is required, e.g. TARGET_URL=http://127.0.0.1:3015 "
                                + "(start MockVaultServer first)");
            }
            // The SDK consults the VAULT_URL environment variable before its own config and
            // rejects anything that is not https, which a loopback mock is not. The benchmark
            // therefore uses its own variable and hands the URL to VaultConfig directly; an
            // inherited VAULT_URL would still be picked up first and blow up at client build
            // time with a message that has nothing to do with the benchmark, so say so here.
            String inherited = System.getenv("VAULT_URL");
            if (inherited != null && !inherited.trim().isEmpty()
                    && !inherited.trim().toLowerCase(Locale.ROOT).startsWith("https://")) {
                throw new IllegalStateException(
                        "VAULT_URL is set to '" + inherited.trim() + "'. The SDK reads that "
                                + "variable first and requires https, so unset it and use "
                                + "TARGET_URL for the benchmark.");
            }
            while (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            this.vaultUrl = url;
            this.vaultId = env("VAULT_ID", "benchmarkvault");
            this.clusterId = env("CLUSTER_ID", "benchmarkcluster");
            // A syntactically valid key short-circuits the SDK's token minting, so no auth
            // round-trip lands inside the first measured call. The mock does not check it.
            this.apiKey = env("API_KEY", "sky-bench-0123456789abcdef0123456789abcdef");
            this.table = env("TABLE", "benchmark_table");
            this.column = env("COLUMN", "card_number");
            this.rows = intEnv("ROWS", 10);
            this.batchSize = intEnv("BATCH_SIZE", 15);
            this.vus = intEnv("VUS", 1);
            this.iterations = intEnv("ITERATIONS", 2000);
            this.warmupIterations = intEnv("WARMUP_ITERATIONS", 500);
            this.mode = env("MODE", "interleaved").toLowerCase(Locale.ROOT);
            this.csvDir = Paths.get(env("CSV_DIR", "."));
            this.logSpans = Boolean.parseBoolean(env("LOG_SPANS", "false"));
        }

        static Config fromEnvironment(String operation) {
            return new Config(operation);
        }

        boolean runsSdk() {
            return !"raw".equals(mode);
        }

        boolean runsRaw() {
            return !"sdk".equals(mode);
        }

        String endpointPath() {
            return "insert".equals(operation) ? "/v2/records/insert" : "/v2/tokens/detokenize";
        }

        void print() {
            System.out.println("── Skyflow Java SDK latency benchmark ──────────────────────────");
            System.out.printf("  operation        %s%n", operation);
            System.out.printf("  target           %s%s%n", vaultUrl, endpointPath());
            System.out.printf("  mode             %s%n", mode);
            System.out.printf("  vus              %d%n", vus);
            System.out.printf("  iterations       %d (warmup %d)%n", iterations, warmupIterations);
            if ("insert".equals(operation)) {
                System.out.printf("  records/request  %d into %s.%s%n", rows, table, column);
            } else {
                System.out.printf("  tokens/request   %d%n", batchSize);
            }
            System.out.println();
        }
    }

    // ── Client construction ──────────────────────────────────────────────────

    private static Skyflow buildSkyflowClient(Config config) throws SkyflowException {
        Credentials credentials = new Credentials();
        credentials.setApiKey(config.apiKey);

        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(config.vaultId);
        vaultConfig.setClusterId(config.clusterId);
        vaultConfig.setEnv(Env.PROD);
        vaultConfig.setCredentials(credentials);
        // Pointing the SDK at the mock through config rather than the environment. Both paths
        // then resolve to the same host and port, which is the whole basis of the comparison.
        vaultConfig.setVaultUrl(config.vaultUrl);

        return Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)
                .addVaultConfig(vaultConfig)
                .build();
    }

    /**
     * Mirrors the SDK's own OkHttp configuration — same pool size, same idle timeout, same call
     * timeout — so the baseline differs from the SDK only in the Skyflow code above the socket.
     * The SDK's retry interceptor is deliberately not mirrored: it is SDK cost, and leaving it
     * out means any latency it adds is charged to the SDK rather than hidden in both columns.
     */
    private static OkHttpClient buildRawClient() {
        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(10, 1, TimeUnit.MINUTES))
                .callTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .eventListenerFactory(CallMarks.factory())
                .build();
    }

    // ── Instrumentation self-check ───────────────────────────────────────────

    /**
     * Times the span materialisation the harness performs after every iteration and prints it.
     *
     * <p>A benchmark that reports microsecond differences has to say what its own instrumentation
     * costs, otherwise the reader cannot tell a real difference from an artefact. This figure sits
     * outside every measured window by construction, so it does not contaminate the numbers; it is
     * printed so that claim is checkable rather than asserted.
     */
    private static void reportInstrumentationCost(Telemetry telemetry) {
        int samples = 2000;
        long[] costs = new long[samples];
        for (int i = 0; i < samples; i++) {
            long before = System.nanoTime();
            Span parent = telemetry.tracer().spanBuilder("selfcheck")
                    .setStartTimestamp(telemetry.clock().toEpochNanos(before), TimeUnit.NANOSECONDS)
                    .startSpan();
            Span child = telemetry.tracer().spanBuilder("selfcheck.child")
                    .setParent(Context.root().with(parent))
                    .setStartTimestamp(telemetry.clock().toEpochNanos(before), TimeUnit.NANOSECONDS)
                    .startSpan();
            child.end(telemetry.clock().toEpochNanos(before + 1000), TimeUnit.NANOSECONDS);
            parent.end(telemetry.clock().toEpochNanos(before + 1000), TimeUnit.NANOSECONDS);
            costs[i] = System.nanoTime() - before;
        }
        telemetry.recorder().reset();

        Stats stats = Stats.of("instrumentation", costs);
        System.out.printf(
                "instrumentation self-check: emitting one parent+child span pair costs "
                        + "p50 %s / p99 %s — this happens after each measured window, never inside one%n%n",
                Stats.ms(stats.percentileMs(0.50)), Stats.ms(stats.percentileMs(0.99)));
    }

    // ── Execution ────────────────────────────────────────────────────────────

    /** Counters the span recorder does not capture. */
    static final class Outcome {
        final AtomicLong sdkOk = new AtomicLong();
        final AtomicLong sdkErrors = new AtomicLong();
        final AtomicLong rawOk = new AtomicLong();
        final AtomicLong rawErrors = new AtomicLong();
    }

    static final class Runner {
        private final Config config;
        private final Telemetry telemetry;
        private final VaultController vault;
        private final OkHttpClient rawClient;
        private final String endpoint;
        private final String metadataHeader;
        private final List<String> tokenPool = Collections.synchronizedList(new ArrayList<String>());

        Runner(Config config, Telemetry telemetry, VaultController vault, OkHttpClient rawClient) {
            this.config = config;
            this.telemetry = telemetry;
            this.vault = vault;
            this.rawClient = rawClient;
            this.endpoint = config.vaultUrl + config.endpointPath();
            // The SDK attaches a metrics header to every request. Sending the identical header on
            // the raw path keeps the bytes on the wire equal; building it is still SDK work, and
            // that cost stays charged to the SDK because the raw path computes it once up front.
            this.metadataHeader = com.skyflow.utils.Utils.getMetrics().toString();
            seedTokenPool();
        }

        /** Detokenize needs tokens that exist; insert supplies them. */
        private void seedTokenPool() {
            if (!"detokenize".equals(config.operation)) {
                return;
            }
            for (int i = 0; i < Math.max(config.batchSize * 4, 64); i++) {
                tokenPool.add(UUID.randomUUID().toString());
            }
        }

        Outcome run(int iterations, boolean warmup) throws InterruptedException {
            Outcome outcome = new Outcome();
            AtomicLong cursor = new AtomicLong();
            ExecutorService pool = Executors.newFixedThreadPool(config.vus);
            CountDownLatch done = new CountDownLatch(config.vus);

            for (int v = 0; v < config.vus; v++) {
                pool.submit(() -> {
                    try {
                        while (true) {
                            long i = cursor.getAndIncrement();
                            if (i >= iterations) {
                                return;
                            }
                            // Interleaving alternates paths on consecutive iterations so that a
                            // GC pause or a CPU frequency change hits both columns, not one.
                            boolean useSdk = "sdk".equals(config.mode)
                                    || ("interleaved".equals(config.mode) && (i % 2 == 0));
                            if (useSdk && config.runsSdk()) {
                                runSdkIteration(outcome, warmup);
                            } else if (config.runsRaw()) {
                                runRawIteration(outcome, warmup);
                            }
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
            done.await();
            pool.shutdownNow();
            return outcome;
        }

        // ── SDK path ─────────────────────────────────────────────────────────

        private void runSdkIteration(Outcome outcome, boolean warmup) {
            long[] interceptorMark = new long[] {CallMarks.UNSET};
            boolean ok = true;
            long start;
            long end;

            if ("insert".equals(config.operation)) {
                List<InsertRequestRecord> records = new ArrayList<>(config.rows);
                for (int r = 0; r < config.rows; r++) {
                    Map<String, Object> data = new HashMap<>();
                    data.put(config.column, randomValue());
                    records.add(InsertRequestRecord.builder()
                            .tableName(config.table).data(data).build());
                }
                InsertRequest request = InsertRequest.builder().records(records).build();
                InsertOptions options = InsertOptions.builder()
                        .interceptor(context -> interceptorMark[0] = System.nanoTime())
                        .build();

                // ── measured window opens ──
                start = System.nanoTime();
                InsertResponse response = null;
                try {
                    response = vault.insert(request, options);
                } catch (SkyflowException e) {
                    ok = false;
                }
                end = System.nanoTime();
                // ── measured window closed; everything below is off the clock ──

                if (ok && response != null) {
                    harvestTokens(response);
                }
            } else {
                DetokenizeRequest request = DetokenizeRequest.builder()
                        .tokens(sampleTokens()).build();
                DetokenizeOptions options = DetokenizeOptions.builder()
                        .interceptor(context -> interceptorMark[0] = System.nanoTime())
                        .build();

                start = System.nanoTime();
                try {
                    DetokenizeResponse response = vault.detokenize(request, options);
                    ok = response != null;
                } catch (SkyflowException e) {
                    ok = false;
                }
                end = System.nanoTime();
            }

            if (ok) {
                outcome.sdkOk.incrementAndGet();
            } else {
                outcome.sdkErrors.incrementAndGet();
            }
            if (!warmup) {
                emitSdkSpans(start, end, interceptorMark[0], ok);
                telemetry.recordDuration((end - start) / 1e6, "sdk", config.operation, ok ? "ok" : "error");
            }
        }

        private void emitSdkSpans(long start, long end, long interceptorMark, boolean ok) {
            Span call = telemetry.tracer().spanBuilder(SPAN_SDK)
                    .setSpanKind(SpanKind.CLIENT)
                    .setStartTimestamp(telemetry.clock().toEpochNanos(start), TimeUnit.NANOSECONDS)
                    .setAttribute(ATTR_PATH, "sdk")
                    .setAttribute(ATTR_OPERATION, config.operation)
                    .startSpan();
            if (!ok) {
                call.setStatus(StatusCode.ERROR);
            }

            if (interceptorMark != CallMarks.UNSET && interceptorMark >= start && interceptorMark <= end) {
                Context parent = Context.root().with(call);
                // Before the wire: input validation, credential resolution, and mapping the
                // caller's request objects into the generated wire types.
                child(parent, SPAN_SDK_PREPARE, start, interceptorMark, "pre_request");
                // From the wire onwards: Jackson serialisation, the interceptor chain, the HTTP
                // exchange itself, deserialisation, and mapping back to the public response types.
                child(parent, SPAN_SDK_TRANSPORT, interceptorMark, end, "transport_and_decode");
            }
            call.end(telemetry.clock().toEpochNanos(end), TimeUnit.NANOSECONDS);
        }

        // ── Raw path ─────────────────────────────────────────────────────────

        private void runRawIteration(Outcome outcome, boolean warmup) {
            String body = "insert".equals(config.operation)
                    ? insertBody()
                    : detokenizeBody();
            Request request = new Request.Builder()
                    .url(endpoint)
                    .header("Authorization", "Bearer " + config.apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("sky-metadata", metadataHeader)
                    .post(RequestBody.create(body, JSON))
                    .build();

            boolean ok = true;
            // ── measured window opens ──
            long start = System.nanoTime();
            try (Response response = rawClient.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                // Reading the body is part of the call: the SDK deserialises it, so a baseline
                // that discarded it would be comparing a complete operation with a partial one.
                String payload = responseBody == null ? "" : responseBody.string();
                ok = response.isSuccessful() && !payload.isEmpty();
            } catch (IOException e) {
                ok = false;
            }
            long end = System.nanoTime();
            // ── measured window closed ──

            CallMarks marks = CallMarks.takeCurrent();
            if (ok) {
                outcome.rawOk.incrementAndGet();
            } else {
                outcome.rawErrors.incrementAndGet();
            }
            if (!warmup) {
                emitRawSpans(start, end, marks, body.length(), ok);
                telemetry.recordDuration((end - start) / 1e6, "raw", config.operation, ok ? "ok" : "error");
            }
        }

        private void emitRawSpans(long start, long end, CallMarks marks, int bodyBytes, boolean ok) {
            Span call = telemetry.tracer().spanBuilder(SPAN_RAW)
                    .setSpanKind(SpanKind.CLIENT)
                    .setStartTimestamp(telemetry.clock().toEpochNanos(start), TimeUnit.NANOSECONDS)
                    .setAttribute(ATTR_PATH, "raw")
                    .setAttribute(ATTR_OPERATION, config.operation)
                    .setAttribute(ATTR_PAYLOAD_BYTES, (long) bodyBytes)
                    .startSpan();
            if (!ok) {
                call.setStatus(StatusCode.ERROR);
            }

            if (marks != null) {
                Context parent = Context.root().with(call);
                if (marks.has(marks.callStart, marks.connectionAcquired)) {
                    child(parent, SPAN_RAW_CONNECT, marks.callStart, marks.connectionAcquired, "connect");
                }
                long requestFinished = marks.requestFinished();
                if (marks.has(requestFinished, marks.responseHeadersStart)) {
                    // Time the client spent waiting with nothing to do: network round-trip plus
                    // whatever the server did. Neither path can be blamed for this, and it is the
                    // figure that dwarfs everything else against a real vault.
                    child(parent, SPAN_RAW_SERVER, requestFinished, marks.responseHeadersStart, "server_wait");
                }
                if (marks.has(marks.responseHeadersStart, marks.responseBodyEnd)) {
                    child(parent, SPAN_RAW_READ, marks.responseHeadersStart, marks.responseBodyEnd, "read_response");
                }
            }
            call.end(telemetry.clock().toEpochNanos(end), TimeUnit.NANOSECONDS);
        }

        private void child(Context parent, String name, long start, long end, String phase) {
            telemetry.tracer().spanBuilder(name)
                    .setParent(parent)
                    .setStartTimestamp(telemetry.clock().toEpochNanos(start), TimeUnit.NANOSECONDS)
                    .setAttribute(Telemetry.PHASE, phase)
                    .startSpan()
                    .end(telemetry.clock().toEpochNanos(end), TimeUnit.NANOSECONDS);
        }

        // ── Payloads ─────────────────────────────────────────────────────────

        /** Same shape the SDK serialises for {@code insert}, so the server sees equal bytes. */
        private String insertBody() {
            JsonArray records = new JsonArray();
            for (int r = 0; r < config.rows; r++) {
                JsonObject data = new JsonObject();
                data.addProperty(config.column, randomValue());
                JsonObject record = new JsonObject();
                record.add("data", data);
                record.addProperty("tableName", config.table);
                records.add(record);
            }
            JsonObject body = new JsonObject();
            body.addProperty("vaultID", config.vaultId);
            body.add("records", records);
            return body.toString();
        }

        /** Same shape the SDK serialises for {@code detokenize}. */
        private String detokenizeBody() {
            JsonArray tokens = new JsonArray();
            for (String token : sampleTokens()) {
                tokens.add(token);
            }
            JsonObject body = new JsonObject();
            body.addProperty("vaultID", config.vaultId);
            body.add("tokens", tokens);
            return body.toString();
        }

        private List<String> sampleTokens() {
            List<String> batch = new ArrayList<>(config.batchSize);
            ThreadLocalRandom random = ThreadLocalRandom.current();
            synchronized (tokenPool) {
                int size = tokenPool.size();
                for (int i = 0; i < config.batchSize; i++) {
                    batch.add(tokenPool.get(random.nextInt(size)));
                }
            }
            return batch;
        }

        private void harvestTokens(InsertResponse response) {
            if (response.getRecords() == null || tokenPool.size() > 4096) {
                return;
            }
            for (InsertResponseRecord record : response.getRecords()) {
                if (record.getTokens() == null) {
                    continue;
                }
                List<Token> tokens = record.getTokens().get(config.column);
                if (tokens != null && !tokens.isEmpty() && tokens.get(0) != null) {
                    tokenPool.add(tokens.get(0).getToken());
                }
            }
        }

        private static String randomValue() {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            StringBuilder builder = new StringBuilder(16);
            for (int i = 0; i < 16; i++) {
                builder.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
            }
            return builder.toString();
        }
    }

    // ── Reporting ────────────────────────────────────────────────────────────

    static final class Report {
        private final Config config;
        private final SpanRecorder recorder;
        private final Outcome outcome;
        private final double elapsedSeconds;
        private final List<Stats> rows = new ArrayList<>();

        Report(Config config, SpanRecorder recorder, Outcome outcome, double elapsedSeconds) {
            this.config = config;
            this.recorder = recorder;
            this.outcome = outcome;
            this.elapsedSeconds = elapsedSeconds;
            for (String span : Arrays.asList(SPAN_SDK, SPAN_SDK_PREPARE, SPAN_SDK_TRANSPORT,
                    SPAN_RAW, SPAN_RAW_CONNECT, SPAN_RAW_SERVER, SPAN_RAW_READ)) {
                Stats stats = Stats.of(span, recorder.latenciesNanos(span));
                if (!stats.isEmpty()) {
                    rows.add(stats);
                }
            }
        }

        void printToConsole() {
            System.out.println();
            System.out.println("── Latency by span (source: OpenTelemetry span durations) ───────");
            System.out.println(Stats.HEADER);
            for (Stats stats : rows) {
                System.out.println(stats.row());
            }

            System.out.println();
            System.out.printf("requests: sdk %d ok / %d error, raw %d ok / %d error, wall %.1fs%n",
                    outcome.sdkOk.get(), outcome.sdkErrors.get(),
                    outcome.rawOk.get(), outcome.rawErrors.get(), elapsedSeconds);
            if (recorder.droppedSpans() > 0) {
                System.out.printf("note: %d spans exceeded the recorder cap and were counted only%n",
                        recorder.droppedSpans());
            }
            printVerdict();
        }

        /**
         * The conclusion, stated as a subtraction the reader can redo from the table above.
         * Deliberately prints the difference at several percentiles: a customer complaining about
         * latency usually means their tail, and an SDK could plausibly be free at p50 while
         * adding cost at p99.
         */
        private void printVerdict() {
            Stats sdk = find(SPAN_SDK);
            Stats raw = find(SPAN_RAW);
            if (sdk == null || raw == null) {
                System.out.println();
                System.out.println("verdict: needs MODE=interleaved (both paths) to compare");
                return;
            }

            System.out.println();
            System.out.println("── SDK overhead = sdk.call - raw.call, same server, same payload ─");
            System.out.printf("%-10s %14s %14s %14s %10s%n",
                    "percentile", "sdk.call", "raw.call", "overhead", "share");
            for (double quantile : new double[] {0.50, 0.90, 0.95, 0.99}) {
                double sdkMs = sdk.percentileMs(quantile);
                double rawMs = raw.percentileMs(quantile);
                double overheadMs = sdkMs - rawMs;
                String share = sdkMs > 0
                        ? String.format(Locale.US, "%.1f%%", 100.0 * overheadMs / sdkMs)
                        : "-";
                System.out.printf("%-10s %14s %14s %14s %10s%n",
                        "p" + (int) Math.round(quantile * 100),
                        Stats.ms(sdkMs), Stats.ms(rawMs), Stats.ms(overheadMs), share);
            }

            Stats prepare = find(SPAN_SDK_PREPARE);
            if (prepare != null) {
                System.out.println();
                System.out.printf(
                        "of which validation + request mapping (before any bytes are written): p50 %s, p99 %s%n",
                        Stats.ms(prepare.percentileMs(0.50)), Stats.ms(prepare.percentileMs(0.99)));
            }
            Stats serverWait = find(SPAN_RAW_SERVER);
            if (serverWait != null) {
                System.out.printf(
                        "server + network time, which neither path controls: p50 %s, p99 %s%n",
                        Stats.ms(serverWait.percentileMs(0.50)), Stats.ms(serverWait.percentileMs(0.99)));
            }
        }

        private Stats find(String spanName) {
            for (Stats stats : rows) {
                if (stats.label.equals(spanName)) {
                    return stats;
                }
            }
            return null;
        }

        void writeCsv(Path directory) throws IOException {
            Files.createDirectories(directory);
            Path summary = directory.resolve("sdk-latency-" + config.operation + ".csv");
            List<String> lines = new ArrayList<>();
            lines.add(Stats.csvHeader());
            for (Stats stats : rows) {
                lines.add(stats.csvRow());
            }
            Files.write(summary, lines, StandardCharsets.UTF_8);
            System.out.println();
            System.out.printf("wrote %s%n", summary.toAbsolutePath());
        }
    }

    // ── Env helpers ──────────────────────────────────────────────────────────

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static int intEnv(String key, int fallback) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? fallback : Integer.parseInt(value.trim());
    }

    private SdkLatencyBenchmark() {
    }
}
