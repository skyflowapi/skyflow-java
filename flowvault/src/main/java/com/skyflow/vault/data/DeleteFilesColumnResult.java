package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyflow.enums.DeleteFilesStatus;

/** The delete outcome of one file column. */
public class DeleteFilesColumnResult {
    private final String column;
    private final DeleteFilesStatus status;
    private final String rawStatus;

    public DeleteFilesColumnResult(String column, String rawStatus) {
        this.column = column;
        this.status = DeleteFilesStatus.fromWire(rawStatus);
        this.rawStatus = rawStatus;
    }

    public String getColumn() {
        return column;
    }

    /** The delete outcome; {@link DeleteFilesStatus#UNKNOWN} for a status this SDK does not recognize. */
    public DeleteFilesStatus getStatus() {
        return status;
    }

    /** The status exactly as the vault sent it, e.g. {@code "DELETED"}, or null when it sent none. */
    public String getRawStatus() {
        return rawStatus;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
