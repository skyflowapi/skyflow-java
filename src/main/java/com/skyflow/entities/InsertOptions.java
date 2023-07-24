/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * Contains the additional parameters for the insert method.
 */
public class InsertOptions {

    private boolean tokens;
    private UpsertOption[] upsertOptions;

    /**
     * @ignore
     */
    public InsertOptions() {
        this.tokens = true;
        this.upsertOptions = null;
    }

    /**
     * @ignore
     */
    public InsertOptions(boolean tokens) {
        this.tokens = tokens;
        this.upsertOptions = null;
    }

    /**
     * @ignore
     */
    public InsertOptions(UpsertOption[] upsertOptions) {
        this.tokens = true;
        this.upsertOptions = upsertOptions;
    }

    /**
     * @param tokens Indicates whether to return tokens for the collected data.
     * @param upsertOptions To support upsert operations while collecting data from Skyflow Elements, you should pass the table and column marked as unique in the table.
     */
    public InsertOptions(boolean tokens, UpsertOption[] upsertOptions) {
        this.tokens = tokens;
        this.upsertOptions = upsertOptions;
    }

    /**
     * Checks whether tokens are to be returned or not.
     * @return Returns the value of tokens option.
     */
    public boolean isTokens() {
        return tokens;
    }

    /**
     * Supports upsert operations while collecting data from Skyflow elements, when we pass the table and column marked as unique in the table.
     * @return Returns the value of upsert options.
     */
    public UpsertOption[] getUpsertOptions() {
        return upsertOptions;
    }
}
