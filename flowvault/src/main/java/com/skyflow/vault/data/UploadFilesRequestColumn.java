package com.skyflow.vault.data;

import java.io.File;

/**
 * One file column to upload. Provide exactly one of {@code filePath}, {@code base64} or
 * {@code fileObject}; a column that sets none, or more than one, fails validation.
 */
public class UploadFilesRequestColumn {
    private final String column;
    private final String filePath;
    private final String base64;
    private final File fileObject;
    private final String fileName;
    private final String contentType;

    protected UploadFilesRequestColumn(UploadFilesRequestColumnBuilder builder) {
        this.column = builder.column;
        this.filePath = builder.filePath;
        this.base64 = builder.base64;
        this.fileObject = builder.fileObject;
        this.fileName = builder.fileName;
        this.contentType = builder.contentType;
    }

    public static UploadFilesRequestColumnBuilder builder() {
        return new UploadFilesRequestColumnBuilder();
    }

    public String getColumn() {
        return this.column;
    }

    /** Local path of the file; it must exist. */
    public String getFilePath() {
        return this.filePath;
    }

    /** Base64-encoded file content; requires {@code fileName}. */
    public String getBase64() {
        return this.base64;
    }

    public File getFileObject() {
        return this.fileObject;
    }

    /**
     * Name to store the file under. Derived from {@code filePath} or {@code fileObject} when
     * omitted; if there is still none, the server generates a random 16-byte UUID name.
     */
    public String getFileName() {
        return this.fileName;
    }

    /** MIME type sent with the file. Inferred from the file name when omitted. */
    public String getContentType() {
        return this.contentType;
    }

    public static class UploadFilesRequestColumnBuilder {
        protected String column;
        protected String filePath;
        protected String base64;
        protected File fileObject;
        protected String fileName;
        protected String contentType;

        protected UploadFilesRequestColumnBuilder() {}

        public UploadFilesRequestColumnBuilder column(String column) {
            this.column = column;
            return this;
        }

        public UploadFilesRequestColumnBuilder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public UploadFilesRequestColumnBuilder base64(String base64) {
            this.base64 = base64;
            return this;
        }

        public UploadFilesRequestColumnBuilder fileObject(File fileObject) {
            this.fileObject = fileObject;
            return this;
        }

        public UploadFilesRequestColumnBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public UploadFilesRequestColumnBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public UploadFilesRequestColumn build() {
            return new UploadFilesRequestColumn(this);
        }
    }
}
