package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TokenizeResponse {
    private List<TokenizeData> tokenizedData;
    private final ArrayList<HashMap<String, Object>> errors;

    public TokenizeResponse(ArrayList<HashMap<String, Object>> errors) {
        this.errors = errors;
    }
    public TokenizeResponse(){
        this.errors = new ArrayList<>();
    }
    public List<TokenizeData> getTokenizedData(){
        return this.tokenizedData;
    }
    public void setTokenizedData(List<TokenizeData> tokenizedData) {
        this.tokenizedData = tokenizedData;
    }
    public ArrayList<HashMap<String, Object>> getErrors(){
        return this.errors;
    }
}
