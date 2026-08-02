package com.skyflow.vault.data;

// Shared extension point for module-specific detokenize request data. Intentionally empty:
// v2 and flowvault no longer have any field in common here, so each owns its own state.
// Retained so the modules keep a shared supertype for future use.
public class BaseDetokenizeData {
}
