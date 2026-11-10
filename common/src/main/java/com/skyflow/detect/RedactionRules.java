package com.skyflow.detect;

import com.skyflow.generated.common.rest.types.RedactionLevel;
import com.skyflow.generated.common.rest.types.RedactionLevelEntityName;
import com.skyflow.generated.common.rest.types.RedactionLevelRedactionType;

/**
 * Internal: turns an SDK {@link RedactionRule} into the API's {@code RedactionLevel}, which carries all four
 * fields for every vault type. Not part of the public contract.
 */
public final class RedactionRules {

    private RedactionRules() {
    }

    /**
     * @param rule a rule built with this SDK's {@code RedactionLevel}
     * @return the wire representation with only the fields the rule carries
     */
    public static RedactionLevel toApi(RedactionRule rule) {
        RedactionLevel.Builder builder = RedactionLevel.builder();
        if (rule.apiEntityName() != null) {
            builder.entityName(RedactionLevelEntityName.valueOf(rule.apiEntityName()));
        }
        if (rule.apiRedactionType() != null) {
            builder.redactionType(RedactionLevelRedactionType.valueOf(rule.apiRedactionType()));
        }
        if (rule.apiTokenGroupName() != null) {
            builder.tokenGroupName(rule.apiTokenGroupName());
        }
        if (rule.apiRedactionPattern() != null) {
            builder.redactionPattern(rule.apiRedactionPattern());
        }
        return builder.build();
    }
}
