package com.skyflow.vault.detect;

/**
 * Whether the entity label is included as a prefix in a generated vault token. Only applies when the
 * deidentification type is VAULT_TOKEN. INCLUDE_PREFIX_UNSPECIFIED leaves the field unset so the server
 * applies its default, which is YES. NO returns the raw vault token, which cannot be re-identified later.
 */
public enum IncludePrefix {
    INCLUDE_PREFIX_UNSPECIFIED,
    YES,
    NO;
}
