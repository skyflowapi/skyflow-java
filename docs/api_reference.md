# API Reference

A reference for the public Skyflow Java SDK surface: client-management methods, config classes, request and response objects, helper classes, enums, and service-account utilities. For task-oriented usage and examples, see the [README](../README.md).

All attributes, parameters, and enum values below are taken directly from the SDK source.

## Table of Contents

- [Client management methods](#client-management-methods)
- [Config classes](#config-classes)
- [Request objects](#request-objects)
- [Response objects](#response-objects)
- [Helper classes](#helper-classes)
- [Enums](#enums)
- [Service account utilities](#service-account-utilities)

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

### `DeidentifyTextRequest`

`com.skyflow.vault.detect` — passed to `detect().deidentifyText()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `text(String)` | _(required)_ | Text to de-identify. |
| `entities(List<DetectEntities>)` | `null` | Entity types to detect. See [`DetectEntities`](#detectentities). |
| `allowRegexList(List<String>)` | `null` | Regex patterns to always treat as detectable. |
| `restrictRegexList(List<String>)` | `null` | Regex patterns to exclude from detection. |
| `tokenFormat(TokenFormat)` | `null` | Token format controlling token types per entity. See [`TokenFormat`](#tokenformat). |
| `transformations(Transformations)` | `null` | Data transformations (e.g. date shifting). See [`Transformations`](#transformations). |

### `ReidentifyTextRequest`

`com.skyflow.vault.detect` — passed to `detect().reidentifyText()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `text(String)` | _(required)_ | The redacted/de-identified text to re-identify. |
| `redactedEntities(List<DetectEntities>)` | `null` | Entity types to keep redacted. |
| `maskedEntities(List<DetectEntities>)` | `null` | Entity types to mask. |
| `plainTextEntities(List<DetectEntities>)` | `null` | Entity types to reveal as plain text. |

### `DeidentifyFileRequest`

`com.skyflow.vault.detect` — passed to `detect().deidentifyFile()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `file(FileInput)` | _(required)_ | File source. See [`FileInput`](#fileinput). |
| `entities(List<DetectEntities>)` | `null` | Entity types to detect. |
| `allowRegexList(List<String>)` | `null` | Regex patterns to always treat as detectable. |
| `restrictRegexList(List<String>)` | `null` | Regex patterns to exclude. |
| `tokenFormat(TokenFormat)` | `null` | Token format per entity. |
| `transformations(Transformations)` | `null` | Transformations (not supported for Documents/Images/PDFs). |
| `outputProcessedImage(Boolean)` | `false` | Include the processed image in the response. |
| `outputOcrText(Boolean)` | `false` | Include OCR text in the response. |
| `maskingMethod(MaskingMethod)` | `null` | See [`MaskingMethod`](#maskingmethod). |
| `pixelDensity(Number)` | `null` | Pixel density for PDF processing. |
| `maxResolution(Number)` | `null` | Max resolution for PDF processing. |
| `outputProcessedAudio(Boolean)` | `false` | Include processed audio in the response. |
| `outputTranscription(DetectOutputTranscriptions)` | `null` | See [`DetectOutputTranscriptions`](#detectoutputtranscriptions). |
| `bleep(AudioBleep)` | `null` | Audio bleep configuration. See [`AudioBleep`](#audiobleep). |
| `outputDirectory(String)` | `null` | Directory to write the processed file. |
| `waitTime(Integer)` | `null` | Max seconds to wait for async file processing (≤ 64). |

### `GetDetectRunRequest`

`com.skyflow.vault.detect` — passed to `detect().getDetectRun()`.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `runId(String)` | _(required)_ | The `run_id` returned by a prior `deidentifyFile` call. |

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

### `DeidentifyTextResponse`

`com.skyflow.vault.detect` — returned by `detect().deidentifyText()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getProcessedText()` | `String` | The de-identified text with entity values replaced by tokens. |
| `getEntities()` | `List<EntityInfo>` | Detected entities. See [`EntityInfo`](#entityinfo). |
| `getWordCount()` | `int` | Word count of the input text. |
| `getCharCount()` | `int` | Character count of the input text. |

### `ReidentifyTextResponse`

`com.skyflow.vault.detect` — returned by `detect().reidentifyText()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getProcessedText()` | `String` | The re-identified text with tokens replaced by their original values. |

### `DeidentifyFileResponse`

`com.skyflow.vault.detect` — returned by `detect().deidentifyFile()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getFile()` | `FileInfo` | Metadata about the processed file. See [`FileInfo`](#fileinfo). |
| `getFileBase64()` | `String` | Base64-encoded processed file content (when `outputProcessedImage=true`). |
| `getType()` | `String` | MIME type of the output file. |
| `getExtension()` | `String` | File extension of the output file. |
| `getWordCount()` | `Integer` | Word count (text/document files). |
| `getCharCount()` | `Integer` | Character count (text/document files). |
| `getSizeInKb()` | `Float` | Output file size in kilobytes. |
| `getDurationInSeconds()` | `Float` | Duration in seconds (audio/video files). |
| `getPageCount()` | `Integer` | Page count (PDF files). |
| `getSlideCount()` | `Integer` | Slide count (presentation files). |
| `getEntities()` | `List<FileEntityInfo>` | Detected entities. See [`FileEntityInfo`](#fileentityinfo). |
| `getRunId()` | `String` | Run ID for async polling with `getDetectRun()`. |
| `getStatus()` | `String` | Processing status. See [`DeidentifyFileStatus`](#deidentifyfilestatus). |

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

### `FileInput`

`com.skyflow.vault.detect` — used inside [`DeidentifyFileRequest`](#deidentifyfilerequest). Provide exactly one source.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `file(File)` | `null` | A `java.io.File` object. |
| `filePath(String)` | `null` | Path to a file. |

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getFile()` | `File` | The file object. |
| `getFilePath()` | `String` | The file path. |

### `AudioBleep`

`com.skyflow.vault.detect` — used inside [`DeidentifyFileRequest`](#deidentifyfilerequest) for audio bleeping.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `gain(Double)` | `null` | Gain level of the bleep tone. |
| `frequency(Double)` | `null` | Frequency (Hz) of the bleep tone. |
| `startPadding(Double)` | `null` | Seconds of silence before the bleep. |
| `stopPadding(Double)` | `null` | Seconds of silence after the bleep. |

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getGain()` | `Double` | Gain level. |
| `getFrequency()` | `Double` | Frequency. |
| `getStartPadding()` | `Double` | Start padding in seconds. |
| `getStopPadding()` | `Double` | Stop padding in seconds. |

### `TokenFormat`

`com.skyflow.vault.detect` — used inside [`DeidentifyTextRequest`](#deidentifytextrequest) and [`DeidentifyFileRequest`](#deidentifyfilerequest) to control token types per entity.

| Builder method | Default | Description |
|----------------|---------|-------------|
| `defaultType(TokenType)` | `TokenType.ENTITY_UNIQUE_COUNTER` | Default token type for entities not explicitly listed. See [`TokenType`](#tokentype). |
| `vaultToken(List<DetectEntities>)` | `null` | Entities to tokenize as vault tokens. |
| `entityUniqueCounter(List<DetectEntities>)` | `null` | Entities to tokenize as entity-unique-counter tokens. |
| `entityOnly(List<DetectEntities>)` | `null` | Entities to tokenize as entity-only tokens. |

### `Transformations`

`com.skyflow.vault.detect` — used inside [`DeidentifyTextRequest`](#deidentifytextrequest) and [`DeidentifyFileRequest`](#deidentifyfilerequest).

| Constructor | Description |
|-------------|-------------|
| `new Transformations(DateTransformation shiftDates)` | Apply date shifting. |

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getShiftDates()` | `DateTransformation` | The date transformation to apply. |

### `DateTransformation`

`com.skyflow.vault.detect` — used inside [`Transformations`](#transformations).

| Constructor | Description |
|-------------|-------------|
| `new DateTransformation(int max, int min, List<DetectEntities> entities)` | Shift dates by a random amount within `[min, max]` days. |

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getMax()` | `int` | Maximum day-shift value. |
| `getMin()` | `int` | Minimum day-shift value. |
| `getEntities()` | `List<DetectEntities>` | Entity types to date-shift. |

### `EntityInfo`

`com.skyflow.vault.detect` — element of `DeidentifyTextResponse.getEntities()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getToken()` | `String` | The token that replaced the original entity value. |
| `getValue()` | `String` | The original entity value. |
| `getTextIndex()` | `TextIndex` | Character offsets of the entity in the original text. |
| `getProcessedIndex()` | `TextIndex` | Character offsets of the token in the processed text. |
| `getEntity()` | `String` | Entity type label (e.g. `"EMAIL_ADDRESS"`). |
| `getScores()` | `Map<String, Double>` | Confidence scores per entity type. |

### `TextIndex`

`com.skyflow.vault.detect` — used in [`EntityInfo`](#entityinfo).

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getStart()` | `int` | Start character offset (inclusive). |
| `getEnd()` | `int` | End character offset (exclusive). |

### `FileEntityInfo`

`com.skyflow.vault.detect` — element of `DeidentifyFileResponse.getEntities()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getFile()` | `String` | File name or identifier. |
| `getType()` | `String` | Output file type. |
| `getExtension()` | `String` | Output file extension. |

### `FileInfo`

`com.skyflow.vault.detect` — returned by `DeidentifyFileResponse.getFile()`.

| Getter | Return type | Description |
|--------|-------------|-------------|
| `getName()` | `String` | Original file name. |
| `getSize()` | `long` | File size in bytes. |
| `getType()` | `String` | MIME type. |
| `getLastModified()` | `long` | Last-modified timestamp (milliseconds since epoch). |

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

`com.skyflow.enums`

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

### `TokenType`

`com.skyflow.enums` — used in [`TokenFormat`](#tokenformat) for Detect operations.

| Value | Description |
|-------|-------------|
| `VAULT_TOKEN` | Token stored in the vault and associated with a skyflow_id. |
| `ENTITY_UNIQUE_COUNTER` | Deterministic token unique to the entity value (consistent across occurrences). |
| `ENTITY_ONLY` | Token represents the entity type only, with no value stored in the vault. |

### `RequestMethod`

`com.skyflow.enums` — used in [`InvokeConnectionRequest`](#invokeconnectionrequest).

| Value | Description |
|-------|-------------|
| `GET` | HTTP GET. |
| `POST` | HTTP POST. _(default for connections)_ |
| `PUT` | HTTP PUT. |
| `PATCH` | HTTP PATCH. |
| `DELETE` | HTTP DELETE. |

### `MaskingMethod`

`com.skyflow.enums` — used in [`DeidentifyFileRequest`](#deidentifyfilerequest).

| Value | Description |
|-------|-------------|
| `BLACKBOX` | Cover detected entities with a solid black rectangle. |
| `BLUR` | Blur detected entities. |

### `DetectOutputTranscriptions`

`com.skyflow.enums` — used in [`DeidentifyFileRequest`](#deidentifyfilerequest).

| Value | Description |
|-------|-------------|
| `TRANSCRIPTION` | Standard transcription. |
| `DIARIZED_TRANSCRIPTION` | Transcription with speaker diarization. |
| `MEDICAL_TRANSCRIPTION` | Medical-domain transcription. |
| `MEDICAL_DIARIZED_TRANSCRIPTION` | Medical transcription with speaker diarization. |

### `DeidentifyFileStatus`

`com.skyflow.enums` — returned in `DeidentifyFileResponse.getStatus()`.

| Value | Description |
|-------|-------------|
| `IN_PROGRESS` | File processing is ongoing. Poll with `getDetectRun()`. |
| `SUCCESS` | Processing completed successfully. |
| `FAILED` | Processing failed. |
| `UNKNOWN` | Status could not be determined. |

### `DetectEntities`

`com.skyflow.enums` — entity type values for Detect operations.

| Category | Values |
|----------|--------|
| Personal identity | `NAME`, `NAME_GIVEN`, `NAME_FAMILY`, `NAME_MEDICAL_PROFESSIONAL`, `DOB`, `AGE`, `GENDER`, `MARITAL_STATUS`, `SEXUALITY`, `RELIGION`, `POLITICAL_AFFILIATION`, `PHYSICAL_ATTRIBUTE`, `ORIGIN`, `NATIONALITY` |
| Contact | `EMAIL_ADDRESS`, `PHONE_NUMBER`, `LOCATION`, `LOCATION_ADDRESS`, `LOCATION_ADDRESS_STREET`, `LOCATION_CITY`, `LOCATION_STATE`, `LOCATION_COUNTRY`, `LOCATION_ZIP`, `LOCATION_COORDINATE` |
| Financial | `CREDIT_CARD`, `CREDIT_CARD_EXPIRATION`, `CVV`, `BANK_ACCOUNT`, `ACCOUNT_NUMBER`, `ROUTING_NUMBER`, `MONEY`, `FINANCIAL_METRIC`, `CORPORATE_ACTION` |
| Government ID | `SSN`, `DRIVER_LICENSE`, `PASSPORT_NUMBER`, `HEALTHCARE_NUMBER`, `ORGANIZATION_ID`, `NUMERICAL_PII` |
| Medical | `BLOOD_TYPE`, `CONDITION`, `DRUG`, `DOSE`, `EFFECT`, `INJURY`, `MEDICAL_CODE`, `MEDICAL_PROCESS`, `ORGANIZATION_MEDICAL_FACILITY` |
| Date & time | `DATE`, `DAY`, `MONTH`, `YEAR`, `TIME`, `DURATION`, `DATE_INTERVAL`, `EVENT` |
| Technology | `IP_ADDRESS`, `URL`, `USERNAME`, `PASSWORD`, `FILENAME`, `VEHICLE_ID` |
| Organization | `ORGANIZATION`, `OCCUPATION`, `PROJECT`, `PRODUCT` |
| Other | `LANGUAGE`, `STATISTICS`, `TREND`, `ZODIAC_SIGN`, `ALL` |

Use `DetectEntities.ALL` to detect all supported entity types.

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
