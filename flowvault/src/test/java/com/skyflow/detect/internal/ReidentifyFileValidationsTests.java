package com.skyflow.detect.internal;

import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.RedactionType;
import com.skyflow.detect.ReidentifyFileRequest;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ReidentifyFileValidationsTests {

    private static ReidentifyFileRequest.ReidentifyFileRequestBuilder valid() {
        return ReidentifyFileRequest.builder().dataSource(DataSourceType.BASE64).value("Zm9v");
    }

    private static void assertRejected(ReidentifyFileRequest request, ErrorMessage expected) {
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
        assertRejected(null, ErrorMessage.ReidentifyFileRequestNull);
    }

    @Test
    public void rejectsMissingDataSource() {
        assertRejected(ReidentifyFileRequest.builder().value("Zm9v").build(), ErrorMessage.InvalidDataSourceInReidentifyFile);
    }

    @Test
    public void rejectsNullOrBlankValue() {
        assertRejected(ReidentifyFileRequest.builder().dataSource(DataSourceType.BASE64).build(), ErrorMessage.InvalidValueInReidentifyFile);
        assertRejected(valid().value("").build(), ErrorMessage.InvalidValueInReidentifyFile);
        assertRejected(valid().value("   \t").build(), ErrorMessage.InvalidValueInReidentifyFile);
    }

    @Test
    public void checksFieldsInOrderDataSourceBeforeValue() {
        // Both missing: dataSource is reported first.
        assertRejected(ReidentifyFileRequest.builder().build(), ErrorMessage.InvalidDataSourceInReidentifyFile);
    }

    @Test
    public void acceptsMinimalAndFullValidRequests() throws SkyflowException {
        ReidentifyFileValidations.validateReidentifyFileRequest(valid().build());
        ReidentifyFileValidations.validateReidentifyFileRequest(valid()
                .dataSource(DataSourceType.PRESIGNED_URL).value("https://example.com/f")
                .redactionLevel(Arrays.asList(
                        RedactionLevel.builder().tokenGroupName("g").redactionPattern("p").build(),
                        RedactionLevel.builder().entityName(EntityType.NAME).redactionType(RedactionType.REDACTED).build(),
                        RedactionLevel.builder().tokenGroupName("g2").redactionType(RedactionType.PLAIN_TEXT).build(),
                        RedactionLevel.builder().entityName(EntityType.SSN).redactionPattern("p2").build()))
                .build());
    }

    @Test
    public void nullOrEmptyRedactionLevelListIsAllowed() throws SkyflowException {
        ReidentifyFileValidations.validateReidentifyFileRequest(valid().redactionLevel(null).build());
        ReidentifyFileValidations.validateReidentifyFileRequest(valid().redactionLevel(Collections.<RedactionLevel>emptyList()).build());
    }

    @Test
    public void anyRedactionLevelShapePassesThrough() throws SkyflowException {
        // Every RedactionLevel field is optional on the API; the server applies its own rules.
        ReidentifyFileValidations.validateReidentifyFileRequest(valid().redactionLevel(Arrays.asList(
                null,
                RedactionLevel.builder().build(),
                RedactionLevel.builder().tokenGroupName("g").build(),
                RedactionLevel.builder().tokenGroupName("g").entityName(EntityType.NAME)
                        .redactionPattern("p").redactionType(RedactionType.MASKED).build())).build());
    }
}
