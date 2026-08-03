package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the single-field options/builder classes: {@link InsertOptions},
 * {@link DetokenizeOptions}, {@link TokenizeOptions}, {@link DeleteTokensOptions} and the bulk
 * subclasses {@link BulkTokenizeOptions} and {@link BulkDeleteTokensOptions}.
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

    // ── BulkTokenizeOptions ──────────────────────────────────────────────────

    @Test
    public void testBulkTokenizeOptions_withInterceptor() {
        BulkTokenizeOptions options = BulkTokenizeOptions.builder().interceptor(INTERCEPTOR).build();
        Assert.assertSame(INTERCEPTOR, options.getInterceptor());
    }

    @Test
    public void testBulkTokenizeOptions_withoutInterceptor() {
        BulkTokenizeOptions options = BulkTokenizeOptions.builder().build();
        Assert.assertNull(options.getInterceptor());
    }

    @Test
    public void testBulkTokenizeOptions_isATokenizeOptions() {
        BulkTokenizeOptions options = BulkTokenizeOptions.builder().interceptor(INTERCEPTOR).build();
        Assert.assertTrue(options instanceof TokenizeOptions);
        // the inherited accessor sees the same interceptor
        Assert.assertSame(INTERCEPTOR, ((TokenizeOptions) options).getInterceptor());
    }

    @Test
    public void testBulkTokenizeOptions_builderStaysBulkTypedWhileChaining() {
        // builder() hides the parent's, so the override must return the bulk builder for
        // chaining to keep compiling without a cast
        BulkTokenizeOptions.BulkTokenizeOptionsBuilder builder =
                BulkTokenizeOptions.builder().interceptor(INTERCEPTOR);
        BulkTokenizeOptions options = builder.build();
        Assert.assertSame(INTERCEPTOR, options.getInterceptor());
    }

    // ── BulkDeleteTokensOptions ──────────────────────────────────────────────

    @Test
    public void testBulkDeleteTokensOptions_withInterceptor() {
        BulkDeleteTokensOptions options =
                BulkDeleteTokensOptions.builder().interceptor(INTERCEPTOR).build();
        Assert.assertSame(INTERCEPTOR, options.getInterceptor());
    }

    @Test
    public void testBulkDeleteTokensOptions_withoutInterceptor() {
        BulkDeleteTokensOptions options = BulkDeleteTokensOptions.builder().build();
        Assert.assertNull(options.getInterceptor());
    }

    @Test
    public void testBulkDeleteTokensOptions_isADeleteTokensOptions() {
        BulkDeleteTokensOptions options =
                BulkDeleteTokensOptions.builder().interceptor(INTERCEPTOR).build();
        Assert.assertTrue(options instanceof DeleteTokensOptions);
        Assert.assertSame(INTERCEPTOR, ((DeleteTokensOptions) options).getInterceptor());
    }

    @Test
    public void testBulkDeleteTokensOptions_builderStaysBulkTypedWhileChaining() {
        BulkDeleteTokensOptions.BulkDeleteTokensOptionsBuilder builder =
                BulkDeleteTokensOptions.builder().interceptor(INTERCEPTOR);
        BulkDeleteTokensOptions options = builder.build();
        Assert.assertSame(INTERCEPTOR, options.getInterceptor());
    }
}
