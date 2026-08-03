package com.skyflow.vault.detect;

import java.io.File;

public class FileInfo {
    private String name;
    private long size;
    private String type;
    private long lastModified;

    public FileInfo(File file) {
        this.name = file.getName();
        this.size = file.length();
        this.type = "";
        this.lastModified = file.lastModified();
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public long getLastModified() {
        return lastModified;
    }
}
