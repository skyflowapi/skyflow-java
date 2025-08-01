package com.skyflow.vault.detect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.List;

public class DeidentifyFileResponse {
    private final FileInfo file;
    private final String fileBase64;
    private final String type;
    private final String extension;
    private final Integer wordCount;
    private final Integer charCount;
    private final Double sizeInKb;
    private final Double durationInSeconds;
    private final Integer pageCount;
    private final Integer slideCount;
    private final List<FileEntityInfo> entities;
    private final String runId;
    private final String status;
    private final List<String> errors;


    public DeidentifyFileResponse(FileInfo file, String fileBase64, String type, String extension,
                                  Integer wordCount, Integer charCount, Double sizeInKb,
                                  Double durationInSeconds, Integer pageCount, Integer slideCount,
                                  List<FileEntityInfo> entities, String runId, String status, List<String> errors) {
        this.file = file;
        this.fileBase64 = fileBase64;
        this.type = type;
        this.extension = extension;
        this.wordCount = wordCount;
        this.charCount = charCount;
        this.sizeInKb = sizeInKb;
        this.durationInSeconds = durationInSeconds;
        this.pageCount = pageCount;
        this.slideCount = slideCount;
        this.entities = entities;
        this.runId = runId;
        this.status = status;
        this.errors = errors;
    }

    public DeidentifyFileResponse(String runId, String status) {
        this(null, null, null, null, null, null, null, null, null, null, null, runId, status, null);
    }

    public FileInfo getFile() {
        return file;
    }

    public String getFileBase64(){
        return fileBase64;
    }

    public String getType() {
        return type;
    }

    public String getExtension() {
        return extension;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public Integer getCharCount() {
        return charCount;
    }

    public Double getSizeInKb() {
        return sizeInKb;
    }

    public Double getDurationInSeconds() {
        return durationInSeconds;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public Integer getSlideCount() {
        return slideCount;
    }

    public List<FileEntityInfo> getEntities() {
        return entities;
    }

    public String getRunId() {
        return runId;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }

}
