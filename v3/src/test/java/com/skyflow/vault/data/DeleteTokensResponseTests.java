package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DeleteTokensResponseTests {

    @Test
    public void testDeleteTokensResponseGettersAndSummary() {
        List<DeleteTokensSuccess> success = Arrays.asList(
                new DeleteTokensSuccess(0, "token1"),
                new DeleteTokensSuccess(1, "token2")
        );
        List<ErrorRecord> errors = Arrays.asList(
                new ErrorRecord(2, "error1", 404)
        );
        List<String> originalPayload = Arrays.asList("token1", "token2", "token3");
        DeleteTokensResponse response = new DeleteTokensResponse(success, errors, originalPayload);

        Assert.assertEquals(success, response.getSuccess());
        Assert.assertEquals(errors, response.getErrors());
        Assert.assertNotNull(response.getSummary());
        Assert.assertEquals(3, response.getSummary().getTotalTokens());
        Assert.assertEquals(2, response.getSummary().getTotalDeleted());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
    }

    @Test
    public void testDeleteTokensResponseToString() {
        List<DeleteTokensSuccess> success = Collections.singletonList(
                new DeleteTokensSuccess(0, "token1")
        );
        List<ErrorRecord> errors = Collections.singletonList(
                new ErrorRecord(1, "error1", 400)
        );
        List<String> originalPayload = Arrays.asList("token1", "token2");
        DeleteTokensResponse response = new DeleteTokensResponse(success, errors, originalPayload);
        String json = response.toString();
        Assert.assertTrue(json.contains("token1"));
        Assert.assertTrue(json.contains("error1"));
        Assert.assertTrue(json.contains("summary"));
    }

    @Test
    public void testDeleteTokensResponseConstructorWithoutPayload() {
        List<DeleteTokensSuccess> success = Collections.emptyList();
        List<ErrorRecord> errors = Collections.emptyList();
        DeleteTokensResponse response = new DeleteTokensResponse(success, errors);
        Assert.assertEquals(success, response.getSuccess());
        Assert.assertEquals(errors, response.getErrors());
        Assert.assertNull(response.getSummary());
    }

    @Test
    public void testDeleteTokensResponseAllSuccess() {
        List<DeleteTokensSuccess> success = Arrays.asList(
                new DeleteTokensSuccess(0, "token1"),
                new DeleteTokensSuccess(1, "token2"),
                new DeleteTokensSuccess(2, "token3")
        );
        List<ErrorRecord> errors = Collections.emptyList();
        List<String> originalPayload = Arrays.asList("token1", "token2", "token3");
        DeleteTokensResponse response = new DeleteTokensResponse(success, errors, originalPayload);

        Assert.assertEquals(3, response.getSummary().getTotalTokens());
        Assert.assertEquals(3, response.getSummary().getTotalDeleted());
        Assert.assertEquals(0, response.getSummary().getTotalFailed());
    }

    @Test
    public void testDeleteTokensResponseAllErrors() {
        List<DeleteTokensSuccess> success = Collections.emptyList();
        List<ErrorRecord> errors = Arrays.asList(
                new ErrorRecord(0, "error1", 404),
                new ErrorRecord(1, "error2", 500)
        );
        List<String> originalPayload = Arrays.asList("token1", "token2");
        DeleteTokensResponse response = new DeleteTokensResponse(success, errors, originalPayload);

        Assert.assertEquals(2, response.getSummary().getTotalTokens());
        Assert.assertEquals(0, response.getSummary().getTotalDeleted());
        Assert.assertEquals(2, response.getSummary().getTotalFailed());
    }

    @Test
    public void testDeleteTokensSuccessGetters() {
        DeleteTokensSuccess s = new DeleteTokensSuccess(3, "my-token");
        Assert.assertEquals(3, s.getIndex());
        Assert.assertEquals("my-token", s.getToken());
    }

    @Test
    public void testDeleteTokensSuccessToString() {
        DeleteTokensSuccess s = new DeleteTokensSuccess(0, "tok");
        String json = s.toString();
        Assert.assertTrue(json.contains("tok"));
    }

    @Test
    public void testDeleteTokensSummaryGetters() {
        DeleteTokensSummary summary = new DeleteTokensSummary(10, 8, 2);
        Assert.assertEquals(10, summary.getTotalTokens());
        Assert.assertEquals(8, summary.getTotalDeleted());
        Assert.assertEquals(2, summary.getTotalFailed());
    }

    @Test
    public void testDeleteTokensSummaryToString() {
        DeleteTokensSummary summary = new DeleteTokensSummary(5, 4, 1);
        String json = summary.toString();
        Assert.assertTrue(json.contains("totalTokens") || json.contains("5"));
    }
}
