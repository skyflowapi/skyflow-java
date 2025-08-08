package com.skyflow.common.config;

import java.util.ArrayList;

public class Credentials {
    private String path;
    private ArrayList<String> roles;
    private String context;
    private String credentialsString;
    private String token;
    private String apiKey;

    public Credentials() {
        this.path = null;
        this.context = null;
        this.credentialsString = null;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }

    public void setRoles(ArrayList<String> roles) {
        this.roles = roles;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getCredentialsString() {
        return credentialsString;
    }

    public void setCredentialsString(String credentialsString) {
        this.credentialsString = credentialsString;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
