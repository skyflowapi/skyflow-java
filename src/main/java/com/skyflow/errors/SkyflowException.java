/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.errors;

import org.json.simple.JSONObject;

/**
 * This is the description for SkyflowException Class.
 */
public final class SkyflowException extends Exception {
    private int code;
    private JSONObject data;

    /**
     * @ignore
     */
    public SkyflowException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.setCode(errorCode.getCode());
    }

    /**
     * @ignore
     */
    public SkyflowException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDescription(), cause);
        this.setCode(errorCode.getCode());
    }

    /**
     * @param code This is the description of the code parameter.
     * @param description This is the description of the description parameter.
     * @ignore
     */
    public SkyflowException(int code, String description) {
        super(description);
        this.setCode(code);
    }

    /**
     * @ignore
     */
    public SkyflowException(int code, String description, Throwable cause) {
        super(description, cause);
        this.setCode(code);
    }

    /**
     * @ignore
     */
    public SkyflowException(int code, String description, JSONObject data) {
        super(description);
        this.setCode(code);
        setData(data);
    }

    /**
     * This is the description for getCode method.
     * @return This is the description of what the method returns.
     */
    public int getCode() {
        return code;
    }

    /**
     * This is the description for setCode method.
     * @param code This is the description of the code parameter.
     */
    void setCode(int code) {
        this.code = code;
    }


    /**
     * This is the description for getData method.
     * @return This is the description of what the method returns.
     */
    public JSONObject getData() {
        return data;
    }

    /**
     * This is the description for setData method.
     * @param data This is the description of the data parameter.
     */
    void setData(JSONObject data) {
        this.data = data;
    }
}
