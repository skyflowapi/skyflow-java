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

	// ── DetokenizeSummary direct tests ────────────────────────────────────────

	@Test
	public void detokenizeSummary_paramConstructor_setsAllFields() {
		DetokenizeSummary summary = new DetokenizeSummary(10, 7, 3);
		Assert.assertEquals(10, summary.getTotalTokens());
		Assert.assertEquals(7, summary.getTotalDetokenized());
		Assert.assertEquals(3, summary.getTotalFailed());
	}

	@Test
	public void detokenizeSummary_defaultConstructor_allZero() {
		DetokenizeSummary summary = new DetokenizeSummary();
		Assert.assertEquals(0, summary.getTotalTokens());
		Assert.assertEquals(0, summary.getTotalDetokenized());
		Assert.assertEquals(0, summary.getTotalFailed());
	}

	@Test
	public void detokenizeSummary_toString_containsAllFields() {
		DetokenizeSummary summary = new DetokenizeSummary(5, 4, 1);
		String json = summary.toString();
		Assert.assertTrue(json.contains("totalTokens"));
		Assert.assertTrue(json.contains("totalDetokenized"));
		Assert.assertTrue(json.contains("totalFailed"));
		Assert.assertTrue(json.contains("5"));
		Assert.assertTrue(json.contains("4"));
		Assert.assertTrue(json.contains("1"));
	}

	@Test
	public void detokenizeSummary_allSuccess_zeroFailed() {
		DetokenizeSummary summary = new DetokenizeSummary(3, 3, 0);
		Assert.assertEquals(3, summary.getTotalTokens());
		Assert.assertEquals(3, summary.getTotalDetokenized());
		Assert.assertEquals(0, summary.getTotalFailed());
	}

	@Test
	public void detokenizeSummary_allFailed_zeroDetokenized() {
		DetokenizeSummary summary = new DetokenizeSummary(4, 0, 4);
		Assert.assertEquals(4, summary.getTotalTokens());
		Assert.assertEquals(0, summary.getTotalDetokenized());
		Assert.assertEquals(4, summary.getTotalFailed());
	}

	// ── DetokenizeResponseObject direct tests ─────────────────────────────────

	@Test
	public void detokenizeResponseObject_constructor_setsAllFields() {
		Map<String, Object> meta = new HashMap<>();
		meta.put("key", "val");
		DetokenizeResponseObject obj = new DetokenizeResponseObject(2, "tok-x", "plain", "grp1", "some error", meta);
		Assert.assertEquals(2, obj.getIndex());
		Assert.assertEquals("tok-x", obj.getToken());
		Assert.assertEquals("plain", obj.getValue());
		Assert.assertEquals("grp1", obj.getTokenGroupName());
		Assert.assertEquals("some error", obj.getError());
		Assert.assertEquals(meta, obj.getMetadata());
	}

	@Test
	public void detokenizeResponseObject_nullFields_returnsNull() {
		DetokenizeResponseObject obj = new DetokenizeResponseObject(0, null, null, null, null, null);
		Assert.assertEquals(0, obj.getIndex());
		Assert.assertNull(obj.getToken());
		Assert.assertNull(obj.getValue());
		Assert.assertNull(obj.getTokenGroupName());
		Assert.assertNull(obj.getError());
		Assert.assertNull(obj.getMetadata());
	}

	@Test
	public void detokenizeResponseObject_toString_containsToken() {
		DetokenizeResponseObject obj = new DetokenizeResponseObject(1, "tok-abc", "secret-val", "group-x", null, null);
		String json = obj.toString();
		Assert.assertTrue(json.contains("tok-abc"));
		Assert.assertTrue(json.contains("secret-val"));
		Assert.assertTrue(json.contains("group-x"));
	}

	@Test
	public void detokenizeResponseObject_toString_withMetadata() {
		Map<String, Object> meta = new java.util.HashMap<>();
		meta.put("region", "us-east-1");
		DetokenizeResponseObject obj = new DetokenizeResponseObject(0, "tok1", "val1", "grp", null, meta);
		String json = obj.toString();
		Assert.assertTrue(json.contains("us-east-1"));
	}

	@Test
	public void detokenizeResponseObject_withError_getterReturnsError() {
		DetokenizeResponseObject obj = new DetokenizeResponseObject(3, "bad-tok", null, null, "Token not found", null);
		Assert.assertEquals("Token not found", obj.getError());
		Assert.assertNull(obj.getValue());
	}
}