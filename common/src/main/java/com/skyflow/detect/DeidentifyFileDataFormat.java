package com.skyflow.detect;

/**
 * File formats accepted by {@code deidentifyFile} as the request {@code dataFormat}. The wire value is the
 * lower-case extension. Mirrors the API's {@code DeidentifyFileRequestV2.dataFormat} enum.
 *
 * <p>The {@code VAULT_TOKEN} de-identification type is supported only for {@link #TXT}, {@link #CSV},
 * {@link #JSON}, {@link #JSONL}, {@link #XML}, {@link #DOCX}, {@link #XLSX} and {@link #PPTX}; these are also
 * the formats {@link ReidentifyFileDataFormat} offers for re-identification. The outputs of {@code deidentifyFile}
 * and {@code getRun} report their format with this enum on {@link FileOutput}.
 */
public enum DeidentifyFileDataFormat {
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
