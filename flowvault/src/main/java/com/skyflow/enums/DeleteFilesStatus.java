package com.skyflow.enums;

/** Outcome of deleting the file in one column, as the vault reported it. */
public enum DeleteFilesStatus {
    /** The file was deleted. */
    DELETED,
    /**
     * The vault reported a status this SDK version does not recognise, or none at all. The value
     * as received is available from {@code DeleteFilesColumnResult.getRawStatus()}.
     */
    UNKNOWN;

    /**
     * The status for a wire value, matched case-insensitively.
     *
     * @param value the status as the vault sent it; may be null
     * @return the matching status, or {@link #UNKNOWN} when there is none
     */
    public static DeleteFilesStatus fromWire(String value) {
        if (value != null) {
            for (DeleteFilesStatus status : values()) {
                if (status != UNKNOWN && status.name().equalsIgnoreCase(value.trim())) {
                    return status;
                }
            }
        }
        return UNKNOWN;
    }
}
