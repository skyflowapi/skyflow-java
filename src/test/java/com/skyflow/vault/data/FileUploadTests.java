package com.skyflow.vault.data;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class FileUploadTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String vaultID = null;
    private static String clusterID = null;
    private static String skyflowID = null;
    private static String tableName = null;
    private static String columnName = null;
    private static String filePath = null;
    private static String fileName = null;
    private static File testFile = null;
    private static String validbase64String = "YmFzZTY0RW5jb2RlZFN0cmluZw==";

    @BeforeClass
    public static void setup() {
        vaultID = "vault123";
        clusterID = "cluster123";

        Credentials credentials = new Credentials();
        credentials.setToken("valid-token");

        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(vaultID);
        vaultConfig.setClusterId(clusterID);
        vaultConfig.setEnv(Env.DEV);
        skyflowID = "test_file_upload_id_1";
        tableName = "test_table";
        columnName = "profile_image";
        fileName = "notJson.txt";
        filePath = "src/test/resources/" + fileName;
        testFile = new File(filePath);
    }

    @Test
    public void testValidFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .fileObject(testFile)
                    .tableName(tableName)
                    .columnName(columnName)
                    .skyflowId(skyflowID)
                    .fileName(fileName)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.assertEquals(tableName, request.getTableName());
            Assert.assertEquals(columnName, request.getColumnName());
            Assert.assertEquals(testFile, request.getFileObject());
            Assert.assertEquals(fileName, request.getFileName());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN + " " + e);
        }
    }

    @Test
    public void testNullRequestInFileUploadRequestValidations() {
        try {
            Validations.validateFileUploadRequest(null);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidFileUploadRequest.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoTableInFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .fileObject(testFile)
                    .columnName(columnName)
                    .fileName(fileName)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableNameKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyTableInFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .fileObject(testFile)
                    .tableName("")
                    .columnName(columnName)
                    .fileName(fileName)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyTableName.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoColumnInFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .fileObject(testFile)
                    .tableName(tableName)
                    .skyflowId(skyflowID)
                    .fileName(fileName)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.ColumnNameKeyErrorInFileUpload.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptyColumnInFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .fileObject(testFile)
                    .tableName(tableName)
                    .columnName("")
                    .skyflowId(skyflowID)
                    .fileName(fileName)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyColumnNameInFileUpload.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoSkyflowIdInFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .fileObject(testFile)
                    .tableName(tableName)
                    .columnName(columnName)
                    .fileName(fileName)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.SkyflowIdKeyErrorInFileUpload.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptySkyflowIdInFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .fileObject(testFile)
                    .tableName(tableName)
                    .columnName(columnName)
                    .fileName(fileName)
                    .skyflowId("")
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptySkyflowIdInFileUpload.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoFileInUploadRequestValidations() {
        FileUploadRequest request = FileUploadRequest.builder()
                .tableName(tableName)
                .columnName(columnName)
                .skyflowId(skyflowID)
                .fileName(fileName)
                .build();
        try {
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.MissingFileSourceInFileUpload.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testMultipleFileSourcesInFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .fileObject(testFile)
                    .filePath(filePath)
                    .tableName(tableName)
                    .columnName(columnName)
                    .fileName(fileName)
                    .skyflowId(skyflowID)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.MultipleFileSourcesInFileUpload.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testNoFileNameForBase64InFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .base64(validbase64String)
                    .tableName(tableName)
                    .columnName(columnName)
                    .skyflowId(skyflowID)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.MissingFileNameForBase64.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testInvalidFileObjectInFileUploadRequestValidations() {
        try {
            File invalidFile = new File("nonexistent.txt");
            FileUploadRequest request = FileUploadRequest.builder()
                    .fileObject(invalidFile)
                    .tableName(tableName)
                    .columnName(columnName)
                    .fileName(fileName)
                    .skyflowId(skyflowID)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.InvalidFileObjectInFileUpload.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testValidBase64InFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .base64(validbase64String)
                    .tableName(tableName)
                    .columnName(columnName)
                    .fileName(fileName)
                    .skyflowId(skyflowID)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.assertEquals(tableName, request.getTableName());
            Assert.assertEquals(columnName, request.getColumnName());
            Assert.assertEquals(fileName, request.getFileName());
            Assert.assertEquals(validbase64String, request.getBase64());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidFilePathInFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .filePath(filePath)
                    .tableName(tableName)
                    .columnName(columnName)
                    .skyflowId(skyflowID)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.assertEquals(tableName, request.getTableName());
            Assert.assertEquals(columnName, request.getColumnName());
            Assert.assertEquals(filePath, request.getFilePath());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testEmptyFilePathInFileUploadRequestValidations() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .filePath("")
                    .tableName(tableName)
                    .columnName(columnName)
                    .skyflowId(skyflowID)
                    .build();
            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptyFilePathInFileUpload.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testFileUploadResponse() {
        try {
            FileUploadResponse response = new FileUploadResponse(skyflowID);
            String responseString = "{\"skyflowId\":\"" + skyflowID + "\",\"errors\":null}";
            Assert.assertEquals(skyflowID, response.getSkyflowId());
            Assert.assertEquals(responseString, response.toString());
        } catch (Exception e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
}
