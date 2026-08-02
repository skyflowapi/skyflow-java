package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertResponse extends BaseInsertResponse {
    public InsertResponse(ArrayList<HashMap<String, Object>> insertedFields, ArrayList<HashMap<String, Object>> errors) {
        super(insertedFields, errors);
    }
}
