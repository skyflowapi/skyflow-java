package com.skyflow.errors;

import com.skyflow.errors.ErrorCodesEnum;

public class SkyflowException extends Exception {
    private com.skyflow.errors.ErrorCodesEnum code;

    public SkyflowException(com.skyflow.errors.ErrorCodesEnum code, String message) {
        super(message);
        this.setCode(code);
    }

    public SkyflowException(com.skyflow.errors.ErrorCodesEnum code, String message, Throwable cause) {
        super(message, cause);
        this.setCode(code);
    }

    public com.skyflow.errors.ErrorCodesEnum getCode() {
        return code;
    }

     void setCode(ErrorCodesEnum code) {
        this.code = code;
    }
}
