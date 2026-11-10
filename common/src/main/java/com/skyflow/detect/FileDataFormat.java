package com.skyflow.detect;

/**
 * File format of an output returned by Detect file operations, reported as {@code processedFileExtension} on each
 * {@link FileOutput}. The wire value is the lower-case extension. Mirrors the API's
 * {@code FileOutput.processedFileExtension} enum. Request formats use {@link DeidentifyFileDataFormat} and
 * {@link ReidentifyFileDataFormat}.
 */
public enum FileDataFormat {
    BMP,
    CSV,
    DCM,
    DOC,
    DOCX,
    GIF,
    JPEG,
    JPG,
    JSON,
    JSONL,
    MP3,
    PDF,
    PNG,
    PPT,
    PPTX,
    TIF,
    TIFF,
    TXT,
    WAV,
    XLS,
    XLSX,
    XML,
    ZIP;
}
