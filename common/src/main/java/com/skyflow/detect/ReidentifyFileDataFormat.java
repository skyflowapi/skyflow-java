package com.skyflow.detect;

/**
 * File formats accepted by {@code reidentifyFile} as the request {@code dataFormat}: the formats that support the
 * {@code VAULT_TOKEN} de-identification type. The wire value is the lower-case extension. Mirrors the API's
 * {@code ReidentifyFileRequestV2.dataFormat} enum, which is narrower than {@link DeidentifyFileDataFormat}.
 * The outputs of {@code reidentifyFile} report their format with this enum on {@link ReidentifyFileOutput}.
 */
public enum ReidentifyFileDataFormat {
    CSV,
    DOCX,
    JSON,
    JSONL,
    PPTX,
    TXT,
    XLSX,
    XML;
}
