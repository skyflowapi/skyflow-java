package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeleteTokensResponse {
    private final List<String> tokens;
    private final ArrayList<HashMap<String, Object>> errors;

    public DeleteTokensResponse(List<String> tokens, ArrayList<HashMap<String, Object>> errors) {
        this.tokens = tokens;
        this.errors = errors;
    }

    public List<String> getTokens() {
        return tokens;
    }
    public ArrayList<HashMap<String, Object>> getErrors(){
        return this.errors;
    }
}
