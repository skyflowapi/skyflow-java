package com.skyflow.vault.data;

import java.io.File;

public class FileUploadRequest {

    private final FileUploadRequest.FileUploadRequestBuilder builder;

    private FileUploadRequest(FileUploadRequest.FileUploadRequestBuilder builder) {
        this.builder = builder;
    }

    public static FileUploadRequest.FileUploadRequestBuilder builder() {
        return new FileUploadRequest.FileUploadRequestBuilder();
    }

    public String getTable() {
        return this.builder.table;
    }

    public String getSkyflowId() {
        return this.builder.skyflowId;
    }

    public String getColumnName() {
        return this.builder.columnName;
    }

    public String getFilePath() {
        return this.builder.filePath;
    }

    public String getBase64() {
        return this.builder.base64;
    }

    public File getFileObject() {
        return this.builder.fileObject;
    }

    public String getFileName() {
        return this.builder.fileName;
    }

    public static final class FileUploadRequestBuilder {

        private String table;
        private String skyflowId;
        private String columnName;
        private String filePath;
        private String base64;
        private File fileObject;
        private String fileName;

        private FileUploadRequestBuilder() {
        }

        public FileUploadRequest.FileUploadRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public FileUploadRequest.FileUploadRequestBuilder skyflowId(String skyflowId) {
            this.skyflowId = skyflowId;
            return this;
        }

        public FileUploadRequest.FileUploadRequestBuilder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public FileUploadRequest.FileUploadRequestBuilder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public FileUploadRequest.FileUploadRequestBuilder base64(String base64) {
            this.base64 = base64;
            return this;
        }

        public FileUploadRequest.FileUploadRequestBuilder fileObject(File fileObject) {
            this.fileObject = fileObject;
            return this;
        }

        public FileUploadRequest.FileUploadRequestBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public FileUploadRequest build() {
            return new FileUploadRequest(this);
        }
    }
}
