package com.skyflow.enums;

/** Outcome of uploading one file column. */
public enum UploadStatus {
    /** The file was uploaded to its signed URL. */
    UPLOADED,
    /** The upload to the signed URL failed; see the column's error. */
    FAILED,
    /** No upload was attempted, because the record failed or no signed URL was returned. */
    SKIPPED
}
