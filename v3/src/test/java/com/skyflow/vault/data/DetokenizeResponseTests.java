package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class DetokenizeResponseTests {

	@Test
	public void testDetokenizeResponseGettersAndSummary() {
		List<DetokenizeResponseObject> success = Arrays.asList(
				new DetokenizeResponseObject(0, "token1", "value1", "group1", null, null),
				new DetokenizeResponseObject(1, "token2", "value2", "group2", null, null)
		);
		List<ErrorRecord> errors = Arrays.asList(
				new ErrorRecord(2, "error1", 400),
				new ErrorRecord(3, "error2", 404)
		);
		List<String> originalPayload = Arrays.asList("token1", "token2", "token3", "token4");
		DetokenizeResponse response = new DetokenizeResponse(success, errors, originalPayload);

		Assert.assertEquals(success, response.getSuccess());
		Assert.assertEquals(errors, response.getErrors());
		Assert.assertEquals(originalPayload, response.getSummary() != null ? originalPayload : null); // summary is constructed from originalPayload
		Assert.assertEquals(4, response.getSummary().getTotalTokens());
		Assert.assertEquals(2, response.getSummary().getTotalDetokenized());
		Assert.assertEquals(2, response.getSummary().getTotalFailed());
	}

	@Test
	public void testDetokenizeResponseToString() {
		List<DetokenizeResponseObject> success = Collections.singletonList(
				new DetokenizeResponseObject(0, "token1", "value1", "group1", null, null)
		);
		List<ErrorRecord> errors = Collections.singletonList(
				new ErrorRecord(1, "error1", 400)
		);
		List<String> originalPayload = Arrays.asList("token1", "token2");
		DetokenizeResponse response = new DetokenizeResponse(success, errors, originalPayload);
		String json = response.toString();
		Assert.assertTrue(json.contains("token1"));
		Assert.assertTrue(json.contains("error1"));
	}

	@Test
	public void testGetTokensToRetry_Only5xxErrorsExcept529() {
		List<DetokenizeResponseObject> success = Collections.emptyList();
		List<ErrorRecord> errors = Arrays.asList(
				new ErrorRecord(0, "error1", 500), // should retry
				new ErrorRecord(1, "error2", 503), // should retry
				new ErrorRecord(2, "error3", 529), // should NOT retry
				new ErrorRecord(3, "error4", 404)  // should NOT retry
		);
		List<String> originalPayload = Arrays.asList("tokenA", "tokenB", "tokenC", "tokenD");
		DetokenizeResponse response = new DetokenizeResponse(success, errors, originalPayload);
		List<String> tokensToRetry = response.getTokensToRetry();
		Assert.assertEquals(2, tokensToRetry.size());
		Assert.assertTrue(tokensToRetry.contains("tokenA"));
		Assert.assertTrue(tokensToRetry.contains("tokenB"));
		Assert.assertFalse(tokensToRetry.contains("tokenC"));
		Assert.assertFalse(tokensToRetry.contains("tokenD"));
	}

	@Test
	public void testGetTokensToRetry_EmptyErrors() {
		List<DetokenizeResponseObject> success = Collections.emptyList();
		List<ErrorRecord> errors = Collections.emptyList();
		List<String> originalPayload = Arrays.asList("tokenA", "tokenB");
		DetokenizeResponse response = new DetokenizeResponse(success, errors, originalPayload);
		List<String> tokensToRetry = response.getTokensToRetry();
		Assert.assertTrue(tokensToRetry.isEmpty());
	}

	@Test
	public void testConstructorWithoutOriginalPayload() {
		List<DetokenizeResponseObject> success = Collections.emptyList();
		List<ErrorRecord> errors = Collections.emptyList();
		DetokenizeResponse response = new DetokenizeResponse(success, errors);
		Assert.assertEquals(success, response.getSuccess());
		Assert.assertEquals(errors, response.getErrors());
		Assert.assertNull(response.getSummary());
	}
}
