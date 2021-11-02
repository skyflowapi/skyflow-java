package com.skyflow.entities;

public class DetokenizeRecord {
    private String token;
    DetokenizeRecord(){

    }
    public DetokenizeRecord(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    void setToken(String token) {
        this.token = token;
    }
}
