package com.skyflow.vault.data;

import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.validations.Validations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class FileUploadTests {
    private static final String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";
    private static final String EXCEPTION_NOT_THROWN = "Should have thrown an exception";
    private static String table;
    private static String skyflowId;
    private static String columnName;
    private static String filePath;
    private static String base64Content;
    private static File fileObject;
    private static String fileName;

    @BeforeClass
    public static void setup() {
        table = "test_table";
        skyflowId = "test_id";
        columnName = "file_column";
        filePath = "src/test/resources/notJson.txt";
        base64Content = "SGVsbG8gV29ybGQ=";
        fileObject = new File(filePath);
        fileName = "notJson.txt";
    }

    @Test
    public void testValidFileUploadRequestWithFilePath() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .table(table)
                    .skyflowId(skyflowId)
                    .columnName(columnName)
                    .filePath(filePath)
                    .fileName(fileName)
                    .build();

            Validations.validateFileUploadRequest(request);
            Assert.assertEquals(table, request.getTable());
            Assert.assertEquals(skyflowId, request.getSkyflowId());
            Assert.assertEquals(columnName, request.getColumnName());
            Assert.assertEquals(filePath, request.getFilePath());
            Assert.assertEquals(fileName, request.getFileName());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidFileUploadRequestWithBase64() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .table(table)
                    .skyflowId(skyflowId)
                    .columnName(columnName)
                    .base64(base64Content)
                    .fileName(fileName)
                    .build();

            Validations.validateFileUploadRequest(request);
            Assert.assertEquals(base64Content, request.getBase64());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testValidFileUploadRequestWithFileObject() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .table(table)
                    .skyflowId(skyflowId)
                    .columnName(columnName)
                    .fileObject(fileObject)
                    .fileName(fileName)
                    .build();

            Validations.validateFileUploadRequest(request);
            Assert.assertEquals(fileObject, request.getFileObject());
        } catch (SkyflowException e) {
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }

    @Test
    public void testMissingTable() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .skyflowId(skyflowId)
                    .columnName(columnName)
                    .filePath(filePath)
                    .fileName(fileName)
                    .build();

            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.TableKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testMissingSkyflowId() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .table(table)
                    .columnName(columnName)
                    .fileName(fileName)
                    .build();

            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.SkyflowIdKeyError.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testEmptySkyflowId() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .table(table)
                    .skyflowId("")
                    .columnName(columnName)
                    .fileName(fileName)
                    .build();

            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.EmptySkyflowId.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testMissingColumnName() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .table(table)
                    .skyflowId(skyflowId)
                    .filePath(filePath)
                    .fileName(fileName)
                    .build();

            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.ColumnNameKeyErrorFileUpload.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testMissingFileData() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .table(table)
                    .skyflowId(skyflowId)
                    .columnName(columnName)
                    .fileName(fileName)
                    .build();

            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.MissingFileSourceInUploadFileRequest.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }

    @Test
    public void testMissingFileNameWithBase64Invalid() {
        try {
            FileUploadRequest request = FileUploadRequest.builder()
                    .table(table)
                    .skyflowId(skyflowId)
                    .columnName(columnName)
                    .base64(base64Content)
                    .build();

            Validations.validateFileUploadRequest(request);
            Assert.fail(EXCEPTION_NOT_THROWN);
        } catch (SkyflowException e) {
            Assert.assertEquals(ErrorCode.INVALID_INPUT.getCode(), e.getHttpCode());
            Assert.assertEquals(
                    Utils.parameterizedString(ErrorMessage.FileNameMustBeProvidedWithFileObject.getMessage(), Constants.SDK_PREFIX),
                    e.getMessage()
            );
        }
    }
    
}
