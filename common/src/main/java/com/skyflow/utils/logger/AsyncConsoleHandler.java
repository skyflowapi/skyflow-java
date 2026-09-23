package com.skyflow.utils.logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Wraps a delegate {@link Handler} (in practice a {@link java.util.logging.ConsoleHandler}) so that
 * {@link #publish(LogRecord)} never performs blocking I/O on the calling thread.
 * <p>
 * {@code ConsoleHandler.publish} writes to and flushes the underlying stream synchronously, and does
 * so under a lock shared by every thread using the logger. Since {@code LogUtil.printInfoLog}/etc. are
 * called on every SDK request (often several times per call), that turns console logging into a
 * per-request blocking-I/O + lock-contention point under concurrent load. This handler hands each
 * {@link LogRecord} off to a single background daemon thread instead, which performs the actual write;
 * the calling thread only enqueues.
 * <p>
 * The handoff never blocks or applies backpressure to the caller: if the queue is momentarily full
 * (a sustained logging flood, or the writer thread stalled) the record is dropped rather than slowing
 * down request-serving threads — logging must never become the bottleneck it was flagged for.
 */
final class AsyncConsoleHandler extends Handler {

    /** Bounds worst-case memory use if the writer thread falls behind; excess records are dropped. */
    private static final int QUEUE_CAPACITY = 10_000;

    private final Handler delegate;
    private final LinkedBlockingQueue<LogRecord> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final Thread writer;
    private volatile boolean closed = false;

    AsyncConsoleHandler(Handler delegate) {
        this.delegate = delegate;
        setLevel(delegate.getLevel());
        this.writer = new Thread(this::drain, "skyflow-sdk-log-writer");
        this.writer.setDaemon(true);
        this.writer.start();
    }

    @Override
    public void publish(LogRecord record) {
        if (closed || !isLoggable(record)) {
            return;
        }
        // offer() never blocks: a full queue means "drop", never "wait".
        queue.offer(record);
    }

    private void drain() {
        try {
            while (true) {
                LogRecord record = queue.take();
                try {
                    delegate.publish(record);
                } catch (RuntimeException e) {
                    reportError(null, e, ErrorManager.WRITE_FAILURE);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void flush() {
        // Best-effort: drain what's queued right now onto the delegate, then flush it.
        LogRecord record;
        while ((record = queue.poll()) != null) {
            delegate.publish(record);
        }
        delegate.flush();
    }

    @Override
    public void close() {
        closed = true;
        writer.interrupt();
        flush();
        delegate.close();
    }
}
