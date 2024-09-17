package com.skyflow.entities;

public class GetOptions {
    private boolean tokens;

    public GetOptions(){
        this.tokens = false;
    }
    public GetOptions(boolean tokens){
        this.tokens = tokens;
    }

    public boolean getOptionToken(){
        return tokens;
    }
}
