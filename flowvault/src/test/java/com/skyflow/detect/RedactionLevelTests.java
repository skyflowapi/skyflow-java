package com.skyflow.detect;

import org.junit.Assert;
import org.junit.Test;

public class RedactionLevelTests {

    @Test
    public void flowvaultRuleCarriesTokenGroupAndPatternOnly() {
        RedactionLevel rule = RedactionLevel.builder().tokenGroupName("names").redactionPattern("mask").build();
        Assert.assertEquals("names", rule.getTokenGroupName());
        Assert.assertEquals("mask", rule.getRedactionPattern());

        com.skyflow.generated.common.rest.types.RedactionLevel api = RedactionRules.toApi(rule);
        Assert.assertEquals("names", api.getTokenGroupName().get());
        Assert.assertEquals("mask", api.getRedactionPattern().get());
        Assert.assertFalse(api.getEntityName().isPresent());
        Assert.assertFalse(api.getRedactionType().isPresent());
    }

    @Test
    public void unsetFieldsAreNotSent() {
        com.skyflow.generated.common.rest.types.RedactionLevel api = RedactionRules.toApi(RedactionLevel.builder().build());
        Assert.assertFalse(api.getTokenGroupName().isPresent());
        Assert.assertFalse(api.getRedactionPattern().isPresent());
    }
}
