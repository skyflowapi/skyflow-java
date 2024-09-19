package com.skyflow.vault.controller.vault;

import com.skyflow.config.VaultConfig;

public class VaultController {
    // members
    private VaultConfig vaultConfig;

    public VaultController(VaultConfig vaultConfig) {
        this.vaultConfig = vaultConfig;
    }

    public void setVaultConfig(VaultConfig vaultConfig) {
        this.vaultConfig = vaultConfig;
    }

    public VaultConfig getVaultConfig() {
        return vaultConfig;
    }

    public T insert() {
        return T;
    }

    public T detokenize() {
        return T;
    }

    public T get() {
        return T;
    }

    public T update() {
        return T;
    }

    public T delete() {
        return T;
    }

    public T uploadFile() {
        return T;

    }

    public T query() {
        return T;
    }

    public T tokenize() {
        return T;
    }

    public T lookUpBin() {
        // cache bin lookup object if created
        // return bin lookup object using static func
        return T;
    }

    public T audit() {
        // cache audit object if created
        // return audit object using static func
        return T;
    }

    public T detect() {
        // cache detect object if created
        // return detect object using static func
        return T;
    }
}
