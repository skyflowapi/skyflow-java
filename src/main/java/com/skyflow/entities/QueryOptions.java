/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.skyflow.entities;

public class QueryOptions {

    private boolean tokens;

    public QueryOptions() {
        this.tokens = true;
    }

    public QueryOptions(boolean tokens) {
        this.tokens = tokens;
    }

    public boolean isTokens() {
        return tokens;
    }
}
