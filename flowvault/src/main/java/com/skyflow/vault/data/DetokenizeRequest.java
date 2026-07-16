package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.List;

public class DetokenizeRequest extends BaseDetokenizeRequest{
    private final DetokenizeRequestBuilder builder;

    private DetokenizeRequest(DetokenizeRequestBuilder builder) {
        this.builder = builder;
    }

    public static DetokenizeRequestBuilder builder() {
        return new DetokenizeRequestBuilder();
    }

    public ArrayList<DetokenizeData> getDetokenizeData() {
        return this.builder.detokenizeData;
    }
    public List<TokenGroupRedactions> getTokenGroupRedactions(){
        return this.builder.tokenGroupRedactions;
    }

    public static final class DetokenizeRequestBuilder {
        private ArrayList<DetokenizeData> detokenizeData;
        private List<TokenGroupRedactions> tokenGroupRedactions;

        public DetokenizeRequestBuilder detokenizeData(ArrayList<DetokenizeData> detokenizeData) {
            this.detokenizeData = detokenizeData;
            return this;
        }
        public DetokenizeRequestBuilder tokenGroupRedactions(List<TokenGroupRedactions> tokenGroupRedactions){
            this.tokenGroupRedactions = tokenGroupRedactions;
            return this;
        }
        public DetokenizeRequest build() {
            return new DetokenizeRequest(this);
        }
    }

}
