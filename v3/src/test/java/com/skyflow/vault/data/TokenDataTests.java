package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class TokenDataTests {

    @Test
    public void constructor_setsTokenAndGroupName() {
        Token token = new Token("tok-abc", "non_deterministic");
        Assert.assertEquals("tok-abc", token.getToken());
        Assert.assertEquals("non_deterministic", token.getTokenGroupName());
    }

    @Test
    public void constructor_nullToken_allowsNull() {
        Token token = new Token(null, "grp");
        Assert.assertNull(token.getToken());
        Assert.assertEquals("grp", token.getTokenGroupName());
    }

    @Test
    public void constructor_nullGroupName_allowsNull() {
        Token token = new Token("tok-123", null);
        Assert.assertEquals("tok-123", token.getToken());
        Assert.assertNull(token.getTokenGroupName());
    }

    @Test
    public void constructor_bothNull_allowsNull() {
        Token token = new Token(null, null);
        Assert.assertNull(token.getToken());
        Assert.assertNull(token.getTokenGroupName());
    }

    @Test
    public void constructor_preservesExactValues() {
        String tokenValue = "sky-tok-abcdef1234567890";
        String groupName = "deterministic_string_tg";
        Token token = new Token(tokenValue, groupName);
        Assert.assertSame(tokenValue, token.getToken());
        Assert.assertSame(groupName, token.getTokenGroupName());
    }
}
