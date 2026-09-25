package com.example.otel.tracing;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A minimal OpenTelemetry setup for applications that do not already have one.
 *
 * <p>If your service is already instrumented — a Spring Boot starter, the OTel Java agent, your own
 * SDK wiring — skip this class entirely and pass your existing instance straight to
 * {@link TracedVault#of(com.skyflow.vault.controller.VaultController, io.opentelemetry.api.OpenTelemetry)}.
 * {@code TracedVault} emits through the standard API and does not care where the SDK came from.
 *
 * <p>Otherwise {@link #install()} gives you a working pipeline from two environment variables:
 * <pre>
 *   OTEL_SERVICE_NAME            defaults to "skyflow-sdk-client"
 *   OTEL_EXPORTER_OTLP_ENDPOINT  e.g. http://localhost:4318 — unset means log to the console
 * </pre>
 */
public final class SkyflowTracing {

    private static final String DEFAULT_SERVICE_NAME = "skyflow-sdk-client";
    private static final AtomicBoolean SHUTDOWN = new AtomicBoolean();

    /**
     * Bucket boundaries in milliseconds. The default OpenTelemetry boundaries start at 0ms and jump
     * to 5ms, which puts almost every SDK-side measurement in one bucket and makes the histogram
     * useless for exactly the question being asked. These resolve the sub-millisecond range where
     * the SDK's own work lives, while still reaching far enough to cover a slow network call.
     */
    private static final List<Double> BUCKETS_MS = Arrays.asList(
            0.05, 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0,
            7.5, 10.0, 15.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0);

    private SkyflowTracing() {
    }

    /** Builds an SDK that reports to the console and registers it as the global instance. */
    public static OpenTelemetrySdk install() {
        String serviceName = env("OTEL_SERVICE_NAME", DEFAULT_SERVICE_NAME);

        Resource resource = Resource.getDefault().merge(Resource.create(
                Attributes.of(AttributeKey.stringKey("service.name"), serviceName)));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                .build();

        SdkMeterProvider meterProvider = meterProvider(resource, PeriodicMetricReader
                .builder(LoggingMetricExporter.create())
                .setInterval(Duration.ofSeconds(Long.parseLong(env("OTEL_METRIC_INTERVAL_SECONDS", "30"))))
                .build());

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .buildAndRegisterGlobal();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(sdk)));
        return sdk;
    }

    private static SdkMeterProvider meterProvider(Resource resource, PeriodicMetricReader reader) {
        SdkMeterProviderBuilder builder = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(reader);
        for (String metric : new String[] {TracedVault.DURATION_METRIC, TracedVault.PREPARE_METRIC}) {
            builder.registerView(
                    InstrumentSelector.builder().setName(metric).build(),
                    View.builder().setAggregation(Aggregation.explicitBucketHistogram(BUCKETS_MS)).build());
        }
        return builder.build();
    }

    /** Flushes and shuts down both pipelines. Safe to call more than once. */
    public static void shutdown(OpenTelemetrySdk sdk) {
        // A shutdown hook is registered as well, so this normally runs twice: once from the
        // application and once on exit. Without the guard the second pass re-exports every
        // cumulative histogram and the console output appears duplicated.
        if (sdk == null || !SHUTDOWN.compareAndSet(false, true)) {
            return;
        }
        sdk.getSdkTracerProvider().forceFlush().join(10, TimeUnit.SECONDS);
        sdk.getSdkMeterProvider().forceFlush().join(10, TimeUnit.SECONDS);
        sdk.close();
    }

    /**
     * Re-attaches a console handler to the root {@code java.util.logging} logger.
     *
     * <p>Call this <em>after</em> building your Skyflow client if your application logs through JUL.
     * The SDK's logging setup begins with {@code LogManager.getLogManager().reset()}, which is a
     * global operation: it removes every handler on the root logger, including ones the application
     * installed. Anything logging through JUL — OpenTelemetry's own console exporters among them —
     * goes silent from that point on, with no error to explain it.
     *
     * <p>Irrelevant if you export over OTLP or log through SLF4J/Logback.
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

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
