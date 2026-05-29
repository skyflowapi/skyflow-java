package com.skyflow.enums;

import com.skyflow.enums.LogLevel;
import com.skyflow.generated.rest.types.V1Byot;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class TokenModeTest {

    private static class CapturingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();
        @Override public void publish(LogRecord r) { records.add(r); }
        @Override public void flush() {}
        @Override public void close() {}
    }

    private CapturingHandler attachCapture() {
        CapturingHandler handler = new CapturingHandler();
        handler.setLevel(Level.ALL);
        Logger.getLogger(LogUtil.class.getName()).addHandler(handler);
        return handler;
    }

    // --- getByot() ---

    @Test
    public void testGetByotDisable() {
        Assert.assertEquals(V1Byot.DISABLE, TokenMode.DISABLE.getByot());
    }

    @Test
    public void testGetByotEnable() {
        Assert.assertEquals(V1Byot.ENABLE, TokenMode.ENABLE.getByot());
    }

    @Test
    public void testGetByotEnableStrict() {
        Assert.assertEquals(V1Byot.ENABLE_STRICT, TokenMode.ENABLE_STRICT.getByot());
    }

    // --- getBYOT() delegates and emits a runtime warning ---

    @Test
    @SuppressWarnings("deprecation")
    public void testGetBYOTDelegatesToGetByotDisable() {
        Assert.assertEquals(V1Byot.DISABLE, TokenMode.DISABLE.getBYOT());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetBYOTDelegatesToGetByotEnable() {
        Assert.assertEquals(V1Byot.ENABLE, TokenMode.ENABLE.getBYOT());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetBYOTDelegatesToGetByotEnableStrict() {
        Assert.assertEquals(V1Byot.ENABLE_STRICT, TokenMode.ENABLE_STRICT.getBYOT());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetBYOTEmitsDeprecationWarning() {
        LogUtil.setupLogger(LogLevel.INFO);
        CapturingHandler handler = attachCapture();

        TokenMode.ENABLE.getBYOT();

        boolean warnFired = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.WARNING)
                        && r.getMessage().contains(InfoLogs.DEPRECATED_GET_BYOT.getLog()));
        Assert.assertTrue("getBYOT() should emit a deprecation warning log", warnFired);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testGetBYOTWarningIsSuppressedAtErrorLevel() {
        LogUtil.setupLogger(LogLevel.ERROR);
        CapturingHandler handler = attachCapture();

        TokenMode.ENABLE.getBYOT();

        boolean warnFired = handler.records.stream()
                .anyMatch(r -> r.getLevel().equals(Level.WARNING));
        Assert.assertFalse("getBYOT() warning should be suppressed at ERROR log level", warnFired);
    }

    // --- toString() ---

    @Test
    public void testToStringDisable() {
        Assert.assertEquals("DISABLE", TokenMode.DISABLE.toString());
    }

    @Test
    public void testToStringEnable() {
        Assert.assertEquals("ENABLE", TokenMode.ENABLE.toString());
    }

    @Test
    public void testToStringEnableStrict() {
        Assert.assertEquals("ENABLE_STRICT", TokenMode.ENABLE_STRICT.toString());
    }
}
