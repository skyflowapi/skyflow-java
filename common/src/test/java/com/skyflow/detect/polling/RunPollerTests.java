package com.skyflow.detect.polling;

import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.detect.PollOptions;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RunPollerTests {

    private static GetRunResponse run(DetectRunStatus status) {
        return new GetRunResponse("run-1", status, status == DetectRunStatus.SUCCESS ? DataSourceType.BASE64 : null,
                Collections.emptyList(), null, status == DetectRunStatus.FAILED ? "boom" : null);
    }

    /** Serves a scripted sequence of statuses and records every sleep instead of sleeping. */
    private static final class Script implements RunPoller.RunFetcher, RunPoller.Sleeper {
        final Iterator<DetectRunStatus> statuses;
        final List<Long> sleepsMillis = new ArrayList<>();
        int fetches;

        Script(DetectRunStatus... statuses) {
            this.statuses = Arrays.asList(statuses).iterator();
        }

        @Override
        public GetRunResponse fetch(String runId) {
            Assert.assertEquals("run-1", runId);
            fetches++;
            Assert.assertTrue("fetched more times than scripted", statuses.hasNext());
            return run(statuses.next());
        }

        @Override
        public void sleep(long millis) {
            sleepsMillis.add(millis);
        }
    }

    private static PollOptions options(Integer waitTime, Integer maxAttempts) {
        return PollOptions.builder().waitTime(waitTime).maxAttempts(maxAttempts).build();
    }

    @Test
    public void terminalStatusesStopImmediatelyWithoutSleeping() throws SkyflowException {
        for (DetectRunStatus terminal : new DetectRunStatus[]{DetectRunStatus.SUCCESS, DetectRunStatus.FAILED, DetectRunStatus.UNKNOWN}) {
            Script script = new Script(terminal);
            GetRunResponse result = RunPoller.poll("run-1", options(null, null), script, script);
            Assert.assertEquals(terminal, result.getStatus());
            Assert.assertEquals(1, script.fetches);
            Assert.assertTrue(script.sleepsMillis.isEmpty());
        }
    }

    @Test
    public void nonTerminalStatusesKeepPollingWithExponentialBackoff() throws SkyflowException {
        Script script = new Script(DetectRunStatus.QUEUED, DetectRunStatus.QUEUED, DetectRunStatus.IN_PROGRESS,
                DetectRunStatus.IN_PROGRESS, DetectRunStatus.SUCCESS);
        GetRunResponse result = RunPoller.poll("run-1", options(null, null), script, script);
        Assert.assertEquals(DetectRunStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(5, script.fetches);
        Assert.assertEquals(Arrays.asList(1000L, 2000L, 4000L, 8000L), script.sleepsMillis);
    }

    @Test
    public void defaultWaitTimeIsSpentExactlyThenRunIdIsReturned() throws SkyflowException {
        DetectRunStatus[] forever = new DetectRunStatus[30];
        Arrays.fill(forever, DetectRunStatus.IN_PROGRESS);
        Script script = new Script(forever);
        GetRunResponse result = RunPoller.poll("run-1", options(null, null), script, script);
        Assert.assertEquals(DetectRunStatus.IN_PROGRESS, result.getStatus());
        // 1+2+4+8+16 = 31, then the 32 s step is trimmed to 29 so the total is exactly 60.
        Assert.assertEquals(Arrays.asList(1000L, 2000L, 4000L, 8000L, 16000L, 29000L), script.sleepsMillis);
        Assert.assertEquals(7, script.fetches);
    }

    @Test
    public void delayPlateausAt32SecondsForLongWaits() throws SkyflowException {
        DetectRunStatus[] forever = new DetectRunStatus[30];
        Arrays.fill(forever, DetectRunStatus.QUEUED);
        Script script = new Script(forever);
        RunPoller.poll("run-1", options(300, null), script, script);
        // 1+2+4+8+16 = 31, then eight 32 s steps reach 287, and the last step is trimmed to 13 so the total is exactly 300.
        Assert.assertEquals(Arrays.asList(1000L, 2000L, 4000L, 8000L, 16000L, 32000L, 32000L, 32000L, 32000L, 32000L,
                32000L, 32000L, 32000L, 13000L), script.sleepsMillis);
        Assert.assertEquals(15, script.fetches);
    }

    @Test
    public void maxAttemptsCapsLookupsBeforeWaitTime() throws SkyflowException {
        Script script = new Script(DetectRunStatus.IN_PROGRESS, DetectRunStatus.IN_PROGRESS, DetectRunStatus.IN_PROGRESS);
        GetRunResponse result = RunPoller.poll("run-1", options(300, 3), script, script);
        Assert.assertEquals(DetectRunStatus.IN_PROGRESS, result.getStatus());
        Assert.assertEquals(3, script.fetches);
        Assert.assertEquals(Arrays.asList(1000L, 2000L), script.sleepsMillis);
    }

    @Test
    public void singleAttemptNeverSleeps() throws SkyflowException {
        Script script = new Script(DetectRunStatus.QUEUED);
        RunPoller.poll("run-1", options(null, 1), script, script);
        Assert.assertEquals(1, script.fetches);
        Assert.assertTrue(script.sleepsMillis.isEmpty());
    }

    @Test
    public void shortWaitTimeTrimsTheFirstSleep() throws SkyflowException {
        Script script = new Script(DetectRunStatus.QUEUED, DetectRunStatus.QUEUED);
        RunPoller.poll("run-1", options(1, null), script, script);
        Assert.assertEquals(Collections.singletonList(1000L), script.sleepsMillis);
        Assert.assertEquals(2, script.fetches);
    }

    @Test
    public void interruptionReturnsLastRunAndRestoresFlag() throws SkyflowException {
        Script script = new Script(DetectRunStatus.IN_PROGRESS, DetectRunStatus.SUCCESS);
        RunPoller.Sleeper interrupting = millis -> {
            throw new InterruptedException();
        };
        try {
            GetRunResponse result = RunPoller.poll("run-1", options(null, null), script, interrupting);
            Assert.assertEquals(DetectRunStatus.IN_PROGRESS, result.getStatus());
            Assert.assertEquals(1, script.fetches);
            Assert.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted(); // clear so later tests are unaffected
        }
    }

    @Test
    public void nullRunOrStatusFromFetcherIsTerminalUnknown() throws SkyflowException {
        GetRunResponse fromNull = RunPoller.poll("run-1", options(null, null), runId -> null, millis -> { });
        Assert.assertEquals("run-1", fromNull.getRunId());
        Assert.assertEquals(DetectRunStatus.UNKNOWN, fromNull.getStatus());
        Assert.assertTrue(fromNull.getOutput().isEmpty());

        GetRunResponse fromNullStatus = RunPoller.poll("run-1", options(null, null),
                runId -> new GetRunResponse(runId, null, null, Collections.emptyList(), null, null), millis -> { });
        Assert.assertEquals(DetectRunStatus.UNKNOWN, fromNullStatus.getStatus());
    }

    @Test
    public void fetchFailurePropagates() {
        RunPoller.RunFetcher failing = runId -> {
            throw new SkyflowException(404, "run not found");
        };
        try {
            RunPoller.poll("run-1", options(null, null), failing, millis -> { });
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertEquals(404, e.getHttpCode());
        }
    }
}
