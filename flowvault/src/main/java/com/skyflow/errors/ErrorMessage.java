package com.skyflow.errors;

import com.skyflow.utils.Constants;

/**
 * Validation messages specific to the flowvault SDK. Messages shared by every SDK live in
 * {@link BaseErrorMessage} in common; this enum holds only what common and skyvault do not use.
 */
public enum ErrorMessage {
    // Vault config
    EmptyVaultUrl("%s0 Initialization failed. Vault URL is empty. Specify a valid vault URL."),
    InvalidVaultUrlFormat("%s0 Initialization failed. Vault URL must start with 'https://'."),
    EitherVaultUrlOrClusterIdRequired("%s0 Initialization failed. Specify either 'clusterId' or 'vaultURL'."),
    // Insert / upsert
    TableSpecifiedInRequestAndRecordObject("%s0 Validation error. Table name cannot be specified at both the request and record levels. Please specify the table name in only one place."),
    TableNotSpecifiedInRequestAndRecordObject("%s0 Validation error. Table name is missing. Table name should be specified at one place either at the request level or record level. Please specify the table name at one place."),
    UpsertTableRequestAtRecordLevel("%s0 Validation error. Table name should be present at each record level when upsert is present at record level."),
    UpsertTableRequestAtRequestLevel("%s0 Validation error. Upsert should be present at each record level when table name is present at record level."),
    EmptyRecords("%s0 Validation error. 'records' can't be empty. Specify records."),
    EmptyKeyInRecords("%s0 Validation error. Invalid key in data in records. Specify a valid key."),
    EmptyValueInRecords("%s0 Validation error. Invalid value in records. Specify a valid value."),
    RecordsKeyError("%s0 Validation error. 'records' key is missing from the payload. Specify a 'records' key."),
    InvalidUpsertUpdateType("%s0 Validation error. Invalid upsert updateType. Specify either 'UPDATE' or 'REPLACE'."),
    EmptyUpsertValues("%s0 Validation error. Upsert column values can't be empty. Specify at least one upsert column."),
    RecordSizeExceedError("%s0 Maximum number of records exceeded. The limit is 100000."),
    InvalidRecord("%s0 Validation error. InsertRecord object in the list is invalid. Specify a valid InsertRecord object."),
    BothSingleTableFieldsAndRecordsSpecified("%s0 Validation error. Both single-table lookup fields ('table', 'ids', 'fields', 'uniqueValues', 'columnRedactions') and 'records' can't be specified. Either specify single-table fields or 'records'."),
    // Detokenize / delete tokens
    EmptyValues("%s0 Validation error. 'values' can't be empty. Specify values."),
    EmptyValueInValues("%s0 Validation error. Invalid value in values. Specify a valid value."),
    EmptyValueInTokens("%s0 Validation error. Invalid value in tokens. Specify a valid value."),
    TokensSizeExceedError("%s0 Maximum number of tokens exceeded. The limit is 100000."),
    DeleteTokensRequestNull("%s0 Validation error. DeleteTokensRequest object is null. Specify a valid DeleteTokensRequest object."),
    EmptyDeleteTokensData("%s0 Validation error. Tokens list is empty. Specify at least one token to delete."),
    EmptyTokenInDeleteTokensData("%s0 Validation error. Invalid token in delete tokens request. Specify a valid token."),
    DeleteTokensSizeExceedError("%s0 Maximum number of tokens exceeded. The limit is 100000."),
    NullTokenGroupRedactions("%s0 Validation error. TokenGroupRedaction in the list is null. Specify a valid TokenGroupRedactions object."),
    NullRedactionInTokenGroup("%s0 Validation error. Redaction in TokenGroupRedactions is null or empty. Specify a valid redaction."),
    NullTokenGroupNameInTokenGroup("%s0 Validation error. TokenGroupName in TokenGroupRedactions is null or empty. Specify a valid tokenGroupName."),
    // Get / update
    IdsOrUniqueValuesKeyError("%s0 Validation error. 'ids' or 'uniqueValues' key is missing from the payload. Specify ids or uniqueValues in payload."),
    BothIdsAndUniqueValuesSpecified("%s0 Validation error. Both Skyflow IDs and unique values can't be specified. Either specify Skyflow IDs or unique values."),
    EmptyUniqueValues("%s0 Validation error. 'uniqueValues' can't be empty. Specify at least one unique value."),
    EmptyUniqueValueInUniqueValues("%s0 Validation error. Invalid unique value in 'uniqueValues'. Specify a valid unique value."),
    NullColumnRedactions("%s0 Validation error. Column redaction object can not be null. Specify a valid column redaction object."),
    NullColumnNameInColumnRedaction("%s0 Validation error. Column name can not be null or empty in column redaction. Specify a valid column name."),
    NullRedactionInColumnRedaction("%s0 Validation error. Redaction can not be null or empty in column redaction. Specify a valid redaction."),
    NullGetRecordRequest("%s0 Validation error. Record in 'records' is null. Specify a valid record."),
    UpdateRecordNull("%s0 Validation error. UpdateRequestRecord object in the list is null. Specify a valid UpdateRequestRecord object."),
    RecordSkyflowIdKeyError("%s0 Validation error. 'skyflowId' key is missing from the record. Specify a 'skyflowId' key."),
    EmptySkyflowIdInRecord("%s0 Validation error. 'skyflowId' can't be empty in the record. Specify a valid skyflow ID."),
    // Tokenize
    EmptyTokenizeData("%s0 Validation error. Tokenize data is empty. Specify at least one tokenize record."),
    TokenizeRecordNull("%s0 Validation error. TokenizeRecord in the list is null. Specify a valid TokenizeRecord object."),
    InvalidBulkTokenizeRecordType("%s0 Validation error. Record in the list must be of type BulkTokenizeRequestRecord. Specify a valid BulkTokenizeRequestRecord object."),
    EmptyValueInTokenizeRecord("%s0 Validation error. Value in TokenizeRecord is null or empty. Specify a valid value."),
    EmptyTokenGroupNamesInTokenizeRecord("%s0 Validation error. TokenGroupNames in TokenizeRecord is null or empty. Specify at least one token group name."),
    EmptyTokenGroupNameInTokenizeRecord("%s0 Validation error. Token group name in TokenizeRecord is null or empty. Specify a valid token group name."),
    TokenizeDataSizeExceedError("%s0 Maximum number of tokenize records exceeded. The limit is 100000."),
    MissingIndexInBulkTokenizeRecord("%s0 Validation error. Index in BulkTokenizeRequestRecord is null. Specify an index for every record."),
    DuplicateIndexInBulkTokenizeRecord("%s0 Validation error. Duplicate index in BulkTokenizeRequestRecord. Specify a unique index for every record."),
    // Get Tokens
    GetTokensRequestNull("%s0 Validation error. GetTokensRequest object is null. Specify a valid GetTokensRequest object."),
    EmptyGetTokensRecords("%s0 Validation error. 'records' can't be empty. Specify at least one GetTokensRequestRecord."),
    GetTokensRecordNull("%s0 Validation error. GetTokensRequestRecord in the list is null. Specify a valid GetTokensRequestRecord object."),
    EmptyValueInGetTokensRecord("%s0 Validation error. 'value' in GetTokensRequestRecord is null or empty. Specify a valid value."),
    EmptyTokenGroupNameInGetTokensRecord("%s0 Validation error. 'tokenGroupName' in GetTokensRequestRecord is null or empty. Specify a valid token group name."),

    // Upload Files
    UploadFilesRequestNull("%s0 Validation error. UploadFilesRequest object is null. Specify a valid UploadFilesRequest object."),
    EmptyUploadFilesRecords("%s0 Validation error. 'records' can't be empty. Specify at least one UploadFilesRequestRecord."),
    UploadFilesRecordNull("%s0 Validation error. UploadFilesRequestRecord in the list is null. Specify a valid UploadFilesRequestRecord object."),
    EmptyTableNameInUploadFilesRecord("%s0 Validation error. 'tableName' in UploadFilesRequestRecord is null or empty. Specify a valid table name."),
    EmptySkyflowIdInUploadFilesRecord("%s0 Validation error. 'skyflowId' in UploadFilesRequestRecord is empty. Specify a valid skyflow ID, or omit it to create a new record."),
    EmptyUploadFilesColumns("%s0 Validation error. 'columns' in UploadFilesRequestRecord can't be empty. Specify at least one UploadFilesRequestColumn."),
    UploadFilesColumnNull("%s0 Validation error. UploadFilesRequestColumn in the list is null. Specify a valid UploadFilesRequestColumn object."),
    EmptyColumnInUploadFilesColumn("%s0 Validation error. 'column' in UploadFilesRequestColumn is null or empty. Specify a valid column name."),
    DuplicateColumnInUploadFilesRecord("%s0 Validation error. 'columns' in UploadFilesRequestRecord contains a duplicate column name. Specify each column only once."),
    MissingFileSourceInUploadFilesColumn("%s0 Validation error. UploadFilesRequestColumn has no file. Provide one of filePath, base64, or fileObject."),
    MultipleFileSourcesInUploadFilesColumn("%s0 Validation error. UploadFilesRequestColumn has more than one file. Provide exactly one of filePath, base64, or fileObject."),
    InvalidFilePathInUploadFilesColumn("%s0 Validation error. No readable file at 'filePath' in UploadFilesRequestColumn. Specify the path of an existing file."),
    InvalidBase64InUploadFilesColumn("%s0 Validation error. 'base64' in UploadFilesRequestColumn is not a valid base64 string. Specify valid base64-encoded content."),
    FileNameRequiredWithBase64InUploadFilesColumn("%s0 Validation error. 'fileName' is required in UploadFilesRequestColumn when 'base64' is used. Specify a file name."),
    InvalidFileObjectInUploadFilesColumn("%s0 Validation error. 'fileObject' in UploadFilesRequestColumn is not an existing file. Specify a valid file object."),

    // Delete Files
    DeleteFilesRequestNull("%s0 Validation error. DeleteFilesRequest object is null. Specify a valid DeleteFilesRequest object."),
    EmptyDeleteFilesRecords("%s0 Validation error. 'records' can't be empty. Specify at least one DeleteFilesRequestRecord."),
    DeleteFilesRecordNull("%s0 Validation error. DeleteFilesRequestRecord in the list is null. Specify a valid DeleteFilesRequestRecord object."),
    EmptyTableNameInDeleteFilesRecord("%s0 Validation error. 'tableName' in DeleteFilesRequestRecord is null or empty. Specify a valid table name."),
    EmptyDeleteFilesColumns("%s0 Validation error. 'columns' in DeleteFilesRequestRecord can't be empty. Specify at least one file column."),
    EmptyColumnInDeleteFilesColumns("%s0 Validation error. 'columns' in DeleteFilesRequestRecord contains a null or empty column name. Specify valid column names."),
    DuplicateColumnInDeleteFilesColumns("%s0 Validation error. 'columns' in DeleteFilesRequestRecord contains a duplicate column name. Specify each column only once."),
    EmptySkyflowIdInDeleteFilesRecord("%s0 Validation error. 'skyflowId' in DeleteFilesRequestRecord is empty. Specify a valid skyflow ID, or use 'uniqueValues' instead."),
    InvalidIdOrUniqueValuesInDeleteFilesRecord("%s0 Validation error. DeleteFilesRequestRecord must set exactly one of 'skyflowId' or 'uniqueValues'. Specify one of them."),
    // Detect (flowvault only)
    ;

    private final String message;

    ErrorMessage(String message) {
        this.message = message.replace("%s0", Constants.SDK_PREFIX);
    }

    public String getMessage() {
        return message;
    }
}
