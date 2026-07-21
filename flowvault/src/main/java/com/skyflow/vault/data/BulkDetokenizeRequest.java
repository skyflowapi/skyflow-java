package com.skyflow.vault.data;

import java.util.List;

public class BulkDetokenizeRequest {
    private final BulkDetokenizeRequestBuilder builder;
    private BulkDetokenizeRequest(BulkDetokenizeRequestBuilder builder){
        this.builder = builder;
    }

    public static BulkDetokenizeRequestBuilder builder(){
        return new BulkDetokenizeRequestBuilder();
    }
    public List<String> getTokens(){
        return this.builder.tokens;
    }
    public List<BulkTokenGroupRedactions> getTokenGroupRedactions(){
        return this.builder.tokenGroupRedactions;
    }

    public static final class BulkDetokenizeRequestBuilder{
        private List<String> tokens;

        private List<BulkTokenGroupRedactions> tokenGroupRedactions;

        public BulkDetokenizeRequestBuilder tokens(List<String> tokens){
            this.tokens = tokens;
            return this;
        }
        public BulkDetokenizeRequestBuilder tokenGroupRedactions(List<BulkTokenGroupRedactions> tokenGroupRedactions){
            this.tokenGroupRedactions = tokenGroupRedactions;
            return this;
        }
        public BulkDetokenizeRequest build(){
            return new BulkDetokenizeRequest(this);
        }


    }

}
