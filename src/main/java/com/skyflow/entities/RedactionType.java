/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * Supported redaction types.
 */
public enum RedactionType {
    DEFAULT("DEFAULT"),
    PLAIN_TEXT("PLAIN_TEXT"),
    MASKED("MASKED"),
    REDACTED("REDACTED"),
    ;

    private final String text;

    RedactionType(final String text) {
        this.text = text;
    }

    /**
     * Fetchs the set redaction type.
     * @return Returns the redaction type.
     */
    @Override
    public String toString() {
        return text;
    }

}
