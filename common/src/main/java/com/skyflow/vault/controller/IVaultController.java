package com.skyflow.vault.controller;

import com.skyflow.errors.SkyflowException;

// Common interface — ONLY operations supported on both vault types.
public interface IVaultController<InsertReq, InsertResp, DetokenizeReq, DetokenizeResp> {
    InsertResp insert(InsertReq request) throws SkyflowException;
    DetokenizeResp detokenize(DetokenizeReq request) throws SkyflowException;
}