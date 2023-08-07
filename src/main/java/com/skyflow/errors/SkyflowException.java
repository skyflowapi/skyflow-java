/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.errors;

import org.json.simple.JSONObject;

/**
 * Exceptions thrown by the Skyflow SDK.
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
     * @param code Error code for the exception.
     * @param description The description of the exception.
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
     *  Gets the code.
     * @return Returns the code.
     */
    public int getCode() {
        return code;
    }

    /**
     *  Sets the code.
     * @return Type of the code.
     */
    void setCode(int code) {
        this.code = code;
    }


    /**
     * Gets the data.
     * @return Returns the data.
     */
    public JSONObject getData() {
        return data;
    }

    /**
     * Sets the data.
     * @param data Type of the data.
     */
    void setData(JSONObject data) {
        this.data = data;
    }
}
