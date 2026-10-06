package com.skyflow.detect.internal;

import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DeidentifyFileRequest;
import com.skyflow.detect.PollOptions;
import com.skyflow.errors.BaseErrorMessage;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;

public class DeidentifyFileValidationsTests {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static void assertRejected(DeidentifyFileRequest request, BaseErrorMessage expected) {
        try {
            DetectValidations.validateDeidentifyFileRequest(request);
            Assert.fail("expected SkyflowException: " + expected);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    private static DeidentifyFileRequest.DeidentifyFileRequestBuilder raw() {
        return DeidentifyFileRequest.builder().dataSource(DataSourceType.BASE64).value("Zm9v");
    }

    @Test
    public void rejectsNullRequest() {
        assertRejected(null, BaseErrorMessage.DeidentifyFileRequestNull);
    }

    @Test
    public void rawValueRequiresDataSourceAndValue() throws SkyflowException {
        assertRejected(DeidentifyFileRequest.builder().value("x").build(), BaseErrorMessage.InvalidDataSourceInDeidentifyFile);
        assertRejected(DeidentifyFileRequest.builder().dataSource(DataSourceType.SKYFLOW_ID).build(), BaseErrorMessage.InvalidValueInDeidentifyFile);
        assertRejected(raw().value(" ").build(), BaseErrorMessage.InvalidValueInDeidentifyFile);
        DetectValidations.validateDeidentifyFileRequest(raw().build());
        DetectValidations.validateDeidentifyFileRequest(raw().configurationId("cfg").build());
    }

    @Test
    public void fileRequestsNeedAnExistingFileAndNoValue() throws Exception {
        File existing = folder.newFile("notes.txt");
        Files.write(existing.toPath(), "hello".getBytes("UTF-8"));
        DetectValidations.validateDeidentifyFileRequest(DeidentifyFileRequest.builder().file(existing).build());
        DetectValidations.validateDeidentifyFileRequest(DeidentifyFileRequest.builder().filePath(existing.getPath()).build());

        assertRejected(DeidentifyFileRequest.builder().file(existing).value("Zm9v").build(), BaseErrorMessage.MultipleFileSourcesInDeidentifyFile);
        assertRejected(DeidentifyFileRequest.builder().filePath(new File(folder.getRoot(), "missing.txt").getPath()).build(),
                BaseErrorMessage.FileNotFoundToDeidentify);
        assertRejected(DeidentifyFileRequest.builder().file(folder.getRoot()).build(), BaseErrorMessage.FileNotFoundToDeidentify);
    }

    @Test
    public void pollOptionsBoundsAreEnforced() throws SkyflowException {
        DetectValidations.validateDeidentifyFileRequest(raw().pollOptions(PollOptions.builder().build()).build());
        DetectValidations.validateDeidentifyFileRequest(raw().pollOptions(PollOptions.builder().waitTime(1).maxAttempts(1).build()).build());
        DetectValidations.validateDeidentifyFileRequest(raw().pollOptions(PollOptions.builder().waitTime(300).maxAttempts(23).build()).build());
        for (int bad : new int[]{0, -1, 301}) {
            assertRejected(raw().pollOptions(PollOptions.builder().waitTime(bad).build()).build(), BaseErrorMessage.InvalidWaitTimeInPollOptions);
        }
        for (int bad : new int[]{0, -5, 24}) {
            assertRejected(raw().pollOptions(PollOptions.builder().maxAttempts(bad).build()).build(), BaseErrorMessage.InvalidMaxAttemptsInPollOptions);
        }
    }

    @Test
    public void outputDirectoryRequiresPollOptions() throws SkyflowException {
        assertRejected(raw().outputDirectory("/tmp/out").build(), BaseErrorMessage.OutputDirectoryWithoutPollOptions);
        DetectValidations.validateDeidentifyFileRequest(raw().outputDirectory("  ").build());
        DetectValidations.validateDeidentifyFileRequest(raw().outputDirectory("/tmp/out").pollOptions(PollOptions.builder().build()).build());
    }
}
