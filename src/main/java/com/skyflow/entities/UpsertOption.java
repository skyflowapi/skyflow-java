/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.skyflow.entities;

/**
 * Contains the parameters for the upsert operation.
 */
public class UpsertOption {
    private String table;
    private String column;

    /**
     * @param table Data belongs to the table.
     * @param column Name of the unique column.
     */
    public UpsertOption(String table, String column) {
        this.table = table;
        this.column = column;
    }

    /**
     * Gets the table
     * @return Returns the table.
     */
    public String getTable() {
        return table;
    }

    /**
     * Gets the column
     * @return Returns the column.
     */
    public String getColumn() {
        return column;
    }
}
