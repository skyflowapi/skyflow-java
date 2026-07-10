package com.skyflow.utils.logger;

import com.skyflow.enums.LogLevel;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogUtilLevelTests {

    private static class CapturingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override public void flush() {}
        @Override public void close() {}
    }

    // setupLogger calls LogManager.reset() which clears all handlers,
    // so the capturing handler must be attached after setupLogger runs.
    private CapturingHandler attachCapture() {
        CapturingHandler handler = new CapturingHandler();
        handler.setLevel(Level.ALL);
        Logger.getLogger(LogUtil.class.getName()).addHandler(handler);
        return handler;
    }

    @Test
    public void testWarnLogAppearsWhenLogLevelIsInfo() {
        LogUtil.setupLogger(LogLevel.INFO);
        CapturingHandler handler = attachCapture();

        LogUtil.printWarningLog("deprecation warning");

        boolean warnCaptured = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.WARNING)
                        && r.getMessage().contains("deprecation warning"));
        Assert.assertTrue("WARN log should appear when LogLevel is INFO", warnCaptured);
    }

    @Test
    public void testWarnLogAppearsWhenLogLevelIsWarn() {
        LogUtil.setupLogger(LogLevel.WARN);
        CapturingHandler handler = attachCapture();

        LogUtil.printWarningLog("warn level warning");

        boolean warnCaptured = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.WARNING)
                        && r.getMessage().contains("warn level warning"));
        Assert.assertTrue("WARN log should appear when LogLevel is WARN", warnCaptured);
    }

    @Test
    public void testWarnLogAppearsWhenLogLevelIsDebug() {
        LogUtil.setupLogger(LogLevel.DEBUG);
        CapturingHandler handler = attachCapture();

        LogUtil.printWarningLog("debug level warning");

        boolean warnCaptured = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.WARNING)
                        && r.getMessage().contains("debug level warning"));
        Assert.assertTrue("WARN log should appear when LogLevel is DEBUG", warnCaptured);
    }

    @Test
    public void testWarnLogSuppressedWhenLogLevelIsError() {
        LogUtil.setupLogger(LogLevel.ERROR);
        CapturingHandler handler = attachCapture();

        LogUtil.printWarningLog("suppressed warning");

        boolean warnCaptured = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.WARNING));
        Assert.assertFalse("WARN log should NOT appear when LogLevel is ERROR", warnCaptured);
    }

    @Test
    public void testInfoLogSuppressedWhenLogLevelIsWarn() {
        LogUtil.setupLogger(LogLevel.WARN);
        CapturingHandler handler = attachCapture();

        LogUtil.printInfoLog("info message");

        boolean infoCaptured = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.INFO));
        Assert.assertFalse("INFO log should NOT appear when LogLevel is WARN", infoCaptured);
    }
}
