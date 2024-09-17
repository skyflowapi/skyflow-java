package com.skyflow.serviceaccount.util;

import com.skyflow.errors.ErrorCode;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SignedDataTokenResponseTest {
    @Test
    public void testSignedDataTokenResponse() {
        String response = "{dataToken: token,signedDataToken: signed_token}";
        SignedDataTokenResponse res = new SignedDataTokenResponse("token", "signed_token");
        Assert.assertNotNull(res);
        assertEquals(response,res.toString());
        assertEquals(res.dataToken, "token");
        assertEquals(res.signedDataToken, "signed_token");
    }
}
