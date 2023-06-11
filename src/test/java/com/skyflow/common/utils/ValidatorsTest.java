package com.skyflow.common.utils;


import com.skyflow.entities.GetOptions;
import com.skyflow.entities.GetRecordInput;
import com.skyflow.entities.RedactionType;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = "com.skyflow.common.utils.TokenUtils")
public class ValidatorsTest {
    private static String tableName = null;
    private static String columnName = null;
    private static String[] columnValue = new String[1];
    private static String[] ids =  new String[1];
    private static String INVALID_EXCEPTION_THROWN = "Should not have thrown any exception";

    @BeforeClass
    public static void setup() throws SkyflowException {
        PowerMockito.mockStatic(TokenUtils.class);
        PowerMockito.when(TokenUtils.isTokenValid("valid_token")).thenReturn(true);
        PowerMockito.when(TokenUtils.isTokenValid("not_a_valid_token")).thenReturn(false);

        tableName = "account_details";
        columnName = "card_number";
        columnValue[0] = "123451234554321";
        ids[0] = "123451234554321";
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void validateGetRequestRecordValidTest() {
        //column values with redaction
        GetRecordInput recordInput = new GetRecordInput();

        recordInput.setTable(tableName);
        recordInput.setColumnValues(columnValue);
        recordInput.setColumnName(columnName);
        recordInput.setRedaction(RedactionType.PLAIN_TEXT);
        try {
            Validators.validateGetRequestRecord(recordInput, new GetOptions(false));
        } catch (SkyflowException exception) {
            exception.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void validateGetRequestRecordValidIdTest() {
        //Id values with redaction

        GetRecordInput recordInput = new GetRecordInput();
        recordInput.setTable(tableName);
        recordInput.setIds(ids);
        recordInput.setRedaction(RedactionType.PLAIN_TEXT);
        try {
            Validators.validateGetRequestRecord(recordInput, new GetOptions(false));
        } catch (SkyflowException exception) {
            exception.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
        // missing redaction when token is false
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void validateGetRequestRecordMisiingRedaction() {
        //Id values with redaction

        GetRecordInput recordInput = new GetRecordInput();
        recordInput.setTable(tableName);
        recordInput.setIds(ids);
        try {
            Validators.validateGetRequestRecord(recordInput, new GetOptions(false));
        } catch (SkyflowException exception) {
            Assert.assertTrue(ErrorCode.MissingRedaction.getDescription().contains(exception.getMessage()));
            exception.printStackTrace();
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void validateGetRequestRecordValidIdTokenTrueTest() throws SkyflowException {
        //Id values with redaction

        GetRecordInput recordInput = new GetRecordInput();
        recordInput.setTable(tableName);
        recordInput.setIds(ids);
        try {
            Validators.validateGetRequestRecord(recordInput, new GetOptions(true));
        } catch (SkyflowException exception) {
            exception.printStackTrace();
            Assert.fail(INVALID_EXCEPTION_THROWN);
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void validateGetRequestRecordIdRedactionTokenTest() throws SkyflowException {
        //Id values with redaction

        GetRecordInput recordInput = new GetRecordInput();
        recordInput.setTable(tableName);
        recordInput.setIds(ids);
        recordInput.setRedaction(RedactionType.PLAIN_TEXT);

        try {
            Validators.validateGetRequestRecord(recordInput, new GetOptions(true));
        } catch (SkyflowException e) {
            Assert.assertTrue(ErrorCode.RedactionWithTokenNotSupported.getDescription().contains(e.getMessage()));
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void validateGetRequestRecordColumnRedactionTokenTest() {
        //Id values with redaction
        GetRecordInput recordInput = new GetRecordInput();
        recordInput.setTable(tableName);
        recordInput.setColumnName(columnName);
        recordInput.setColumnValues(columnValue);
        recordInput.setRedaction(RedactionType.PLAIN_TEXT);

        try {
            Validators.validateGetRequestRecord(recordInput, new GetOptions(true));
        } catch (SkyflowException e) {
            Assert.assertTrue(ErrorCode.TokensGetColumnNotSupported.getDescription().contains(e.getMessage()));
        }
    }
    @Test
    @PrepareForTest(fullyQualifiedNames = {"com.skyflow.common.utils.HttpUtility", "com.skyflow.common.utils.TokenUtils"})
    public void validateGetRecordColumnNameIDSBothSpecified() {
        GetRecordInput recordInput = new GetRecordInput();
        recordInput.setTable(tableName);
        recordInput.setIds(ids);
        recordInput.setColumnName(columnName);
        recordInput.setColumnValues(columnValue);
        recordInput.setRedaction(RedactionType.PLAIN_TEXT);

        try {
            Validators.validateGetRequestRecord(recordInput, new GetOptions(false));
        } catch (SkyflowException e) {
            Assert.assertTrue(ErrorCode.SkyflowIdAndColumnNameBothSpecified.getDescription().contains(e.getMessage()));
        }
    }
    @Test
    public void getOptionsTest(){
        GetOptions options = new GetOptions();
        Assert.assertFalse(options.getOptionToken());
    }
    @Test
    public void getOptionsTestWhenTokenTrue(){
        GetOptions options = new GetOptions(true);
        Assert.assertTrue(options.getOptionToken());
    }

}
