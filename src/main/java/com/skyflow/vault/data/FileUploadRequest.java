package com.skyflow.vault.data;

import java.io.File;

public class FileUploadRequest {
  private final FileUploadRequestBuilder builder;

  private FileUploadRequest(FileUploadRequestBuilder builder) {
    this.builder = builder;
  }

  public static FileUploadRequestBuilder builder() {
    return new FileUploadRequestBuilder();
  }

  public String getTableName() {
    return this.builder.tableName;
  }

  public String getColumnName() {
    return this.builder.columnName;
  }

  public String getSkyflowId() {
    return this.builder.skyflowId;
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
    private String tableName;
    private String columnName;
    private String skyflowId;
    private String filePath;
    private String base64;
    private File fileObject;
    private String fileName;

    private FileUploadRequestBuilder() {
    }

    public FileUploadRequestBuilder tableName(String tableName) {
      this.tableName = tableName;
      return this;
    }

    public FileUploadRequestBuilder columnName(String columnName) {
      this.columnName = columnName;
      return this;
    }

    public FileUploadRequestBuilder skyflowId(String skyflowId) {
      this.skyflowId = skyflowId;
      return this;
    }

    public FileUploadRequestBuilder filePath(String filePath) {
      this.filePath = filePath;
      return this;
    }

    public FileUploadRequestBuilder base64(String base64) {
      this.base64 = base64;
      return this;
    }

    public FileUploadRequestBuilder fileObject(File fileObject) {
      this.fileObject = fileObject;
      return this;
    }

    public FileUploadRequestBuilder fileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public FileUploadRequest build() {
      return new FileUploadRequest(this);
    }
  }
}
