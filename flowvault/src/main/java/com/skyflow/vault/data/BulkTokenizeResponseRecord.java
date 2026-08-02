package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * A {@link TokenizeResponseRecord} carrying the index of the input value it belongs to. The index
 * is the one supplied on the matching {@link BulkTokenizeRequestRecord}, echoed back unchanged.
 */
public class BulkTokenizeResponseRecord extends TokenizeResponseRecord {
    @Expose(serialize = true)
    private final int index;

    public BulkTokenizeResponseRecord(int index, Object value, List<TokenizeResponseToken> tokens) {
        super(value, tokens);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
