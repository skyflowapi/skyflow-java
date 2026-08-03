package com.skyflow.vault.connection;

import com.skyflow.enums.RequestMethod;

import java.util.Map;

public class InvokeConnectionRequest {

    private final InvokeConnectionRequestBuilder builder;

    private InvokeConnectionRequest(InvokeConnectionRequestBuilder builder) {
        this.builder = builder;
    }

    public static InvokeConnectionRequestBuilder builder() {
        return new InvokeConnectionRequestBuilder();
    }

    public RequestMethod getMethod() {
        return builder.method;
    }

    public Map<String, String> getPathParams() {
        return builder.pathParams;
    }

    public Map<String, String> getQueryParams() {
        return builder.queryParams;
    }

    public Map<String, String> getRequestHeaders() {
        return builder.requestHeaders;
    }

    public Object getRequestBody() {
        return builder.requestBody;
    }

    public static final class InvokeConnectionRequestBuilder {
        private RequestMethod method;
        private Map<String, String> pathParams;
        private Map<String, String> queryParams;
        private Map<String, String> requestHeaders;
        private Object requestBody;

        private InvokeConnectionRequestBuilder() {
            this.method = RequestMethod.POST;
            this.requestBody = new Object();
        }

        public InvokeConnectionRequestBuilder method(RequestMethod method) {
            this.method = method == null ? RequestMethod.POST : method;
            return this;
        }

        public InvokeConnectionRequestBuilder pathParams(Map<String, String> pathParams) {
            this.pathParams = pathParams;
            return this;
        }

        public InvokeConnectionRequestBuilder queryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public InvokeConnectionRequestBuilder requestHeaders(Map<String, String> requestHeaders) {
            this.requestHeaders = requestHeaders;
            return this;
        }

        public InvokeConnectionRequestBuilder requestBody(Object requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public InvokeConnectionRequest build() {
            return new InvokeConnectionRequest(this);
        }

    }

}
