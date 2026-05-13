package com.skyflow.serviceaccount.util;

import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Utils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BearerTokenTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String invalidJsonFilePath = null;
    private static String credentialsFilePath = null;
    private static String invalidFilePath = null;
    private static String credentialsString = null;
    private static String context = null;
    private static ArrayList<String> roles = null;
    private static String role = null;

    @BeforeClass
    public static void setup() {
        credentialsFilePath = "./credentials.json";
        invalidJsonFilePath = "./src/test/resources/notJson.txt";
        invalidFilePath = "./src/test/credentials.json";
        credentialsString = "{\"key\":\"value\"}";
        context = "test_context";
        roles = new ArrayList<>();
        role = "test_role";
    }

    @Test
    public void testBearerTokenBuilderWithCredentialsFile() {
        try {
            roles.add(role);
            File file = new File(credentialsFilePath);
            BearerToken.builder().setCredentials(file).setCtx(context).setRoles(roles).build();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testBearerTokenBuilderWithCredentialsString() {
        try {
            BearerToken.builder().setCredentials(credentialsString).setCtx(context).setRoles(roles).build();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testBearerTokenBuilderWithMapContext() {
        try {
            Map<String, Object> ctxMap = new HashMap<>();
            ctxMap.put("role", "admin");
            ctxMap.put("department", "finance");
            ctxMap.put("user_id", "user_12345");
            File file = new File(credentialsFilePath);
            BearerToken.builder().setCredentials(file).setCtx(ctxMap).build();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testBearerTokenBuilderWithMapContextFromString() {
        try {
            Map<String, Object> ctxMap = new HashMap<>();
            ctxMap.put("role", "analyst");
            ctxMap.put("level", 3);
            BearerToken.builder().setCredentials(credentialsString).setCtx(ctxMap).build();
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testEmptyCredentialsFilePath() {
        try {
            File file = new File("");
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.FileNotFound.getMessage(), ""), e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidFilePath() {
        try {
            File file = new File(invalidFilePath);
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.FileNotFound.getMessage(), invalidFilePath),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidCredentialsFile() {
        try {
            File file = new File(invalidJsonFilePath);
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.FileInvalidJson.getMessage(), invalidJsonFilePath),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyCredentialsString() {
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials("").build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidCredentials.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidCredentialsString() {
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(invalidFilePath).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.CredentialsStringInvalidJson.getMessage(), e.getMessage());
        }
    }


    @Test
    public void testNoPrivateKeyInCredentialsForCredentials() {
        String filePath = "./src/test/resources/noPrivateKeyCredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingPrivateKey.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNoClientIDInCredentialsForCredentials() {
        String filePath = "./src/test/resources/noClientIDCredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingClientId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNoKeyIDInCredentialsForCredentials() {
        String filePath = "./src/test/resources/noKeyIDCredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingKeyId.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNoTokenURIInCredentialsForCredentials() {
        String filePath = "./src/test/resources/noTokenURICredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.MissingTokenUri.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidPrivateKeyInCredentialsForCredentials() {
        String filePath = "./src/test/resources/invalidPrivateKeyCredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.JwtInvalidFormat.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidKeySpecInCredentialsForCredentials() {
        String credentialsString = "{\"privateKey\": \"-----BEGIN PRIVATE KEY-----\\ncHJpdmF0ZV9rZXlfdmFsdWU=\\n-----END PRIVATE KEY-----\", \"clientID\": \"client_id_value\", \"keyID\": \"key_id_value\", \"tokenURI\": \"invalid_token_uri\"}";
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(credentialsString).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidKeySpec.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvalidTokenURIInCredentialsForCredentials() throws SkyflowException {
        String filePath = "./src/test/resources/invalidTokenURICredentials.json";
        File file = new File(filePath);
        try {
            BearerToken bearerToken = BearerToken.builder().setCredentials(file).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(ErrorMessage.InvalidTokenUri.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testBearerTokenWithNewFormCredentialKeys() {
        try {
            String credentialsString = "{\"privateKey\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCzLp0TVwidRMtZ\\n4tGLHPDEF6ihmE4OHSR/r5rZGqE+PNtw/uwXzBrfz1Mktb0hddMZNwC2IKhHE0Yw\\nvtBT0jsfy4OUQR13Mohn9znz+5TES/yXjkvZjhZKzs5rxNw/cO8lpKYUYdwbFzwl\\n9e3joCsWBXBDCbXdLQGPyggJV+KBI0LBal+LngNLU/U680LRlybCKCTyyrF0SERD\\npytcpnq41CS2Q0ZDfkK/zLrvsCkEBU8xYeAf/TphXMKeqvMGTqxxg6IPOKfYya7Q\\nnH9eZ1pn1SCe6N5XBUpQpB4K+1IZKvadOYpYWzRgM+tT5k4UVsg6s7kUm8k9n85/\\nNQMjMY2XAgMBAAECggEASlg05ClgcaBxn0H1H3tKipImbaX7/O8qjbAW162s6V3m\\nzuN2ogkVvXcQUFL3vkJc7EFeEjNKnvLoVKFXXvADiBWw6np591MINdrmOM1R1ICS\\ntW9dGU9TAIb+LsjneYsqLrw6DIruAG+LjVSU97UlK2XmRmppAvQBid+Rpg7I9Dsy\\naJyGjDHeC3RyYYNfpei2dBPUYlUjOkBqgYGOOyjYxHzzgYtdVZku0JPtsAey3WKL\\nSbu8ryugu7r23fxP50H3FtYz91TPlVu1zVEk9Viizp2c9642ZKEoA0bB/bSNMUnt\\nZ/kemZENAzC7tnoYgwN09rI3h0+U5jaU1BhXbrLpAQKBgQDt8eaywv6j+Hdv8i7S\\nyMnZE4CaM70Z319ctJPlt2QdCZp8dtac858qnnrrZSCWV3n3yMv//bf1WZB4Lssw\\nuxBzSCFI/imG6eY9uQA6yXLl1TY9DA5IJ8s2LGzwmtA1q+vC+jzWs+0+S/evUewo\\nTZGQuNjHMHoM22jeLErqQZkHUQKBgQDAxz1WY56ZHdC3Y4aXkDeb5Ag+ZJV8Uqwn\\nootA2zHCaEx8gM9CzChCl4pQcghHFXv4eEKqezdWSK+SIRA1CtR+q8g5dP8YtAkR\\n9Uav6/fEkM8iCUvhZg+1DPRShu15nQF0ZAleSJ9OiSW5pIfAbY79RHru8H31azhE\\nDOWezXbcZwKBgB9LAAckg+62n6aWWDgadglZekFNaqI7cUQ073p3mvACslGKI4Fy\\nvM0TGKFapGWBTaYbv1CEYqwewlQ7+zcGcwxmQRJjcryuiDw312Lj2XuGheKTclFl\\nAmG2iAFAqv9UA+aZmGS4NwxJW2KwSHmocetxk/jmVDbaqDkH5DZYuDJxAoGBAJqn\\n/PRujVEnk0dc6CB1ybcd9OMhTK/ln0lY5MDOWRgvFpWXvS9InE/4RTWOlkd42/EV\\ngd5FZbqqK3hfYCI9owZQiBxYWUMXRGOM0/3Un/ypdBNJQ//7IkTMtMH0j1XOeNlI\\nXB+wwWV/L63EakgdXOag5sMEWvjl4MjvU9PX4DCnAoGAR0c567DWbkTXvcNIjvNF\\nNK8suq/fGt4dpbkkFOEHjgqFd5RsjFHKc98JVrudPweUR7YjpeKQaeNKXfVFd4+N\\nDPOs0zWSsaHckh1g9djkZlidha9SD/V6cOpxi3g2okcn/LI7h8NyNlAwDSn2mPEi\\nMd3mrgMCZwJsXLndGQSDVUw=\\n-----END PRIVATE KEY-----\\n\", "
                    + "\"clientId\": \"client_id_value\", \"keyId\": \"key_id_value\", \"tokenUri\": \"invalid_token_uri\"}";
            BearerToken bearerToken = BearerToken.builder().setCredentials(credentialsString).build();
            bearerToken.getBearerToken();
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            // InvalidTokenUri means new-form keys were resolved successfully — failure is at URL parsing, not field lookup
            Assert.assertEquals(ErrorMessage.InvalidTokenUri.getMessage(), e.getMessage());
        }
    }
}
