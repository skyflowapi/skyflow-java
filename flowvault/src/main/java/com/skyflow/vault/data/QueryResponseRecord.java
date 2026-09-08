package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

// A single row returned by a query, as the free-form column/value map the vault sends back.
// The query API has no notion of tokens, so unlike insert/detokenize there is no typed Token data here.
public class QueryResponseRecord {
    private final Map<String, Object> data;

    public QueryResponseRecord(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
