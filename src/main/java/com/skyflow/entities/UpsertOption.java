/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.skyflow.entities;

/**
 * This is the description for UpsertOption Class.
 */
public class UpsertOption {
    private String table;
    private String column;

    /**
     * @param table This is the description of the table parameter.
     * @param column This is the description of the column parameter.
     */
    public UpsertOption(String table, String column) {
        this.table = table;
        this.column = column;
    }

    /**
     * This is the description for getTable method.
     * @return This is the description of what the method returns.
     */
    public String getTable() {
        return table;
    }

    /**
     * This is the description for getColumn method.
     * @return This is the description of what the method returns.
     */
    public String getColumn() {
        return column;
    }
}
