package com.skyflow.vault.detect;

/**
 * One re-identification rendering rule, the element of {@code redactionLevel} on {@code ReidentifyStringRequest}
 * and {@code ReidentifyFileRequest}. Each SDK ships its own {@code com.skyflow.vault.detect.RedactionLevel} with the
 * pair of fields its vault type uses: skyvault names an {@code entityName} and a {@code redactionType}; flowvault
 * names a {@code tokenGroupName} and a {@code redactionPattern}. Only those classes can extend this type.
 */
public abstract class RedactionRule {

    RedactionRule() {
        // subclasses live in this package, one per SDK
    }

    /** @return the wire {@code entityName}, or null when this SDK's rule does not carry one */
    abstract String apiEntityName();

    /** @return the wire {@code redactionType}, or null when this SDK's rule does not carry one */
    abstract String apiRedactionType();

    /** @return the wire {@code tokenGroupName}, or null when this SDK's rule does not carry one */
    abstract String apiTokenGroupName();

    /** @return the wire {@code redactionPattern}, or null when this SDK's rule does not carry one */
    abstract String apiRedactionPattern();
}
