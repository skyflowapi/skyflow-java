package com.skyflow.detect;

import com.skyflow.enums.RedactionType;
import org.junit.Assert;
import org.junit.Test;

public class RedactionLevelTests {

    @Test
    public void skyvaultRuleCarriesEntityNameAndRedactionTypeOnly() {
        RedactionLevel rule = RedactionLevel.builder().entityName(EntityType.NAME).redactionType(RedactionType.MASKED).build();
        Assert.assertEquals(EntityType.NAME, rule.getEntityName());
        Assert.assertEquals(RedactionType.MASKED, rule.getRedactionType());

        com.skyflow.generated.common.rest.types.RedactionLevel api = RedactionRules.toApi(rule);
        Assert.assertEquals("NAME", api.getEntityName().get().toString());
        Assert.assertEquals("MASKED", api.getRedactionType().get().toString());
        Assert.assertFalse(api.getTokenGroupName().isPresent());
        Assert.assertFalse(api.getRedactionPattern().isPresent());
    }

    @Test
    public void unsetFieldsAreNotSent() {
        com.skyflow.generated.common.rest.types.RedactionLevel api = RedactionRules.toApi(RedactionLevel.builder().build());
        Assert.assertFalse(api.getEntityName().isPresent());
        Assert.assertFalse(api.getRedactionType().isPresent());
    }
}
