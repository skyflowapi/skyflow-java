package com.skyflow.enums;

import org.junit.Assert;
import org.junit.Test;

public class TokenTypeTest {

    @Test
    public void testVaultToken() {
        Assert.assertEquals("vault_token", TokenType.VAULT_TOKEN.getTokenType());
        Assert.assertEquals("vault_token", TokenType.VAULT_TOKEN.toString());
    }

    @Test
    public void testEntityUniqueCounter() {
        Assert.assertEquals("entity_unq_counter", TokenType.ENTITY_UNIQUE_COUNTER.getTokenType());
        Assert.assertEquals("entity_unq_counter", TokenType.ENTITY_UNIQUE_COUNTER.toString());
    }

    @Test
    public void testEntityOnly() {
        Assert.assertEquals("entity_only", TokenType.ENTITY_ONLY.getTokenType());
        Assert.assertEquals("entity_only", TokenType.ENTITY_ONLY.toString());
    }

    @Test
    public void testGetDefault() {
        Assert.assertEquals(TokenType.ENTITY_UNIQUE_COUNTER.getTokenType(), TokenType.VAULT_TOKEN.getDefault());
    }
}
