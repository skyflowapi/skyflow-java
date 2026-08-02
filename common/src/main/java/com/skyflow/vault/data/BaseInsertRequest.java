package com.skyflow.vault.data;

// Shared extension point for module-specific insert requests. Intentionally empty:
// v2 and flowvault insert requests no longer have any field in common, so each owns
// its own state. Retained so the modules keep a shared supertype for future use.
public class BaseInsertRequest {
}
