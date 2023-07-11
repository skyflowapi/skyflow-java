/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * This is the description for RedactionType Enum.
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
     * This is the description for toString method.
     * @return This is the description of what the method returns.
     */
    @Override
    public String toString() {
        return text;
    }

}
