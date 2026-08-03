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

    @Test
    public void testDebugLogAppearsWhenLogLevelIsDebug() {
        LogUtil.setupLogger(LogLevel.DEBUG);
        CapturingHandler handler = attachCapture();

        LogUtil.printDebugLog("debug message");

        boolean debugCaptured = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.CONFIG)
                        && r.getMessage().contains("debug message"));
        Assert.assertTrue("DEBUG log should appear when LogLevel is DEBUG", debugCaptured);
    }

    @Test
    public void testDebugLogSuppressedWhenLogLevelIsInfo() {
        LogUtil.setupLogger(LogLevel.INFO);
        CapturingHandler handler = attachCapture();

        LogUtil.printDebugLog("suppressed debug message");

        boolean debugCaptured = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.CONFIG));
        Assert.assertFalse("DEBUG log should NOT appear when LogLevel is INFO", debugCaptured);
    }

    @Test
    public void testErrorLogAppearsWhenLogLevelIsError() {
        LogUtil.setupLogger(LogLevel.ERROR);
        CapturingHandler handler = attachCapture();

        LogUtil.printErrorLog("error message");

        boolean errorCaptured = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.SEVERE)
                        && r.getMessage().contains("error message"));
        Assert.assertTrue("ERROR log should appear when LogLevel is ERROR", errorCaptured);
    }

    @Test
    public void testErrorLogAppearsWhenLogLevelIsDebug() {
        LogUtil.setupLogger(LogLevel.DEBUG);
        CapturingHandler handler = attachCapture();

        LogUtil.printErrorLog("debug level error message");

        boolean errorCaptured = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.SEVERE)
                        && r.getMessage().contains("debug level error message"));
        Assert.assertTrue("ERROR log should appear when LogLevel is DEBUG", errorCaptured);
    }

    @Test
    public void testNoLogsAppearWhenLogLevelIsOff() {
        LogUtil.setupLogger(LogLevel.OFF);
        CapturingHandler handler = attachCapture();

        LogUtil.printErrorLog("off error message");
        LogUtil.printWarningLog("off warning message");
        LogUtil.printInfoLog("off info message");
        LogUtil.printDebugLog("off debug message");

        Assert.assertTrue("No logs should appear when LogLevel is OFF", handler.records.isEmpty());
    }
}
