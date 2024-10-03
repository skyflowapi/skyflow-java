package com.skyflow.config;

import java.util.ArrayList;

public class Credentials {
    private String path;
    private ArrayList<String> roles;
    private String context;
    private String credentialsString;
    private String token;

    public Credentials() {
        this.path = null;
        this.roles = new ArrayList<>();
        this.context = null;
        this.credentialsString = null;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setRoles(ArrayList<String> roles) {
        this.roles = roles;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setCredentialsString(String credentialsString) {
        this.credentialsString = credentialsString;
    }
}
