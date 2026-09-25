package com.example.otel.tracing;

import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.controller.VaultController;
import com.skyflow.vault.data.BulkDeleteTokensOptions;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;
import com.skyflow.vault.data.BulkDetokenizeOptions;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkInsertOptions;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.BulkTokenizeOptions;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeResponse;
import com.skyflow.vault.data.DeleteOptions;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;
import com.skyflow.vault.data.DetokenizeOptions;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.GetOptions;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.InsertOptions;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.RequestContext;
import com.skyflow.vault.data.RequestInterceptor;
import com.skyflow.vault.data.UpdateOptions;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateResponse;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OpenTelemetry instrumentation for the Skyflow vault SDK, written entirely in application code.
 *
 * <p>Nothing in the SDK is modified, subclassed or reflected into. Every signal below comes from
 * two public surfaces: the return/throw of the vault method itself, and {@link RequestInterceptor},
 * the per-call hook the SDK already offers. That is deliberate — the numbers this produces describe
 * the SDK a customer actually runs, from the only vantage point a customer actually has.
 *
 * <p>Swap {@code vault.insert(request)} for {@code tracedVault.insert(request)} and the call is
 * instrumented. Signatures, exceptions and return values are unchanged.
 *
 * <h2>What each call produces</h2>
 * <pre>
 *   skyflow.insert                     CLIENT span, the full call as the application experiences it
 *   └── skyflow.insert prepare         validation + auth + request mapping, before any bytes go out
 * </pre>
 * plus {@code skyflow.client.operation.duration} (histogram, ms),
 * {@code skyflow.client.operation.prepare.duration} (histogram, ms) and
 * {@code skyflow.client.operation.errors} (counter).
 *
 * <h2>What it deliberately cannot show</h2>
 * The SDK builds its own {@code OkHttpClient} and installs its retry and auth interceptors inside
 * it, so from here the span after {@code prepare} is one opaque block. It contains connect, TLS,
 * server time, any retries and response decoding, with no way to separate them. Token minting runs
 * before the interceptor fires, so it is charged to {@code prepare} rather than broken out. Those
 * are limits of the public API, not of this class.
 */
public final class TracedVault {

    /** Which vault method ran: "insert", "bulkDetokenize", and so on. */
    public static final AttributeKey<String> OPERATION = AttributeKey.stringKey("skyflow.operation");
    /** "ok" or "error". Kept on the histogram so a latency query can exclude failures. */
    public static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("skyflow.outcome");
    /** Number of batches the SDK split the request into, when it batched at all. */
    public static final AttributeKey<Long> BATCHES = AttributeKey.longKey("skyflow.batches");
    /** Position of one batch within a batched request. */
    public static final AttributeKey<Long> BATCH_INDEX = AttributeKey.longKey("skyflow.batch_index");
    public static final AttributeKey<Long> HTTP_STATUS = AttributeKey.longKey("skyflow.http_status");
    /** Skyflow's own request id, for joining this span to Skyflow-side logs. Errors only. */
    public static final AttributeKey<String> REQUEST_ID = AttributeKey.stringKey("skyflow.request_id");
    public static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

    public static final String DURATION_METRIC = "skyflow.client.operation.duration";
    public static final String PREPARE_METRIC = "skyflow.client.operation.prepare.duration";
    public static final String ERROR_METRIC = "skyflow.client.operation.errors";

    private static final String INSTRUMENTATION_SCOPE = "com.example.otel.tracing.TracedVault";
    private static final String OK = "ok";
    private static final String ERROR = "error";

    private final VaultController vault;
    private final Tracer tracer;
    private final DoubleHistogram duration;
    private final DoubleHistogram prepareDuration;
    private final LongCounter errors;

    /**
     * Instruments a vault using whatever OpenTelemetry instance is already installed globally —
     * the usual case when the application is set up by the OTel Java agent, a Spring Boot starter
     * or an autoconfigured SDK.
     */
    public static TracedVault of(VaultController vault) {
        return of(vault, GlobalOpenTelemetry.get());
    }

    /** Instruments a vault using an explicitly supplied OpenTelemetry instance. */
    public static TracedVault of(VaultController vault, OpenTelemetry openTelemetry) {
        return new TracedVault(vault, openTelemetry);
    }

    private TracedVault(VaultController vault, OpenTelemetry openTelemetry) {
        if (vault == null) {
            throw new IllegalArgumentException("vault must not be null");
        }
        if (openTelemetry == null) {
            throw new IllegalArgumentException("openTelemetry must not be null");
        }
        this.vault = vault;
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE);
        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);
        this.duration = meter.histogramBuilder(DURATION_METRIC)
                .setDescription("Duration of a Skyflow vault operation, as the caller experiences it")
                .setUnit("ms")
                .build();
        this.prepareDuration = meter.histogramBuilder(PREPARE_METRIC)
                .setDescription("Time spent in the SDK before the HTTP request is issued")
                .setUnit("ms")
                .build();
        this.errors = meter.counterBuilder(ERROR_METRIC)
                .setDescription("Failed Skyflow vault operations")
                .build();
    }

    /** The underlying vault, for operations this class does not wrap. */
    public VaultController unwrap() {
        return vault;
    }

    // ── Insert ────────────────────────────────────────────────────────────────

    public InsertResponse insert(InsertRequest request) throws SkyflowException {
        return insert(request, null);
    }

    public InsertResponse insert(InsertRequest request, InsertOptions options) throws SkyflowException {
        return trace("insert", interceptorOf(options), probe ->
                vault.insert(request, InsertOptions.builder().interceptor(probe).build()));
    }

    public BulkInsertResponse bulkInsert(BulkInsertRequest request) throws SkyflowException {
        return bulkInsert(request, null);
    }

    public BulkInsertResponse bulkInsert(BulkInsertRequest request, BulkInsertOptions options)
            throws SkyflowException {
        return trace("bulkInsert", interceptorOf(options), probe ->
                vault.bulkInsert(request, BulkInsertOptions.builder().interceptor(probe).build()));
    }

    public CompletableFuture<BulkInsertResponse> bulkInsertAsync(BulkInsertRequest request)
            throws SkyflowException {
        return bulkInsertAsync(request, null);
    }

    public CompletableFuture<BulkInsertResponse> bulkInsertAsync(
            BulkInsertRequest request, BulkInsertOptions options) throws SkyflowException {
        return traceAsync("bulkInsertAsync", interceptorOf(options), probe ->
                vault.bulkInsertAsync(request, BulkInsertOptions.builder().interceptor(probe).build()));
    }

    // ── Detokenize ────────────────────────────────────────────────────────────

    public DetokenizeResponse detokenize(DetokenizeRequest request) throws SkyflowException {
        return detokenize(request, null);
    }

    public DetokenizeResponse detokenize(DetokenizeRequest request, DetokenizeOptions options)
            throws SkyflowException {
        return trace("detokenize", interceptorOf(options), probe ->
                vault.detokenize(request, DetokenizeOptions.builder().interceptor(probe).build()));
    }

    public BulkDetokenizeResponse bulkDetokenize(BulkDetokenizeRequest request) throws SkyflowException {
        return bulkDetokenize(request, null);
    }

    public BulkDetokenizeResponse bulkDetokenize(
            BulkDetokenizeRequest request, BulkDetokenizeOptions options) throws SkyflowException {
        return trace("bulkDetokenize", interceptorOf(options), probe ->
                vault.bulkDetokenize(request, BulkDetokenizeOptions.builder().interceptor(probe).build()));
    }

    public CompletableFuture<BulkDetokenizeResponse> bulkDetokenizeAsync(BulkDetokenizeRequest request)
            throws SkyflowException {
        return bulkDetokenizeAsync(request, null);
    }

    public CompletableFuture<BulkDetokenizeResponse> bulkDetokenizeAsync(
            BulkDetokenizeRequest request, BulkDetokenizeOptions options) throws SkyflowException {
        return traceAsync("bulkDetokenizeAsync", interceptorOf(options), probe ->
                vault.bulkDetokenizeAsync(request, BulkDetokenizeOptions.builder().interceptor(probe).build()));
    }

    // ── Tokenize ──────────────────────────────────────────────────────────────

    public BulkTokenizeResponse bulkTokenize(BulkTokenizeRequest request) throws SkyflowException {
        return bulkTokenize(request, null);
    }

    public BulkTokenizeResponse bulkTokenize(BulkTokenizeRequest request, BulkTokenizeOptions options)
            throws SkyflowException {
        return trace("bulkTokenize", interceptorOf(options), probe ->
                vault.bulkTokenize(request, BulkTokenizeOptions.builder().interceptor(probe).build()));
    }

    public CompletableFuture<BulkTokenizeResponse> bulkTokenizeAsync(BulkTokenizeRequest request)
            throws SkyflowException {
        return bulkTokenizeAsync(request, null);
    }

    public CompletableFuture<BulkTokenizeResponse> bulkTokenizeAsync(
            BulkTokenizeRequest request, BulkTokenizeOptions options) throws SkyflowException {
        return traceAsync("bulkTokenizeAsync", interceptorOf(options), probe ->
                vault.bulkTokenizeAsync(request, BulkTokenizeOptions.builder().interceptor(probe).build()));
    }

    // ── Get / Update / Delete ─────────────────────────────────────────────────

    public GetResponse get(GetRequest request) throws SkyflowException {
        return get(request, null);
    }

    public GetResponse get(GetRequest request, GetOptions options) throws SkyflowException {
        return trace("get", interceptorOf(options), probe ->
                vault.get(request, GetOptions.builder().interceptor(probe).build()));
    }

    public UpdateResponse update(UpdateRequest request) throws SkyflowException {
        return update(request, null);
    }

    public UpdateResponse update(UpdateRequest request, UpdateOptions options) throws SkyflowException {
        return trace("update", interceptorOf(options), probe ->
                vault.update(request, UpdateOptions.builder().interceptor(probe).build()));
    }

    public DeleteResponse delete(DeleteRequest request) throws SkyflowException {
        return delete(request, null);
    }

    public DeleteResponse delete(DeleteRequest request, DeleteOptions options) throws SkyflowException {
        return trace("delete", interceptorOf(options), probe ->
                vault.delete(request, DeleteOptions.builder().interceptor(probe).build()));
    }

    public BulkDeleteTokensResponse bulkDeleteTokens(BulkDeleteTokensRequest request)
            throws SkyflowException {
        return bulkDeleteTokens(request, null);
    }

    public BulkDeleteTokensResponse bulkDeleteTokens(
            BulkDeleteTokensRequest request, BulkDeleteTokensOptions options) throws SkyflowException {
        return trace("bulkDeleteTokens", interceptorOf(options), probe ->
                vault.bulkDeleteTokens(request, BulkDeleteTokensOptions.builder().interceptor(probe).build()));
    }

    public CompletableFuture<BulkDeleteTokensResponse> bulkDeleteTokensAsync(
            BulkDeleteTokensRequest request) throws SkyflowException {
        return bulkDeleteTokensAsync(request, null);
    }

    public CompletableFuture<BulkDeleteTokensResponse> bulkDeleteTokensAsync(
            BulkDeleteTokensRequest request, BulkDeleteTokensOptions options) throws SkyflowException {
        return traceAsync("bulkDeleteTokensAsync", interceptorOf(options), probe ->
                vault.bulkDeleteTokensAsync(request,
                        BulkDeleteTokensOptions.builder().interceptor(probe).build()));
    }

    // ── Instrumentation core ──────────────────────────────────────────────────

    /** A vault call, parameterised by the interceptor this class needs to slip into its options. */
    @FunctionalInterface
    private interface VaultCall<T> {
        T execute(RequestInterceptor probe) throws SkyflowException;
    }

    private <T> T trace(String operation, RequestInterceptor delegate, VaultCall<T> call)
            throws SkyflowException {
        Span span = startSpan(operation);
        long startNanos = System.nanoTime();
        Probe probe = new Probe(operation, span, delegate);
        String outcome = OK;
        try (Scope ignored = span.makeCurrent()) {
            return call.execute(probe);
        } catch (SkyflowException e) {
            outcome = ERROR;
            recordFailure(span, operation, e);
            throw e;
        } catch (RuntimeException e) {
            outcome = ERROR;
            recordFailure(span, operation, e);
            throw e;
        } finally {
            // If the call failed before the SDK reached its HTTP layer, the interceptor never fired
            // and the prepare span is still open. Closing it here makes that case visible: prepare
            // covers the whole call, which is exactly what a validation or auth failure looks like.
            probe.closeOnce(null);
            duration.record(millisSince(startNanos), Attributes.of(OPERATION, operation, OUTCOME, outcome));
            span.end();
        }
    }

    private <T> CompletableFuture<T> traceAsync(
            String operation, RequestInterceptor delegate, VaultCall<CompletableFuture<T>> call)
            throws SkyflowException {
        Span span = startSpan(operation);
        long startNanos = System.nanoTime();
        Probe probe = new Probe(operation, span, delegate);
        CompletableFuture<T> future;
        try (Scope ignored = span.makeCurrent()) {
            future = call.execute(probe);
        } catch (SkyflowException | RuntimeException e) {
            probe.closeOnce(null);
            recordFailure(span, operation, e);
            duration.record(millisSince(startNanos), Attributes.of(OPERATION, operation, OUTCOME, ERROR));
            span.end();
            throw e;
        }
        // The span has to outlive this method: the work is still running. Ending it on the return
        // of bulkInsertAsync would time the dispatch, not the operation.
        return future.whenComplete((result, error) -> {
            probe.closeOnce(null);
            String outcome = OK;
            if (error != null) {
                outcome = ERROR;
                recordFailure(span, operation, unwrapCompletion(error));
            }
            duration.record(millisSince(startNanos), Attributes.of(OPERATION, operation, OUTCOME, outcome));
            span.end();
        });
    }

    private Span startSpan(String operation) {
        return tracer.spanBuilder("skyflow." + operation)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(OPERATION, operation)
                .startSpan();
    }

    private void recordFailure(Span span, String operation, Throwable error) {
        String type = error.getClass().getSimpleName();
        span.setStatus(StatusCode.ERROR, error.getMessage() != null ? error.getMessage() : type);
        span.recordException(error);
        span.setAttribute(ERROR_TYPE, type);

        AttributesBuilder attributes = Attributes.builder()
                .put(OPERATION, operation)
                .put(ERROR_TYPE, type);
        if (error instanceof SkyflowException) {
            SkyflowException failure = (SkyflowException) error;
            span.setAttribute(HTTP_STATUS, failure.getHttpCode());
            attributes.put(HTTP_STATUS, failure.getHttpCode());
            // Present on failures only; the SDK does not surface a request id on the success path,
            // so a successful span cannot be joined to a Skyflow-side log this way.
            String requestId = failure.getRequestId();
            if (requestId != null && !requestId.isEmpty()) {
                span.setAttribute(REQUEST_ID, requestId);
            }
        }
        errors.add(1, attributes.build());
    }

    private static Throwable unwrapCompletion(Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }

    private static double millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    private static RequestInterceptor interceptorOf(InsertOptions options) {
        return options == null ? null : options.getInterceptor();
    }

    private static RequestInterceptor interceptorOf(DetokenizeOptions options) {
        return options == null ? null : options.getInterceptor();
    }

    private static RequestInterceptor interceptorOf(BulkTokenizeOptions options) {
        return options == null ? null : options.getInterceptor();
    }

    private static RequestInterceptor interceptorOf(BulkDeleteTokensOptions options) {
        return options == null ? null : options.getInterceptor();
    }

    private static RequestInterceptor interceptorOf(GetOptions options) {
        return options == null ? null : options.getInterceptor();
    }

    private static RequestInterceptor interceptorOf(UpdateOptions options) {
        return options == null ? null : options.getInterceptor();
    }

    private static RequestInterceptor interceptorOf(DeleteOptions options) {
        return options == null ? null : options.getInterceptor();
    }

    /**
     * The interceptor handed to the SDK. It closes the prepare span at the moment the SDK is about
     * to issue the HTTP request, then forwards to whatever interceptor the caller supplied, so
     * wrapping a call never costs the caller their own hook.
     */
    private final class Probe implements RequestInterceptor {
        private final String operation;
        private final Span parent;
        private final Span prepareSpan;
        private final RequestInterceptor delegate;
        private final long startNanos;
        private final AtomicBoolean closed = new AtomicBoolean();

        Probe(String operation, Span parent, RequestInterceptor delegate) {
            this.operation = operation;
            this.parent = parent;
            this.delegate = delegate;
            this.startNanos = System.nanoTime();
            this.prepareSpan = tracer.spanBuilder("skyflow." + operation + " prepare")
                    .setParent(Context.current().with(parent))
                    .setAttribute(OPERATION, operation)
                    .startSpan();
        }

        @Override
        public void intercept(RequestContext context) {
            closeOnce(context);
            // A batched request fires this hook once per batch. Only the first closes prepare; the
            // rest become events, so the fan-out is visible without inventing a span per batch.
            if (context != null && context.getTotalBatches() > 1) {
                parent.addEvent("skyflow.batch.dispatched",
                        Attributes.of(BATCH_INDEX, (long) context.getBatchIndex()));
            }
            if (delegate != null) {
                delegate.intercept(context);
            }
        }

        void closeOnce(RequestContext context) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            double elapsed = millisSince(startNanos);
            if (context != null && context.getTotalBatches() > 0) {
                prepareSpan.setAttribute(BATCHES, (long) context.getTotalBatches());
                parent.setAttribute(BATCHES, (long) context.getTotalBatches());
            }
            prepareSpan.end();
            prepareDuration.record(elapsed, Attributes.of(OPERATION, operation));
        }
    }
}
