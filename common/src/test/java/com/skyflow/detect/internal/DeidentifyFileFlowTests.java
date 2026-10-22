package com.skyflow.detect.internal;

import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DeidentifyFileRequest;
import com.skyflow.detect.DeidentifyFileResponse;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.FileOutput;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.detect.Metrics;
import com.skyflow.detect.PollOptions;
import com.skyflow.detect.polling.RunPoller;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class DeidentifyFileFlowTests {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final RunPoller.Sleeper NO_SLEEP = millis -> { };

    private static GetRunResponse success() {
        return success(DataSourceType.BASE64);
    }

    private static GetRunResponse success(DataSourceType outputType) {
        return new GetRunResponse("run-1", DetectRunStatus.SUCCESS, outputType,
                Arrays.asList(
                        new FileOutput(Base64.getEncoder().encodeToString("redacted".getBytes()),
                                "REDACTED_FILE", FileDataFormat.TXT),
                        new FileOutput(Base64.getEncoder().encodeToString("[]".getBytes()),
                                "ENTITIES", FileDataFormat.JSON)),
                new Metrics(null, 1, 8, null, null, null), null);
    }

    @Test
    public void withoutPollOptionsReturnsRunIdAndNeverFetches() throws Exception {
        AtomicInteger fetches = new AtomicInteger();
        DeidentifyFileResponse response = DeidentifyFileFlow.complete(
                DeidentifyFileRequest.builder().dataSource(DataSourceType.BASE64).value("x").build(), "run-1",
                id -> { fetches.incrementAndGet(); return success(); }, NO_SLEEP);
        Assert.assertEquals("run-1", response.getRunId());
        Assert.assertNull(response.getStatus());
        Assert.assertTrue(response.getOutput().isEmpty());
        Assert.assertEquals(0, fetches.get());
    }

    @Test
    public void missingRunIdSkipsPollingEvenWhenRequested() throws Exception {
        AtomicInteger fetches = new AtomicInteger();
        DeidentifyFileResponse response = DeidentifyFileFlow.complete(
                DeidentifyFileRequest.builder().dataSource(DataSourceType.BASE64).value("x")
                        .pollOptions(PollOptions.builder().build()).build(), null,
                id -> { fetches.incrementAndGet(); return success(); }, NO_SLEEP);
        Assert.assertNull(response.getRunId());
        Assert.assertNull(response.getStatus());
        Assert.assertEquals(0, fetches.get());
    }

    @Test
    public void polledRawRequestMergesRunButWritesNothing() throws Exception {
        DeidentifyFileResponse response = DeidentifyFileFlow.complete(
                DeidentifyFileRequest.builder().dataSource(DataSourceType.SKYFLOW_ID).value("rec")
                        .pollOptions(PollOptions.builder().build()).outputDirectory(folder.getRoot().getPath()).build(),
                "run-1", id -> success(), NO_SLEEP);
        Assert.assertEquals(DetectRunStatus.SUCCESS, response.getStatus());
        Assert.assertEquals(2, response.getOutput().size());
        Assert.assertEquals(Integer.valueOf(8), response.getMetrics().getCharacterCount());
        Assert.assertEquals(0, folder.getRoot().list().length);
    }

    @Test
    public void polledFileRequestWritesOutputsOnSuccess() throws Exception {
        File input = folder.newFile("notes.txt");
        Files.write(input.toPath(), "hello".getBytes());
        File outDir = folder.newFolder("out");

        DeidentifyFileResponse response = DeidentifyFileFlow.complete(
                DeidentifyFileRequest.builder().file(input).pollOptions(PollOptions.builder().build())
                        .outputDirectory(outDir.getPath()).build(),
                "run-1", id -> success(), NO_SLEEP);

        Assert.assertEquals(DetectRunStatus.SUCCESS, response.getStatus());
        File written = new File(outDir, "processed-notes.txt");
        Assert.assertTrue(written.isFile());
        Assert.assertEquals("redacted", new String(Files.readAllBytes(written.toPath())));
        File entities = new File(outDir, "processed-entities-notes.json");
        Assert.assertTrue(entities.isFile());
        Assert.assertEquals("[]", new String(Files.readAllBytes(entities.toPath())));
        Assert.assertEquals(2, outDir.list().length);
    }

    @Test
    public void polledFileRequestWritesNothingForPresignedUrlOutputs() throws Exception {
        File input = folder.newFile("notes.txt");
        File outDir = folder.newFolder("out");

        DeidentifyFileResponse response = DeidentifyFileFlow.complete(
                DeidentifyFileRequest.builder().file(input).pollOptions(PollOptions.builder().build())
                        .outputDirectory(outDir.getPath()).build(),
                "run-1", id -> success(DataSourceType.PRESIGNED_URL), NO_SLEEP);

        Assert.assertEquals(DetectRunStatus.SUCCESS, response.getStatus());
        Assert.assertEquals(DataSourceType.PRESIGNED_URL, response.getOutputType());
        Assert.assertEquals(2, response.getOutput().size());
        Assert.assertEquals(0, outDir.list().length);
    }

    @Test
    public void polledFileRequestWritesNothingWhenRunFailsOrIsPending() throws Exception {
        File input = folder.newFile("notes.txt");
        File outDir = folder.newFolder("out");
        DeidentifyFileRequest request = DeidentifyFileRequest.builder().file(input)
                .pollOptions(PollOptions.builder().maxAttempts(1).build()).outputDirectory(outDir.getPath()).build();

        DeidentifyFileResponse failed = DeidentifyFileFlow.complete(request, "run-1",
                id -> new GetRunResponse(id, DetectRunStatus.FAILED, null, Collections.emptyList(), null, "bad input"), NO_SLEEP);
        Assert.assertEquals(DetectRunStatus.FAILED, failed.getStatus());
        Assert.assertEquals("bad input", failed.getMessage());

        DeidentifyFileResponse pending = DeidentifyFileFlow.complete(request, "run-2",
                id -> new GetRunResponse(id, DetectRunStatus.IN_PROGRESS, null, Collections.emptyList(), null, null), NO_SLEEP);
        Assert.assertEquals("run-2", pending.getRunId());
        Assert.assertEquals(DetectRunStatus.IN_PROGRESS, pending.getStatus());
        Assert.assertEquals(0, outDir.list().length);
    }
}
