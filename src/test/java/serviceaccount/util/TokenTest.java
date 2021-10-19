package serviceaccount.util;

import entities.ResponseToken;
import errors.SkyflowException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;

public class TokenTest {

    @Test
    public void testInvalidFilePath() {
        Exception exception = Assert.assertThrows(SkyflowException.class, () -> {
            Token.GenerateToken("");
        });
        String expectedMessage = "Unable to open credentials";
        Assert.assertTrue(exception.getMessage().contains(expectedMessage));
    }

    @Test
    public void testInvalidFileContent() {
        Exception exception = Assert.assertThrows(SkyflowException.class, () -> {
            Token.GenerateToken(Paths.get("./src/test/resources/invalidCredentials.json").toString());
        });
        String expectedMessage = "Unable to read clientID";
        Assert.assertTrue(exception.getMessage().contains(expectedMessage));
    }

    @Test
    public void testGetToken() throws SkyflowException {
        ResponseToken res = Token.GenerateToken("src/test/resources/validCredentials.json");
        Assert.assertEquals(res.getTokenType(), "Bearer");
        Assert.assertNotNull(res.getAccessToken());
    }
}
