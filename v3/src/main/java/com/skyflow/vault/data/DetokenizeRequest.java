package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.List;

public class DetokenizeRequest {
    private final DetokenizeRequestBuilder builder;
    private DetokenizeRequest(DetokenizeRequestBuilder builder){
        this.builder = builder;
    }

    public static DetokenizeRequestBuilder builder(){
        return new DetokenizeRequestBuilder();
    }
    public List<String> getTokens(){
        return this.builder.tokens;
    }
    public List<TokenGroupRedactions> getTokenGroupRedactions(){
        return this.builder.tokenGroupRedactions;
    }

    public static final class DetokenizeRequestBuilder{
        private List<String> tokens;

        private List<TokenGroupRedactions> tokenGroupRedactions;

        public DetokenizeRequestBuilder tokens(List<String> tokens){
            this.tokens = tokens;
            return this;
        }
        public DetokenizeRequestBuilder tokenGroupRedactions(List<TokenGroupRedactions> tokenGroupRedactions){
            this.tokenGroupRedactions = tokenGroupRedactions;
            return this;
        }
        public DetokenizeRequest build(){
            return new DetokenizeRequest(this);
        }


    }

}
