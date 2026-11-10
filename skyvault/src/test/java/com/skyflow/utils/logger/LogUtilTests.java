package com.skyflow.utils.logger;

import com.skyflow.enums.LogLevel;
import com.skyflow.utils.Constants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogUtilTests {

    private static final Logger LOGGER = Logger.getLogger(LogUtil.class.getName());

    private boolean originalSetupDone;
    private Level originalLevel;

    private static class CapturingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override public void flush() {}
        @Override public void close() {}
    }

    private static Field setupDoneField() throws NoSuchFieldException {
        Field field = LogUtil.class.getDeclaredField("isLoggerSetupDone");
        field.setAccessible(true);
        return field;
    }

    @Before
    public void saveLoggerState() throws Exception {
        originalSetupDone = setupDoneField().getBoolean(null);
        originalLevel = LOGGER.getLevel();
    }

    @After
    public void restoreLoggerState() throws Exception {
        setupDoneField().setBoolean(null, originalSetupDone);
        LOGGER.setLevel(originalLevel);
    }

    // setupLogger calls LogManager.reset() which clears all handlers,
    // so the capturing handler must be attached after setupLogger runs.
    private CapturingHandler attachCapture() {
        CapturingHandler handler = new CapturingHandler();
        handler.setLevel(Level.ALL);
        LOGGER.addHandler(handler);
        return handler;
    }

    @Test
    public void testErrorLogBeforeSetupSetsUpLoggerAtErrorLevel() throws Exception {
        setupDoneField().setBoolean(null, false);

        LogUtil.printErrorLog("first error before any setup");

        Assert.assertTrue("the first error log should set the logger up", setupDoneField().getBoolean(null));
        Assert.assertEquals(Level.SEVERE, LOGGER.getLevel());
    }

    @Test
    public void testErrorLogAfterSetupIsLoggedWithSdkPrefix() {
        LogUtil.setupLogger(LogLevel.ERROR);
        CapturingHandler handler = attachCapture();

        LogUtil.printErrorLog("error after setup");

        Assert.assertEquals(1, handler.records.size());
        LogRecord record = handler.records.get(0);
        Assert.assertEquals(Level.SEVERE, record.getLevel());
        Assert.assertEquals("[" + Constants.SDK_PREFIX + "] error after setup", record.getMessage());
    }

    @Test
    public void testInfoWarningAndDebugLogsAreSkippedBeforeSetup() throws Exception {
        LogUtil.setupLogger(LogLevel.DEBUG);
        CapturingHandler handler = attachCapture();
        setupDoneField().setBoolean(null, false);

        LogUtil.printInfoLog("info before setup");
        LogUtil.printWarningLog("warning before setup");
        LogUtil.printDebugLog("debug before setup");

        Assert.assertTrue(handler.records.isEmpty());
        Assert.assertFalse("only an error log sets the logger up", setupDoneField().getBoolean(null));
    }
}
