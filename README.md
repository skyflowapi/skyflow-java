# Description
skyflow-java is the Skyflow SDK for the Java programming language.

## Usage

### Service Account Token Generation
[This](https://github.com/skyflowapi/skyflow-java/tree/master/src/main/java/serviceaccount) java module is used to generate service account tokens from service account credentials file which is downloaded upon creation of service account. The token generated from this module is valid for 60 minutes and can be used to make API calls to vault services as well as management API(s) based on the permissions of the service account.

[Example](https://github.com/skyflowapi/skyflow-java/blob/master/src/main/java/examples/serviceaccount/token/main/serviceAccountToken.java):

```java
import entities.ResponseToken;
import errors.SkyflowException;
import serviceaccount.util.Token;

public class ServiceAccountToken {

    public static void main(String args[]) {
        ResponseToken res;
        try {
            String filePath = "";
            res = Token.GenerateToken(filePath);
            System.out.println(res.getAccessToken() + ":" + res.getTokenType());
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

    }
}
```