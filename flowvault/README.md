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
- [VaultController — Unary operations](#vaultcontroller--unary-operations)
  - [Unary vs. bulk](#unary-vs-bulk)
  - [Vault type support](#vault-type-support)
- [Bulk Insert](#bulk-insert)
- [Bulk Tokenize](#bulk-tokenize)
- [Bulk Detokenize](#bulk-detokenize)
- [Bulk Delete Tokens](#bulk-delete-tokens)
- [Insert](#insert)
- [Detokenize](#detokenize)
- [Get](#get)
- [Update](#update)
- [Delete](#delete)
- [Query](#query)
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
- Perform unary Vault API operations — insert, detokenize, get, update, delete, and query — a single API call each, for when you want a plain request and response rather than the bulk batching machinery. See [VaultController — Unary operations](#vaultcontroller--unary-operations).
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

# VaultController — Unary operations

Alongside the bulk methods, `VaultController` exposes six **unary** operations. Each sends exactly one API call and hands the result straight back:

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `insert(InsertRequest)` | `InsertRequest`, optional `InsertOptions` | `InsertResponse` | Insert records, optionally across multiple tables, in one call |
| `detokenize(DetokenizeRequest)` | `DetokenizeRequest`, optional `DetokenizeOptions` | `DetokenizeResponse` | Detokenize tokens, optionally with a redaction override per token group |
| `get(GetRequest)` | `GetRequest`, optional `GetOptions` | `GetResponse` | Read records by skyflow ID or unique value, optionally with a redaction override per column |
| `update(UpdateRequest)` | `UpdateRequest`, optional `UpdateOptions` | `UpdateResponse` | Update records by skyflow ID |
| `delete(DeleteRequest)` | `DeleteRequest`, optional `DeleteOptions` | `DeleteResponse` | Delete records by skyflow ID or unique value |
| `query(QueryRequest)` | `QueryRequest`, optional `QueryOptions` | `QueryResponse` | Run a SQL-style query against the vault |

`insert` and `detokenize` are the unary counterparts of `bulkInsert` and `bulkDetokenize` — the same request builders, sent as one call instead of many batches. `get`, `update`, `delete`, and `query` have no bulk counterpart at all; they exist only in this unary form.

Each method also accepts an optional options object (`InsertOptions`, `DetokenizeOptions`, `GetOptions`, `UpdateOptions`, `DeleteOptions`, `QueryOptions`) — see [Custom Request Headers](#custom-request-headers).

## Unary vs. bulk

Everything the bulk machinery adds — batching, concurrency, the payload ceiling, the summary, the per-item index — is absent here. What survives is the per-record reporting:

| | Bulk operations | Unary operations |
|---|---|---|
| Async variant | Yes — `bulkInsertAsync`, and so on | **No.** Wrap the call yourself if you need one |
| Batching and concurrency | Configured per operation — see [Batching and concurrency](#batching-and-concurrency) | Not applicable — one payload, one call |
| Payload ceiling | 10,000 records or tokens per call | Not enforced by the SDK; the vault's own request limits still apply |
| Response summary | `getSummary()` | None — read the records list |
| Per-item `getIndex()` / `getRequestId()` | Yes | No. Records come back in submitted order, and the `x-request-id` of the single call reaches you only through a thrown `SkyflowException` |
| Retry helper | `getRecordsToRetry()` / `getTokensToRetry()` | None — filter the records yourself, see [Retrying the failed records](#retrying-the-failed-records) |
| Per-item `getHttpCode()` / `getError()` | Yes | Yes, on every unary operation except `query` |

## Vault type support

The same distinction as [Schema vs. schemaless vaults](#schema-vs-schemaless-vaults) applies. Five of the six unary operations address records inside a table, so they only make sense against a structured vault:

| Operation | Supported on |
|---|---|
| `insert` | Structured (schema) vaults — inserts into a table's columns. |
| `get` | Structured vaults — reads a table's records by skyflow ID or unique value. |
| `update` | Structured vaults — updates a table's records by skyflow ID. |
| `delete` | Structured vaults — deletes a table's records. Distinct from `bulkDeleteTokens`, which removes tokens only and leaves the record in place. |
| `query` | Structured vaults — the query itself addresses tables and columns. |
| `detokenize` | Both — detokenizing only needs the token itself, not a table, so it works regardless of which kind of vault the token came from. |

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

`record.getMetadata()` is typed as a `DetokenizeResponseRecordMetadata` with `getSkyflowId()`/`getTableName()` — no casting into the raw map required (`null` on records that errored, same as above):

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

# Insert

Insert records in a single API call — the unary counterpart of [Bulk Insert](#bulk-insert), with no batching or concurrency involved. Each record is an `InsertRequestRecord` with its own `data` and, optionally, its own `tableName`, `tokens`, and `upsert`.

> **Vault type supported:** structured (schema) vaults. See [Vault type support](#vault-type-support).

**Note:**

- `tableName` must be specified at exactly one level: either on the request (`InsertRequest.builder().tableName(...)`) or on **every** record (`InsertRequestRecord.builder().tableName(...)`) — not both, and not neither. Same rule as bulk insert.
- `upsert` is optional, but wherever you supply it, it must sit at the same level as `tableName`.
- `tokens` is optional; when supplied, the map must not be empty and no key or value may be blank.

### Construct an insert request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.InsertResponseRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsertExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Build each record. Here tableName lives on the request, so no record carries it.
        Map<String, Object> recordData = new HashMap<>();
        recordData.put("card_number", "4111111111111111");
        recordData.put("cardholder_name", "john doe");

        InsertRequestRecord record = InsertRequestRecord.builder()
                .data(recordData)
                .build();

        List<InsertRequestRecord> records = new ArrayList<>();
        records.add(record);

        // Step 2: Build the InsertRequest
        InsertRequest insertRequest = InsertRequest.builder()
                .tableName("table1")
                .records(records)
                .build();

        // Step 3: Perform the insert
        InsertResponse insertResponse = vault.insert(insertRequest);
        System.out.println(insertResponse);
    }
}
```

To put the table name on the records instead, drop `tableName` from the request and set it — along with any `upsert` — on every `InsertRequestRecord`, exactly as [Bulk Insert](#bulk-insert) shows.

There is no async variant: `insert` returns its `InsertResponse` directly.

Sample response:

```json
{
  "records": [
    {
      "tableName": "table1",
      "skyflowId": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
      "tokens": {
        "card_number": [
          { "token": "5484-7829-1702-9110", "tokenGroupName": "card_number_cg" }
        ],
        "cardholder_name": [
          { "token": "b2308e2a-c1f5-469b-97b7-1f193159399b", "tokenGroupName": "deterministic_string" }
        ]
      },
      "data": { "card_number": "4111-1111-1111-1111", "cardholder_name": "John Doe" },
      "hashedData": { "card_number": "b6e6d...c3f9" },
      "httpCode": 200,
      "error": null,
      "requestId": null
    }
  ]
}
```

There is no `summary` and no per-record `index` — the records come back in the order you submitted them. `requestId` behaves exactly as it does on a bulk record: `null` on success, the failing call's `x-request-id` on error. `getTokens()` returns the same parsed `Map<String, List<Token>>` described under [Bulk Insert](#bulk-insert), so `Token.getToken()`, `Token.getTokenGroupName()`, and `Token.getPath()` are available with no casting.

Accessors: `insertResponse.getRecords()`, and on each record `getTableName()`, `getSkyflowId()`, `getTokens()`, `getData()`, `getHashedData()`, `getHttpCode()`, `getError()`, `getRequestId()`.

> **Deprecation notice:** `InsertResponseRecord.getFields()` is deprecated in favor of `getTokens()` here too — it is kept only for backward compatibility and will be removed in a future release.

```java
for (InsertResponseRecord record : insertResponse.getRecords()) {
    if (record.getError() == null) {
        System.out.println(record.getTableName() + " -> " + record.getSkyflowId());
    } else {
        System.err.println("insert failed [" + record.getHttpCode() + "] " + record.getError());
    }
}
```

# Detokenize

Detokenize tokens in a single API call — the unary counterpart of [Bulk Detokenize](#bulk-detokenize), optionally overriding the redaction applied per token group via `tokenGroupRedactions`.

> **Vault type supported:** both. See [Vault type support](#vault-type-support).

**Note:**

- `tokens` is required and must not be empty, and no entry may be blank.
- `tokenGroupRedactions` is optional; when supplied, each entry needs a non-blank `tokenGroupName` and `redaction`.

### Construct a detokenize request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.DetokenizeResponseRecord;
import com.skyflow.vault.data.TokenGroupRedactions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DetokenizeExample {
    public static void main(String[] args) throws SkyflowException {
        List<String> tokens = new ArrayList<>(Arrays.asList(
                "5479-4229-4622-1393",
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        ));

        // redaction is a free-form string understood by the vault (e.g. "PLAIN_TEXT",
        // "MASKED", "REDACTED", "DEFAULT")
        TokenGroupRedactions redaction = TokenGroupRedactions.builder()
                .tokenGroupName("card_number_cg")
                .redaction("MASKED")
                .build();

        DetokenizeRequest detokenizeRequest = DetokenizeRequest.builder()
                .tokens(tokens)
                .tokenGroupRedactions(Arrays.asList(redaction))
                .build();

        DetokenizeResponse detokenizeResponse = vault.detokenize(detokenizeRequest);
        System.out.println(detokenizeResponse);
    }
}
```

There is no async variant: `detokenize` returns its `DetokenizeResponse` directly.

Sample response:

```json
{
  "records": [
    {
      "token": "5479-4229-4622-1393",
      "value": "4111111111111111",
      "tokenGroupName": "card_number_cg",
      "metadata": { "skyflowId": "9fac9201-7b8a-4446-93f8-5244e1213bd1", "tableName": "table1" },
      "httpCode": 200,
      "error": null,
      "requestId": null
    },
    {
      "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "value": null,
      "tokenGroupName": null,
      "metadata": null,
      "httpCode": 404,
      "error": "Token Not Found",
      "requestId": "a1b2c3d4-..."
    }
  ]
}
```

Same per-record shape as bulk detokenize, minus `index` — `requestId` behaves the same on both: `null` on success, the failing call's `x-request-id` on error. `record.getMetadata()` is a typed `DetokenizeResponseRecordMetadata` with `getSkyflowId()`/`getTableName()` — `null` on records that errored:

```java
for (DetokenizeResponseRecord record : detokenizeResponse.getRecords()) {
    if (record.getError() == null) {
        System.out.println(record.getToken() + " -> " + record.getValue()
                + " (" + record.getTokenGroupName() + ")");
    } else {
        System.err.println(record.getToken() + " failed ["
                + record.getHttpCode() + "] " + record.getError());
    }
}
```

Accessors: `detokenizeResponse.getRecords()`, and on each record `getToken()`, `getValue()`, `getTokenGroupName()`, `getMetadata()`, `getHttpCode()`, `getError()`, `getRequestId()`. `getValue()` is typed `Object`, passed straight through from the API.

# Get

Read records back from a table, by skyflow ID or by unique value, optionally overriding the redaction applied per column via `columnRedactions`.

> **Vault type supported:** structured (schema) vaults. See [Vault type support](#vault-type-support).

**Note:**

- A `GetRequest` works in one of two modes, and they are mutually exclusive: **single-table** (`table`, `ids`/`uniqueValues`, `fields`, `columnRedactions`, `limit`, `offset`) or **multi-table** (`records`, a list of `GetRequestRecord`). Setting fields from both modes fails validation.
- `table` is required, and exactly one of `ids` or `uniqueValues` must be supplied — both, or neither, fails validation. This holds per record in multi-table mode.
- `uniqueValues` is a `List<Map<String, Object>>`: one map per record, each holding the unique column-name/value pairs that identify it.
- `fields` selects the columns to return; omit it for all of them. When supplied, it must be non-empty with no blank entries.
- `limit` and `offset` apply to the call as a whole and are **only sent in single-table mode** — a `GetRequestRecord` has no `limit`/`offset` of its own, and values set on a multi-table request are not sent.

### Construct a get request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.ColumnRedactions;
import com.skyflow.vault.data.GetRequest;
import com.skyflow.vault.data.GetResponse;
import com.skyflow.vault.data.GetResponseRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class GetExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Optionally override how individual columns come back. Anything not listed
        // uses the redaction configured on the vault's policy.
        ColumnRedactions redaction = ColumnRedactions.builder()
                .columnName("card_number")
                .redaction("MASKED")
                .build();

        // Step 2: Build the GetRequest — single-table mode, selecting records by skyflow ID
        GetRequest getRequest = GetRequest.builder()
                .table("table1")
                .ids(new ArrayList<>(Arrays.asList(
                        "9fac9201-7b8a-4446-93f8-5244e1213bd1",
                        "b2308e2a-c1f5-469b-97b7-1f193159399b")))
                .fields(new ArrayList<>(Arrays.asList("card_number", "cardholder_name")))
                .columnRedactions(Collections.singletonList(redaction))
                .limit(10)
                .offset(0)
                .build();

        // Step 3: Perform the get
        GetResponse getResponse = vault.get(getRequest);
        System.out.println(getResponse);
    }
}
```

To select records by unique value instead of skyflow ID, swap `ids(...)` for `uniqueValues(...)`:

```java
Map<String, Object> uniqueValue = new HashMap<>();
uniqueValue.put("email", "jane.doe@example.com");

GetRequest getRequest = GetRequest.builder()
        .table("table2")
        .uniqueValues(Collections.singletonList(uniqueValue))
        .build();
```

To read from more than one table in a single call, use multi-table mode — each `GetRequestRecord` carries its own table and lookup fields, and none of the single-table fields may be set on the request itself:

```java
GetRequestRecord fromTable1 = GetRequestRecord.builder()
        .table("table1")
        .ids(Arrays.asList("9fac9201-7b8a-4446-93f8-5244e1213bd1"))
        .fields(Arrays.asList("card_number"))
        .build();

GetRequestRecord fromTable2 = GetRequestRecord.builder()
        .table("table2")
        .uniqueValues(Collections.singletonList(uniqueValue))
        .build();

GetRequest getRequest = GetRequest.builder()
        .records(Arrays.asList(fromTable1, fromTable2))
        .build();
```

There is no async variant: `get` returns its `GetResponse` directly.

Sample response:

```json
{
  "records": [
    {
      "tableName": "table1",
      "skyflowId": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
      "tokens": {
        "card_number": [
          { "token": "5484-7829-1702-9110", "tokenGroupName": "card_number_cg" }
        ]
      },
      "data": { "card_number": "4111-XXXX-XXXX-1111", "cardholder_name": "John Doe" },
      "hashedData": null,
      "httpCode": 200,
      "error": null,
      "requestId": null
    }
  ]
}
```

`GetResponseRecord` carries the same fields as an insert record — the vault returns the same object shape for both — so the accessors are identical: `getTableName()`, `getSkyflowId()`, `getTokens()`, `getData()`, `getHashedData()`, `getHttpCode()`, `getError()`, `getRequestId()`.

```java
for (GetResponseRecord record : getResponse.getRecords()) {
    if (record.getError() == null) {
        System.out.println(record.getSkyflowId() + " -> " + record.getData());
    } else {
        System.err.println(record.getSkyflowId() + " failed ["
                + record.getHttpCode() + "] " + record.getError());
    }
}
```

# Update

Update records in a table by skyflow ID, in a single API call.

> **Vault type supported:** structured (schema) vaults. See [Vault type support](#vault-type-support).

**Note:**

- `tableName` is required on the request. A record may override it with its own `tableName`, which applies to that record only.
- Every `UpdateRequestRecord` needs a non-blank `skyflowId`.
- `data` holds the columns to change; no key or value may be blank. `tokens` is optional, and when supplied must be non-empty with no blank keys or values.
- `updateType` accepts `"UPDATE"` (merge the supplied columns) or `"REPLACE"` (overwrite the whole record). Any other value fails validation; omitting it sends no `updateType`, which the vault treats the same as `"UPDATE"`. It is a request-level setting — records have no `updateType` of their own.

### Construct an update request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.data.UpdateRequestRecord;
import com.skyflow.vault.data.UpdateResponse;
import com.skyflow.vault.data.UpdateResponseRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UpdateExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Build the record — the columns to change, keyed by the record's skyflow ID
        Map<String, Object> data = new HashMap<>();
        data.put("cardholder_name", "jane doe");

        UpdateRequestRecord record = UpdateRequestRecord.builder()
                .skyflowId("9fac9201-7b8a-4446-93f8-5244e1213bd1")
                .data(data)
                .build();

        // Step 2: Build the UpdateRequest
        UpdateRequest updateRequest = UpdateRequest.builder()
                .tableName("table1")
                .records(Collections.singletonList(record))
                .updateType("UPDATE")
                .build();

        // Step 3: Perform the update
        UpdateResponse updateResponse = vault.update(updateRequest);
        System.out.println(updateResponse);
    }
}
```

There is no async variant: `update` returns its `UpdateResponse` directly.

Sample response:

```json
{
  "records": [
    {
      "tableName": "table1",
      "skyflowId": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
      "tokens": {
        "cardholder_name": [
          { "token": "f1a2b3c4-d5e6-7890-abcd-ef1234567890", "tokenGroupName": "deterministic_string" }
        ]
      },
      "data": { "cardholder_name": "Jane Doe" },
      "hashedData": null,
      "httpCode": 200,
      "error": null,
      "requestId": null
    }
  ]
}
```

Like `GetResponseRecord`, `UpdateResponseRecord` carries the insert record's fields — the vault returns the same object shape — so the accessors are `getTableName()`, `getSkyflowId()`, `getTokens()`, `getData()`, `getHashedData()`, `getHttpCode()`, `getError()`, `getRequestId()`.

```java
for (UpdateResponseRecord record : updateResponse.getRecords()) {
    if (record.getError() == null) {
        System.out.println(record.getSkyflowId() + " updated");
    } else {
        System.err.println(record.getSkyflowId() + " failed ["
                + record.getHttpCode() + "] " + record.getError());
    }
}
```

# Delete

Delete records from a table by skyflow ID or unique value, in a single API call.

> **Vault type supported:** structured (schema) vaults. See [Vault type support](#vault-type-support).

**Note:**

- This deletes the records themselves. [Bulk Delete Tokens](#bulk-delete-tokens) is a different operation — it removes tokens and leaves the underlying record in place.
- `table` is required, and exactly one of `ids` or `uniqueValues` must be supplied — both, or neither, fails validation.
- `uniqueValues` takes the same shape as in [Get](#get): one `Map<String, Object>` per record, holding the unique column-name/value pairs that identify it.

### Construct a delete request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DeleteRequest;
import com.skyflow.vault.data.DeleteResponse;
import com.skyflow.vault.data.DeleteResponseRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeleteExample {
    public static void main(String[] args) throws SkyflowException {
        List<String> ids = new ArrayList<>(Arrays.asList(
                "9fac9201-7b8a-4446-93f8-5244e1213bd1",
                "b2308e2a-c1f5-469b-97b7-1f193159399b"
        ));

        DeleteRequest deleteRequest = DeleteRequest.builder()
                .table("table1")
                .ids(ids)
                .build();

        DeleteResponse deleteResponse = vault.delete(deleteRequest);
        System.out.println(deleteResponse);
    }
}
```

There is no async variant: `delete` returns its `DeleteResponse` directly.

Sample response:

```json
{
  "records": [
    { "skyflowId": "9fac9201-7b8a-4446-93f8-5244e1213bd1", "httpCode": 200, "error": null, "requestId": null },
    { "skyflowId": "b2308e2a-c1f5-469b-97b7-1f193159399b", "httpCode": 404, "error": "Record Not Found", "requestId": "a1b2c3d4-..." }
  ]
}
```

`DeleteResponseRecord` is flatter than the insert-shaped records above — the vault returns no data, tokens, or hashed data for a delete. Accessors: `deleteResponse.getRecords()`, and on each record `getSkyflowId()`, `getHttpCode()`, `getError()`, `getRequestId()`.

```java
for (DeleteResponseRecord record : deleteResponse.getRecords()) {
    if (record.getError() == null) {
        System.out.println(record.getSkyflowId() + " deleted");
    } else {
        System.err.println(record.getSkyflowId() + " failed ["
                + record.getHttpCode() + "] " + record.getError());
    }
}
```

# Query

Run a SQL-style query against the vault in a single API call.

> **Vault type supported:** structured (schema) vaults. See [Vault type support](#vault-type-support).

**Note:**

- `query` is required and must not be blank. That is the whole request — there are no other fields.
- This is the one unary operation with **no per-record status**: rows either come back or the call throws. There is no `httpCode` or `error` on a `QueryResponseRecord`.

### Construct a query request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.QueryRequest;
import com.skyflow.vault.data.QueryResponse;
import com.skyflow.vault.data.QueryResponseRecord;

public class QueryExample {
    public static void main(String[] args) throws SkyflowException {
        QueryRequest queryRequest = QueryRequest.builder()
                .query("SELECT card_number, cardholder_name FROM table1 LIMIT 10")
                .build();

        QueryResponse queryResponse = vault.query(queryRequest);
        System.out.println(queryResponse);
    }
}
```

There is no async variant: `query` returns its `QueryResponse` directly.

Sample response:

```json
{
  "records": [
    { "data": { "card_number": "4111-1111-1111-1111", "cardholder_name": "John Doe" } },
    { "data": { "card_number": "5484-7829-1702-9110", "cardholder_name": "Jane Doe" } }
  ],
  "metadata": {
    "columns": ["card_number", "cardholder_name"]
  }
}
```

Each row is a free-form column/value map — the query API has no notion of tokens, so unlike insert or detokenize there is no typed `Token` data here. `getMetadata()` returns a `QueryResponseMetadata` wrapping the query's return columns via `getColumns()`; both `getMetadata()` and `getColumns()` are `null` when the vault doesn't report columns.

Accessors: `queryResponse.getRecords()` and `queryResponse.getMetadata()`, and on each record `getData()`.

```java
System.out.println("columns: " + (queryResponse.getMetadata() != null ? queryResponse.getMetadata().getColumns() : null));
for (QueryResponseRecord row : queryResponse.getRecords()) {
    System.out.println(row.getData());
}
```

# Custom Request Headers

To include custom HTTP headers on an outgoing request — bulk or unary — pass a `RequestInterceptor` via that operation's options object. The headers available are defined by the `CustomHeaderKey` enum:

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

The interceptor runs **once per batch**, not once per bulk call — so a value generated inside it (a fresh request id, say) differs between the batches a single bulk call is split into. On a unary operation there is only ever one call, so it runs exactly once.

The same pattern applies to every operation, via its corresponding options class:

| Operation | Options class |
|---|---|
| `bulkInsert` / `bulkInsertAsync` | `BulkInsertOptions` |
| `bulkTokenize` / `bulkTokenizeAsync` | `BulkTokenizeOptions` |
| `bulkDetokenize` / `bulkDetokenizeAsync` | `BulkDetokenizeOptions` |
| `bulkDeleteTokens` / `bulkDeleteTokensAsync` | `BulkDeleteTokensOptions` |
| `insert` | `InsertOptions` |
| `detokenize` | `DetokenizeOptions` |
| `get` | `GetOptions` |
| `update` | `UpdateOptions` |
| `delete` | `DeleteOptions` |
| `query` | `QueryOptions` |

# Error Handling

## Two layers of errors

This is the mental model to hold for every operation, bulk or unary:

| Layer | What it covers | How you see it |
|---|---|---|
| **Request-level** | The call could not be made or the whole call failed: invalid request shape, missing credentials, auth failure, payload over the 10,000-item limit. | A thrown `SkyflowException`. No results at all. |
| **Record-level** | The call succeeded, but individual records or tokens inside it did not. | A returned response. **Nothing is thrown.** Each entry in `getRecords()` reports its own `httpCode` and `error`. |

The second layer is what distinguishes `flowvault` from an all-or-nothing API: **a call that returns normally can still contain failures, and a call where every single record failed also returns normally rather than throwing.** Checking only for a thrown exception will silently miss failed records — always read the summary and the per-record results.

Unary operations follow the same two layers. Their records carry the same `requestId` behavior as bulk records — `null` on success, the failing call's `x-request-id` on error — the only structural differences are that unary records have no `getIndex()` (there is no batch position to report), and `query` has no record-level layer at all — rows either come back or the call throws, so `QueryResponse` carries no per-record `requestId` either.

## Per-record success and failure

Every bulk response exposes `getSummary()` and `getRecords()`. The records list has one entry per submitted item, in the order you submitted it, and each entry carries:

| Field | Present on | Meaning |
|---|---|---|
| `getIndex()` | bulk only | Position of this item in the payload you submitted — use it to line results back up with your input. |
| `getHttpCode()` | always | Per-item status. `2xx` for success; `4xx`/`5xx` for failure. |
| `getError()` | failures only | Error message for this item. `null` means this item succeeded. |
| `getRequestId()` | failures only | The `x-request-id` of the call this item was part of — quote it in support escalations. In bulk responses, items from the same batch share one id. Present on both bulk and unary per-record types; `QueryResponse` is the one exception (see below). |

The success payload sits alongside those fields on the same object: `getSkyflowId()`/`getTokens()`/`getData()` for insert (`getFields()` is deprecated — it returns the same data in its original, pre-typed `Map<String, Object>` shape, not `getTokens()`'s `Token` objects), `getValue()`/`getTokenGroupName()`/`getMetadata()` for detokenize, `getValue()`/`getTokenGroupName()`/`getToken()` for tokenize, `getToken()` for delete.

Summaries per operation:

| Response | Summary type | Fields |
|---|---|---|
| `BulkInsertResponse` | `BulkSummary` | `totalRecords`, `totalInserted`, `totalFailed` |
| `BulkTokenizeResponse` | `TokenizeSummary` | `totalTokens`, `totalTokenized`, `totalPartial`, `totalFailed` |
| `BulkDetokenizeResponse` | `DetokenizeSummary` | `totalTokens`, `totalDetokenized`, `totalFailed` |
| `BulkDeleteTokensResponse` | `DeleteTokensSummary` | `totalTokens`, `totalDeleted`, `totalFailed` |

A unary response has no summary and no `getIndex()` — just `getRecords()`, in submitted order, with `getHttpCode()`, `getError()`, and `getRequestId()` on each entry alongside that operation's payload: `getSkyflowId()`/`getTokens()`/`getData()`/`getHashedData()` for `insert`, `get`, and `update`; `getSkyflowId()` alone for `delete`; `getToken()`/`getValue()`/`getTokenGroupName()`/`getMetadata()` for `detokenize`. `QueryResponse` is the exception — its records carry only `getData()`, with no `getRequestId()` anywhere on the response, and the call's return columns on `getMetadata().getColumns()`.

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
