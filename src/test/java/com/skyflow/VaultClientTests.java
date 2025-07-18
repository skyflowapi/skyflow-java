package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.*;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.resources.files.FilesClient;
import com.skyflow.generated.rest.resources.files.requests.*;
import com.skyflow.generated.rest.resources.query.QueryClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBatchOperationBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceInsertRecordBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceUpdateRecordBody;
import com.skyflow.generated.rest.resources.strings.StringsClient;
import com.skyflow.generated.rest.resources.strings.requests.DeidentifyStringRequest;
import com.skyflow.generated.rest.resources.strings.requests.ReidentifyStringRequest;
import com.skyflow.generated.rest.resources.tokens.TokensClient;
import com.skyflow.generated.rest.resources.tokens.requests.V1DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.V1TokenizePayload;
import com.skyflow.generated.rest.types.DeidentifyStringResponse;
import com.skyflow.generated.rest.types.DetectedEntity;
import com.skyflow.generated.rest.types.EntityLocation;
import com.skyflow.generated.rest.types.V1Byot;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.vault.data.FileUploadRequest;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.DeidentifyTextRequest;
import com.skyflow.vault.detect.*;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.*;

public class VaultClientTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static VaultClient vaultClient;
    private static String vaultID = null;
    private static String clusterID = null;
    private static String token = null;
    private static String table = null;
    private static String value = null;
    private static String columnGroup = null;
    private static String apiKey = "sky-ab123-abcd1234cdef1234abcd4321cdef4321";
    private static ArrayList<DetokenizeData> detokenizeData = null;
    private static ArrayList<HashMap<String, Object>> insertValues = null;
    private static ArrayList<HashMap<String, Object>> insertTokens = null;
    private static HashMap<String, Object> valueMap = null;
    private static HashMap<String, Object> tokenMap = null;
    private static VaultConfig vaultConfig;

    @BeforeClass
    public static void setup() throws SkyflowException {
        vaultID = "vault123";
        clusterID = "cluster123";
        token = "test_token";
        detokenizeData = new ArrayList<>();
        table = "test_table";
        value = "test_value";
        columnGroup = "test_column_group";
        apiKey = null;
        insertValues = new ArrayList<>();
        insertTokens = new ArrayList<>();
        valueMap = new HashMap<>();
        tokenMap = new HashMap<>();

        vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.PROD);

        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        vaultConfig.setCredentials(credentials);
        vaultClient = new VaultClient(vaultConfig, credentials);
        vaultClient.setBearerToken();
    }

    @Test
    public void testVaultClientGetRecordsAPI() {
        try {
            RecordsClient recordsClient = vaultClient.getRecordsApi();
            Assert.assertNotNull(recordsClient);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN + e.getMessage());
        }
    }

    @Test
    public void testVaultClientDetectAPI() {
        try {
            FilesClient filesClient = vaultClient.getDetectFileAPi();
            Assert.assertNotNull(filesClient);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN + e.getMessage());
        }
    }

    @Test
    public void testVaultClientDetectTextAPI() {
        try {
            StringsClient stringsClient = vaultClient.getDetectTextApi();
            Assert.assertNotNull(stringsClient);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN + e.getMessage());
        }
    }

    @Test
    public void testVaultClientGetTokensAPI() {
        try {
            TokensClient tokensClient = vaultClient.getTokensApi();
            Assert.assertNotNull(tokensClient);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testVaultClientGetQueryAPI() {
        try {
            QueryClient queryClient = vaultClient.getQueryApi();
            Assert.assertNotNull(queryClient);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testVaultClientGetVaultConfig() {
        try {
            VaultConfig config = vaultClient.getVaultConfig();
            Assert.assertNotNull(config);
            Assert.assertEquals(vaultID, config.getVaultId());
            Assert.assertEquals(clusterID, config.getClusterId());
            Assert.assertEquals(Env.PROD, config.getEnv());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetDetokenizePayload() {
        try {
            DetokenizeData detokenizeDataRecord1 = new DetokenizeData(token);
            DetokenizeData detokenizeDataRecord2 = new DetokenizeData(token);
            detokenizeData.add(detokenizeDataRecord1);
            detokenizeData.add(detokenizeDataRecord2);
            DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                    .detokenizeData(detokenizeData)
                    .downloadURL(true)
                    .continueOnError(false)
                    .build();
            V1DetokenizePayload payload = vaultClient.getDetokenizePayload(detokenizeRequest);
            Assert.assertFalse(payload.getContinueOnError().get());
            Assert.assertTrue(payload.getDownloadUrl().get());
            Assert.assertEquals(2, payload.getDetokenizationParameters().get().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetBulkInsertRequestBody() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            insertValues.clear();
            insertValues.add(valueMap);
            insertValues.add(valueMap);
            insertTokens.clear();
            insertTokens.add(tokenMap);
            InsertRequest insertRequest1 = InsertRequest.builder()
                    .table(table)
                    .values(insertValues)
                    .tokens(insertTokens)
                    .tokenMode(TokenMode.ENABLE)
                    .returnTokens(true)
                    .build();
            RecordServiceInsertRecordBody body1 = vaultClient.getBulkInsertRequestBody(insertRequest1);
            Assert.assertTrue(body1.getTokenization().get());
            Assert.assertEquals(V1Byot.ENABLE, body1.getByot().get());
            Assert.assertEquals(2, body1.getRecords().get().size());

            InsertRequest insertRequest2 = InsertRequest.builder()
                    .table(table)
                    .values(insertValues)
                    .build();
            RecordServiceInsertRecordBody body2 = vaultClient.getBulkInsertRequestBody(insertRequest2);
            Assert.assertEquals(2, body2.getRecords().get().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetBatchInsertRequestBody() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            insertValues.clear();
            insertValues.add(valueMap);
            insertValues.add(valueMap);
            insertTokens.clear();
            insertTokens.add(tokenMap);
            InsertRequest insertRequest1 = InsertRequest.builder()
                    .table(table)
                    .values(insertValues)
                    .tokens(insertTokens)
                    .tokenMode(TokenMode.ENABLE)
                    .returnTokens(false)
                    .build();
            RecordServiceBatchOperationBody body1 = vaultClient.getBatchInsertRequestBody(insertRequest1);
            Assert.assertTrue(body1.getContinueOnError().get());
            Assert.assertEquals(2, body1.getRecords().get().size());

            InsertRequest insertRequest2 = InsertRequest.builder().table(table).values(insertValues).build();
            RecordServiceBatchOperationBody body2 = vaultClient.getBatchInsertRequestBody(insertRequest2);
            Assert.assertEquals(2, body2.getRecords().get().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetUpdateRequestBodyWithTokens() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            UpdateRequest updateRequest = UpdateRequest.builder()
                    .table(table)
                    .data(valueMap)
                    .tokens(tokenMap)
                    .returnTokens(true)
                    .build();
            RecordServiceUpdateRecordBody body = vaultClient.getUpdateRequestBody(updateRequest);
            Assert.assertTrue(body.getTokenization().get());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetUpdateRequestBodyWithoutTokens() {
        try {
            valueMap.clear();
            valueMap.put("test_column_1", "test_value_1");
            valueMap.put("test_column_2", "test_value_2");
            tokenMap.clear();
            tokenMap.put("test_column_1", "test_token_1");
            UpdateRequest updateRequest = UpdateRequest.builder()
                    .table(table)
                    .data(valueMap)
                    .returnTokens(false)
                    .build();
            RecordServiceUpdateRecordBody body = vaultClient.getUpdateRequestBody(updateRequest);
            Assert.assertFalse(body.getTokenization().get());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetTokenizePayload() {
        try {
            ColumnValue columnValue = ColumnValue.builder().value(value).columnGroup(columnGroup).build();
            List<ColumnValue> columnValues = new ArrayList<>();
            columnValues.add(columnValue);
            TokenizeRequest tokenizeRequest = TokenizeRequest.builder().values(columnValues).build();
            V1TokenizePayload payload = vaultClient.getTokenizePayload(tokenizeRequest);
            Assert.assertEquals(1, payload.getTokenizationParameters().get().size());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetUploadFileColumnNameWithInvalidBase64String() {
        try {
            String invalidbase64String = "invalidBase64String!";
            String columnName = "columnName";
            String skyflowId = "skyflowId";
            String fileName = "fileName.txt";
            FileUploadRequest request = FileUploadRequest.builder()
                    .tableName(table)
                    .columnName(columnName)
                    .skyflowId(skyflowId)
                    .base64(invalidbase64String)
                    .fileName(fileName)
                    .build();
            vaultClient.getUploadFileColumnName(request);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidBase64InFileUpload.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetUploadFileColumnNameWithValidBase64String() {
        try {
            String invalidbase64String = "YmFzZTY0RW5jb2RlZFN0cmluZw==";
            String columnName = "columnName";
            String skyflowId = "skyflowId";
            String fileName = "fileName.txt";
            FileUploadRequest request = FileUploadRequest.builder()
                    .tableName(table)
                    .columnName(columnName)
                    .skyflowId(skyflowId)
                    .base64(invalidbase64String)
                    .fileName(fileName)
                    .build();
            vaultClient.getUploadFileColumnName(request);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetUploadFileColumnNameWithInvalidFilePath() {
        try {
            String invalidFilePath = "invalid/file/path/file.txt";
            String columnName = "columnName";
            String skyflowId = "skyflowId";
            FileUploadRequest request = FileUploadRequest.builder()
                    .tableName(table)
                    .columnName(columnName)
                    .skyflowId(skyflowId)
                    .filePath(invalidFilePath)
                    .build();
            vaultClient.getUploadFileColumnName(request);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidFilePath.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetUploadFileColumnNameWithValidFile() {
        try {
            String columnName = "columnName";
            String skyflowId = "skyflowId";
            String filePath = "src/test/resources/notJson.txt";
            FileUploadRequest request = FileUploadRequest.builder()
                    .tableName(table)
                    .columnName(columnName)
                    .skyflowId(skyflowId)
                    .fileObject(new File(filePath))
                    .build();
            vaultClient.getUploadFileColumnName(request);
            Assert.assertTrue(request.getFileObject().exists());
            Assert.assertEquals("notJson.txt", request.getFileObject().getName());
        } catch (Exception e) {
            System.out.println(e);
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testSetBearerToken() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
            vaultConfig.setCredentials(credentials);
            vaultClient = new VaultClient(vaultConfig, credentials);
            vaultClient.setBearerToken();

            Assert.assertNotNull(vaultClient.getTokensApi());

            vaultClient.setBearerToken();
            Assert.assertNotNull(vaultClient.getTokensApi());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }

    @Test
    public void testSetBearerTokenWithApiKey() {
        try {
            Credentials credentials = new Credentials();
            credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321"); // Use a non-null dummy API key
            vaultConfig.setCredentials(credentials);
            vaultClient.updateVaultConfig();
            vaultClient.setCommonCredentials(credentials);

            // regular scenario
            vaultClient.setBearerToken();

            // re-use scenario
            vaultClient.setBearerToken();

            // If no exception is thrown, the test passes
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + ": " + e.getMessage());
        }
    }

    @Test
    public void testSetBearerTokenWithEnvCredentials() {
        try {
            Dotenv dotenv = Dotenv.load();
            vaultConfig.setCredentials(null);
            vaultClient.updateVaultConfig();
            vaultClient.setCommonCredentials(null);
            vaultClient.setBearerToken();
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), 400);
            Assert.assertNull(vaultClient.getVaultConfig().getCredentials());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testGetDeIdentifyTextResponse() {
        List<DetectedEntity> entities = new ArrayList<>();
        EntityLocation location = EntityLocation.builder()
                .startIndex(2)
                .endIndex(10)
                .startIndexProcessed(3)
                .endIndexProcessed(8)
                .build();

        DetectedEntity detectedEntity = DetectedEntity.builder()
                .token("token123")
                .value("value123")
                .location(location)
                .build();
        entities.add(detectedEntity);

        DeidentifyStringResponse response = DeidentifyStringResponse.builder()
                .processedText("processed text")
                .wordCount(2)
                .characterCount(13)
                .entities(entities)
                .build();


        DeidentifyTextResponse result = vaultClient.getDeIdentifyTextResponse(response);

        Assert.assertNotNull(result);
        Assert.assertEquals("processed text", result.getProcessedText());
        Assert.assertEquals(2, result.getWordCount());
        Assert.assertEquals(13, result.getCharCount());
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
        Assert.assertEquals("token123", result.getEntities().get(0).getToken());
        Assert.assertEquals("value123", result.getEntities().get(0).getValue());
        Assert.assertEquals(2, result.getEntities().get(0).getTextIndex().getStart());
        Assert.assertEquals(10, result.getEntities().get(0).getTextIndex().getEnd());
        Assert.assertEquals(3, result.getEntities().get(0).getProcessedIndex().getStart());
        Assert.assertEquals(8, result.getEntities().get(0).getProcessedIndex().getEnd());
    }

    @Test
    public void testGetDeidentifyStringRequest() {

        List<DetectEntities> detectEntitiesList = new ArrayList<>();
        detectEntitiesList.add(DetectEntities.NAME);

        List<DetectEntities> vaultTokenList = new ArrayList<>();
        vaultTokenList.add(DetectEntities.SSN);


        List<DetectEntities> entityOnlyList = new ArrayList<>();
        entityOnlyList.add(DetectEntities.DOB);

        List<DetectEntities> entityUniqueCounterList = new ArrayList<>();
        entityUniqueCounterList.add(DetectEntities.NAME);


        List<String> restrictRegexList = new ArrayList<>();
        restrictRegexList.add("([0-9]{3}-[0-9]{2}-[0-9]{4})");

        TokenFormat tokenFormat = TokenFormat.builder()
                .vaultToken(vaultTokenList)
                .entityOnly(entityOnlyList)
                .entityUniqueCounter(entityUniqueCounterList)
                .build();


        List<DetectEntities> detectEntitiesTransformationList = new ArrayList<>();
        detectEntitiesTransformationList.add(DetectEntities.DOB);
        detectEntitiesTransformationList.add(DetectEntities.DATE);

        DateTransformation dateTransformation = new DateTransformation(20, 5, detectEntitiesTransformationList);
        Transformations transformations = new Transformations(dateTransformation);


        DeidentifyTextRequest req = DeidentifyTextRequest.builder()
                .text("Sensitive data to deidentify, like Name: Joy SSN 123-45-6789 and DOB 01-01-2000.")
                .entities(detectEntitiesList)
                .restrictRegexList(restrictRegexList)
                .tokenFormat(tokenFormat)
                .transformations(transformations)
                .build();

    }

    @Test
    public void testDeidentifyFileRequestBuilderAndGetters() {
        File file = new File("testfile.txt");
        FileInput fileInput = FileInput.builder().file(file).build();
        DetectEntities entity = DetectEntities.NAME;
        String allowRegex = "^[A-Za-z]+$";
        String restrictRegex = "\\d+";
        TokenFormat tokenFormat = TokenFormat.builder().vaultToken(Collections.singletonList(entity)).build();
        Boolean outputProcessedImage = true;
        Boolean outputOcrText = true;
        MaskingMethod maskingMethod = MaskingMethod.BLACKBOX;
        Double pixelDensity = 300.0;
        Double maxResolution = 1024.0;
        Boolean outputProcessedAudio = false;
        DetectOutputTranscriptions outputTranscription = DetectOutputTranscriptions.TRANSCRIPTION;
        AudioBleep bleep = AudioBleep.builder().gain(20.0).build();
        String outputDirectory = "/tmp";
        Integer waitTime = 10;

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(Arrays.asList(entity))
                .allowRegexList(Collections.singletonList(allowRegex))
                .restrictRegexList(Collections.singletonList(restrictRegex))
                .tokenFormat(tokenFormat)
                .outputProcessedImage(outputProcessedImage)
                .outputOcrText(outputOcrText)
                .maskingMethod(maskingMethod)
                .pixelDensity(pixelDensity)
                .maxResolution(maxResolution)
                .outputProcessedAudio(outputProcessedAudio)
                .outputTranscription(outputTranscription)
                .bleep(bleep)
                .outputDirectory(outputDirectory)
                .waitTime(waitTime)
                .build();

        Assert.assertEquals(file, request.getFileInput().getFile());
        Assert.assertEquals(1, request.getEntities().size());
        Assert.assertEquals(allowRegex, request.getAllowRegexList().get(0));
        Assert.assertEquals(restrictRegex, request.getRestrictRegexList().get(0));
        Assert.assertEquals(tokenFormat, request.getTokenFormat());
        Assert.assertEquals(outputProcessedImage, request.getOutputProcessedImage());
        Assert.assertEquals(outputOcrText, request.getOutputOcrText());
        Assert.assertEquals(maskingMethod, request.getMaskingMethod());
        Assert.assertEquals(pixelDensity, request.getPixelDensity());
        Assert.assertEquals(maxResolution, request.getMaxResolution());
        Assert.assertEquals(outputProcessedAudio, request.getOutputProcessedAudio());
        Assert.assertEquals(outputTranscription, request.getOutputTranscription());
        Assert.assertEquals(bleep, request.getBleep());
        Assert.assertEquals(outputDirectory, request.getOutputDirectory());
        Assert.assertEquals(waitTime, request.getWaitTime());
    }

    @Test
    public void testDeidentifyFileRequestBuilderDefaults() {
        DeidentifyFileRequest request = DeidentifyFileRequest.builder().build();
        Assert.assertNull(request.getEntities());
        Assert.assertNull(request.getAllowRegexList());
        Assert.assertNull(request.getRestrictRegexList());
        Assert.assertNull(request.getTokenFormat());
        Assert.assertNull(request.getTransformations());
        Assert.assertEquals(false, request.getOutputProcessedImage());
        Assert.assertEquals(false, request.getOutputOcrText());
        Assert.assertNull(request.getMaskingMethod());
        Assert.assertNull(request.getPixelDensity());
        Assert.assertNull(request.getMaxResolution());
        Assert.assertEquals(false, request.getOutputProcessedAudio());
        Assert.assertNull(request.getOutputTranscription());
        Assert.assertNull(request.getBleep());
        Assert.assertNull(request.getOutputDirectory());
        Assert.assertNull(request.getWaitTime());
    }

    @Test
    public void testGetDeidentifyImageRequest() {
        File file = new File("test.jpg");
        FileInput fileInput = FileInput.builder().file(file).build();
        List<DetectEntities> entities = Arrays.asList(DetectEntities.NAME, DetectEntities.DOB);
        TokenFormat tokenFormat = TokenFormat.builder().entityOnly(entities).build();

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(entities)
                .tokenFormat(tokenFormat)
                .outputProcessedImage(true)
                .outputOcrText(true)
                .build();

        String vaultId = "vault123";
        String base64Content = "base64string";
        String format = "jpg";

        DeidentifyImageRequest imageRequest = vaultClient.getDeidentifyImageRequest(request, vaultId, base64Content, format);

        Assert.assertEquals(vaultId, imageRequest.getVaultId());
        Assert.assertEquals(base64Content, imageRequest.getFile().getBase64());
        Assert.assertEquals(format.toUpperCase(), imageRequest.getFile().getDataFormat().name());
    }

    @Test
    public void testGetDeidentifyPresentationRequest() {
        File file = new File("test.pptx");
        FileInput fileInput = FileInput.builder().file(file).build();
        List<DetectEntities> entities = Arrays.asList(DetectEntities.NAME);
        TokenFormat tokenFormat = TokenFormat.builder().entityOnly(entities).build();

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(entities)
                .tokenFormat(tokenFormat)
                .build();

        String vaultId = "vault123";
        String base64Content = "base64string";
        String format = "pptx";

        DeidentifyPresentationRequest presRequest = vaultClient.getDeidentifyPresentationRequest(request, vaultId, base64Content, format);

        Assert.assertEquals(vaultId, presRequest.getVaultId());
        Assert.assertEquals(base64Content, presRequest.getFile().getBase64());
        Assert.assertEquals(format.toUpperCase(), presRequest.getFile().getDataFormat().name());
    }

    @Test
    public void testGetDeidentifySpreadsheetRequest() {
        File file = new File("test.csv");
        FileInput fileInput = FileInput.builder().file(file).build();
        List<DetectEntities> entities = Arrays.asList(DetectEntities.NAME);
        TokenFormat tokenFormat = TokenFormat.builder().entityOnly(entities).build();

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(entities)
                .tokenFormat(tokenFormat)
                .build();

        String vaultId = "vault123";
        String base64Content = "base64string";
        String format = "csv";

        DeidentifySpreadsheetRequest spreadsheetRequest = vaultClient.getDeidentifySpreadsheetRequest(request, vaultId, base64Content, format);

        Assert.assertEquals(vaultId, spreadsheetRequest.getVaultId());
        Assert.assertEquals(base64Content, spreadsheetRequest.getFile().getBase64());
        Assert.assertEquals(format.toUpperCase(), spreadsheetRequest.getFile().getDataFormat().name());
    }

    @Test
    public void testGetDeidentifyStructuredTextRequest() {
        File file = new File("test.json");
        FileInput fileInput = FileInput.builder().file(file).build();
        List<DetectEntities> entities = Arrays.asList(DetectEntities.NAME);
        TokenFormat tokenFormat = TokenFormat.builder().entityOnly(entities).build();

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(entities)
                .tokenFormat(tokenFormat)
                .build();

        String vaultId = "vault123";
        String base64Content = "base64string";
        String format = "json";

        DeidentifyStructuredTextRequest structuredTextRequest = vaultClient.getDeidentifyStructuredTextRequest(request, vaultId, base64Content, format);

        Assert.assertEquals(vaultId, structuredTextRequest.getVaultId());
        Assert.assertEquals(base64Content, structuredTextRequest.getFile().getBase64());
        Assert.assertEquals(format.toUpperCase(), structuredTextRequest.getFile().getDataFormat().name());
    }

    @Test
    public void testGetDeidentifyDocumentRequest() {
        File file = new File("test.docx");
        FileInput fileInput = FileInput.builder().file(file).build();
        List<DetectEntities> entities = Arrays.asList(DetectEntities.NAME);
        TokenFormat tokenFormat = TokenFormat.builder().entityOnly(entities).build();

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(entities)
                .tokenFormat(tokenFormat)
                .build();

        String vaultId = "vault123";
        String base64Content = "base64string";
        String format = "docx";

        DeidentifyDocumentRequest documentRequest = vaultClient.getDeidentifyDocumentRequest(request, vaultId, base64Content, format);

        Assert.assertEquals(vaultId, documentRequest.getVaultId());
        Assert.assertEquals(base64Content, documentRequest.getFile().getBase64());
        Assert.assertEquals(format.toUpperCase(), documentRequest.getFile().getDataFormat().name());
    }

    @Test
    public void testGetDeidentifyPdfRequest() {
        File file = new File("test.pdf");
        FileInput fileInput = FileInput.builder().file(file).build();
        List<DetectEntities> entities = Arrays.asList(DetectEntities.NAME);
        TokenFormat tokenFormat = TokenFormat.builder().entityOnly(entities).build();

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(entities)
                .tokenFormat(tokenFormat)
                .pixelDensity(200)
                .maxResolution(300)
                .build();

        String vaultId = "vault123";
        String base64Content = "base64string";

        DeidentifyPdfRequest pdfRequest = vaultClient.getDeidentifyPdfRequest(request, vaultId, base64Content);

        Assert.assertEquals(vaultId, pdfRequest.getVaultId());
        Assert.assertEquals(base64Content, pdfRequest.getFile().getBase64());
    }

    @Test
    public void testGetDeidentifyAudioRequest() throws SkyflowException {
        File file = new File("test.mp3");
        FileInput fileInput = FileInput.builder().file(file).build();
        List<DetectEntities> entities = Arrays.asList(DetectEntities.NAME);
        TokenFormat tokenFormat = TokenFormat.builder().entityOnly(entities).build();
        AudioBleep bleep = AudioBleep.builder().frequency(1000.0).gain(10.0).startPadding(1.0).stopPadding(1.0).build();

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(entities)
                .tokenFormat(tokenFormat)
                .bleep(bleep)
                .outputProcessedAudio(true)
                .outputTranscription(DetectOutputTranscriptions.PLAINTEXT_TRANSCRIPTION)
                .build();

        String vaultId = "vault123";
        String base64Content = "base64string";
        String dataFormat = "mp3";

        DeidentifyAudioRequest audioRequest = vaultClient.getDeidentifyAudioRequest(request, vaultId, base64Content, dataFormat);

        Assert.assertEquals(vaultId, audioRequest.getVaultId());
        Assert.assertEquals(base64Content, audioRequest.getFile().getBase64());
        Assert.assertEquals(dataFormat, audioRequest.getFile().getDataFormat().toString());
    }

    @Test
    public void testGetDeidentifyTextFileRequest() {
        File file = new File("test.txt");
        FileInput fileInput = FileInput.builder().file(file).build();
        List<DetectEntities> entities = Arrays.asList(DetectEntities.NAME, DetectEntities.DOB);
        TokenFormat tokenFormat = TokenFormat.builder().entityOnly(entities).build();

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(entities)
                .tokenFormat(tokenFormat)
                .build();

        String vaultId = "vault123";
        String base64Content = "base64string";

        com.skyflow.generated.rest.resources.files.requests.DeidentifyTextRequest textRequest =
                vaultClient.getDeidentifyTextFileRequest(request, vaultId, base64Content);

        Assert.assertEquals(vaultId, textRequest.getVaultId());
        Assert.assertEquals(base64Content, textRequest.getFile().getBase64());
    }

    @Test
    public void testGetDeidentifyStringRequest_AllTokenFormatFields() throws Exception {
        List<DetectEntities> entities = Arrays.asList(DetectEntities.DOB);
        List<DetectEntities> vaultToken = Collections.singletonList(DetectEntities.SSN);
        List<DetectEntities> entityOnly = Collections.singletonList(DetectEntities.DOB);
        List<DetectEntities> entityUniqueCounter = Collections.singletonList(DetectEntities.NAME);

        TokenFormat tokenFormat = TokenFormat.builder()
                .vaultToken(vaultToken)
                .entityOnly(entityOnly)
                .entityUniqueCounter(entityUniqueCounter)
                .build();

        List<String> allowRegex = Collections.singletonList("a.*");
        List<String> restrictRegex = Collections.singletonList("b.*");

        DateTransformation dateTransformation = new DateTransformation(10, 5, entities);
        Transformations transformations = new Transformations(dateTransformation);

        DeidentifyTextRequest req = DeidentifyTextRequest.builder()
                .text("Sensitive data")
                .entities(entities)
                .allowRegexList(allowRegex)
                .restrictRegexList(restrictRegex)
                .tokenFormat(tokenFormat)
                .transformations(transformations)
                .build();

        DeidentifyStringRequest result = vaultClient.getDeidentifyStringRequest(req, "vaultId");
        Assert.assertNotNull(result);
        Assert.assertEquals("vaultId", result.getVaultId());
        Assert.assertEquals("Sensitive data", result.getText());
        Assert.assertTrue(result.getAllowRegex().isPresent());
        Assert.assertTrue(result.getRestrictRegex().isPresent());
        Assert.assertTrue(result.getTransformations().isPresent());
    }

    @Test
    public void testGetDeidentifyStringRequest_NullTokenFormatAndEntities() throws Exception {
        DeidentifyTextRequest req = DeidentifyTextRequest.builder()
                .text("No entities or tokenFormat")
                .build();

        DeidentifyStringRequest result = vaultClient.getDeidentifyStringRequest(req, "vaultId");
        Assert.assertNotNull(result);
        Assert.assertEquals("vaultId", result.getVaultId());
        Assert.assertEquals("No entities or tokenFormat", result.getText());
    }

    @Test
    public void testGetReidentifyStringRequest_AllFields() throws Exception {
        List<DetectEntities> masked = Arrays.asList(DetectEntities.NAME, DetectEntities.DOB);
        List<DetectEntities> plaintext = Collections.singletonList(DetectEntities.SSN);
        List<DetectEntities> redacted = Collections.singletonList(DetectEntities.DATE);

        ReidentifyTextRequest req = ReidentifyTextRequest.builder()
                .text("Sensitive data")
                .maskedEntities(masked)
                .plainTextEntities(plaintext)
                .redactedEntities(redacted)
                .build();

        ReidentifyStringRequest result = vaultClient.getReidentifyStringRequest(req, "vaultId");
        Assert.assertNotNull(result);
        Assert.assertEquals("vaultId", result.getVaultId());
        Assert.assertEquals("Sensitive data", result.getText());
        Assert.assertNotNull(result.getFormat());
    }

    @Test
    public void testGetReidentifyStringRequest_NullFields() throws Exception {
        ReidentifyTextRequest req = ReidentifyTextRequest.builder()
                .text("No entities")
                .build();

        ReidentifyStringRequest result = vaultClient.getReidentifyStringRequest(req, "vaultId");
        Assert.assertNotNull(result);
        Assert.assertEquals("vaultId", result.getVaultId());
        Assert.assertEquals("No entities", result.getText());
        Assert.assertNotNull(result.getFormat());
    }

    @Test
    public void testGetTransformations_NullInput() throws Exception {
        // Should return null if input is null
        java.lang.reflect.Method method = VaultClient.class.getDeclaredMethod("getTransformations", Transformations.class);
        method.setAccessible(true);
        Object result = method.invoke(vaultClient, new Object[]{null});
        Assert.assertNull(result);
    }

    @Test
    public void testGetTransformations_NullShiftDates() throws Exception {
        Transformations transformations = new Transformations(null);
        java.lang.reflect.Method method = VaultClient.class.getDeclaredMethod("getTransformations", Transformations.class);
        method.setAccessible(true);
        Object result = method.invoke(vaultClient, transformations);
        Assert.assertNull(result);
    }

    @Test
    public void testGetTransformations_EmptyEntities() throws Exception {
        DateTransformation dateTransformation = new DateTransformation(10, 5, new ArrayList<>());
        Transformations transformations = new Transformations(dateTransformation);
        java.lang.reflect.Method method = VaultClient.class.getDeclaredMethod("getTransformations", Transformations.class);
        method.setAccessible(true);
        Object result = method.invoke(vaultClient, transformations);
        Assert.assertNotNull(result);
        // Should have empty entityTypes list
        com.skyflow.generated.rest.types.Transformations restTransform = (com.skyflow.generated.rest.types.Transformations) result;
        Assert.assertTrue(restTransform.getShiftDates().get().getEntityTypes().get().isEmpty());
    }

    @Test
    public void testGetTransformations_WithEntities() throws Exception {
        List<DetectEntities> entities = Arrays.asList(DetectEntities.DOB, DetectEntities.DATE);
        DateTransformation dateTransformation = new DateTransformation(20, 5, entities);
        Transformations transformations = new Transformations(dateTransformation);
        java.lang.reflect.Method method = VaultClient.class.getDeclaredMethod("getTransformations", Transformations.class);
        method.setAccessible(true);
        Object result = method.invoke(vaultClient, transformations);
        Assert.assertNotNull(result);
        com.skyflow.generated.rest.types.Transformations restTransform = (com.skyflow.generated.rest.types.Transformations) result;
        Assert.assertEquals(2, restTransform.getShiftDates().get().getEntityTypes().get().size());
    }

    @Test
    public void testGetDeidentifyGenericFileRequest_AllFields() {
        File file = new File("test.custom");
        FileInput fileInput = FileInput.builder().file(file).build();
        List<DetectEntities> entities = Arrays.asList(DetectEntities.NAME, DetectEntities.DOB);
        TokenFormat tokenFormat = TokenFormat.builder().entityOnly(entities).build();

        DeidentifyFileRequest request = DeidentifyFileRequest.builder()
                .file(fileInput)
                .entities(entities)
                .tokenFormat(tokenFormat)
                .allowRegexList(Arrays.asList("a.*"))
                .restrictRegexList(Arrays.asList("b.*"))
                .build();

        String vaultId = "vault123";
        String base64Content = "base64string";
        String fileExtension = "txt";

        com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequest genericRequest =
                vaultClient.getDeidentifyGenericFileRequest(request, vaultId, base64Content, fileExtension);

        Assert.assertEquals(vaultId, genericRequest.getVaultId());
        Assert.assertEquals(base64Content, genericRequest.getFile().getBase64());
        Assert.assertNotNull(genericRequest.getEntityTypes());
        Assert.assertNotNull(genericRequest.getTokenType());
        Assert.assertTrue(genericRequest.getAllowRegex().isPresent());
        Assert.assertTrue(genericRequest.getRestrictRegex().isPresent());
    }

    @Test
    public void testMapAudioDataFormat_mp3() throws Exception {
        java.lang.reflect.Method method = VaultClient.class.getDeclaredMethod("mapAudioDataFormat", String.class);
        method.setAccessible(true);
        Object result = method.invoke(vaultClient, "mp3");
        Assert.assertEquals(com.skyflow.generated.rest.resources.files.types.DeidentifyAudioRequestFileDataFormat.MP_3, result);
    }

    @Test
    public void testMapAudioDataFormat_wav() throws Exception {
        java.lang.reflect.Method method = VaultClient.class.getDeclaredMethod("mapAudioDataFormat", String.class);
        method.setAccessible(true);
        Object result = method.invoke(vaultClient, "wav");
        Assert.assertEquals(com.skyflow.generated.rest.resources.files.types.DeidentifyAudioRequestFileDataFormat.WAV, result);
    }

    @Test
    public void testMapAudioDataFormat_invalid() throws Exception {
        java.lang.reflect.Method method = VaultClient.class.getDeclaredMethod("mapAudioDataFormat", String.class);
        method.setAccessible(true);
        try {
            method.invoke(vaultClient, "ogg");
            Assert.fail("Should throw SkyflowException for invalid audio type");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof SkyflowException);
        }
    }

    @Test
    public void testPrioritiseCredentials_VaultConfigCredentials() throws Exception {
        Credentials creds = new Credentials();
        creds.setApiKey("test_api_key");
        vaultConfig.setCredentials(creds);

        java.lang.reflect.Method method = VaultClient.class.getDeclaredMethod("prioritiseCredentials");
        method.setAccessible(true);
        method.invoke(vaultClient);

        Assert.assertEquals(creds, getPrivateField(vaultClient, "finalCredentials"));
    }

    @Test
    public void testPrioritiseCredentials_CommonCredentials() throws Exception {
        vaultConfig.setCredentials(null);
        Credentials creds = new Credentials();
        creds.setApiKey("common_api_key");
        setPrivateField(vaultClient, "commonCredentials", creds);

        java.lang.reflect.Method method = VaultClient.class.getDeclaredMethod("prioritiseCredentials");
        method.setAccessible(true);
        method.invoke(vaultClient);

        Assert.assertEquals(creds, getPrivateField(vaultClient, "finalCredentials"));
    }

    // Helper methods for reflection field access
    private Object getPrivateField(Object obj, String fieldName) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
