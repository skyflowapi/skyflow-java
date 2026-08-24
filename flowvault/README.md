# Skyflow FlowVault Java SDK

The `flowvault` module is a Skyflow Java SDK built for high-throughput vault operations. It shares its client, credentials, and configuration classes with the [skyvault SDK](../skyvault/README.md) (both depend on the `common` module) but exposes a different, narrower surface: **bulk** vault operations only.

> Meant for **Flow DB** vaults.

> **`flowvault` is a new SDK, versioned independently of `skyvault`.** It started at `1.0.0` while `skyvault` (`com.skyflow:skyflow-java`) is at `2.x`. The two artifacts have separate version lines, so a lower `flowvault` version number does not mean it is older or behind — it is a first release, not a downgrade. Upgrade each artifact on its own.

[![CI](https://img.shields.io/static/v1?label=CI&message=passing&color=green?style=plastic&logo=github)](https://github.com/skyflowapi/skyflow-java/actions)
[![License](https://img.shields.io/github/license/skyflowapi/skyflow-java)](https://github.com/skyflowapi/skyflow-java/blob/main/LICENSE)

# Table of Contents

- [Table of Contents](#table-of-contents)
- [Overview](#overview)
- [Install](#install)
  - [Requirements](#requirements)
  - [Configuration](#configuration)
- [Quickstart](#quickstart)
- [Authenticate](#authenticate)
  - [Credential types](#credential-types)
  - [Where credentials can be set](#where-credentials-can-be-set)
  - [Generate a bearer token](#generate-a-bearer-token)
  - [Context-aware and scoped tokens](#context-aware-and-scoped-tokens)
- [Initialize the client](#initialize-the-client)
  - [VaultConfig reference](#vaultconfig-reference)
  - [Skyflow.builder() reference](#skyflowbuilder-reference)
  - [Timeouts and retries](#timeouts-and-retries)
  - [Logging](#logging)
- [VaultController — Bulk operations](#vaultcontroller--bulk-operations)
  - [Schema vs. schemaless vaults](#schema-vs-schemaless-vaults)
  - [Batching and concurrency](#batching-and-concurrency)
- [Bulk Insert](#bulk-insert)
- [Bulk Tokenize](#bulk-tokenize)
- [Bulk Detokenize](#bulk-detokenize)
- [Bulk Delete Tokens](#bulk-delete-tokens)
- [Custom Request Headers](#custom-request-headers)
- [Error Handling](#error-handling)
  - [Two layers of errors](#two-layers-of-errors)
  - [Per-record success and failure](#per-record-success-and-failure)
  - [Catching SkyflowException](#catching-skyflowexception)
  - [SkyflowException properties](#skyflowexception-properties)
  - [Retrying the failed records](#retrying-the-failed-records)

# Overview

- Authenticate using a Skyflow service account, an API key, or a bearer token — see [Authenticate](#authenticate).
- Perform bulk Vault API operations — insert, tokenize, detokenize, and delete tokens — each with a synchronous and an async variant, built for high-throughput Flow DB workloads.
- **Per-record reporting, not all-or-nothing.** A bulk call succeeds as a call even when individual records fail; every response reports a summary plus the outcome of each individual record or token. See [Error Handling](#error-handling).

# Install

## Requirements

- Java 8 and above

## Configuration

### Gradle users

```
implementation 'com.skyflow:skyflow-flowvault-java:1.0.1'
```

### Maven users

```xml
<dependency>
    <groupId>com.skyflow</groupId>
    <artifactId>skyflow-flowvault-java</artifactId>
    <version>1.0.1</version>
</dependency>
```

# Quickstart

```java
import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.vault.controller.VaultController;

Credentials credentials = new Credentials();
credentials.setApiKey("<API_KEY>"); // or setToken / setCredentialsString / setPath

VaultConfig vaultConfig = new VaultConfig();
vaultConfig.setVaultId("<VAULT_ID>");
vaultConfig.setClusterId("<CLUSTER_ID>"); // part of the vault URL, e.g. https://{clusterId}.vault.skyflowapis.com
vaultConfig.setEnv(Env.PROD);
vaultConfig.setCredentials(credentials);

Skyflow skyflowClient = Skyflow.builder()
        .addVaultConfig(vaultConfig)
        .build();

// Returns the controller for the first configured vault
VaultController vault = skyflowClient.vault();
```

`vault()` with no arguments returns the controller for the first vault added to the builder. To talk to more than one vault from a single client, register each with `addVaultConfig(...)` and fetch each controller by ID: `skyflowClient.vault("<VAULT_ID>")`.

# Authenticate

Requests are authorized with Skyflow credentials that you attach to a `Credentials` object. `Credentials` comes from the shared `common` module, so it is the same class `skyvault` uses.

## Credential types

Set exactly one of the following on a `Credentials` instance. If you set more than one, **the last one set wins**.

| Credential | Setter | What it is |
|---|---|---|
| API key | `setApiKey(String)` | A long-lived key that authenticates and authorizes requests to the API. Simplest option. |
| Bearer token | `setToken(String)` | A short-lived access token, typically generated from service account credentials. See [Generate a bearer token](#generate-a-bearer-token). |
| Credentials file path | `setPath(String)` | Filesystem path to a service account `credentials.json`. The SDK generates and refreshes bearer tokens from it. |
| Credentials string | `setCredentialsString(String)` | The contents of a service account `credentials.json` as a JSON string — use this when the credentials come from a secret store rather than a file. |

Two optional modifiers apply when the SDK is generating tokens for you (that is, with `setPath` or `setCredentialsString`):

| Setter | Description |
|---|---|
| `setRoles(ArrayList<String>)` | Restrict the generated token to specific role IDs (a scoped token). |
| `setContext(String)` / `setContext(Map<String, Object>)` | Attach context to the generated token for context-aware authorization. |

```java
// API key
Credentials apiKeyCredentials = new Credentials();
apiKeyCredentials.setApiKey("<API_KEY>");

// Bearer token you generated yourself
Credentials tokenCredentials = new Credentials();
tokenCredentials.setToken("<BEARER_TOKEN>");

// Service account credentials file — the SDK handles token generation and refresh
Credentials fileCredentials = new Credentials();
fileCredentials.setPath("<PATH_TO_CREDENTIALS_JSON>");

// Service account credentials as a JSON string
Credentials stringCredentials = new Credentials();
stringCredentials.setCredentialsString("<CREDENTIALS_JSON_STRING>");
```

## Where credentials can be set

Credentials resolve **most specific first**:

1. **Per-vault** — `vaultConfig.setCredentials(credentials)`. Wins for that vault.
2. **Client-wide** — `Skyflow.builder().addSkyflowCredentials(credentials)`. Used by any vault that has none of its own.
3. **Environment** — if neither is provided, the SDK reads the `SKYFLOW_CREDENTIALS` environment variable.

If none of the three yields credentials, the call fails with a `SkyflowException`.

## Generate a bearer token

If you would rather manage tokens yourself, `common` ships the same `BearerToken` utility as `skyvault`:

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;

BearerToken token = BearerToken.builder()
        .setCredentials(new File("<PATH_TO_CREDENTIALS_JSON>")) // or setCredentials(credentialsJsonString)
        .build();

String bearerToken = token.getBearerToken(); // cached and regenerated only when expired

Credentials credentials = new Credentials();
credentials.setToken(bearerToken);
```

`getBearerToken()` caches the token and only mints a new one once the current one has expired, so it is safe to call per request.

## Context-aware and scoped tokens

`BearerToken.builder()` also accepts `setCtx(String | Map<String, Object>)` for context-aware authorization and `setRoles(ArrayList<String>)` for scoped tokens. Signed data tokens are available through `com.skyflow.serviceaccount.util.SignedDataTokens`. These utilities are identical to skyvault's — see [Authenticate with bearer tokens](../skyvault/README.md#authenticate-with-bearer-tokens) for worked examples of every variant.

# Initialize the client

`Skyflow` is the client. Build it once, keep it for the lifetime of your application, and get a `VaultController` from it with `vault()`.

```java
import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.controller.VaultController;

public class InitFlowVaultClient {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Credentials — exactly one credential type
        Credentials credentials = new Credentials();
        credentials.setPath("<PATH_TO_CREDENTIALS_JSON>");

        // Step 2: Vault configuration
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<VAULT_ID>");
        vaultConfig.setClusterId("<CLUSTER_ID>");
        vaultConfig.setEnv(Env.PROD);            // DEV, STAGE, SANDBOX, or PROD (default)
        vaultConfig.setCredentials(credentials);

        // Optional: vault-level HTTP overrides
        vaultConfig.setTimeout(120);             // overall call timeout, in seconds
        vaultConfig.setMaxRetries(2);            // retries after the first failure

        // Step 3: Build the client
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.INFO)      // default is ERROR
                .addVaultConfig(vaultConfig)
                .build();

        // Step 4: Get the controller and issue bulk calls
        VaultController vault = skyflowClient.vault();
    }
}
```

## VaultConfig reference

| Setter | Type | Description |
|---|---|---|
| `setVaultId(String)` | required | The vault's ID. |
| `setClusterId(String)` | required | The cluster portion of the vault URL — `https://{clusterId}.vault.skyflowapis.com`. |
| `setEnv(Env)` | optional | `Env.DEV`, `Env.STAGE`, `Env.SANDBOX`, or `Env.PROD`. Defaults to `PROD`; passing `null` also resolves to `PROD`. |
| `setCredentials(Credentials)` | optional | Credentials for this vault. Falls back to client-wide credentials, then `SKYFLOW_CREDENTIALS`. |
| `setVaultUrl(String)` | optional | Full vault URL, when it cannot be derived from `clusterId` and `env`. |
| `setTimeout(Integer)` | optional | Overall call timeout in seconds, including retries. |
| `setConnectTimeout(Integer)` | optional | Per-attempt connection timeout, in seconds. |
| `setReadTimeout(Integer)` | optional | Per-attempt response-read timeout, in seconds. |
| `setWriteTimeout(Integer)` | optional | Per-attempt request-write timeout, in seconds. |
| `setMaxRetries(Integer)` | optional | Retry attempts after the first failure. |
| `setInitialRetryDelayMillis(Long)` | optional | Backoff before the first retry, in milliseconds. |
| `setMaxRetryDelayMillis(Long)` | optional | Ceiling the exponential backoff grows to, in milliseconds. |

## Skyflow.builder() reference

| Method | Description |
|---|---|
| `addVaultConfig(VaultConfig)` | Register a vault. The first one registered is what `vault()` returns. |
| `updateVaultConfig(VaultConfig)` | Update a registered vault in place. `null` fields mean "leave as is". |
| `removeVaultConfig(String vaultId)` | Unregister a vault. |
| `addSkyflowCredentials(Credentials)` | Client-wide credentials for vaults that don't set their own. |
| `setLogLevel(LogLevel)` | `DEBUG`, `INFO`, `WARN`, `ERROR` (default), or `OFF`. |
| `timeout(int)` / `connectTimeout(int)` / `readTimeout(int)` / `writeTimeout(int)` | Client-wide HTTP timeouts, in seconds. |
| `maxRetries(int)` / `initialRetryDelayMillis(long)` / `maxRetryDelayMillis(long)` | Client-wide retry policy. |
| `build()` | Produce the `Skyflow` client. |

Every method throws `SkyflowException` on validation errors and returns the builder for chaining.

Once built, `skyflowClient.vault()` returns the first registered vault's controller; `skyflowClient.vault("<VAULT_ID>")` returns the controller for a specific registered vault, which is how one client talks to more than one vault.

## Timeouts and retries

Each HTTP setting resolves **most specific first**: the value on `VaultConfig`, else the client-wide value on `Skyflow.builder()`, else the SDK default. Only `null` means "inherit" — an explicit `0` is a real value and overrides the level below it.

| Setting | SDK default |
|---|---|
| `timeout` (overall call, incl. retries) | 60 s |
| `connectTimeout` / `readTimeout` / `writeTimeout` (per attempt) | 10 s (underlying HTTP client default) |
| `maxRetries` | `0` — retries are **opt-in**, so non-idempotent bulk writes are never replayed silently |
| `initialRetryDelayMillis` | 500 ms |
| `maxRetryDelayMillis` | 2000 ms |

```java
// Client-wide policy, overridden for one vault
VaultConfig vaultConfig = new VaultConfig();
vaultConfig.setVaultId("<VAULT_ID>");
vaultConfig.setClusterId("<CLUSTER_ID>");
vaultConfig.setCredentials(credentials);
vaultConfig.setTimeout(300);   // this vault gets 300s...

Skyflow skyflowClient = Skyflow.builder()
        .timeout(60)           // ...instead of the client-wide 60s
        .maxRetries(3)         // this vault inherits 3 retries
        .initialRetryDelayMillis(500L)
        .maxRetryDelayMillis(4000L)
        .addVaultConfig(vaultConfig)
        .build();
```

## Logging

The SDK logs through `java.util.logging` at `LogLevel.ERROR` by default. Levels rank `DEBUG` < `INFO` < `WARN` < `ERROR` < `OFF`; setting a level prints that level and everything above it. Change it with `Skyflow.builder().setLogLevel(LogLevel.DEBUG)`.

# VaultController — Bulk operations

`VaultController` is returned by `skyflowClient.vault()`. `flowvault` exposes these bulk vault operations:

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `bulkInsert(BulkInsertRequest)` | `BulkInsertRequest`, optional `BulkInsertOptions` | `BulkInsertResponse` | Insert many records, optionally across multiple tables, in one call |
| `bulkInsertAsync(BulkInsertRequest)` | same | `CompletableFuture<BulkInsertResponse>` | Async variant of `bulkInsert` |
| `bulkTokenize(BulkTokenizeRequest)` | `BulkTokenizeRequest`, optional `BulkTokenizeOptions` | `BulkTokenizeResponse` | Tokenize many values, each against one or more named token groups |
| `bulkTokenizeAsync(BulkTokenizeRequest)` | same | `CompletableFuture<BulkTokenizeResponse>` | Async variant of `bulkTokenize` |
| `bulkDetokenize(BulkDetokenizeRequest)` | `BulkDetokenizeRequest`, optional `BulkDetokenizeOptions` | `BulkDetokenizeResponse` | Detokenize many tokens, optionally with a redaction override per token group |
| `bulkDetokenizeAsync(BulkDetokenizeRequest)` | same | `CompletableFuture<BulkDetokenizeResponse>` | Async variant of `bulkDetokenize` |
| `bulkDeleteTokens(BulkDeleteTokensRequest)` | `BulkDeleteTokensRequest`, optional `BulkDeleteTokensOptions` | `BulkDeleteTokensResponse` | Delete many tokens in one call |
| `bulkDeleteTokensAsync(BulkDeleteTokensRequest)` | same | `CompletableFuture<BulkDeleteTokensResponse>` | Async variant of `bulkDeleteTokens` |

## Schema vs. schemaless vaults

Which of these operations makes sense depends on whether the vault is **structured** (has a schema — tables and columns) or **schemaless** (stores standalone tokens with no table structure):

| Operation | Supported on |
|---|---|
| `bulkInsert` / `bulkInsertAsync` | Structured (schema) vaults — inserts into a table's columns. |
| `bulkTokenize` / `bulkTokenizeAsync` | Schemaless vaults — tokenizes a raw value directly against named token groups, with no table involved. |
| `bulkDeleteTokens` / `bulkDeleteTokensAsync` | Schemaless vaults. |
| `bulkDetokenize` / `bulkDetokenizeAsync` | Both — detokenizing only needs the token itself, not a table, so it works regardless of which kind of vault the token came from. |

This reflects supported use cases, not something the SDK validates or blocks — nothing stops you from calling, say, `bulkTokenize` against a structured vault; it just isn't the intended usage and isn't a scenario the SDK is tested against.

Each method also accepts an optional options object (`BulkInsertOptions`, `BulkTokenizeOptions`, `BulkDetokenizeOptions`, `BulkDeleteTokensOptions`) — see [Custom Request Headers](#custom-request-headers).

A single bulk call accepts at most **10,000** records or tokens; anything larger is rejected up front with a `SkyflowException`. Under that ceiling the SDK splits the payload into batches and sends them concurrently, which is why errors from one call can carry different `requestId` values.

Every bulk response has the same two-part shape:

- a **summary** — totals for the call (e.g. `totalRecords` / `totalInserted` / `totalFailed` for insert)
- a **records** list — one entry per submitted record or token, in input order, each carrying its own `index`, `httpCode`, and `error`

That per-record shape is the point of these APIs; see [Error Handling](#error-handling) for the full model.

## Batching and concurrency

Batch size and concurrency are configured **per operation** through environment variables — there is no builder or options API for them. Each value is read from the process environment first, then from a `.env` file in the working directory.

| Operation | Batch size variable | Default | Max | Concurrency variable | Default | Max |
|-----------|--------------------|---------|-----|---------------------|---------|-----|
| Bulk insert | `INSERT_BATCH_SIZE` | 50 | 1000 | `INSERT_CONCURRENCY_LIMIT` | 1 | 10 |
| Bulk tokenize | `TOKENIZE_BATCH_SIZE` | 50 | 1000 | `TOKENIZE_CONCURRENCY_LIMIT` | 1 | 10 |
| Bulk detokenize | `DETOKENIZE_BATCH_SIZE` | 50 | 1000 | `DETOKENIZE_CONCURRENCY_LIMIT` | 1 | 10 |
| Bulk delete tokens | `DELETE_TOKENS_BATCH_SIZE` | 50 | 1000 | `DELETE_TOKENS_CONCURRENCY_LIMIT` | 1 | 10 |

Concurrency defaults to **1**, so batches are sent one after another unless you raise the limit.

How each value is resolved:

- **Batch size** — `min(yourValue, max)`. Above the max, the SDK logs a warning and uses the max. Zero, negative, or non-numeric values log a warning and fall back to the default.
- **Concurrency** — `min(yourValue, max, batchCount)`, where `batchCount = ceil(itemCount / batchSize)`. Concurrency never exceeds the number of batches there are to run. Same warning-and-fallback behaviour for invalid values.

Those warnings are emitted at `WARN`, which the default `ERROR` level hides — set `LogLevel.WARN` or below to see them (see [Logging](#logging)).

For example, 500 records with `INSERT_BATCH_SIZE=100` and `INSERT_CONCURRENCY_LIMIT=10` produces 5 batches, all 5 in flight at once — the concurrency is capped to 5, not 10.

```dotenv
# .env
INSERT_BATCH_SIZE=100
INSERT_CONCURRENCY_LIMIT=5
```

The 10,000-item ceiling per bulk call is a separate, fixed limit and is not configurable.

# Bulk Insert

Insert many records — even across different tables — in a single call. Each record is a `BulkInsertRequestRecord` with its own `data` and, optionally, its own `tableName` and `upsert`.

> **Vault type supported:** structured (schema) vaults. See [Schema vs. schemaless vaults](#schema-vs-schemaless-vaults).

**Note:**

- `tableName` must be specified at exactly one level: either on the request (`BulkInsertRequest.builder().tableName(...)`) or on **every** record (`BulkInsertRequestRecord.builder().tableName(...)`) — not both, and not neither.
- `upsert` is optional, but wherever you supply it, it must sit at the same level as `tableName`. Request-level `tableName` pairs with request-level `upsert`; record-level `tableName` pairs with per-record `upsert`.
- `UpsertOptions` requires `uniqueColumns`. `updateType` accepts `"UPDATE"` or `"REPLACE"` — if omitted, the SDK sends no `updateType` at all, and the vault treats that the same as `"UPDATE"`.

### Construct a bulk insert request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertRequestRecord;
import com.skyflow.vault.data.BulkInsertResponse;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.UpsertOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BulkInsertExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Build each record. Here tableName lives on the records, so each one carries it.
        Map<String, Object> record1Data = new HashMap<>();
        record1Data.put("card_number", "4111111111111111");
        record1Data.put("cardholder_name", "john doe");

        BulkInsertRequestRecord record1 = BulkInsertRequestRecord.builder()
                .tableName("table1")
                .data(record1Data)
                .build();

        Map<String, Object> record2Data = new HashMap<>();
        record2Data.put("email", "jane.doe@example.com");

        BulkInsertRequestRecord record2 = BulkInsertRequestRecord.builder()
                .tableName("table2")
                .data(record2Data)
                // upsert sits at the record level here, matching where tableName sits
                .upsert(UpsertOptions.builder()
                        .uniqueColumns(Arrays.asList("email"))
                        .updateType("UPDATE")
                        .build())
                .build();

        List<InsertRequestRecord> records = new ArrayList<>();
        records.add(record1);
        records.add(record2);

        // Step 2: Build the BulkInsertRequest
        BulkInsertRequest insertRequest = BulkInsertRequest.builder()
                .records(records)
                .build();

        // Step 3: Perform the bulk insert
        BulkInsertResponse insertResponse = vault.bulkInsert(insertRequest);
        System.out.println(insertResponse);
    }
}
```

To put the table name on the request instead, drop `tableName` from every record and build the request as:

```java
BulkInsertRequest insertRequest = BulkInsertRequest.builder()
        .tableName("table1")
        .upsert(UpsertOptions.builder().uniqueColumns(Arrays.asList("email")).build())
        .records(records)
        .build();
```

### Async bulk insert

```java
import java.util.concurrent.CompletableFuture;

CompletableFuture<BulkInsertResponse> future = vault.bulkInsertAsync(insertRequest);
future.thenAccept(response -> System.out.println(response));
```

Sample response:

```json
{
  "summary": { "totalRecords": 2, "totalInserted": 1, "totalFailed": 1 },
  "records": [
    {
      "index": 0,
      "requestId": null,
      "tableName": "table1",
      "skyflowId": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
      "tokens": {
        "card_number": [
          { "token": "5484-7829-1702-9110", "tokenGroupName": "card_number_cg" }
        ],
        "cardholder_name": [
          { "token": "b2308e2a-c1f5-469b-97b7-1f193159399b", "tokenGroupName": "deterministic_string" },
          { "token": "f1a2b3c4-d5e6-7890-abcd-ef1234567890", "tokenGroupName": "vault_token_group" }
        ]
      },
      "data": { "card_number": "4111-1111-1111-1111", "cardholder_name": "John Doe" },
      "hashedData": { "card_number": "b6e6d...c3f9" },
      "httpCode": 200,
      "error": null
    },
    {
      "index": 1,
      "requestId": "a1b2c3d4-...",
      "tableName": "table2",
      "skyflowId": null,
      "tokens": null,
      "data": null,
      "hashedData": null,
      "httpCode": 400,
      "error": "Insert failed. Column email is invalid."
    }
  ]
}
```

`getTokens()` returns `Map<String, List<Token>>` — one entry per token group configured on that column, so a column with a single token group still comes back as a one-element list, not a bare string. On the wire the API models this generically (`Object`, not a fixed type) to stay flexible, but the SDK parses it into `Token` objects before handing it back, so callers get `Token.getToken()`/`Token.getTokenGroupName()` directly with no casting required:

```java
for (BulkInsertResponseRecord record : insertResponse.getRecords()) {
    if (record.getTokens() != null) {
        for (Token token : record.getTokens().get("card_number")) {
            System.out.println(token.getTokenGroupName() + " -> " + token.getToken());
        }
    }
}
```

The parser (`Token.parseTokens()`) normalizes every shape the raw wire value is known to take — a list of `{token, tokenGroupName}` entries, a single such entry not wrapped in a list, or a bare token value with no group information — into a consistent `List<Token>`, rather than throwing on an unexpected one. `getTokens()` returns `null` when the record has no tokens (e.g. a failed record).

For a structured column (e.g. an object or array value), each entry also carries `getPath()` — the location within that column's own value the token came from, such as `"street"` or `"phone_numbers[0].type"`. It's `null` for a flat column, where there's nothing to point into.

Accessors: `insertResponse.getSummary()`, `insertResponse.getRecords()`, and on each record `getIndex()`, `getTableName()`, `getSkyflowId()`, `getTokens()`, `getData()`, `getHashedData()`, `getHttpCode()`, `getError()`, `getRequestId()`.

> **Deprecation notice:** `getFields()` is deprecated in favor of `getTokens()` — it is kept only for backward compatibility and will be removed in a future release. Update call sites to `getTokens()`.

Use `insertResponse.getRecordsToRetry()` to get back only the `BulkInsertRequestRecord`s worth resubmitting — see [Retrying the failed records](#retrying-the-failed-records).

# Bulk Tokenize

Tokenize many values in one call. Each value can be tokenized against one or more named token groups.

> **Vault type supported:** schemaless vaults. See [Schema vs. schemaless vaults](#schema-vs-schemaless-vaults).

### Construct a bulk tokenize request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeRequestRecord;
import com.skyflow.vault.data.BulkTokenizeResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BulkTokenizeExample {
    public static void main(String[] args) throws SkyflowException {
        BulkTokenizeRequestRecord record1 = BulkTokenizeRequestRecord.builder()
                .value("4111111111111111")
                .tokenGroupNames(Arrays.asList("card_number_cg"))
                .build();

        BulkTokenizeRequestRecord record2 = BulkTokenizeRequestRecord.builder()
                .value("john.doe@example.com")
                .tokenGroupNames(Arrays.asList("email_cg"))
                .build();

        List<BulkTokenizeRequestRecord> records = new ArrayList<>();
        records.add(record1);
        records.add(record2);

        BulkTokenizeRequest tokenizeRequest = BulkTokenizeRequest.builder()
                .records(records)
                .build();

        BulkTokenizeResponse tokenizeResponse = vault.bulkTokenize(tokenizeRequest);
        System.out.println(tokenizeResponse);
    }
}
```

`BulkTokenizeRequestRecord.builder()` also accepts `token(Object)` to supply your own token for the value instead of having the vault generate one.

### Async bulk tokenize

```java
CompletableFuture<BulkTokenizeResponse> future = vault.bulkTokenizeAsync(tokenizeRequest);
```

Sample response:

```json
{
  "summary": { "totalTokens": 2, "totalTokenized": 1, "totalPartial": 0, "totalFailed": 1 },
  "records": [
    { "index": 0, "value": "4111111111111111", "tokenGroupName": "card_number_cg", "token": "5479-4229-4622-1393", "httpCode": 200, "error": null, "requestId": null },
    { "index": 1, "value": "john.doe@example.com", "tokenGroupName": "email_cg", "token": null, "httpCode": 400, "error": "Token group email_cg not found.", "requestId": "a1b2c3d4-..." }
  ]
}
```

`records` is flat: one entry per (value, token group) outcome, matching the API's own response shape. Because a single value can map to several token groups, several entries can share the same `index` — that's how the SDK tells you which input value an entry belongs to. The summary classifies by index rather than by entry: fully tokenized values (`totalTokenized`), partially tokenized values where some groups succeeded and others failed (`totalPartial`), and fully failed values (`totalFailed`). The three always add up to `totalTokens`, which counts input values, not entries.

```java
for (BulkTokenizeResponseRecord record : tokenizeResponse.getRecords()) {
    if (record.getError() == null) {
        System.out.println(record.getValue() + " -> " + record.getTokenGroupName() + " = " + record.getToken());
    } else {
        System.out.println(record.getValue() + " -> " + record.getTokenGroupName() + " failed: " + record.getError());
    }
}
```

# Bulk Detokenize

Detokenize many tokens in one call, optionally overriding the redaction applied per token group via `tokenGroupRedactions`.

> **Vault type supported:** both. See [Schema vs. schemaless vaults](#schema-vs-schemaless-vaults).

### Construct a bulk detokenize request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.TokenGroupRedactions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BulkDetokenizeExample {
    public static void main(String[] args) throws SkyflowException {
        List<String> tokens = new ArrayList<>(Arrays.asList(
                "5479-4229-4622-1393",
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        ));

        // redaction is a free-form string understood by the vault (e.g. "PLAIN_TEXT",
        // "MASKED", "REDACTED", "DEFAULT" — the same redaction types as skyvault's RedactionType enum)
        TokenGroupRedactions redaction = TokenGroupRedactions.builder()
                .tokenGroupName("card_number_cg")
                .redaction("MASKED")
                .build();

        BulkDetokenizeRequest detokenizeRequest = BulkDetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(Arrays.asList(redaction))
                .build();

        BulkDetokenizeResponse detokenizeResponse = vault.bulkDetokenize(detokenizeRequest);
        System.out.println(detokenizeResponse);
    }
}
```

### Async bulk detokenize

```java
CompletableFuture<BulkDetokenizeResponse> future = vault.bulkDetokenizeAsync(detokenizeRequest);
```

Sample response:

```json
{
  "summary": { "totalTokens": 2, "totalDetokenized": 1, "totalFailed": 1 },
  "records": [
    {
      "index": 0,
      "requestId": null,
      "value": "4111111111111111",
      "tokenGroupName": "card_number_cg",
      "metadata": { "skyflowId": "9fac9201-7b8a-4446-93f8-5244e1213bd1", "tableName": "table1" },
      "httpCode": 200,
      "token": "5479-4229-4622-1393",
      "error": null
    },
    {
      "index": 1,
      "requestId": "a1b2c3d4-...",
      "value": null,
      "tokenGroupName": null,
      "metadata": null,
      "httpCode": 404,
      "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "error": "Token Not Found"
    }
  ]
}
```

`record.getMetadata()` is typed as a `DetokenizeMetadata` with `getSkyflowId()`/`getTableName()` — no casting into the raw map required (`null` on records that errored, same as above):

```java
for (BulkDetokenizeResponseRecord record : detokenizeResponse.getRecords()) {
    if (record.getMetadata() != null) {
        System.out.println(record.getMetadata().getSkyflowId() + " / " + record.getMetadata().getTableName());
    }
}
```

Use `detokenizeResponse.getTokensToRetry()` to get back only the tokens worth resubmitting.

# Bulk Delete Tokens

Delete many tokens in one call.

> **Vault type supported:** schemaless vaults. See [Schema vs. schemaless vaults](#schema-vs-schemaless-vaults).

### Construct a bulk delete tokens request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BulkDeleteTokensExample {
    public static void main(String[] args) throws SkyflowException {
        List<String> tokens = new ArrayList<>(Arrays.asList(
                "5479-4229-4622-1393",
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        ));

        BulkDeleteTokensRequest deleteTokensRequest = BulkDeleteTokensRequest.builder()
                .tokens(tokens)
                .build();

        BulkDeleteTokensResponse deleteTokensResponse = vault.bulkDeleteTokens(deleteTokensRequest);
        System.out.println(deleteTokensResponse);
    }
}
```

### Async bulk delete tokens

```java
CompletableFuture<BulkDeleteTokensResponse> future = vault.bulkDeleteTokensAsync(deleteTokensRequest);
```

Sample response:

```json
{
  "summary": { "totalTokens": 2, "totalDeleted": 2, "totalFailed": 0 },
  "records": [
    { "index": 0, "token": "5479-4229-4622-1393", "httpCode": 200, "error": null, "requestId": null },
    { "index": 1, "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "httpCode": 200, "error": null, "requestId": null }
  ]
}
```

```java
for (BulkDeleteTokensResponseRecord record : deleteTokensResponse.getRecords()) {
    if (record.getError() == null) {
        System.out.println(record.getToken() + " deleted");
    } else {
        System.out.println(record.getToken() + " failed (" + record.getHttpCode() + "): " + record.getError());
    }
}
```

Use `deleteTokensResponse.getTokensToRetry()` to get back only the tokens worth resubmitting.

# Custom Request Headers

To include custom HTTP headers on an outgoing bulk request, pass a `RequestInterceptor` via that operation's options object. The headers available are defined by the `CustomHeaderKey` enum:

| `CustomHeaderKey` | HTTP header name |
|---|---|
| `SKYFLOW_ACCOUNT_ID` | `x-skyflow-account-id` |
| `SKYFLOW_ACCOUNT_NAME` | `x-skyflow-account-name` |
| `REQUEST_ID_HEADER` | `x-request-id` |

```java
import com.skyflow.enums.CustomHeaderKey;
import com.skyflow.vault.data.BulkInsertOptions;

BulkInsertOptions options = BulkInsertOptions.builder()
        .interceptor(context -> context.addHeader(CustomHeaderKey.REQUEST_ID_HEADER, "<YOUR_REQUEST_ID>"))
        .build();

BulkInsertResponse insertResponse = vault.bulkInsert(insertRequest, options);
```

The interceptor runs **once per batch**, not once per bulk call — so a value generated inside it (a fresh request id, say) differs between the batches a single bulk call is split into.

The same pattern applies to every bulk operation, via its corresponding options class:

| Operation | Options class |
|---|---|
| `bulkInsert` / `bulkInsertAsync` | `BulkInsertOptions` |
| `bulkTokenize` / `bulkTokenizeAsync` | `BulkTokenizeOptions` |
| `bulkDetokenize` / `bulkDetokenizeAsync` | `BulkDetokenizeOptions` |
| `bulkDeleteTokens` / `bulkDeleteTokensAsync` | `BulkDeleteTokensOptions` |

# Error Handling

## Two layers of errors

This is the mental model to hold for every bulk operation:

| Layer | What it covers | How you see it |
|---|---|---|
| **Request-level** | The call could not be made or the whole call failed: invalid request shape, missing credentials, auth failure, payload over the 10,000-item limit. | A thrown `SkyflowException`. No results at all. |
| **Record-level** | The call succeeded, but individual records or tokens inside it did not. | A returned response. **Nothing is thrown.** Each entry in `getRecords()` reports its own `httpCode` and `error`. |

The second layer is what distinguishes `flowvault` from an all-or-nothing API: **a bulk call that returns normally can still contain failures, and a call where every single record failed also returns normally rather than throwing.** Checking only for a thrown exception will silently miss failed records — always read the summary and the per-record results.

## Per-record success and failure

Every bulk response exposes `getSummary()` and `getRecords()`. The records list has one entry per submitted item, in the order you submitted it, and each entry carries:

| Field | Present on | Meaning |
|---|---|---|
| `getIndex()` | always | Position of this item in the payload you submitted — use it to line results back up with your input. |
| `getHttpCode()` | always | Per-item status. `2xx` for success; `4xx`/`5xx` for failure. |
| `getError()` | failures only | Error message for this item. `null` means this item succeeded. |
| `getRequestId()` | failures only | The `x-request-id` of the batch this item was in — quote it in support escalations. Items from the same batch share one id. |

The success payload sits alongside those fields on the same object: `getSkyflowId()`/`getTokens()`/`getData()` for insert (`getFields()` is deprecated — it returns the same data in its original, pre-typed `Map<String, Object>` shape, not `getTokens()`'s `Token` objects), `getValue()`/`getTokenGroupName()`/`getMetadata()` for detokenize, `getValue()`/`getTokenGroupName()`/`getToken()` for tokenize, `getToken()` for delete.

Summaries per operation:

| Response | Summary type | Fields |
|---|---|---|
| `BulkInsertResponse` | `BulkSummary` | `totalRecords`, `totalInserted`, `totalFailed` |
| `BulkTokenizeResponse` | `TokenizeSummary` | `totalTokens`, `totalTokenized`, `totalPartial`, `totalFailed` |
| `BulkDetokenizeResponse` | `DetokenizeSummary` | `totalTokens`, `totalDetokenized`, `totalFailed` |
| `BulkDeleteTokensResponse` | `DeleteTokensSummary` | `totalTokens`, `totalDeleted`, `totalFailed` |

The idiomatic way to consume a bulk response:

```java
BulkInsertResponse response = vault.bulkInsert(insertRequest);

System.out.println("inserted " + response.getSummary().getTotalInserted()
        + " of " + response.getSummary().getTotalRecords());

for (BulkInsertResponseRecord record : response.getRecords()) {
    if (record.getError() == null) {
        System.out.println("row " + record.getIndex() + " -> " + record.getSkyflowId());
    } else {
        System.err.println("row " + record.getIndex() + " failed ["
                + record.getHttpCode() + "] " + record.getError()
                + " (requestId " + record.getRequestId() + ")");
    }
}
```

Tokenize reports one entry per (value, token group) outcome, so a single value can partially succeed — several entries share its `index`:

```java
for (BulkTokenizeResponseRecord record : tokenizeResponse.getRecords()) {
    if (record.getError() == null) {
        System.out.println(record.getIndex() + "/" + record.getTokenGroupName()
                + " -> " + record.getToken());
    } else {
        System.err.println(record.getIndex() + "/" + record.getTokenGroupName()
                + " failed [" + record.getHttpCode() + "] " + record.getError());
    }
}
```

## Catching SkyflowException

`SkyflowException` covers the request-level layer only — client-side validation errors and whole-call API errors. It comes from `common`, so it is the same exception type `skyvault` throws.

```java
import com.skyflow.errors.SkyflowException;

try {
    BulkInsertResponse response = vault.bulkInsert(insertRequest);
    // reaching here means the CALL succeeded — individual records may still have failed
} catch (SkyflowException e) {
    System.err.println("Skyflow error:");
    System.err.println("  HTTP code : " + e.getHttpCode());
    System.err.println("  Message   : " + e.getMessage());
    System.err.println("  Request ID: " + e.getRequestId());
    System.err.println("  Details   : " + e.getDetails());
} catch (Exception e) {
    System.err.println("Unexpected error: " + e.getMessage());
}
```

For the async variants, the same exception arrives wrapped in a `CompletionException`:

```java
vault.bulkInsertAsync(insertRequest)
        .thenAccept(response -> System.out.println(response))
        .exceptionally(throwable -> {
            System.err.println("bulk insert failed: " + throwable.getCause().getMessage());
            return null;
        });
```

## SkyflowException properties

| Property | Method | Description |
|---|---|---|
| HTTP status code | `getHttpCode()` | Integer status code (e.g. `400`, `404`, `500`). |
| Message | `getMessage()` | Human-readable description of the error. |
| HTTP status string | `getHttpStatus()` | Status string from the server (e.g. `"Bad Request"` for a client-side validation error; for API errors, whatever string the server returns). |
| gRPC code | `getGrpcCode()` | gRPC status code from the server. |
| Request ID | `getRequestId()` | The `x-request-id` header — useful for support escalations. |
| Details | `getDetails()` | `JsonArray` of additional error context from the server. Empty array for validation errors, `null` if the server response omitted the field. |

**Validation errors** (table name at the wrong level, empty token list, payload over 10,000 items, and similar) are thrown before any network call:

- `httpCode` is always `400`
- `requestId` and `grpcCode` are `null`
- `details` is an empty array

**API errors** are returned by the Skyflow server and have all fields populated from the response body and headers.

## Retrying the failed records

Because failures are reported per record, a partial failure can be retried without resubmitting the whole payload. Each response exposes a retry helper that filters its records down to the ones worth resending — **server-side failures (HTTP 500–599), excluding 529**, which is a permanent capacity-limit code:

| Response | Helper | Returns |
|---|---|---|
| `BulkInsertResponse` | `getRecordsToRetry()` | `List<BulkInsertRequestRecord>` — your original record objects, ready to resubmit |
| `BulkTokenizeResponse` | `getRecordsToRetry()` | `List<BulkTokenizeRequestRecord>` — values with at least one retryable token-group failure |
| `BulkDetokenizeResponse` | `getTokensToRetry()` | `List<String>` — the tokens to resubmit |
| `BulkDeleteTokensResponse` | `getTokensToRetry()` | `List<String>` — the tokens to resubmit |

```java
BulkInsertResponse response = vault.bulkInsert(insertRequest);

List<BulkInsertRequestRecord> retryable = response.getRecordsToRetry();
if (!retryable.isEmpty()) {
    BulkInsertResponse retryResponse = vault.bulkInsert(
            BulkInsertRequest.builder()
                    .tableName("table1")
                    .records(new ArrayList<>(retryable))
                    .build());
}
```

Client-side (`4xx`) failures are deliberately excluded — those need a fix to the data, not a retry. This is separate from the transport-level `maxRetries` setting in [Timeouts and retries](#timeouts-and-retries), which retries whole HTTP attempts and is off by default.
