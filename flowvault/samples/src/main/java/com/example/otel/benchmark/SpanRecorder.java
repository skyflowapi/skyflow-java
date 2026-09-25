package com.example.otel.benchmark;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link SpanProcessor} that keeps every ended span's duration in memory, bucketed by span name.
 *
 * <p>This is what makes the benchmark's summary genuinely OpenTelemetry-derived rather than
 * OpenTelemetry-flavoured: the percentile table and the CSV are computed from the durations
 * OpenTelemetry recorded on real spans, not from a side-channel of raw timings the harness kept
 * for itself. A customer can point the same spans at a collector and reproduce the numbers.
 *
 * <p>{@link #onEnd} does the minimum possible work — read the latency, append to a queue — because
 * it runs synchronously on the thread that ended the span. In this harness spans are ended outside
 * every measured window (see {@link EpochClock}), so even that cost lands nowhere sensitive, but
 * keeping it trivial means the recorder stays safe if someone later ends a span inline.
 */
public final class SpanRecorder implements SpanProcessor {

    /** Guards against an unbounded run eating the heap; excess spans are counted, not stored. */
    private final int maxPerName;

    private final Map<String, ConcurrentLinkedQueue<Long>> latenciesByName = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> countsByName = new ConcurrentHashMap<>();
    private final AtomicLong dropped = new AtomicLong();

    public SpanRecorder(int maxPerName) {
        this.maxPerName = maxPerName;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // Nothing to do; durations are only known at end.
    }

    @Override
    public boolean isStartRequired() {
        return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        String name = span.getName();
        AtomicLong count = countsByName.computeIfAbsent(name, k -> new AtomicLong());
        long seen = count.incrementAndGet();
        if (seen > maxPerName) {
            dropped.incrementAndGet();
            return;
        }
        latenciesByName.computeIfAbsent(name, k -> new ConcurrentLinkedQueue<Long>())
                .add(span.getLatencyNanos());
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    /** Durations, in nanoseconds, of every recorded span with the given name. */
    public long[] latenciesNanos(String spanName) {
        ConcurrentLinkedQueue<Long> queue = latenciesByName.get(spanName);
        if (queue == null) {
            return new long[0];
        }
        List<Long> snapshot = new ArrayList<>(queue);
        long[] out = new long[snapshot.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = snapshot.get(i);
        }
        return out;
    }

    /** Span names seen so far, in no particular order. */
    public Map<String, Long> counts() {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> e : countsByName.entrySet()) {
            out.put(e.getKey(), e.getValue().get());
        }
        return Collections.unmodifiableMap(out);
    }

    public long droppedSpans() {
        return dropped.get();
    }

    /** Forgets everything recorded so far — used to discard warmup before the measured phase. */
    public void reset() {
        latenciesByName.clear();
        countsByName.clear();
        dropped.set(0);
    }
}
