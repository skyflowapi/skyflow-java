package com.skyflow.common.utils;

import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertEquals;

class ValidTokenProvider implements TokenProvider {

    @Override
    public String getBearerToken() throws Exception {
        return "aa.valid_token.dd";
    }
}
class InvalidBearerTokenProvider implements TokenProvider {

    @Override
    public String getBearerToken() throws Exception {
        return "aa.invalid_token.dd";
    }
}

class MockDataProvider  {

    public byte[] returnValidDecodeBytes(){
        JSONObject validJson = new JSONObject();
        validJson.put("exp", (System.currentTimeMillis()+3000) / 1000);
        byte[] encodedBytes = Base64.encodeBase64(validJson.toJSONString().getBytes());
        return Base64.decodeBase64(encodedBytes);

    }
    public byte[] returnInValidDecodeBytes(){
        JSONObject expiredJson = new JSONObject();
        expiredJson.put("exp", (System.currentTimeMillis()-3000) / 1000);
        byte[] encodedByte = Base64.encodeBase64(expiredJson.toJSONString().getBytes());
        return Base64.decodeBase64(encodedByte);

    }
}


@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "org.apache.commons.codec.binary.Base64")
public class TokenUtilsValidTokenTest {

    @Before
    public void init(){
        MockDataProvider dd = new MockDataProvider();
        byte[] validDecodeBytes = dd.returnValidDecodeBytes();
        byte[] inValidDecodeBytes = dd.returnInValidDecodeBytes();
        PowerMockito.mockStatic(Base64.class);
        PowerMockito.when(Base64.decodeBase64("valid_token")).thenReturn(validDecodeBytes);
        PowerMockito.when(Base64.decodeBase64("invalid_token")).thenReturn(inValidDecodeBytes);
    }
    @Test
    public void testValidToken() {
        try {
            TokenUtils tokenUtils = new TokenUtils();
            String token = tokenUtils.getBearerToken(new ValidTokenProvider());
            assertEquals(token,"aa.valid_token.dd");
            String secondToken = tokenUtils.getBearerToken(new ValidTokenProvider());
            assertEquals(secondToken,"aa.valid_token.dd");
        } catch (SkyflowException e) {
            Assert.fail("Should not throw EXCEPTION !!");
        }
    }
    
    @Test
    public void testInValidToken() {
        try {
            TokenUtils tokenUtils = new TokenUtils();
            String token = tokenUtils.getBearerToken(new InvalidBearerTokenProvider());
            Assert.fail("should throw execution");
        } catch (SkyflowException e) {
            assertEquals(e.getMessage(), ErrorCode.BearerTokenExpired.getDescription());

        }
    }
}
