package com.skyflow.logs;

public enum ErrorLogs {
    ClientConnetion(""),
    VaultIdIsRequired("Interface: init - Invalid client credentials. vaultID is required."),
    EmptyVaultIdInInit("Interface: init - Invalid client credentials. vaultID cannot be empty."),
    VaultUrlIsRequired("Interface: init - Invalid client credentials. vaultURL is required."),
    EmptyVaultUrlInInit("Interface: init - Invalid client credentials. vaultURL cannot be empty."),
    InvalidVaultUrlInInit("Interface: init - Invalid client credentials. Expecting https://XYZ for vaultURL"),
    GetBearerTokenIsRequired("Interface: init - Invalid client credentials. getBearerToken is required."),
    BearerTokenRejected(""),
    InvalidEncodeUriInGet("Interface: get method - Invalid encodeURI type in get."),
    InvalidBearerToken("Bearer token is invalid or expired."),
    InvalidVaultId("Vault Id is invalid or cannot be found."),
    EmptyVaultId("VaultID is empty"),
    InvalidCredentials("Invalid client credentials."),
    InvalidContainerType("Invalid container type."),
    InvalidCollectValue("Invalid value"),
    InvalidCollectValueWithLabel("Invalid %s1"),
    RecordsKeyNotFound("records object key value not found."),
    EmptyRecords("records object is empty."),
    RecordsKeyError("Key “records” is missing or payload is incorrectly formatted."),
    MissingRecords(""),
    InvalidRecords("Invalid Records"),
    EmptyRecordIds(""),
    EmptyRecordColumnvalues("Record column values cannot be empty."),
    InvalidRecordIdType("Invalid Type of Records Id."),
    InvalidRecordColumnValueType(""),
    InvalidRecordLabel("Invalid Record Label Type."),
    InvalidRecordAltText("Invalid Record altText Type."),
    FetchRecordsRejected(""),
    InsertRecordsRejected(""),
    GetBySkyflowIdRejected(""),
    SendInvokeConnectionRejected("Invoke connection request rejected."),
    UpdateRequestRejected("Update request is rejected."),
    InvalidTableName("Table Name passed doesn’t exist in the vault with id."),
    EmptyTableName("Table Name is empty."),
    EmptyTableAndFields("table or fields parameter cannot be passed as empty at index %s1 in records array.");

    private final String log;

    ErrorLogs(String log) {
        this.log = log;
    }

    public final String getLog() {
        return log;
    }
}
