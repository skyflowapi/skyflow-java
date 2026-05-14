package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class OptionsTests {

    // ── InsertOptions ─────────────────────────────────────────────────────────

    // ── DetokenizeOptions ─────────────────────────────────────────────────────

    // ── TokenizeOptions ───────────────────────────────────────────────────────

    // ── DeleteTokensOptions ───────────────────────────────────────────────────

    // ── Interceptor getters ───────────────────────────────────────────────────

    @Test
    public void insertOptions_defaultInterceptor_isNull() {
        InsertOptions opts = InsertOptions.builder().build();
        Assert.assertNull(opts.getInterceptor());
    }

    @Test
    public void insertOptions_interceptor_storedAndRetrieved() {
        RequestInterceptor interceptor = ctx -> {};
        InsertOptions opts = InsertOptions.builder().interceptor(interceptor).build();
        Assert.assertSame(interceptor, opts.getInterceptor());
    }

    @Test
    public void detokenizeOptions_defaultInterceptor_isNull() {
        DetokenizeOptions opts = DetokenizeOptions.builder().build();
        Assert.assertNull(opts.getInterceptor());
    }

    @Test
    public void detokenizeOptions_interceptor_storedAndRetrieved() {
        RequestInterceptor interceptor = ctx -> {};
        DetokenizeOptions opts = DetokenizeOptions.builder().interceptor(interceptor).build();
        Assert.assertSame(interceptor, opts.getInterceptor());
    }

    @Test
    public void tokenizeOptions_defaultInterceptor_isNull() {
        TokenizeOptions opts = TokenizeOptions.builder().build();
        Assert.assertNull(opts.getInterceptor());
    }

    @Test
    public void tokenizeOptions_interceptor_storedAndRetrieved() {
        RequestInterceptor interceptor = ctx -> {};
        TokenizeOptions opts = TokenizeOptions.builder().interceptor(interceptor).build();
        Assert.assertSame(interceptor, opts.getInterceptor());
    }

    @Test
    public void deleteTokensOptions_defaultInterceptor_isNull() {
        DeleteTokensOptions opts = DeleteTokensOptions.builder().build();
        Assert.assertNull(opts.getInterceptor());
    }

    @Test
    public void deleteTokensOptions_interceptor_storedAndRetrieved() {
        RequestInterceptor interceptor = ctx -> {};
        DeleteTokensOptions opts = DeleteTokensOptions.builder().interceptor(interceptor).build();
        Assert.assertSame(interceptor, opts.getInterceptor());
    }
}
