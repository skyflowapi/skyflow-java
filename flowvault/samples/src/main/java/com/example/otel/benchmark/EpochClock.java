package com.example.otel.benchmark;

/**
 * Converts {@link System#nanoTime()} marks into the absolute epoch nanos OpenTelemetry wants.
 *
 * <p>This exists so that the benchmark's hot path never calls into OpenTelemetry. A measured
 * window records nothing but {@code System.nanoTime()} longs; spans are materialised afterwards
 * with explicit start/end timestamps derived from those longs. The alternative — starting and
 * ending real spans inside the window — makes the instrumentation part of what is being measured,
 * which is fatal for a benchmark whose whole purpose is to attribute microseconds.
 *
 * <p>nanoTime is monotonic but has no defined origin, so a single pair of readings taken at
 * startup anchors it to wall-clock. The two clocks drift relative to one another over long runs,
 * which shifts a span's absolute placement on a trace timeline; it does not affect any duration,
 * since every duration is a difference of two nanoTime marks.
 */
public final class EpochClock {

    private final long epochNanosAtBase;
    private final long nanoTimeAtBase;

    public EpochClock() {
        this.epochNanosAtBase = System.currentTimeMillis() * 1_000_000L;
        this.nanoTimeAtBase = System.nanoTime();
    }

    /** Absolute epoch nanos for a mark previously taken from {@link System#nanoTime()}. */
    public long toEpochNanos(long nanoTimeMark) {
        return epochNanosAtBase + (nanoTimeMark - nanoTimeAtBase);
    }
}
