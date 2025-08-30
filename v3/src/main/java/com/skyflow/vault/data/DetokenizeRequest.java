package com.skyflow.vault.data;

import java.util.ArrayList;

public class DetokenizeRequest {
    private final DetokenizeRequestBuilder builder;
    private DetokenizeRequest(DetokenizeRequestBuilder builder){
        this.builder = builder;
    }

    public static DetokenizeRequestBuilder builder(){
        return new DetokenizeRequestBuilder();
    }
    public ArrayList<String> getTokens(){
        return this.builder.tokens;
    }
    public ArrayList<TokenGroupRedactions> getTokenGroupRedactions(){
        return this.builder.tokenGroupRedactions;
    }

    public static final class DetokenizeRequestBuilder{
        private ArrayList<String> tokens;

        private ArrayList<TokenGroupRedactions> tokenGroupRedactions;

        public DetokenizeRequestBuilder tokens(ArrayList<String> tokens){
            this.tokens = tokens;
            return this;
        }
        public DetokenizeRequestBuilder tokenGroupRedactions(ArrayList<TokenGroupRedactions> tokenGroupRedactions){
            this.tokenGroupRedactions = tokenGroupRedactions;
            return this;
        }
        public DetokenizeRequest build(){
            return new DetokenizeRequest(this);
        }


    }

}
