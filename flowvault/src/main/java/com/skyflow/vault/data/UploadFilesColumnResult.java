package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyflow.enums.UploadStatus;

/** The upload outcome of one file column. */
public class UploadFilesColumnResult {
    private final String column;
    private final String fileName;
    private final UploadStatus uploadStatus;
    private final String error;

    public UploadFilesColumnResult(String column, String fileName, UploadStatus uploadStatus, String error) {
        this.column = column;
        this.fileName = fileName;
        this.uploadStatus = uploadStatus;
        this.error = error;
    }

    public String getColumn() {
        return column;
    }

    public String getFileName() {
        return fileName;
    }

    public UploadStatus getUploadStatus() {
        return uploadStatus;
    }

    /** Why the upload failed or was skipped; null when uploaded. */
    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
