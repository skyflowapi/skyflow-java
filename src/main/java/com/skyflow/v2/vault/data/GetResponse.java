package com.skyflow.v2.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class GetResponse {
    private final ArrayList<HashMap<String, Object>> data;
    private final ArrayList<HashMap<String, Object>> errors;

    public GetResponse(ArrayList<HashMap<String, Object>> data, ArrayList<HashMap<String, Object>> errors) {
        this.data = data;
        this.errors = errors;
    }

    public ArrayList<HashMap<String, Object>> getData() {
        return data;
    }

    public ArrayList<HashMap<String, Object>> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
