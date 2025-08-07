package com.skyflow.v2.vault.detect;

import java.io.File;

public class FileInput {
    private final FileInputBuilder builder;

    private FileInput(FileInputBuilder builder) {
        this.builder = builder;
    }

    public static FileInputBuilder builder() {
        return new FileInputBuilder();
    }

    public File getFile() {
        return this.builder.file;
    }

    public String getFilePath() {
        return this.builder.filePath;
    }

    public static final class FileInputBuilder {
        private File file;
        private String filePath;

        private FileInputBuilder() {
            // Default constructor
        }

        public FileInputBuilder file(File file) {
            this.file = file;
            return this;
        }

        public FileInputBuilder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public FileInput build() {
            return new FileInput(this);
        }
    }
}