package com.skyflow.errors;

import org.json.simple.JSONObject;

public final class SkyflowException extends Exception {
    private int code;
    private JSONObject data;

    public SkyflowException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.setCode(errorCode.getCode());
    }

    public SkyflowException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDescription(), cause);
        this.setCode(errorCode.getCode());
    }

    public SkyflowException(int code, String description) {
        super(description);
        this.setCode(code);
    }

    public SkyflowException(int code, String description, Throwable cause) {
        super(description, cause);
        this.setCode(code);
    }

    public SkyflowException(int code, String description, JSONObject data) {
        super(description);
        this.setCode(code);
        setData(data);
    }

    public int getCode() {
        return code;
    }

    void setCode(int code) {
        this.code = code;
    }


    public JSONObject getData() {
        return data;
    }

    void setData(JSONObject data) {
        this.data = data;
    }
}
