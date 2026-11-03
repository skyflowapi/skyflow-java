package com.skyflow.detect;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DetectSourceTypesTests {

    private static List<String> names(Enum<?>[] values) {
        List<String> names = new ArrayList<>();
        for (Enum<?> value : values) {
            names.add(value.name());
        }
        return names;
    }

    @Test
    public void deidentifySourcesOfferedOnFlowvault() {
        Assert.assertEquals(Arrays.asList("BASE64", "PRESIGNED_URL"), names(DeidentifySourceType.values()));
        DeidentifyFileSource asSource = DeidentifySourceType.BASE64;
        Assert.assertEquals("BASE64", asSource.name());
    }

    @Test
    public void reidentifySourcesNeverIncludeSkyflowId() {
        Assert.assertEquals(Arrays.asList("BASE64", "PRESIGNED_URL"), names(ReidentifySourceType.values()));
        try {
            ReidentifySourceType.valueOf("SKYFLOW_ID");
            Assert.fail("re-identification does not accept a skyflow id");
        } catch (IllegalArgumentException expected) {
            // not offered
        }
        ReidentifyFileSource asSource = ReidentifySourceType.PRESIGNED_URL;
        Assert.assertEquals("PRESIGNED_URL", asSource.name());
    }
}
