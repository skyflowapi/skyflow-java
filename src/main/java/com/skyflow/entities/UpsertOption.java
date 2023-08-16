/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.skyflow.entities;

/**
 * Parameters for upsert.
 */
public class UpsertOption {
    private String table;
    private String column;

    /**
     * @param table Table that the data belongs to.
     * @param column Column that the data belongs to.
     */
    public UpsertOption(String table, String column) {
        this.table = table;
        this.column = column;
    }

    /**
     * Gets the table.
     * @return Returns the table.
     */
    public String getTable() {
        return table;
    }

    /**
     * Gets the column.
     * @return Returns the column.
     */
    public String getColumn() {
        return column;
    }
}
