package com.skyflow.vault.detect;

import com.skyflow.generated.rest.types.DeidentifiedFileOutputProcessedFileExtension;
import com.skyflow.generated.rest.types.DeidentifiedFileOutputProcessedFileType;

public class FileEntityInfo {
    private final String file;
    private final String type;
    private final String extension;

    public FileEntityInfo(String file, DeidentifiedFileOutputProcessedFileType type, DeidentifiedFileOutputProcessedFileExtension extension) {
        this.file = file;
        this.type = String.valueOf(type);
        this.extension = String.valueOf(extension);
    }

    public String getFile() { return file; }
    public String getType() { return type; }
    public String getExtension() { return extension; }
}