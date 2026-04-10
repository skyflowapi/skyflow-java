package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TokenizeResponseTests {

    private static TokenizeSuccess makeSuccess(int index, String value, String groupName, String token) {
        TokenizeSuccess s = new TokenizeSuccess(index, value);
        s.addToken(groupName, token);
        return s;
    }

    @Test
    public void testTokenizeResponseGettersAndSummary() {
        // index 0 & 1 in success, index 2 in errors only → totalTokenized=2, totalPartial=0, totalFailed=1
        List<TokenizeSuccess> success = Arrays.asList(
                makeSuccess(0, "sachin", "non_deterministic", "tok1"),
                makeSuccess(1, "sachin", "deterministic", "tok2")
        );
        List<ErrorRecord> errors = Collections.singletonList(
                new ErrorRecord(2, "error1", 404)
        );
        List<TokenizeRecord> originalPayload = Arrays.asList(
                TokenizeRecord.builder().value("sachin").tokenGroupNames(Collections.singletonList("g1")).build(),
                TokenizeRecord.builder().value("dhoni").tokenGroupNames(Collections.singletonList("g2")).build(),
                TokenizeRecord.builder().value("kohli").tokenGroupNames(Collections.singletonList("g3")).build()
        );
        TokenizeResponse response = new TokenizeResponse(success, errors, originalPayload);

        Assert.assertEquals(success, response.getSuccess());
        Assert.assertEquals(errors, response.getErrors());
        Assert.assertNotNull(response.getSummary());
        Assert.assertEquals(3, response.getSummary().getTotalTokens());
        Assert.assertEquals(2, response.getSummary().getTotalTokenized());
        Assert.assertEquals(0, response.getSummary().getTotalPartial());
        Assert.assertEquals(1, response.getSummary().getTotalFailed());
    }

    @Test
    public void testTokenizeResponseToString() {
        List<TokenizeSuccess> success = Collections.singletonList(
                makeSuccess(0, "val1", "group1", "tok1")
        );
        List<ErrorRecord> errors = Collections.singletonList(
                new ErrorRecord(1, "some error", 400)
        );
        List<TokenizeRecord> originalPayload = Arrays.asList(
                TokenizeRecord.builder().value("v1").tokenGroupNames(Collections.singletonList("g1")).build(),
                TokenizeRecord.builder().value("v2").tokenGroupNames(Collections.singletonList("g2")).build()
        );
        TokenizeResponse response = new TokenizeResponse(success, errors, originalPayload);
        String json = response.toString();
        Assert.assertTrue(json.contains("tok1"));
        Assert.assertTrue(json.contains("some error"));
        Assert.assertTrue(json.contains("summary"));
    }

    @Test
    public void testTokenizeResponseConstructorWithoutPayload() {
        List<TokenizeSuccess> success = Collections.emptyList();
        List<ErrorRecord> errors = Collections.emptyList();
        TokenizeResponse response = new TokenizeResponse(success, errors);
        Assert.assertEquals(success, response.getSuccess());
        Assert.assertEquals(errors, response.getErrors());
        Assert.assertNull(response.getSummary());
    }

    @Test
    public void testTokenizeResponsePartialSuccess() {
        // index 0 in both success and errors → totalPartial=1; index 1 success only → totalTokenized=1
        TokenizeSuccess s0 = new TokenizeSuccess(0, "test");
        s0.addToken("deterministic_string_tg", "tok_d");

        TokenizeSuccess s1 = new TokenizeSuccess(1, "sachin22");
        s1.addToken("non_deterministic", "tok_nd2");
        s1.addToken("deterministic_string_tg", "tok_d2");

        List<TokenizeSuccess> success = Arrays.asList(s0, s1);
        List<ErrorRecord> errors = Collections.singletonList(
                new ErrorRecord(0, "Token group non_deterministi is invalid.", 400)
        );
        List<TokenizeRecord> originalPayload = Arrays.asList(
                TokenizeRecord.builder().value("test").tokenGroupNames(Arrays.asList("non_deterministi", "deterministic_string_tg")).build(),
                TokenizeRecord.builder().value("sachin22").tokenGroupNames(Arrays.asList("non_deterministic", "deterministic_string_tg")).build()
        );
        TokenizeResponse response = new TokenizeResponse(success, errors, originalPayload);

        Assert.assertEquals(2, response.getSummary().getTotalTokens());
        Assert.assertEquals(1, response.getSummary().getTotalTokenized());  // index 1: all succeeded
        Assert.assertEquals(1, response.getSummary().getTotalPartial());    // index 0: some succeeded, some failed
        Assert.assertEquals(0, response.getSummary().getTotalFailed());     // no record had zero successes
        // totalTokenized + totalPartial + totalFailed == totalTokens
        Assert.assertEquals(
                response.getSummary().getTotalTokens(),
                response.getSummary().getTotalTokenized() +
                response.getSummary().getTotalPartial() +
                response.getSummary().getTotalFailed()
        );
    }

    @Test
    public void testTokenizeResponseAllSuccess() {
        List<TokenizeSuccess> success = Arrays.asList(
                makeSuccess(0, "v1", "g1", "tok1"),
                makeSuccess(1, "v2", "g2", "tok2")
        );
        List<ErrorRecord> errors = Collections.emptyList();
        List<TokenizeRecord> originalPayload = Arrays.asList(
                TokenizeRecord.builder().value("v1").tokenGroupNames(Collections.singletonList("g1")).build(),
                TokenizeRecord.builder().value("v2").tokenGroupNames(Collections.singletonList("g2")).build()
        );
        TokenizeResponse response = new TokenizeResponse(success, errors, originalPayload);

        Assert.assertEquals(2, response.getSummary().getTotalTokens());
        Assert.assertEquals(2, response.getSummary().getTotalTokenized());
        Assert.assertEquals(0, response.getSummary().getTotalPartial());
        Assert.assertEquals(0, response.getSummary().getTotalFailed());
    }

    @Test
    public void testTokenizeResponseAllErrors() {
        List<TokenizeSuccess> success = Collections.emptyList();
        List<ErrorRecord> errors = Arrays.asList(
                new ErrorRecord(0, "err1", 404),
                new ErrorRecord(1, "err2", 500)
        );
        List<TokenizeRecord> originalPayload = Arrays.asList(
                TokenizeRecord.builder().value("v1").tokenGroupNames(Collections.singletonList("g1")).build(),
                TokenizeRecord.builder().value("v2").tokenGroupNames(Collections.singletonList("g2")).build()
        );
        TokenizeResponse response = new TokenizeResponse(success, errors, originalPayload);

        Assert.assertEquals(2, response.getSummary().getTotalTokens());
        Assert.assertEquals(0, response.getSummary().getTotalTokenized());
        Assert.assertEquals(0, response.getSummary().getTotalPartial());
        Assert.assertEquals(2, response.getSummary().getTotalFailed());
    }

    @Test
    public void testTokenizeSuccessGetters() {
        TokenizeSuccess s = new TokenizeSuccess(3, "myValue");
        s.addToken("myGroup", "myToken");
        Assert.assertEquals(3, s.getIndex());
        Assert.assertEquals("myValue", s.getValue());
        Assert.assertNotNull(s.getTokens());
        Assert.assertEquals("myToken", s.getTokens().get("myGroup"));
    }

    @Test
    public void testTokenizeSuccessMultipleTokenGroups() {
        TokenizeSuccess s = new TokenizeSuccess(0, "sachin");
        s.addToken("non_deterministic", "tok_nd");
        s.addToken("deterministic_string_tg", "tok_d");
        Assert.assertEquals(2, s.getTokens().size());
        Assert.assertEquals("tok_nd", s.getTokens().get("non_deterministic"));
        Assert.assertEquals("tok_d", s.getTokens().get("deterministic_string_tg"));
    }

    @Test
    public void testTokenizeSuccessToString() {
        TokenizeSuccess s = new TokenizeSuccess(0, "val");
        s.addToken("grp", "tok");
        String json = s.toString();
        Assert.assertTrue(json.contains("tok") || json.contains("grp"));
    }

    @Test
    public void testTokenizeSummaryGetters() {
        // totalTokens=10, totalTokenized=6, totalPartial=2, totalFailed=2  => 6+2+2=10
        TokenizeSummary summary = new TokenizeSummary(10, 6, 2, 2);
        Assert.assertEquals(10, summary.getTotalTokens());
        Assert.assertEquals(6, summary.getTotalTokenized());
        Assert.assertEquals(2, summary.getTotalPartial());
        Assert.assertEquals(2, summary.getTotalFailed());
        Assert.assertEquals(summary.getTotalTokens(),
                summary.getTotalTokenized() + summary.getTotalPartial() + summary.getTotalFailed());
    }

    @Test
    public void testTokenizeSummaryToString() {
        TokenizeSummary summary = new TokenizeSummary(5, 3, 1, 1);
        String json = summary.toString();
        Assert.assertNotNull(json);
        Assert.assertFalse(json.isEmpty());
        Assert.assertTrue(json.contains("totalPartial"));
    }

    @Test
    public void testPartialSuccessDoesNotCountAsFailure() {
        // Record 0: 1 success + 1 error → partial
        // Record 1: 2 successes → fully tokenized
        // Record 2: 0 successes, 1 error → fully failed
        TokenizeSuccess s0 = new TokenizeSuccess(0, "test");
        s0.addToken("deterministic_string_tg", "tok_d");

        TokenizeSuccess s1 = new TokenizeSuccess(1, "sachin22");
        s1.addToken("non_deterministic", "tok_nd");
        s1.addToken("deterministic_string_tg", "tok_d2");

        List<TokenizeSuccess> success = Arrays.asList(s0, s1);
        List<ErrorRecord> errors = Arrays.asList(
                new ErrorRecord(0, "Token group non_deterministi is invalid", 400),
                new ErrorRecord(2, "Token group xyz is invalid", 400)
        );
        List<TokenizeRecord> originalPayload = Arrays.asList(
                TokenizeRecord.builder().value("test").tokenGroupNames(Arrays.asList("non_deterministi", "deterministic_string_tg")).build(),
                TokenizeRecord.builder().value("sachin22").tokenGroupNames(Arrays.asList("non_deterministic", "deterministic_string_tg")).build(),
                TokenizeRecord.builder().value("other").tokenGroupNames(Collections.singletonList("xyz")).build()
        );
        TokenizeResponse response = new TokenizeResponse(success, errors, originalPayload);

        Assert.assertEquals(3, response.getSummary().getTotalTokens());
        Assert.assertEquals(1, response.getSummary().getTotalTokenized());  // only record 1 (all groups succeeded)
        Assert.assertEquals(1, response.getSummary().getTotalPartial());    // record 0 (some succeeded, some failed)
        Assert.assertEquals(1, response.getSummary().getTotalFailed());     // record 2 (all failed)
    }
}
