package com.example.otel.benchmark;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Boots an OpenTelemetry SDK for the latency benchmark and exposes the three things the harness
 * needs: a {@link Tracer}, a latency {@link DoubleHistogram}, and the {@link SpanRecorder} the
 * summary is computed from.
 *
 * <p><b>Everything here lives in the sample application.</b> The Skyflow SDK is not modified,
 * recompiled, or wrapped — which is the point. If the numbers this produces required patching the
 * SDK, they would prove nothing about the SDK a customer actually runs.
 *
 * <p>Export is deliberately console-only. A collector, an OTLP endpoint and a Grafana dashboard
 * would all add moving parts between the measurement and the claim; a customer disputing the
 * result can run this with nothing installed and read the numbers off their own terminal.
 */
public final class Telemetry implements AutoCloseable {

    public static final String INSTRUMENTATION_SCOPE = "com.example.otel.SdkLatencyBenchmark";

    /** Metric attribute: {@code sdk} (call went through the Skyflow SDK) or {@code raw}. */
    public static final AttributeKey<String> PATH = AttributeKey.stringKey("skyflow.path");
    /** Metric attribute: the logical operation, e.g. {@code insert} or {@code detokenize}. */
    public static final AttributeKey<String> OPERATION = AttributeKey.stringKey("skyflow.operation");
    /** Metric attribute: {@code ok} or {@code error}. */
    public static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("skyflow.outcome");
    /** Span attribute: which phase of an SDK call a child span covers. */
    public static final AttributeKey<String> PHASE = AttributeKey.stringKey("skyflow.phase");

    public static final String HISTOGRAM_NAME = "skyflow.client.operation.duration";

    /**
     * Bucket boundaries in milliseconds. Weighted toward the sub-millisecond end because against a
     * local mock the interesting question is whether SDK overhead is tens or hundreds of
     * microseconds; the default OpenTelemetry boundaries start at 5 ms and would put every
     * observation in one bucket.
     */
    private static final List<Double> BUCKETS_MS = Arrays.asList(
            0.05, 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0,
            7.5, 10.0, 15.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0);

    private final SdkTracerProvider tracerProvider;
    private final SdkMeterProvider meterProvider;
    private final Tracer tracer;
    private final DoubleHistogram durationMs;
    private final SpanRecorder recorder;
    private final EpochClock clock;

    private Telemetry(OpenTelemetrySdk openTelemetry, SdkTracerProvider tracerProvider,
                      SdkMeterProvider meterProvider, SpanRecorder recorder, EpochClock clock) {
        this.tracerProvider = tracerProvider;
        this.meterProvider = meterProvider;
        this.recorder = recorder;
        this.clock = clock;
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE);
        Meter meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);
        this.durationMs = meter.histogramBuilder(HISTOGRAM_NAME)
                .setUnit("ms")
                .setDescription("Wall-clock duration of one client call, by path")
                .build();
    }

    /**
     * @param serviceName     value for the {@code service.name} resource attribute
     * @param logSpans        when true, every span is also printed as it ends. Off by default:
     *                        at benchmark volumes the stdout lock becomes a bottleneck and the
     *                        printing shows up in the numbers.
     * @param maxSpansPerName recorder cap, so a long run cannot exhaust the heap
     */
    public static Telemetry create(String serviceName, boolean logSpans, int maxSpansPerName) {
        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.of(
                AttributeKey.stringKey("service.name"), serviceName)));

        SpanRecorder recorder = new SpanRecorder(maxSpansPerName);
        SdkTracerProviderBuilder tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(recorder);
        if (logSpans) {
            tracerProvider.addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()));
        }

        // A long interval plus an explicit forceFlush at the end: periodic export mid-run would
        // print partial histograms that invite misreading, and the flush costs nothing once.
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(LoggingMetricExporter.create())
                        .setInterval(Duration.ofHours(1))
                        .build())
                .registerView(
                        InstrumentSelector.builder().setName(HISTOGRAM_NAME).build(),
                        View.builder()
                                .setAggregation(Aggregation.explicitBucketHistogram(BUCKETS_MS))
                                .build())
                .build();

        SdkTracerProvider builtTracerProvider = tracerProvider.build();
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(builtTracerProvider)
                .setMeterProvider(meterProvider)
                .build();

        return new Telemetry(sdk, builtTracerProvider, meterProvider, recorder, new EpochClock());
    }

    /**
     * Re-attaches a console handler to the root JUL logger.
     *
     * <p>Must be called <em>after</em> the Skyflow client is built. The SDK configures its own
     * logger via {@code LogUtil.setupLogger}, which begins with
     * {@code LogManager.getLogManager().reset()} — a global operation that removes every handler
     * from the root logger, not just the SDK's own. OpenTelemetry's logging exporters write
     * through JUL, so after the SDK initialises they emit into a void and the metric histograms
     * silently never appear.
     *
     * <p>This is worth knowing beyond this benchmark: any application that builds a Skyflow client
     * loses root JUL handlers at that moment, which affects anything else routing through
     * java.util.logging.
     */
    public static void restoreConsoleLogging() {
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(Level.INFO);
                root.setLevel(Level.INFO);
                return;
            }
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        root.addHandler(handler);
        root.setLevel(Level.INFO);
    }

    public Tracer tracer() {
        return tracer;
    }

    public SpanRecorder recorder() {
        return recorder;
    }

    public EpochClock clock() {
        return clock;
    }

    public void recordDuration(double millis, String path, String operation, String outcome) {
        durationMs.record(millis, Attributes.of(PATH, path, OPERATION, operation, OUTCOME, outcome));
    }

    /**
     * Prints the accumulated metric histograms via the logging exporter, then shuts the meter
     * provider down so {@link #close()} does not export the same cumulative histogram a second
     * time and leave the reader wondering which copy to trust.
     */
    public void flushMetrics() {
        meterProvider.forceFlush().join(30, java.util.concurrent.TimeUnit.SECONDS);
        meterProvider.shutdown().join(30, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * Closes the tracer provider only. The meter provider is already shut down by
     * {@link #flushMetrics()}; closing the whole SDK here would shut it down a second time and
     * log a "Multiple close calls" warning that looks like a defect and is not one.
     */
    @Override
    public void close() {
        tracerProvider.close();
    }
}
