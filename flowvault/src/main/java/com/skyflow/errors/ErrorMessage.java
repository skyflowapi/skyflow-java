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
    // Detect (flowvault only)
    ReidentifyFileRequestNull("%s0 Validation error. ReidentifyFile request cannot be null."),
    InvalidDataSourceInReidentifyFile("%s0 Validation error. Invalid dataSource in reidentify file request. Specify BASE64, SKYFLOW_ID or PRESIGNED_URL."),
    InvalidValueInReidentifyFile("%s0 Validation error. Invalid value in reidentify file request. Specify the non-empty base64 content, skyflow id or presigned URL of the file."),
    ;

    private final String message;

    ErrorMessage(String message) {
        this.message = message.replace("%s0", Constants.SDK_PREFIX);
    }

    public String getMessage() {
        return message;
    }
}
