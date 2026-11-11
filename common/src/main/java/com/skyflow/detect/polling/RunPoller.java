package com.skyflow.detect.polling;

import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.FileOutput;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.detect.PollOptions;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;

import java.util.Collections;

/**
 * Polls a Detect file run until it reaches a terminal status or the {@link PollOptions} budget is used
 * up. Shared by every SDK: the caller supplies how a run is fetched, so each controller plugs in its
 * own {@code getRun}.
 *
 * <p>Backoff is exponential, 1, 2, 4, 8, 16 then 32 seconds between lookups, with the last sleep
 * trimmed so the total never exceeds {@code waitTime}. The first lookup happens immediately after the
 * submit. SUCCESS, FAILED and UNKNOWN are terminal; every other status keeps polling.
 */
public final class RunPoller {
    /** Longest single pause between two lookups, in seconds. */
    public static final long MAX_DELAY_SECONDS = 32L;

    /** Fetches the current state of a run. */
    public interface RunFetcher {
        GetRunResponse fetch(String runId) throws SkyflowException;
    }

    /** Pauses the calling thread. Replaceable in tests. */
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private RunPoller() {
    }

    /**
     * Polls on the calling thread.
     *
     * @param runId   run to poll
     * @param options wait time and attempt limit, defaults applied
     * @param fetcher how to look the run up
     * @return the terminal run, or the last non-terminal run seen when the budget ran out or the thread was interrupted
     * @throws SkyflowException if a lookup fails
     */
    public static GetRunResponse poll(String runId, PollOptions options, RunFetcher fetcher) throws SkyflowException {
        return poll(runId, options, fetcher, Thread::sleep);
    }

    public static GetRunResponse poll(String runId, PollOptions options, RunFetcher fetcher, Sleeper sleeper)
            throws SkyflowException {
        int waitTime = options.effectiveWaitTime();
        int maxAttempts = options.effectiveMaxAttempts();
        LogUtil.printInfoLog(BaseUtils.parameterizedString(InfoLogs.POLL_RUN_STARTED.getLog(), runId,
                String.valueOf(waitTime), String.valueOf(maxAttempts)));

        long elapsedSeconds = 0;
        long delaySeconds = 1;
        int attempts = 0;
        while (true) {
            GetRunResponse run = fetcher.fetch(runId);
            attempts++;
            if (run == null || run.getStatus() == null) {
                // A fetcher that reports nothing is treated as an unknown, terminal state.
                run = new GetRunResponse(runId, DetectRunStatus.UNKNOWN, null, Collections.<FileOutput>emptyList(), null, null);
            }
            switch (run.getStatus()) {
                case SUCCESS:
                case FAILED:
                case UNKNOWN:
                    LogUtil.printInfoLog(BaseUtils.parameterizedString(InfoLogs.POLL_RUN_TERMINAL.getLog(), runId,
                            run.getStatus().name(), String.valueOf(attempts)));
                    return run;
                default:
                    // QUEUED, IN_PROGRESS: keep polling while the budget allows.
                    break;
            }
            if (attempts >= maxAttempts || elapsedSeconds >= waitTime) {
                LogUtil.printInfoLog(BaseUtils.parameterizedString(InfoLogs.POLL_RUN_BUDGET_EXHAUSTED.getLog(), runId,
                        run.getStatus().name(), String.valueOf(attempts), String.valueOf(elapsedSeconds)));
                return run;
            }
            long sleepSeconds = Math.min(delaySeconds, waitTime - elapsedSeconds);
            try {
                sleeper.sleep(sleepSeconds * 1000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                LogUtil.printInfoLog(BaseUtils.parameterizedString(InfoLogs.POLL_RUN_INTERRUPTED.getLog(), runId,
                        run.getStatus().name()));
                return run;
            }
            elapsedSeconds += sleepSeconds;
            delaySeconds = Math.min(delaySeconds * 2, MAX_DELAY_SECONDS);
        }
    }
}
