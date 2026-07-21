package com.skyflow.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BaseCredentials implements Cloneable {
    private String path;
    private ArrayList<String> roles;
    private String credentialsString;
    private String token;
    private String apiKey;
    private Object context;

    public BaseCredentials() {
        this.path = null;
        this.credentialsString = null;
        this.context = null;
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
    public Object getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() throws CloneNotSupportedException {
        BaseCredentials copy = (BaseCredentials) super.clone();
        if (this.roles != null) {
            copy.roles = new ArrayList<>(this.roles);
        }
        if (this.context instanceof Map) {
            copy.context = new HashMap<>((Map<String, Object>) this.context);
        }
        return copy;
    }
}
