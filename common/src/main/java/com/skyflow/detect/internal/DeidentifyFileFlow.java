package com.skyflow.detect.internal;

import com.skyflow.detect.DeidentifyFileRequest;
import com.skyflow.detect.DeidentifyFileResponse;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.detect.polling.RunPoller;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;

/**
 * Everything {@code deidentifyFile} does after the submit call, shared by every SDK: decide whether to
 * poll, poll, merge the run into the response, and write outputs for file-based requests. Each
 * controller performs the submit itself and hands the run id here.
 */
public final class DeidentifyFileFlow {
    private DeidentifyFileFlow() {
    }

    public static DeidentifyFileResponse complete(DeidentifyFileRequest request, String runId, RunPoller.RunFetcher fetcher)
            throws SkyflowException {
        return complete(request, runId, fetcher, Thread::sleep);
    }

    static DeidentifyFileResponse complete(DeidentifyFileRequest request, String runId, RunPoller.RunFetcher fetcher,
                                           RunPoller.Sleeper sleeper) throws SkyflowException {
        if (request.getPollOptions() == null || runId == null) {
            LogUtil.printInfoLog(BaseUtils.parameterizedString(InfoLogs.DEIDENTIFY_FILE_RUN_SUBMITTED.getLog(), String.valueOf(runId)));
            return DetectResponseMapper.submittedDeidentifyFileResponse(runId);
        }
        GetRunResponse run = RunPoller.poll(runId, request.getPollOptions(), fetcher, sleeper);
        DeidentifyFileResponse response = DetectResponseMapper.toDeidentifyFileResponse(runId, run);
        if (response.getStatus() == DetectRunStatus.SUCCESS && request.getFile() != null) {
            FileOutputWriter.write(response.getOutput(), response.getOutputType(), request.getFile().getName(),
                    request.getOutputDirectory());
        }
        return response;
    }
}
