package com.skyflow.vault.connection;

import com.google.gson.JsonObject;
import com.skyflow.enums.RequestMethod;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class InvokeConnectionTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static Map<String, String> queryParams;
    private static Map<String, String> pathParams;
    private static Map<String, String> requestHeaders;
    private static Map<String, String> requestBody;

    @BeforeClass
    public static void setup() {
        queryParams = new HashMap<>();
        pathParams = new HashMap<>();
        requestHeaders = new HashMap<>();
        requestBody = new HashMap<>();
    }

    @Before
    public void setupTest() {
        queryParams.clear();
        pathParams.clear();
        requestHeaders.clear();
        requestBody.clear();
    }

    @Test
    public void testValidInputInInvokeConnectionRequestValidations() {
        queryParams.put("query_param", "value");
        pathParams.put("path_param", "value");
        requestHeaders.put("header", "value");
        requestBody.put("key", "value");
        try {
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.assertEquals(1, request.getQueryParams().size());
            Assert.assertEquals(1, request.getPathParams().size());
            Assert.assertEquals(1, request.getRequestHeaders().size());
            Assert.assertEquals(RequestMethod.POST, request.getMethodName());
            Assert.assertNotNull(request.getRequestBody());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testEmptyRequestHeadersInInvokeConnectionRequestValidations() {
        try {
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyRequestHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullRequestHeaderKeyInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value1");
            requestHeaders.put(null, "value2");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidRequestHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyRequestHeaderKeyInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            requestHeaders.put("", "value");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidRequestHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullRequestHeaderValueInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            requestHeaders.put("header2", null);
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidRequestHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyRequestHeaderValueInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            requestHeaders.put("header2", "");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidRequestHeaders.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyPathParamsInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyPathParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullPathParamKeyInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            pathParams.put("path_param", "value");
            pathParams.put(null, "value");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidPathParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyPathParamKeyInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            pathParams.put("path_param", "value");
            pathParams.put("", "value");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidPathParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullPathParamValueInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            pathParams.put("path_param1", "value");
            pathParams.put("path_param2", null);
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidPathParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyPathParamValueInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            pathParams.put("path_param1", "value");
            pathParams.put("path_param2", "");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidPathParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyQueryParamsInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            pathParams.put("path_param", "value");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyQueryParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullQueryParamKeyInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            pathParams.put("path_param", "value");
            queryParams.put("query_param", "value");
            queryParams.put(null, "value");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidQueryParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyQueryParamKeyInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            pathParams.put("path_param", "value");
            queryParams.put("query_param", "value");
            queryParams.put("", "value");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidQueryParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testNullQueryParamValueInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            pathParams.put("path_param", "value");
            queryParams.put("query_param1", "value");
            queryParams.put("query_param2", null);
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidQueryParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyQueryParamValueInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header1", "value");
            pathParams.put("path_param", "value");
            queryParams.put("query_param1", "value");
            queryParams.put("query_param2", "");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.InvalidQueryParams.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyRequestBodyInInvokeConnectionRequestValidations() {
        try {
            requestHeaders.put("header", "value");
            pathParams.put("path_param", "value");
            queryParams.put("query_param", "value");
            InvokeConnectionRequest request = InvokeConnectionRequest.builder()
                    .methodName(RequestMethod.POST)
                    .requestHeaders(requestHeaders)
                    .pathParams(pathParams)
                    .queryParams(queryParams)
                    .requestBody(requestBody)
                    .build();
            Validations.validateInvokeConnectionRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getCode());
            Assert.assertEquals(ErrorMessage.EmptyRequestBody.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testInvokeConnectionResponse() {
        try {
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("test_key_1", "test_value_1");
            responseObject.addProperty("test_key_2", "test_value_2");
            InvokeConnectionResponse connectionResponse = new InvokeConnectionResponse(responseObject);
            String responseString = "InvokeConnectionResponse{" + "response=" + responseObject + "}";
            Assert.assertEquals(responseString, connectionResponse.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
