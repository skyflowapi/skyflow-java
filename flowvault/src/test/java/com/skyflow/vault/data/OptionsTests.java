package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the single-field options/builder classes: {@link InsertOptions},
 * {@link DetokenizeOptions}, {@link TokenizeOptions} and {@link DeleteTokensOptions},
 * plus the bulk specialisations {@link BulkInsertOptions} and {@link BulkDetokenizeOptions}.
 * Each class simply wraps a {@link RequestInterceptor} with no validation.
 */
public class OptionsTests {

    private static final RequestInterceptor INTERCEPTOR = context -> {
    };

    // ── InsertOptions ────────────────────────────────────────────────────────

    @Test
    public void testInsertOptions_withInterceptor() {
        InsertOptions options = InsertOptions.builder().interceptor(INTERCEPTOR).build();
        Assert.assertSame(INTERCEPTOR, options.getInterceptor());
    }

    @Test
    public void testInsertOptions_withoutInterceptor() {
        InsertOptions options = InsertOptions.builder().build();
        Assert.assertNull(options.getInterceptor());
    }

    // ── DetokenizeOptions ────────────────────────────────────────────────────

    @Test
    public void testDetokenizeOptions_withInterceptor() {
        DetokenizeOptions options = DetokenizeOptions.builder().interceptor(INTERCEPTOR).build();
        Assert.assertSame(INTERCEPTOR, options.getInterceptor());
    }

    @Test
    public void testDetokenizeOptions_withoutInterceptor() {
        DetokenizeOptions options = DetokenizeOptions.builder().build();
        Assert.assertNull(options.getInterceptor());
    }

    // ── BulkInsertOptions ────────────────────────────────────────────────────

    @Test
    public void testBulkInsertOptions_withInterceptor() {
        BulkInsertOptions options = BulkInsertOptions.builder().interceptor(INTERCEPTOR).build();
        Assert.assertSame(INTERCEPTOR, options.getInterceptor());
        // BulkInsertOptions is a specialisation of InsertOptions, so it flows anywhere the base does
        Assert.assertTrue(options instanceof InsertOptions);
    }

    @Test
    public void testBulkInsertOptions_withoutInterceptor() {
        BulkInsertOptions options = BulkInsertOptions.builder().build();
        Assert.assertNull(options.getInterceptor());
    }

    // ── BulkDetokenizeOptions ────────────────────────────────────────────────

    @Test
    public void testBulkDetokenizeOptions_withInterceptor() {
        BulkDetokenizeOptions options = BulkDetokenizeOptions.builder().interceptor(INTERCEPTOR).build();
        Assert.assertSame(INTERCEPTOR, options.getInterceptor());
        Assert.assertTrue(options instanceof DetokenizeOptions);
    }

    @Test
    public void testBulkDetokenizeOptions_withoutInterceptor() {
        BulkDetokenizeOptions options = BulkDetokenizeOptions.builder().build();
        Assert.assertNull(options.getInterceptor());
    }

    // ── TokenizeOptions ──────────────────────────────────────────────────────

    @Test
    public void testTokenizeOptions_withInterceptor() {
        TokenizeOptions options = TokenizeOptions.builder().interceptor(INTERCEPTOR).build();
        Assert.assertSame(INTERCEPTOR, options.getInterceptor());
    }

    @Test
    public void testTokenizeOptions_withoutInterceptor() {
        TokenizeOptions options = TokenizeOptions.builder().build();
        Assert.assertNull(options.getInterceptor());
    }

    // ── DeleteTokensOptions ──────────────────────────────────────────────────

    @Test
    public void testDeleteTokensOptions_withInterceptor() {
        DeleteTokensOptions options = DeleteTokensOptions.builder().interceptor(INTERCEPTOR).build();
        Assert.assertSame(INTERCEPTOR, options.getInterceptor());
    }

    @Test
    public void testDeleteTokensOptions_withoutInterceptor() {
        DeleteTokensOptions options = DeleteTokensOptions.builder().build();
        Assert.assertNull(options.getInterceptor());
    }
}
