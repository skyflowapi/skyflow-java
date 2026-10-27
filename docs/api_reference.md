# API Reference

A reference for the public Skyflow Java SDK surface: client-management methods, config classes, request and response objects, helper classes, enums, and service-account utilities.

The vault, connection and file-upload classes belong to the skyvault SDK (`com.skyflow:skyflow-java`). The Detect classes in `com.skyflow.detect`, together with the credentials, config and service-account classes, live in the shared `common` module and apply to both the skyvault and the flowvault SDK (`com.skyflow:skyflow-flowvault-java`). Where a Detect field is specific to one vault type the table says so. The flowvault vault-operation classes (bulk insert, tokenize, detokenize and delete) are documented in the [flowvault README](../flowvault/README.md).

For task-oriented usage and examples, see the [skyvault README](../skyvault/README.md) and the [flowvault README](../flowvault/README.md).

All attributes, parameters, and enum values below are taken directly from the SDK source.

## Table of Contents

- [Detect API](#detect-api)
- [Client management methods](#client-management-methods)
- [Config classes](#config-classes)
- [Request objects](#request-objects)
- [Response objects](#response-objects)
- [Helper classes](#helper-classes)
- [Enums](#enums)
- [Service account utilities](#service-account-utilities)

---

## Detect API

Shared by the skyvault and flowvault SDKs. `skyflowClient.detect()` and `skyflowClient.detect(vaultId)` return a `DetectController`; every class below is in `com.skyflow.detect`. Usage and examples: [skyvault README](../skyvault/README.md#detect), [flowvault README](../flowvault/README.md#detect).

| Method | Request | Response |
|--------|---------|----------|
| `deidentifyString` | [`DeidentifyStringRequest`](#deidentifystringrequest) | [`DeidentifyStringResponse`](#deidentifystringresponse) |
| `deidentifyFile` | [`DeidentifyFileRequest`](#deidentifyfilerequest) | [`DeidentifyFileResponse`](#deidentifyfileresponse) |
| `reidentifyString` | [`ReidentifyStringRequest`](#reidentifystringrequest) | [`ReidentifyStringResponse`](#reidentifystringresponse) |
| `reidentifyFile` | [`ReidentifyFileRequest`](#reidentifyfilerequest) | [`ReidentifyFileResponse`](#reidentifyfileresponse) |
| `getRun` | [`GetRunRequest`](#getrunrequest) | [`GetRunResponse`](#getrunresponse) |
| `checkGuardrails` | [`CheckGuardrailsRequest`](#checkguardrailsrequest) | [`CheckGuardrailsResponse`](#checkguardrailsresponse) |

Configuration and helper classes: [`DetectConfiguration`](#detectconfiguration), [`Detect`](#detect), [`Entity`](#entity), [`Transformation` and `ShiftDates`](#transformation-and-shiftdates), [`CustomType`](#customtype), [`ObjectEntity`](#objectentity), [`FileMapping`](#filemapping), [`Media`](#media) (with `Audio`, `Bleep`, `Document`, `Pdf`, `Image`), [`PollOptions`](#polloptions), [`RedactionLevel`](#redactionlevel), [`DetectedEntity`](#detectedentity), [`EntityLocation`](#entitylocation), [`Metrics`](#metrics), [`FileOutput`](#fileoutput). Enums: [Detect enums](#detect-enums).

---

## Client management methods

A built `Skyflow` client exposes methods to mutate its configuration and logging at runtime, in addition to the builder methods used during initialization.

### Builder methods (`Skyflow.builder()`)

| Method | Returns | Description |
|--------|---------|-------------|
| `addVaultConfig(VaultConfig)` | `SkyflowClientBuilder` | Add a vault configuration. |
| `updateVaultConfig(VaultConfig)` | `SkyflowClientBuilder` | Replace an existing vault configuration (matched by `vaultId`). |
| `removeVaultConfig(String vaultId)` | `SkyflowClientBuilder` | Remove a vault configuration. |
| `addConnectionConfig(ConnectionConfig)` | `SkyflowClientBuilder` | Add a connection configuration. |
| `updateConnectionConfig(ConnectionConfig)` | `SkyflowClientBuilder` | Replace an existing connection configuration. |
| `removeConnectionConfig(String connectionId)` | `SkyflowClientBuilder` | Remove a connection configuration. |
| `addSkyflowCredentials(Credentials)` | `SkyflowClientBuilder` | Set client-level credentials applied when a vault or connection config does not specify its own. |
| `setLogLevel(LogLevel)` | `SkyflowClientBuilder` | Set the log level. See [`LogLevel`](#loglevel). |
| `build()` | `Skyflow` | Build and return the `Skyflow` client. |

### Instance methods (built `Skyflow` client)

| Method | Returns | Description |
|--------|---------|-------------|
| `addVaultConfig(VaultConfig)` | `Skyflow` | Add a vault configuration after build. |
| `getVaultConfig(String vaultId)` | `VaultConfig` | Retrieve a vault configuration by ID. |
| `updateVaultConfig(VaultConfig)` | `Skyflow` | Replace a vault configuration (matched by `vaultId`). |
| `removeVaultConfig(String vaultId)` | `Skyflow` | Remove a vault configuration. |
| `addConnectionConfig(ConnectionConfig)` | `Skyflow` | Add a connection configuration. |
| `getConnectionConfig(String connectionId)` | `ConnectionConfig` | Retrieve a connection configuration by ID. |
| `updateConnectionConfig(ConnectionConfig)` | `Skyflow` | Replace a connection configuration. |
| `removeConnectionConfig(String connectionId)` | `Skyflow` | Remove a connection configuration. |
| `updateSkyflowCredentials(Credentials)` | `Skyflow` | Replace the client-level credentials. |
| `updateLogLevel(LogLevel)` | `Skyflow` | Change the log level after initialization. |
| `getLogLevel()` | `LogLevel` | Return the current log level. |
| `vault()` | `VaultController` | Get a controller for the first (or only) vault. |
| `vault(String vaultId)` | `VaultController` | Get a controller for the specified vault. |
| `connection()` | `ConnectionController` | Get a controller for the first (or only) connection. |
| `connection(String connectionId)` | `ConnectionController` | Get a controller for the specified connection. |
| `detect()` | `DetectController` | Get a Detect controller for the first (or only) vault. |
| `detect(String vaultId)` | `DetectController` | Get a Detect controller for the specified vault. |

All mutating instance methods return the `Skyflow` instance for chaining and throw `SkyflowException` on validation errors.

```java
// Example: manage configuration after the client is built
skyflowClient.addVaultConfig(anotherVaultConfig);
skyflowClient.updateLogLevel(LogLevel.DEBUG);
LogLevel currentLevel = skyflowClient.getLogLevel();
```

---

## Config classes

### `VaultConfig`

`com.skyflow.config` — passed to `addVaultConfig()` / `updateVaultConfig()`.

| Setter | Getter | Type | Description |
|--------|--------|------|-------------|
| `setVaultId(String)` | `getVaultId()` | `String` | _(required)_ Vault ID. |
| `setClusterId(String)` | `getClusterId()` | `String` | _(required)_ Cluster ID (first segment of the vault URL). |
| `setEnv(Env)` | `getEnv()` | `Env` | Deployment environment. Default: `Env.PROD`. See [`Env`](#env). |
| `setCredentials(Credentials)` | `getCredentials()` | `Credentials` | Vault-specific credentials. Overrides client-level credentials for this vault. |

### `ConnectionConfig`

`com.skyflow.config` — passed to `addConnectionConfig()` / `updateConnectionConfig()`.

| Setter | Getter | Type | Description |
|--------|--------|------|-------------|
| `setConnectionId(String)` | `getConnectionId()` | `String` | _(required)_ Connection ID. |
| `setConnectionUrl(String)` | `getConnectionUrl()` | `String` | _(required)_ Connection URL. |
| `setCredentials(Credentials)` | `getCredentials()` | `Credentials` | Connection-specific credentials. Overrides client-level credentials for this connection. |

### `Credentials`

`com.skyflow.config` — use exactly one of the authentication fields; the last one set takes precedence.

| Setter | Getter | Type | Description |
|--------|--------|------|-------------|
| `setApiKey(String)` | `getApiKey()` | `String` | API key for direct authentication. |
| `setToken(String)` | `getToken()` | `String` | Static bearer token. |
| `setPath(String)` | `getPath()` | `String` | Path to a service account `credentials.json` file. |
| `setCredentialsString(String)` | `getCredentialsString()` | `String` | Service account credentials as a JSON string. |
| `setRoles(ArrayList<String>)` | `getRoles()` | `ArrayList<String>` | Role IDs to scope the generated bearer token. |
| `setContext(String)` | `getContext()` | `Object` | String context embedded in the bearer token for context-aware authorization. |
| `setContext(Map<String, Object>)` | `getContext()` | `Object` | Map context embedded in the bearer token. Keys must match `[a-zA-Z0-9_]`. |

---

## Request objects

Parameters listed with their defaults as defined in the builders.

### `InsertRequest`

`com.skyflow.vault.data` — passed to `vault().insert()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `table(String)` | _(required)_ | Target table name. |
| `values(ArrayList<HashMap<String, Object>>)` | _(required)_ | List of records to insert. Each record is a `field → value` map. |
| `tokens(ArrayList<HashMap<String, Object>>)` | `null` | Bring-your-own-token values aligned with `values` (used with `tokenMode`). |
| `returnTokens(Boolean)` | `false` | Return tokens for inserted values. |
| `upsert(String)` | `null` | Column name to use as the upsert index (must have a `unique` constraint). |
| `homogeneous(Boolean)` | `false` | Treat the batch as homogeneous (all records share the same columns). |
| `continueOnError(Boolean)` | `false` | Continue the batch despite partial errors. |
| `tokenMode(TokenMode)` | `TokenMode.DISABLE` | BYOT mode. See [`TokenMode`](#tokenmode). |

### `GetRequest`

`com.skyflow.vault.data` — passed to `vault().get()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `table(String)` | _(required)_ | Target table name. |
| `ids(ArrayList<String>)` | `null` | Skyflow IDs to retrieve. Mutually exclusive with `columnName`/`columnValues`. |
| `redactionType(RedactionType)` | `null` | See [`RedactionType`](#redactiontype). |
| `returnTokens(Boolean)` | `false` | Return tokens instead of values. |
| `fields(ArrayList<String>)` | `null` | Specific fields/columns to return. |
| `offset(String)` | `null` | Pagination offset. |
| `limit(String)` | `null` | Pagination limit. |
| `downloadUrl(Boolean)` | `true` | Return file download URLs for file columns. |
| `columnName(String)` | `null` | Unique column to look up by. Mutually exclusive with `ids`. |
| `columnValues(ArrayList<String>)` | `null` | Values for `columnName`. |
| `orderBy(String)` | `"ASC"` | Sort order for results. |

### `UpdateRequest`

`com.skyflow.vault.data` — passed to `vault().update()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `table(String)` | _(required)_ | Target table name. |
| `data(HashMap<String, Object>)` | _(required)_ | Map containing `skyflow_id` and the columns to update. |
| `tokens(HashMap<String, Object>)` | `null` | BYOT values for the updated columns. |
| `returnTokens(Boolean)` | `false` | Return tokens (vs. IDs) for updated record. |
| `tokenMode(TokenMode)` | `TokenMode.DISABLE` | BYOT mode. See [`TokenMode`](#tokenmode). |

### `DeleteRequest`

`com.skyflow.vault.data` — passed to `vault().delete()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `table(String)` | _(required)_ | Target table name. |
| `ids(ArrayList<String>)` | _(required)_ | List of Skyflow IDs to delete. |

### `QueryRequest`

`com.skyflow.vault.data` — passed to `vault().query()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `query(String)` | _(required)_ | SQL query string to execute. |

### `FileUploadRequest`

`com.skyflow.vault.data` — passed to `vault().uploadFile()`. Provide exactly one file source: `fileObject`, `filePath`, or `base64`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `table(String)` | _(required)_ | Target table name. |
| `columnName(String)` | `null` | File column name. |
| `skyflowId(String)` | `null` | Existing record ID. Omit to create a new record. |
| `filePath(String)` | `null` | Path to a file to upload. |
| `base64(String)` | `null` | Base64-encoded file content. |
| `fileObject(File)` | `null` | A `java.io.File` object to upload. |
| `fileName(String)` | `null` | Override the file name sent to the vault. |

### `DetokenizeRequest`

`com.skyflow.vault.tokens` — passed to `vault().detokenize()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `detokenizeData(ArrayList<DetokenizeData>)` | _(required)_ | List of [`DetokenizeData`](#detokenizedata) objects pairing tokens with redaction types. |
| `continueOnError(Boolean)` | `false` | Continue despite per-token errors. |
| `downloadUrl(Boolean)` | `false` | Return file download URLs for file-type tokens. |

### `TokenizeRequest`

`com.skyflow.vault.tokens` — passed to `vault().tokenize()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `values(List<ColumnValue>)` | _(required)_ | List of [`ColumnValue`](#columnvalue) objects to tokenize. |

### `DeidentifyStringRequest`

`com.skyflow.detect` — passed to `detect().deidentifyString()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `text(String)` | _(required)_ | Text to de-identify. |
| `configurationId(String)` | `null` | Id of a Detect configuration created through the management API. Sending it together with `configuration` is rejected by the API. |
| `configuration(DetectConfiguration)` | `null` | Inline configuration. See [`DetectConfiguration`](#detectconfiguration). With neither this nor `configurationId` set, the SDK sends only the vault id and the server applies the vault's defaults. |

### `DeidentifyFileRequest`

`com.skyflow.detect` — passed to `detect().deidentifyFile()`. Supply the file through exactly one of `value` (with a `dataSource`), `file` or `filePath`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `dataSource(DataSourceType)` | `null` | How `value` supplies the file. See [`DataSourceType`](#datasourcetype). |
| `value(String)` | `null` | Base64 content, a skyflow id, or a presigned URL, per `dataSource`. |
| `file(File)` | `null` | Local file; read and base64-encoded when sent, `dataSource` set to `BASE64`. |
| `filePath(String)` | `null` | Same as `file(File)`, from a path. |
| `dataFormat(FileDataFormat)` | inferred from the extension | File format. See [`FileDataFormat`](#filedataformat). |
| `configurationId(String)` | `null` | Id of a stored Detect configuration. Sending it together with `configuration` is rejected by the API. |
| `configuration(DetectConfiguration)` | `null` | Inline configuration. See [`DetectConfiguration`](#detectconfiguration). With neither this nor `configurationId` set, the SDK sends only the vault id and the server applies the vault's defaults. |
| `pollOptions(PollOptions)` | `null` | Poll the run after submitting. `null` submits only and returns the run id. See [`PollOptions`](#polloptions). |
| `outputDirectory(String)` | working directory | Where outputs are written on success. Requires `pollOptions` and a local file; setting it without `pollOptions` fails client-side validation. |

### `ReidentifyStringRequest`

`com.skyflow.detect` — passed to `detect().reidentifyString()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `text(String)` | _(required)_ | The de-identified text to re-identify. |
| `redactionLevel(List<RedactionLevel>)` | `null` | Per-entity redaction rules. See [`RedactionLevel`](#redactionlevel). Without rules every token is replaced by its original value. |

### `ReidentifyFileRequest`

`com.skyflow.detect` — passed to `detect().reidentifyFile()`. Synchronous: the re-identified file comes back in the response.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `dataSource(DataSourceType)` | _(required)_ | `BASE64`, `SKYFLOW_ID` or `PRESIGNED_URL`. |
| `value(String)` | _(required)_ | Base64 content, a skyflow id, or a presigned URL, per `dataSource`. |
| `dataFormat(FileDataFormat)` | `null` | File format. Limited to the formats that support `VAULT_TOKEN`: `TXT`, `CSV`, `JSON`, `JSONL`, `XML`, `DOCX`, `XLSX` or `PPTX`. |
| `redactionLevel(List<RedactionLevel>)` | `null` | Redaction rules. See [`RedactionLevel`](#redactionlevel). |

### `GetRunRequest`

`com.skyflow.detect` — passed to `detect().getRun()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `runId(String)` | _(required)_ | The run id returned by a prior `deidentifyFile` call. |

### `CheckGuardrailsRequest`

`com.skyflow.detect` — passed to `detect().checkGuardrails()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `text(String)` | _(required)_ | Text to screen. |
| `checkToxicity(Boolean)` | `null` | Whether to check for toxicity. Sent only when set. |
| `denyTopics(List<String>)` | `null` | Topics to deny. |

### `InvokeConnectionRequest`

`com.skyflow.vault.connection` — passed to `connection().invoke()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `method(RequestMethod)` | `RequestMethod.POST` | HTTP method. See [`RequestMethod`](#requestmethod). |
| `pathParams(Map<String, String>)` | `null` | Path parameters. |
| `queryParams(Map<String, String>)` | `null` | Query parameters. |
| `requestHeaders(Map<String, String>)` | `null` | Request headers. |
| `requestBody(Object)` | `null` | Request body. |

---

## Response objects

Every vault, token, connection, and Detect operation returns a typed response object. Each attribute below lists its type and meaning.

> **The `errors` attribute** is common to most responses. It is populated only on partial failure (for example when `continueOnError=true`); it is `null` (or empty) when there are no errors.

All response classes serialize to JSON via `toString()`.

### `InsertResponse`

`com.skyflow.vault.data` — returned by `vault().insert()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getInsertedFields()` | `ArrayList<HashMap<String, Object>>` | One entry per inserted record. Each map contains `skyflow_id`; with `returnTokens=true`, also a token per column; with `continueOnError=true`, also a `request_index`. |
| `getErrors()` | `ArrayList<HashMap<String, Object>>` | Per-record errors when `continueOnError=true`. Each map contains `request_index`, `request_id`, `error`, and `http_code`. |

### `GetResponse`

`com.skyflow.vault.data` — returned by `vault().get()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getData()` | `ArrayList<HashMap<String, Object>>` | Retrieved records as `field → value` maps (tokens instead of values when `returnTokens=true`). |
| `getErrors()` | `ArrayList<HashMap<String, Object>>` | Errors when `continueOnError=true`. |

### `UpdateResponse`

`com.skyflow.vault.data` — returned by `vault().update()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getSkyflowId()` | `String` | The Skyflow ID of the updated record. |
| `getTokens()` | `HashMap<String, Object>` | A token per updated column when `returnTokens=true`. |

### `DeleteResponse`

`com.skyflow.vault.data` — returned by `vault().delete()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getDeletedIds()` | `List<String>` | Skyflow IDs of the deleted records. |

### `QueryResponse`

`com.skyflow.vault.data` — returned by `vault().query()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getFields()` | `ArrayList<HashMap<String, Object>>` | Matching records. Each record map also includes a `tokenized_data` entry. |
| `getErrors()` | `ArrayList<HashMap<String, Object>>` | Always `null` for query (errors throw `SkyflowException`). |

### `FileUploadResponse`

`com.skyflow.vault.data` — returned by `vault().uploadFile()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getSkyflowId()` | `String` | ID of the record the file was attached to (or of the newly created record). |
| `getErrors()` | `ArrayList<HashMap<String, Object>>` | Errors, if any. |

### `DetokenizeResponse`

`com.skyflow.vault.tokens` — returned by `vault().detokenize()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getDetokenizedFields()` | `ArrayList<DetokenizeRecordResponse>` | One entry per token. See [`DetokenizeRecordResponse`](#detokenizerecordresponse). |
| `getErrors()` | `ArrayList<DetokenizeRecordResponse>` | Per-token errors when `continueOnError=true`. |

#### `DetokenizeRecordResponse`

Each element returned by `getDetokenizedFields()` and `getErrors()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getToken()` | `String` | The input token value. |
| `getValue()` | `String` | The detokenized plaintext (or masked) value. `null` on error. |
| `getType()` | `String` | The value type (e.g. `"STRING"`). `null` on error. |
| `getError()` | `String` | Error message. `null` on success. |
| `getRequestId()` | `String` | Server request ID for this token. Useful for support escalations. |

### `TokenizeResponse`

`com.skyflow.vault.tokens` — returned by `vault().tokenize()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getTokens()` | `List<String>` | One token per input `ColumnValue`, in order. |

### `DeidentifyStringResponse`

`com.skyflow.detect` — returned by `detect().deidentifyString()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getProcessedText()` | `String` | The de-identified text with entity values replaced by tokens. |
| `getEntities()` | `List<DetectedEntity>` | Detected entities, never `null`. See [`DetectedEntity`](#detectedentity). |
| `getMetrics()` | `Metrics` | Word and character counts. See [`Metrics`](#metrics). |

### `ReidentifyStringResponse`

`com.skyflow.detect` — returned by `detect().reidentifyString()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getProcessedText()` | `String` | The re-identified text. |
| `getMetrics()` | `Metrics` | Word and character counts. |

### `DeidentifyFileResponse`

`com.skyflow.detect` — returned by `detect().deidentifyFile()`. Without `pollOptions` only `getRunId()` is set.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getRunId()` | `String` | Run id, always present. |
| `getStatus()` | `DetectRunStatus` | Run status when polled, `null` when submit only. See [`DetectRunStatus`](#detectrunstatus). |
| `getOutputType()` | `DataSourceType` | `BASE64` or `PRESIGNED_URL`, `null` until the run completes. |
| `getOutput()` | `List<FileOutput>` | Every output of the run, empty until complete, never `null`. See [`FileOutput`](#fileoutput). |
| `getMetrics()` | `Metrics` | Size, counts, pages, slides, duration. `null` until complete. |
| `getMessage()` | `String` | Failure detail when the status is `FAILED`. |

### `ReidentifyFileResponse`

`com.skyflow.detect` — returned by `detect().reidentifyFile()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getStatus()` | `DetectRunStatus` | Status of the re-identification. |
| `getOutputType()` | `DataSourceType` | `BASE64` or `PRESIGNED_URL`. |
| `getOutput()` | `List<FileOutput>` | Re-identified file outputs, never `null`. |
| `getMetrics()` | `Metrics` | Size and counts. |

### `GetRunResponse`

`com.skyflow.detect` — returned by `detect().getRun()`. Same getters as [`DeidentifyFileResponse`](#deidentifyfileresponse).

### `CheckGuardrailsResponse`

`com.skyflow.detect` — returned by `detect().checkGuardrails()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getText()` | `String` | The screened text. |
| `getValidation()` | `GuardrailsValidation` | `PASSED` or `FAILED`. See [`GuardrailsValidation`](#guardrailsvalidation). |
| `getToxic()` | `Boolean` | Whether the text was judged toxic, `null` when the check did not run. |
| `getDeniedTopic()` | `Boolean` | Whether a denied topic was found, `null` when no topics were supplied. |

### `InvokeConnectionResponse`

`com.skyflow.vault.connection` — returned by `connection().invoke()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getData()` | `Object` | The response body from the downstream service. |
| `getMetadata()` | `HashMap<String, String>` | Response metadata (e.g. HTTP headers forwarded by the connection). |
| `getErrors()` | `ArrayList<HashMap<String, Object>>` | Errors, if any. |

---

## Helper classes

### `DetokenizeData`

`com.skyflow.vault.tokens` — used inside [`DetokenizeRequest`](#detokenizerequest).

| Constructor | Description |
|-------------|-------------|
| `new DetokenizeData(String token)` | Detokenize with default redaction (`RedactionType.PLAIN_TEXT`). |
| `new DetokenizeData(String token, RedactionType redactionType)` | Detokenize with a specific redaction type. |

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getToken()` | `String` | The token value. |
| `getRedactionType()` | `RedactionType` | The redaction type applied to the result. |

### `ColumnValue`

`com.skyflow.vault.tokens` — used inside [`TokenizeRequest`](#tokenizerequest).

| Builder method | Default | Description |
|----------------|---------|-------------|
| `value(String)` | _(required)_ | The value to tokenize. |
| `columnGroup(String)` | _(required)_ | The column group that defines the tokenization policy. |

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getValue()` | `String` | The value to tokenize. |
| `getColumnGroup()` | `String` | The column group name. |

### `DetectConfiguration`

`com.skyflow.detect` — inline Detect policy for `deidentifyString` and `deidentifyFile`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `name(String)` | `null` | Configuration name. |
| `description(String)` | `null` | Configuration description. |
| `detect(Detect)` | `null` | What to detect and how to de-identify it. See [`Detect`](#detect). |
| `fileMapping(List<FileMapping>)` | `null` | Per-column handling for structured files. See [`FileMapping`](#filemapping). |
| `media(Media)` | `null` | Audio, document and image options for file calls. See [`Media`](#media). |

### `Detect`

| Builder method | Default | Description |
|----------------|---------|-------------|
| `entities(List<Entity>)` | `null` | Entities to detect, each with its own de-identification type. See [`Entity`](#entity). |
| `objectEntities(List<ObjectEntity>)` | `null` | Visual objects to detect in images. See [`ObjectEntity`](#objectentity). |
| `restrict(List<String>)` | `null` | Regexes to always treat as restricted. |
| `skip(List<String>)` | `null` | Regexes to never treat as sensitive. |
| `customTypes(List<CustomType>)` | `null` | Regex-defined entity types. See [`CustomType`](#customtype). |
| `returnEntities(ReturnEntitiesType)` | `EXCLUDE_SENSITIVE_DATA` | Whether raw values come back in the response. See [`ReturnEntitiesType`](#returnentitiestype). |

### `Entity`

| Builder method | Default | Description |
|----------------|---------|-------------|
| `entityType(EntityType)` | `ALL` | Entity type. See [`EntityType`](#entitytype). |
| `deidentificationType(DeidentificationType)` | `ENTITY_UNIQUE_COUNTER` | How the value is replaced. See [`DeidentificationType`](#deidentificationtype). |
| `destination(String)` | `null` | For `VAULT_TOKEN`: where the value is stored. A `table.column` on skyvault vaults, a token group name on flowvault vaults. Must already exist. |
| `transformation(Transformation)` | `null` | Date shifting. See [`Transformation`](#transformation). |

### `Transformation` and `ShiftDates`

`Transformation.builder().shiftDates(ShiftDates)`; `ShiftDates.builder().minDays(Integer).maxDays(Integer)`. Dates are shifted by a random amount within the range.

### `CustomType`

| Builder method | Default | Description |
|----------------|---------|-------------|
| `label(String)` | `null` | Becomes the entity type on the response. |
| `match(List<String>)` | `null` | Regexes that define the type. |

### `ObjectEntity`

| Builder method | Default | Description |
|----------------|---------|-------------|
| `entityType(ObjectEntityType)` | `null` | `FACE`, `LICENSE_PLATE`, `LOGO`, `SIGNATURE` or `ALL`. |
| `deidentificationType(ObjectDeidentificationType)` | `null` | `REDACT` or `UNREDACT`. |

### `FileMapping`

| Builder method | Description |
|----------------|-------------|
| `source(String)` | Source column. |
| `destination(String)` | Destination column. |
| `outputFileNameSuffix(String)` | Suffix added to the output file name. |
| `entitiesDestination(String)` | Column that receives the detected entities. |
| `objectEntitiesDestination(String)` | Column that receives the detected object entities. |

### `Media`

`Media.builder().audio(Audio).document(Document).image(Image)`.

| Class | Builder methods |
|-------|-----------------|
| `Audio` | `outputProcessedAudio(Boolean)`, `outputTranscription(OutputTranscriptionType)`, `bleep(Bleep)` |
| `Bleep` | `startPadding(Float)`, `stopPadding(Float)`, `frequency(Integer)`, `gain(Integer)` |
| `Document` | `pdf(Pdf)` |
| `Pdf` | `processingMode(PdfProcessingMode)`, `density(Integer)`, `maxResolution(Integer)` |
| `Image` | `outputProcessedImage(Boolean)`, `outputOcrText(Boolean)`, `maskingMethod(MaskingMethod)` |

### `PollOptions`

`com.skyflow.detect` — opts `deidentifyFile` into submit-and-poll mode. The SDK polls with exponential backoff (1, 2, 4, 8, 16, then 32 seconds) on the calling thread.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `waitTime(Integer)` | `60` | Maximum total seconds to keep polling, 1 to 300. |
| `maxAttempts(Integer)` | `23` | Maximum number of run lookups, 1 to 23. |

### `RedactionLevel`

`com.skyflow.detect` — element of `redactionLevel()` on `ReidentifyStringRequest` and `ReidentifyFileRequest`. Each rule names one source and one replacement: `entityName` plus `redactionType` on skyvault vaults, `tokenGroupName` plus `redactionPattern` on flowvault vaults. Every field is optional on the API; the server rejects invalid combinations.

| Builder method | Description |
|----------------|-------------|
| `entityName(EntityType)` | Skyvault vaults: entity type the rule applies to. |
| `redactionType(RedactionType)` | Skyvault vaults: `PLAIN_TEXT`, `MASKED`, `REDACTED` or `DEFAULT`, the same `com.skyflow.enums.RedactionType` used by `get` and `detokenize`. See [`RedactionType`](#redactiontype). |
| `tokenGroupName(String)` | Flowvault vaults: token group the rule applies to. |
| `redactionPattern(String)` | Flowvault vaults: name of a redaction pattern configured in the vault. |

### `DetectedEntity`

`com.skyflow.detect` — element of `DeidentifyStringResponse.getEntities()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getToken()` | `String` | The token that replaced the value, e.g. `NAME_1`. |
| `getValue()` | `String` | The original value; empty unless `returnEntities` is `ALL`. |
| `getLocation()` | `EntityLocation` | Offsets in the original and processed text. |
| `getEntityType()` | `String` | Entity type label, including custom type labels. |
| `getEntityScores()` | `Map<String, Double>` | Confidence scores per entity type. |

### `EntityLocation`

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getStartIndex()` / `getEndIndex()` | `Integer` | Offsets into the original text. |
| `getStartIndexProcessed()` / `getEndIndexProcessed()` | `Integer` | Offsets into the processed text. |

### `Metrics`

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getSize()` | `Double` | Size in kilobytes. |
| `getWordCount()` | `Integer` | Word count. |
| `getCharacterCount()` | `Integer` | Character count. |
| `getPages()` | `Integer` | Page count, documents only. |
| `getSlides()` | `Integer` | Slide count, presentations only. |
| `getDuration()` | `Double` | Duration in seconds, audio only. |

### `FileOutput`

`com.skyflow.detect` — element of `getOutput()` on the file responses.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getProcessedFile()` | `String` | Base64 content or a presigned URL, per the response's `getOutputType()`. |
| `getProcessedFileType()` | `String` | `REDACTED_FILE`, `ENTITIES`, `OBJECT_ENTITIES`, `REDACTED_TEXT`, and so on. |
| `getProcessedFileExtension()` | `FileDataFormat` | Extension of the output. |

---

## Enums

### `LogLevel`

`com.skyflow.enums`

| Value | Description |
|-------|-------------|
| `OFF` | Disable all logging. |
| `ERROR` | Log errors only. _(default)_ |
| `WARN` | Log warnings and errors. |
| `INFO` | Log informational messages, warnings, and errors. |
| `DEBUG` | Log everything (DEBUG, INFO, WARN, ERROR). |

### `Env`

`com.skyflow.enums`

| Value | Description |
|-------|-------------|
| `PROD` | Production environment. _(default)_ |
| `SANDBOX` | Sandbox environment. |
| `STAGE` | Staging environment. |
| `DEV` | Development environment. |

### `RedactionType`

`com.skyflow.enums` — one enum for every place a redaction is chosen: `GetRequest.redactionType`, `DetokenizeData`, and Detect's `RedactionLevel.redactionType` on skyvault vaults.

| Value | Description |
|-------|-------------|
| `PLAIN_TEXT` | Return the original, unmasked value. |
| `MASKED` | Return a partially masked value (e.g. `****1234`). |
| `REDACTED` | Return a fully redacted placeholder. |
| `DEFAULT` | Use the redaction type configured on the vault column. |

### `TokenMode`

`com.skyflow.enums` — controls bring-your-own-token (BYOT) behavior for `insert` and `update`.

| Value | Description |
|-------|-------------|
| `DISABLE` | Do not use BYOT tokens. _(default)_ |
| `ENABLE` | Use provided tokens where supplied; generate tokens for missing fields. |
| `ENABLE_STRICT` | All fields must supply a BYOT token; missing tokens cause an error. |

### `RequestMethod`

`com.skyflow.enums` — used in [`InvokeConnectionRequest`](#invokeconnectionrequest).

| Value | Description |
|-------|-------------|
| `GET` | HTTP GET. |
| `POST` | HTTP POST. _(default for connections)_ |
| `PUT` | HTTP PUT. |
| `PATCH` | HTTP PATCH. |
| `DELETE` | HTTP DELETE. |

### Detect enums

All in `com.skyflow.detect`.

| Enum | Values |
|------|--------|
| `EntityType` | the 70 entity types (`NAME`, `EMAIL_ADDRESS`, `SSN`, `CREDIT_CARD`, `DOB`, ...), plus `ALL` and `NONE` |
| `DeidentificationType` | `ENTITY_UNIQUE_COUNTER` _(default)_, `ENTITY_ONLY`, `VAULT_TOKEN`, `MASK`, `UNKNOWN` |
| `DataSourceType` | `BASE64`, `SKYFLOW_ID`, `PRESIGNED_URL` |
| `FileDataFormat` | `BMP`, `CSV`, `DCM`, `DOC`, `DOCX`, `GIF`, `JPEG`, `JPG`, `JSON`, `JSONL`, `MP3`, `PDF`, `PNG`, `PPT`, `PPTX`, `TIF`, `TIFF`, `TXT`, `WAV`, `XLS`, `XLSX`, `XML`, `ZIP` |
| `DetectRunStatus` | `QUEUED`, `IN_PROGRESS`, `SUCCESS`, `FAILED`, `UNKNOWN`. `SUCCESS`, `FAILED` and `UNKNOWN` end polling. |
| `ReturnEntitiesType` | `EXCLUDE_SENSITIVE_DATA` _(default)_, `ALL`, `NONE` |
| `MaskingMethod` | `NONE`, `BLUR`, `BLACKBOX` |
| `OutputTranscriptionType` | `NONE`, `TRANSCRIPTION`, `MEDICAL_TRANSCRIPTION`, `DIARIZED_TRANSCRIPTION`, `MEDICAL_DIARIZED_TRANSCRIPTION` |
| `PdfProcessingMode` | `NONE`, `OCR`, `TEXT_LAYER` |
| `ObjectEntityType` | `NONE`, `FACE`, `LICENSE_PLATE`, `LOGO`, `SIGNATURE`, `ALL` |
| `ObjectDeidentificationType` | `UNKNOWN`, `REDACT`, `UNREDACT` |
| `GuardrailsValidation` | `PASSED`, `FAILED`, `UNKNOWN` |

---

## Service account utilities

### `BearerToken` builder

`com.skyflow.serviceaccount.util` — generates bearer tokens from service account credentials. Tokens are valid for 60 minutes.

| Builder method | Description |
|----------------|-------------|
| `setCredentials(File)` | Path to a service account `credentials.json` file. |
| `setCredentials(String)` | Service account credentials as a JSON string. |
| `setCtx(String)` | Embed a string context value in the token for context-aware authorization. |
| `setCtx(Map<String, Object>)` | Embed a map of context values. Keys must match `[a-zA-Z0-9_]`. |
| `setRoles(ArrayList<String>)` | Scope the token to specific role IDs. |
| `build()` | Build the `BearerToken` instance. |

| Method | Return type | Description |
|--------|-------------|-------------|
| `getBearerToken()` | `String` | Generate and return a bearer token string. |

```java
// Check expiry before regenerating
if (Token.isExpired(token)) {
    BearerToken bt = BearerToken.builder()
            .setCredentials(new File("<PATH_TO_CREDENTIALS_JSON>"))
            .build();
    token = bt.getBearerToken();
}
```

### `SignedDataTokens` builder

`com.skyflow.serviceaccount.util` — signs data tokens with the service account's private key for secure detokenization.

| Builder method | Description |
|----------------|-------------|
| `setCredentials(File)` | Path to a service account `credentials.json` file. |
| `setCredentials(String)` | Service account credentials as a JSON string. |
| `setCtx(String)` | Embed a string context value. |
| `setCtx(Map<String, Object>)` | Embed a map of context values. |
| `setDataTokens(List<String>)` | The data tokens to sign. |
| `setTimeToLive(int)` | TTL in seconds (default: `60`). |
| `build()` | Build and return signed data token records. |

Response: a `List` of objects, each with:

| Field | Description |
|-------|-------------|
| `dataToken` | The original data token. |
| `signedDataToken` | The signed token string (JWT). |

### `Token` utility

`com.skyflow.serviceaccount.util`

| Method | Return type | Description |
|--------|-------------|-------------|
| `Token.isExpired(String token)` | `boolean` | Returns `true` if the token is `null` or has expired. Use before every API call to avoid mid-flight expiry. |
