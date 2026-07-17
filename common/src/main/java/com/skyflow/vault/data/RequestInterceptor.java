package com.skyflow.vault.data;

@FunctionalInterface
public interface RequestInterceptor {
    void intercept(RequestContext context);
}
