package com.skyflow.detect.internal;

import com.skyflow.detect.CheckGuardrailsRequest;
import com.skyflow.detect.DeidentifyStringRequest;
import com.skyflow.detect.DetectConfiguration;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.GetRunRequest;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.RedactionType;
import com.skyflow.detect.ReidentifyStringRequest;
import com.skyflow.errors.BaseErrorMessage;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DetectValidationsTests {

    private static void assertInvalid(SkyflowException e, BaseErrorMessage expected) {
        Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        Assert.assertEquals(expected.getMessage(), e.getMessage());
    }

    @Test
    public void deidentifyStringRejectsNullRequest() {
        try {
            DetectValidations.validateDeidentifyStringRequest(null);
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            assertInvalid(e, BaseErrorMessage.DeidentifyStringRequestNull);
        }
    }

    @Test
    public void deidentifyStringRejectsBlankText() {
        try {
            DetectValidations.validateDeidentifyStringRequest(
                    DeidentifyStringRequest.builder().text("   ").configurationId("cfg").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            assertInvalid(e, BaseErrorMessage.InvalidTextInDeidentifyString);
        }
    }

    @Test
    public void deidentifyStringDoesNotConstrainConfigurationSources() throws SkyflowException {
        // configurationId and configuration are both optional on the API: none, either or both pass through
        DetectValidations.validateDeidentifyStringRequest(DeidentifyStringRequest.builder().text("hello").build());
        DetectValidations.validateDeidentifyStringRequest(DeidentifyStringRequest.builder().text("hello")
                .configurationId("cfg").configuration(DetectConfiguration.builder().build()).build());
        DetectValidations.validateDeidentifyStringRequest(
                DeidentifyStringRequest.builder().text("hello").configurationId("cfg").build());
        DetectValidations.validateDeidentifyStringRequest(
                DeidentifyStringRequest.builder().text("hello")
                        .configuration(DetectConfiguration.builder().build()).build());
    }

    @Test
    public void reidentifyStringRejectsNullAndBlank() {
        try {
            DetectValidations.validateReidentifyStringRequest(null);
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            assertInvalid(e, BaseErrorMessage.ReidentifyStringRequestNull);
        }
        try {
            DetectValidations.validateReidentifyStringRequest(ReidentifyStringRequest.builder().text("").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            assertInvalid(e, BaseErrorMessage.InvalidTextInReidentifyString);
        }
    }

    @Test
    public void reidentifyStringAcceptsValidRedactionLevels() throws SkyflowException {
        DetectValidations.validateReidentifyStringRequest(ReidentifyStringRequest.builder()
                .text("[NAME_1]")
                .redactionLevel(Arrays.asList(
                        RedactionLevel.builder().entityName(EntityType.NAME).redactionType(RedactionType.MASKED).build(),
                        RedactionLevel.builder().tokenGroupName("group").redactionPattern("pattern").build()))
                .build());
        DetectValidations.validateReidentifyStringRequest(ReidentifyStringRequest.builder().text("[NAME_1]").build());
    }

    @Test
    public void reidentifyStringPassesAnyRedactionLevelShapeThrough() throws SkyflowException {
        // The API marks every RedactionLevel field optional; the server applies its own rules.
        DetectValidations.validateReidentifyStringRequest(ReidentifyStringRequest.builder().text("[NAME_1]")
                .redactionLevel(Arrays.asList(
                        RedactionLevel.builder().build(),
                        RedactionLevel.builder().tokenGroupName("g").build(),
                        RedactionLevel.builder().entityName(EntityType.NAME).redactionType(RedactionType.MASKED).redactionPattern("p").build(),
                        null))
                .build());
    }

    @Test
    public void getRunRequiresRunId() throws SkyflowException {
        try {
            DetectValidations.validateGetRunRequest(null);
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            assertInvalid(e, BaseErrorMessage.GetRunRequestNull);
        }
        try {
            DetectValidations.validateGetRunRequest(GetRunRequest.builder().runId(" ").build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            assertInvalid(e, BaseErrorMessage.InvalidRunIdInGetRun);
        }
        DetectValidations.validateGetRunRequest(GetRunRequest.builder().runId("run-1").build());
    }

    @Test
    public void checkGuardrailsRequiresOnlyText() throws SkyflowException {
        try {
            DetectValidations.validateCheckGuardrailsRequest(null);
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            assertInvalid(e, BaseErrorMessage.CheckGuardrailsRequestNull);
        }
        try {
            DetectValidations.validateCheckGuardrailsRequest(CheckGuardrailsRequest.builder().build());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            assertInvalid(e, BaseErrorMessage.InvalidTextInCheckGuardrails);
        }
        // denyTopics is optional on the API and its entries are not inspected by the SDK.
        DetectValidations.validateCheckGuardrailsRequest(
                CheckGuardrailsRequest.builder().text("hi").denyTopics(Arrays.asList("politics", "", null)).build());
        DetectValidations.validateCheckGuardrailsRequest(CheckGuardrailsRequest.builder().text("hi").build());
        DetectValidations.validateCheckGuardrailsRequest(
                CheckGuardrailsRequest.builder().text("hi").checkToxicity(true).denyTopics(Collections.singletonList("politics")).build());
    }
}
