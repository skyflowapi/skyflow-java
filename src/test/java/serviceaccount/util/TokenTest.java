package serviceaccount.util;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.Token;
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
}
