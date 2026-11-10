package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** The delete outcome of one file column. */
public class DeleteFilesColumnResult {
    private final String column;
    private final String status;

    public DeleteFilesColumnResult(String column, String status) {
        this.column = column;
        this.status = status;
    }

    public String getColumn() {
        return column;
    }

    /** The status the vault reported, e.g. {@code "DELETED"}. */
    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
