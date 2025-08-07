package com.skyflow.v2.vault.detect;

import com.skyflow.v2.generated.rest.types.DeidentifyFileOutputProcessedFileType;

public class FileEntityInfo {
    private final String file;
    private final String type;
    private final String extension;

    public FileEntityInfo(String file, DeidentifyFileOutputProcessedFileType type, String extension) {
        this.file = file;
        this.type = String.valueOf(type);
        this.extension = extension;
    }

    public String getFile() { return file; }
    public String getType() { return type; }
    public String getExtension() { return extension; }
}