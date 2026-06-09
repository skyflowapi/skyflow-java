package com.skyflow.utils.probe;

/**
 * Options controlling how an insert request is processed.
 */
public class ProbeInsertOptions {

    private boolean tokenize;
    private boolean upsert;
    private int batchSize = 25;
    private boolean continueOnError;

    public boolean isTokenize() {
        return tokenize;
    }

    public void setTokenize(boolean tokenize) {
        this.tokenize = tokenize;
    }

    public boolean isUpsert() {
        return upsert;
    }

    public void setUpsert(boolean upsert) {
        this.upsert = upsert;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(boolean continueOnError) {
        this.continueOnError = continueOnError;
    }
}
