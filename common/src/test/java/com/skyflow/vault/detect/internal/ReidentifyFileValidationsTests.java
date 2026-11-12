package com.skyflow.vault.detect.internal;


import com.skyflow.vault.detect.TestReidentifySource;
import com.skyflow.vault.detect.EntityType;
import com.skyflow.vault.detect.TestRedactionLevel;
import com.skyflow.enums.RedactionType;
import com.skyflow.vault.detect.ReidentifyFileRequest;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.BaseErrorMessage;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ReidentifyFileValidationsTests {

    private static ReidentifyFileRequest.ReidentifyFileRequestBuilder valid() {
        return ReidentifyFileRequest.builder().dataSource(TestReidentifySource.BASE64).value("Zm9v");
    }

    private static void assertRejected(ReidentifyFileRequest request, BaseErrorMessage expected) {
        try {
            ReidentifyFileValidations.validateReidentifyFileRequest(request);
            Assert.fail("expected SkyflowException: " + expected);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    @Test
    public void rejectsNullRequest() {
        assertRejected(null, BaseErrorMessage.ReidentifyFileRequestNull);
    }

    @Test
    public void rejectsMissingDataSource() {
        assertRejected(ReidentifyFileRequest.builder().value("Zm9v").build(), BaseErrorMessage.InvalidDataSourceInReidentifyFile);
    }

    @Test
    public void rejectsNullOrBlankValue() {
        assertRejected(ReidentifyFileRequest.builder().dataSource(TestReidentifySource.BASE64).build(), BaseErrorMessage.InvalidValueInReidentifyFile);
        assertRejected(valid().value("").build(), BaseErrorMessage.InvalidValueInReidentifyFile);
        assertRejected(valid().value("   \t").build(), BaseErrorMessage.InvalidValueInReidentifyFile);
    }

    @Test
    public void checksFieldsInOrderDataSourceBeforeValue() {
        // Both missing: dataSource is reported first.
        assertRejected(ReidentifyFileRequest.builder().build(), BaseErrorMessage.InvalidDataSourceInReidentifyFile);
    }

    @Test
    public void acceptsMinimalAndFullValidRequests() throws SkyflowException {
        ReidentifyFileValidations.validateReidentifyFileRequest(valid().build());
        ReidentifyFileValidations.validateReidentifyFileRequest(valid()
                .dataSource(TestReidentifySource.PRESIGNED_URL).value("https://example.com/f")
                .redactionLevel(Arrays.asList(
                        TestRedactionLevel.builder().tokenGroupName("g").redactionPattern("p").build(),
                        TestRedactionLevel.builder().entityName(EntityType.NAME).redactionType(RedactionType.REDACTED).build(),
                        TestRedactionLevel.builder().tokenGroupName("g2").redactionType(RedactionType.PLAIN_TEXT).build(),
                        TestRedactionLevel.builder().entityName(EntityType.SSN).redactionPattern("p2").build()))
                .build());
    }

    @Test
    public void nullOrEmptyRedactionLevelListIsAllowed() throws SkyflowException {
        ReidentifyFileValidations.validateReidentifyFileRequest(valid().redactionLevel(null).build());
        ReidentifyFileValidations.validateReidentifyFileRequest(valid().redactionLevel(Collections.<TestRedactionLevel>emptyList()).build());
    }

    @Test
    public void anyRedactionLevelShapePassesThrough() throws SkyflowException {
        // Every TestRedactionLevel field is optional on the API; the server applies its own rules.
        ReidentifyFileValidations.validateReidentifyFileRequest(valid().redactionLevel(Arrays.asList(
                null,
                TestRedactionLevel.builder().build(),
                TestRedactionLevel.builder().tokenGroupName("g").build(),
                TestRedactionLevel.builder().tokenGroupName("g").entityName(EntityType.NAME)
                        .redactionPattern("p").redactionType(RedactionType.MASKED).build())).build());
    }
}
