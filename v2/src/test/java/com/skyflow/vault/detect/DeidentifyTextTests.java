package com.skyflow.vault.detect;

import com.skyflow.enums.DetectEntities;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.skyflow.errors.ErrorMessage.InvalidTextInDeIdentify;

public class DeidentifyTextTests {

    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";

    private static final String text = "Sensitive data to deidentify";
    private static final List<DetectEntities> detectEntities = new ArrayList<>();
    private static final List<String> allowRegexList = new ArrayList<>();
    private static final List<String> restrictRegexList = new ArrayList<>();
    private static final TokenFormat tokenFormat = TokenFormat.builder()
            .vaultToken(detectEntities)
            .entityUniqueCounter(detectEntities)
            .entityOnly(detectEntities)
            .build();

    private static Transformations transformations = null;


    @BeforeClass
    public static void setup() {
        detectEntities.add(DetectEntities.NAME);
        detectEntities.add(DetectEntities.DOB);

        allowRegexList.add("^[A-Za-z]+$");
        restrictRegexList.add("([0-9]{3}-[0-9]{2}-[0-9]{4})");

        transformations = new Transformations(
                new DateTransformation(20, 5, detectEntities)
        );
    }


    @Test
    public void testValidInputInDeidentifyTextRequestValidations() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder()
                    .text(text)
                    .entities(detectEntities)
                    .allowRegexList(allowRegexList)
                    .restrictRegexList(restrictRegexList)
                    .tokenFormat(tokenFormat)
                    .transformations(transformations)
                    .build();

            Validations.validateDeidentifyTextRequest(request);
            Assert.assertEquals(detectEntities, request.getEntities());
            Assert.assertEquals(allowRegexList, request.getAllowRegexList());
            Assert.assertEquals(restrictRegexList, request.getRestrictRegexList());
            Assert.assertEquals(tokenFormat, request.getTokenFormat());
            Assert.assertEquals(transformations, request.getTransformations());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }


    @Test
    public void testNullTextInDeidentifyTextRequestValidations() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder().text(null).build();
            Validations.validateDeidentifyTextRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(InvalidTextInDeIdentify.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyTextInDeidentifyTextRequestValidations() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder().text("").build();
            Validations.validateDeidentifyTextRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(InvalidTextInDeIdentify.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testEmptyEntitiesInDeidentifyTextRequestValidations() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder()
                    .text(text)
                    .entities(new ArrayList<>())
                    .build();
            Validations.validateDeidentifyTextRequest(request);
            Assert.assertTrue(request.getEntities().isEmpty());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }


    @Test
    public void testNoEntitiesInDeidentifyTextRequestValidations() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder().text("").build();
            Validations.validateDeidentifyTextRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }


    @Test
    public void testEmptyAllowRegexListInDeidentifyTextRequestValidations() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder()
                    .text(text)
                    .allowRegexList(new ArrayList<>())
                    .build();
            Validations.validateDeidentifyTextRequest(request);
            Assert.assertTrue(request.getAllowRegexList().isEmpty());
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testEmptyRestrictRegexListInDeidentifyTextRequestValidations() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder()
                    .text(text)
                    .restrictRegexList(new ArrayList<>())
                    .build();
            Validations.validateDeidentifyTextRequest(request);
            Assert.assertTrue(request.getRestrictRegexList().isEmpty());
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }


    @Test
    public void testNullTokenFormatInDeidentifyTextRequestValidations() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder()
                    .text(text)
                    .entities(detectEntities)
                    .allowRegexList(allowRegexList)
                    .restrictRegexList(restrictRegexList)
                    .tokenFormat(null)
                    .build();
            Validations.validateDeidentifyTextRequest(request);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testNoTransformationsInDeidentifyTextRequestValidations() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder()
                    .text(text)
                    .entities(detectEntities)
                    .allowRegexList(allowRegexList)
                    .restrictRegexList(restrictRegexList)
                    .tokenFormat(tokenFormat)
                    .build();
            Validations.validateDeidentifyTextRequest(request);
            Assert.assertNull(request.getTransformations());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testNullOrEmptyTransformationsInDeidentifyTextRequestValidations() {
        try {
            DeidentifyTextRequest request = DeidentifyTextRequest.builder()
                    .text(text)
                    .entities(detectEntities)
                    .allowRegexList(allowRegexList)
                    .restrictRegexList(restrictRegexList)
                    .tokenFormat(tokenFormat)
                    .transformations(null)
                    .build();
            Validations.validateDeidentifyTextRequest(request);
            Assert.assertNull(request.getTransformations());
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
        }
    }

    @Test
    public void testDeidentifyResponse() {
        try {
            List<EntityInfo> entityInfos = new ArrayList<>();
            int wordCount = 5;
            int charCount = 30;
            DeidentifyTextResponse response = new DeidentifyTextResponse(text, entityInfos, wordCount, charCount);
            Assert.assertEquals(text, response.getProcessedText());
            Assert.assertEquals(wordCount, response.getWordCount());
            Assert.assertEquals(charCount, response.getCharCount());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testDeidentifyResponseGetEntitiesAndToString() {
        TextIndex ti = new TextIndex(0, 4);
        TextIndex pi = new TextIndex(5, 9);
        java.util.Map<String, Double> scores = new java.util.HashMap<>();
        scores.put("confidence", 0.95);
        EntityInfo ei = new EntityInfo("tok1", "John", ti, pi, "NAME", scores);

        List<EntityInfo> entities = new ArrayList<>();
        entities.add(ei);

        DeidentifyTextResponse response = new DeidentifyTextResponse(text, entities, 1, 10);
        Assert.assertEquals(entities, response.getEntities());
        String json = response.toString();
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains(text));
    }

    @Test
    public void testTextIndex() {
        TextIndex ti = new TextIndex(3, 7);
        Assert.assertEquals(3, ti.getStart());
        Assert.assertEquals(7, ti.getEnd());
        String json = ti.toString();
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("3"));
    }

    @Test
    public void testEntityInfoGetters() {
        TextIndex ti = new TextIndex(0, 4);
        TextIndex pi = new TextIndex(5, 9);
        java.util.Map<String, Double> scores = new java.util.HashMap<>();
        scores.put("confidence", 0.9);
        EntityInfo ei = new EntityInfo("tok1", "Alice", ti, pi, "NAME", scores);
        Assert.assertEquals("tok1", ei.getToken());
        Assert.assertEquals("Alice", ei.getValue());
        Assert.assertEquals(ti, ei.getTextIndex());
        Assert.assertEquals(pi, ei.getProcessedIndex());
        Assert.assertEquals("NAME", ei.getEntity());
        Assert.assertEquals(scores, ei.getScores());
    }

    @Test
    public void testTokenFormatGetters() {
        TokenFormat tf = TokenFormat.builder()
                .vaultToken(detectEntities)
                .entityUniqueCounter(detectEntities)
                .entityOnly(detectEntities)
                .build();
        Assert.assertEquals(detectEntities, tf.getVaultToken());
        Assert.assertEquals(detectEntities, tf.getEntityUniqueCounter());
        Assert.assertEquals(detectEntities, tf.getEntityOnly());
        Assert.assertNotNull(tf.getDefault());
    }

    @Test
    public void testTokenFormatDefaultTypeNull() {
        TokenFormat tf = TokenFormat.builder().defaultType(null).build();
        Assert.assertEquals(com.skyflow.enums.TokenType.ENTITY_UNIQUE_COUNTER, tf.getDefault());
    }

    @Test
    public void testTokenFormatDefaultTypeNonNull() {
        TokenFormat tf = TokenFormat.builder()
                .defaultType(com.skyflow.enums.TokenType.VAULT_TOKEN)
                .build();
        Assert.assertEquals(com.skyflow.enums.TokenType.VAULT_TOKEN, tf.getDefault());
    }
}
