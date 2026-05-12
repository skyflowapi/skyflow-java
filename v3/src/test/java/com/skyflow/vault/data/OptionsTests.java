package com.skyflow.vault.data;

import com.skyflow.enums.CustomHeaderKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class OptionsTests {

    // ── InsertOptions ─────────────────────────────────────────────────────────

    @Test
    public void insertOptions_emptyBuilder_returnsEmptyHeaders() {
        InsertOptions opts = InsertOptions.builder().build();
        Assert.assertNotNull(opts.getCustomHeaders());
        Assert.assertTrue(opts.getCustomHeaders().isEmpty());
    }

    @Test
    public void insertOptions_singleHeader_storedCorrectly() {
        InsertOptions opts = InsertOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "acct-123")
                .build();
        Map<CustomHeaderKey, String> headers = opts.getCustomHeaders();
        Assert.assertEquals(1, headers.size());
        Assert.assertEquals("acct-123", headers.get(CustomHeaderKey.SkyflowAccountID));
    }

    @Test
    public void insertOptions_multipleHeaders_allStored() {
        InsertOptions opts = InsertOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "acct-123")
                .addCustomHeader(CustomHeaderKey.SkyflowAccountName, "my-account")
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "req-abc")
                .build();
        Map<CustomHeaderKey, String> headers = opts.getCustomHeaders();
        Assert.assertEquals(3, headers.size());
        Assert.assertEquals("acct-123", headers.get(CustomHeaderKey.SkyflowAccountID));
        Assert.assertEquals("my-account", headers.get(CustomHeaderKey.SkyflowAccountName));
        Assert.assertEquals("req-abc", headers.get(CustomHeaderKey.RequestIDHeader));
    }

    @Test
    public void insertOptions_overwriteSameKey_keepsLastValue() {
        InsertOptions opts = InsertOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "first")
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "second")
                .build();
        Assert.assertEquals("second", opts.getCustomHeaders().get(CustomHeaderKey.SkyflowAccountID));
    }

    @Test
    public void insertOptions_builderMutationAfterBuild_doesNotAffectBuiltInstance() {
        InsertOptions.Builder builder = InsertOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "original");
        InsertOptions opts = builder.build();
        builder.addCustomHeader(CustomHeaderKey.SkyflowAccountID, "added-after-build");
        Assert.assertEquals(1, opts.getCustomHeaders().size());
        Assert.assertEquals("original", opts.getCustomHeaders().get(CustomHeaderKey.RequestIDHeader));
        Assert.assertNull(opts.getCustomHeaders().get(CustomHeaderKey.SkyflowAccountID));
    }

    @Test
    public void insertOptions_mapIsUnmodifiable() {
        InsertOptions opts = InsertOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "r1")
                .build();
        try {
            opts.getCustomHeaders().put(CustomHeaderKey.SkyflowAccountID, "extra");
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    // ── DetokenizeOptions ─────────────────────────────────────────────────────

    @Test
    public void detokenizeOptions_emptyBuilder_returnsEmptyHeaders() {
        DetokenizeOptions opts = DetokenizeOptions.builder().build();
        Assert.assertNotNull(opts.getCustomHeaders());
        Assert.assertTrue(opts.getCustomHeaders().isEmpty());
    }

    @Test
    public void detokenizeOptions_singleHeader_storedCorrectly() {
        DetokenizeOptions opts = DetokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "req-xyz")
                .build();
        Assert.assertEquals("req-xyz", opts.getCustomHeaders().get(CustomHeaderKey.RequestIDHeader));
    }

    @Test
    public void detokenizeOptions_multipleHeaders_allStored() {
        DetokenizeOptions opts = DetokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "a")
                .addCustomHeader(CustomHeaderKey.SkyflowAccountName, "b")
                .build();
        Assert.assertEquals(2, opts.getCustomHeaders().size());
    }

    @Test
    public void detokenizeOptions_builderMutationAfterBuild_doesNotAffectBuiltInstance() {
        DetokenizeOptions.Builder builder = DetokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "original");
        DetokenizeOptions opts = builder.build();
        builder.addCustomHeader(CustomHeaderKey.SkyflowAccountID, "added-after-build");
        Assert.assertEquals(1, opts.getCustomHeaders().size());
        Assert.assertEquals("original", opts.getCustomHeaders().get(CustomHeaderKey.RequestIDHeader));
        Assert.assertNull(opts.getCustomHeaders().get(CustomHeaderKey.SkyflowAccountID));
    }

    @Test
    public void detokenizeOptions_mapIsUnmodifiable() {
        DetokenizeOptions opts = DetokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "r1")
                .build();
        try {
            opts.getCustomHeaders().put(CustomHeaderKey.SkyflowAccountID, "extra");
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    // ── TokenizeOptions ───────────────────────────────────────────────────────

    @Test
    public void tokenizeOptions_emptyBuilder_returnsEmptyHeaders() {
        TokenizeOptions opts = TokenizeOptions.builder().build();
        Assert.assertNotNull(opts.getCustomHeaders());
        Assert.assertTrue(opts.getCustomHeaders().isEmpty());
    }

    @Test
    public void tokenizeOptions_singleHeader_storedCorrectly() {
        TokenizeOptions opts = TokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountName, "my-vault")
                .build();
        Assert.assertEquals("my-vault", opts.getCustomHeaders().get(CustomHeaderKey.SkyflowAccountName));
    }

    @Test
    public void tokenizeOptions_multipleHeaders_allStored() {
        TokenizeOptions opts = TokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "a")
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "b")
                .build();
        Assert.assertEquals(2, opts.getCustomHeaders().size());
    }

    @Test
    public void tokenizeOptions_builderMutationAfterBuild_doesNotAffectBuiltInstance() {
        TokenizeOptions.Builder builder = TokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "original");
        TokenizeOptions opts = builder.build();
        builder.addCustomHeader(CustomHeaderKey.SkyflowAccountID, "added-after-build");
        Assert.assertEquals(1, opts.getCustomHeaders().size());
        Assert.assertEquals("original", opts.getCustomHeaders().get(CustomHeaderKey.RequestIDHeader));
        Assert.assertNull(opts.getCustomHeaders().get(CustomHeaderKey.SkyflowAccountID));
    }

    @Test
    public void tokenizeOptions_mapIsUnmodifiable() {
        TokenizeOptions opts = TokenizeOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "r1")
                .build();
        try {
            opts.getCustomHeaders().put(CustomHeaderKey.SkyflowAccountID, "extra");
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    // ── DeleteTokensOptions ───────────────────────────────────────────────────

    @Test
    public void deleteTokensOptions_emptyBuilder_returnsEmptyHeaders() {
        DeleteTokensOptions opts = DeleteTokensOptions.builder().build();
        Assert.assertNotNull(opts.getCustomHeaders());
        Assert.assertTrue(opts.getCustomHeaders().isEmpty());
    }

    @Test
    public void deleteTokensOptions_singleHeader_storedCorrectly() {
        DeleteTokensOptions opts = DeleteTokensOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "req-del")
                .build();
        Assert.assertEquals("req-del", opts.getCustomHeaders().get(CustomHeaderKey.RequestIDHeader));
    }

    @Test
    public void deleteTokensOptions_multipleHeaders_allStored() {
        DeleteTokensOptions opts = DeleteTokensOptions.builder()
                .addCustomHeader(CustomHeaderKey.SkyflowAccountID, "a")
                .addCustomHeader(CustomHeaderKey.SkyflowAccountName, "b")
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "c")
                .build();
        Assert.assertEquals(3, opts.getCustomHeaders().size());
    }

    @Test
    public void deleteTokensOptions_builderMutationAfterBuild_doesNotAffectBuiltInstance() {
        DeleteTokensOptions.Builder builder = DeleteTokensOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "original");
        DeleteTokensOptions opts = builder.build();
        builder.addCustomHeader(CustomHeaderKey.SkyflowAccountID, "added-after-build");
        Assert.assertEquals(1, opts.getCustomHeaders().size());
        Assert.assertEquals("original", opts.getCustomHeaders().get(CustomHeaderKey.RequestIDHeader));
        Assert.assertNull(opts.getCustomHeaders().get(CustomHeaderKey.SkyflowAccountID));
    }

    @Test
    public void deleteTokensOptions_mapIsUnmodifiable() {
        DeleteTokensOptions opts = DeleteTokensOptions.builder()
                .addCustomHeader(CustomHeaderKey.RequestIDHeader, "r1")
                .build();
        try {
            opts.getCustomHeaders().put(CustomHeaderKey.SkyflowAccountID, "extra");
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }
}