package com.skyflow.vault.tokens;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiException;
import com.skyflow.generated.rest.api.TokensApi;
import com.skyflow.generated.rest.models.V1DetokenizeResponse;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest(fullyQualifiedNames = {
//        "com.skyflow.serviceaccount.util.Token",
//        "com.skyflow.generated.rest.ApiClient",
//        "com.skyflow.generated.rest.api.TokensApi",
//})
public class DetokenizeTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static VaultConfig vaultConfig = null;
    private static String token = null;
    private static ArrayList<String> tokens = null;
    private static Skyflow skyflowClient = null;
    private ApiClient mockApiClient;
    private TokensApi mockTokensApi;

    @BeforeClass
    public static void setup() throws SkyflowException, NoSuchMethodException {
//        PowerMockito.mockStatic(Token.class);
//        PowerMockito.when(Token.isExpired("valid_token")).thenReturn(true);
//        PowerMockito.when(Token.isExpired("not_a_valid_token")).thenReturn(false);

        vaultID = "vault123";
        clusterID = "cluster123";

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");

        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.DEV);
        vaultConfig.setCredentials(credentials);

        token = "test_token_1";
        tokens = new ArrayList<>();

//        System.setProperty("ssl.TrustManagerFactory.algorithm", "PKIX");
    }

//    @Before
//    public void setupMock() throws ApiException {
//        mockApiClient = PowerMockito.mock(ApiClient.class);
//        mockTokensApi = PowerMockito.mock(TokensApi.class);
//
//        V1DetokenizeResponse mockResponse = new V1DetokenizeResponse();
//        PowerMockito
//                .when(mockTokensApi.recordServiceDetokenize(ArgumentMatchers.anyString(), ArgumentMatchers.any()))
//                .thenReturn(mockResponse);
//
//    }

    @Test
    public void testValidInputInDetokenizeRequestValidations() {
        try {
            tokens.add(token);
            DetokenizeRequest request = DetokenizeRequest.builder().
                    tokens(tokens).redactionType(RedactionType.MASKED).continueOnError(false).build();
            Validations.validateDetokenizeRequest(request);
            Assert.assertEquals(1, tokens.size());
            Assert.assertEquals(RedactionType.MASKED, request.getRedactionType());
            Assert.assertFalse(request.getContinueOnError());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNoTokensInDetokenizeRequestValidations() {
        DetokenizeRequest request = DetokenizeRequest.builder().build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidDataTokens.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertEquals(RedactionType.PLAIN_TEXT, request.getRedactionType());
            Assert.assertTrue(request.getContinueOnError());
        }
    }

    @Test
    public void testEmptyTokensInDetokenizeRequestValidations() {
        tokens.clear();
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyDataTokens.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertEquals(RedactionType.PLAIN_TEXT, request.getRedactionType());
            Assert.assertTrue(request.getContinueOnError());
        }
    }

    @Test
    public void testEmptyTokenInTokensInDetokenizeRequestValidations() {
        tokens.add(token);
        tokens.add("");
        DetokenizeRequest request = DetokenizeRequest.builder().tokens(tokens).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyTokenInDataTokens.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
            Assert.assertEquals(RedactionType.PLAIN_TEXT, request.getRedactionType());
            Assert.assertTrue(request.getContinueOnError());
        }
    }

    @Test
    public void testRedactionAndContinueOnErrorInDetokenizeRequestValidations() {
        tokens.clear();
        tokens.add(token);
        DetokenizeRequest request = DetokenizeRequest.builder().
                tokens(tokens).redactionType(null).continueOnError(null).build();
        try {
            Validations.validateDetokenizeRequest(request);
            Assert.assertEquals(RedactionType.PLAIN_TEXT, request.getRedactionType());
            Assert.assertTrue(request.getContinueOnError());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
