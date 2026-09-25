package com.example.otel.benchmark;

import java.util.Arrays;
import java.util.Locale;

/**
 * Percentiles over a set of span durations, plus the formatting used by the console report and
 * the CSV.
 *
 * <p>Percentiles use the nearest-rank method on the sorted sample: p(q) is the smallest value
 * with at least q% of observations at or below it. No interpolation, so every reported figure is
 * an observation that actually happened — which matters when the audience is a customer who may
 * reasonably ask "did you measure that, or compute it?".
 */
public final class Stats {

    public final String label;
    public final long count;
    private final long[] sortedNanos;

    private Stats(String label, long[] sortedNanos) {
        this.label = label;
        this.sortedNanos = sortedNanos;
        this.count = sortedNanos.length;
    }

    public static Stats of(String label, long[] nanos) {
        long[] copy = Arrays.copyOf(nanos, nanos.length);
        Arrays.sort(copy);
        return new Stats(label, copy);
    }

    public boolean isEmpty() {
        return sortedNanos.length == 0;
    }

    /** @param q quantile in [0,1]; {@code percentile(0.99)} is p99, in milliseconds. */
    public double percentileMs(double q) {
        if (sortedNanos.length == 0) {
            return Double.NaN;
        }
        int rank = (int) Math.ceil(q * sortedNanos.length) - 1;
        if (rank < 0) {
            rank = 0;
        }
        if (rank >= sortedNanos.length) {
            rank = sortedNanos.length - 1;
        }
        return sortedNanos[rank] / 1_000_000.0;
    }

    public double minMs() {
        return isEmpty() ? Double.NaN : sortedNanos[0] / 1_000_000.0;
    }

    public double maxMs() {
        return isEmpty() ? Double.NaN : sortedNanos[sortedNanos.length - 1] / 1_000_000.0;
    }

    public double meanMs() {
        if (isEmpty()) {
            return Double.NaN;
        }
        double total = 0;
        for (long n : sortedNanos) {
            total += n;
        }
        return total / sortedNanos.length / 1_000_000.0;
    }

    public static final String HEADER = String.format(Locale.US,
            "%-34s %8s %10s %10s %10s %10s %10s %10s",
            "span", "count", "mean", "p50", "p90", "p95", "p99", "max");

    public String row() {
        return String.format(Locale.US,
                "%-34s %8d %10s %10s %10s %10s %10s %10s",
                label, count,
                ms(meanMs()), ms(percentileMs(0.50)), ms(percentileMs(0.90)),
                ms(percentileMs(0.95)), ms(percentileMs(0.99)), ms(maxMs()));
    }

    /** CSV columns matching {@link #csvHeader()}. */
    public String csvRow() {
        return String.format(Locale.US, "%s,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f",
                label, count, meanMs(), minMs(), percentileMs(0.50), percentileMs(0.90),
                percentileMs(0.95), percentileMs(0.99), maxMs());
    }

    public static String csvHeader() {
        return "span,count,mean_ms,min_ms,p50_ms,p90_ms,p95_ms,p99_ms,max_ms";
    }

    /**
     * Sub-millisecond figures are the whole point of this benchmark, so they are printed in
     * microseconds rather than as a row of zeroes.
     */
    public static String ms(double value) {
        if (Double.isNaN(value)) {
            return "-";
        }
        if (value < 1.0) {
            return String.format(Locale.US, "%.0fus", value * 1000.0);
        }
        return String.format(Locale.US, "%.2fms", value);
    }
}
