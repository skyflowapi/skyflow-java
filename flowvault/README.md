# Skyflow FlowVault Java SDK

The `flowvault` module is a Skyflow Java SDK built for high-throughput vault operations. It shares its client, credentials, and configuration classes with the [skyvault SDK](../skyvault/README.md) (both depend on the `common` module) but currently exposes a different, narrower surface: **bulk** vault operations only.

[![CI](https://img.shields.io/static/v1?label=CI&message=passing&color=green?style=plastic&logo=github)](https://github.com/skyflowapi/skyflow-java/actions)
[![License](https://img.shields.io/github/license/skyflowapi/skyflow-java)](https://github.com/skyflowapi/skyflow-java/blob/main/LICENSE)

# Table of Contents

- [Table of Contents](#table-of-contents)
- [Install](#install)
  - [Requirements](#requirements)
  - [Configuration](#configuration)
- [Quickstart](#quickstart)
- [VaultController — Bulk operations](#vaultcontroller--bulk-operations)
- [Bulk Insert](#bulk-insert)
- [Bulk Tokenize](#bulk-tokenize)
- [Bulk Detokenize](#bulk-detokenize)
- [Bulk Delete Tokens](#bulk-delete-tokens)
- [Error Handling](#error-handling)

# Install

## Requirements

- Java 8 and above

## Configuration

### Gradle users

```
implementation 'com.skyflow:skyflow-flowvault-java:1.0.0'
```

### Maven users

```xml
<dependency>
    <groupId>com.skyflow</groupId>
    <artifactId>skyflow-flowvault-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

# Quickstart

Authentication and client setup are identical to the skyvault SDK — `Skyflow`, `Credentials`, and `VaultConfig` all come from the shared `common` module. See [Authenticate](../skyvault/README.md#authenticate) and [Initialize the client](../skyvault/README.md#initialize-the-client) in the skyvault README for the full set of credential types and multi-vault configuration options.

```java
import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.vault.controller.VaultController;

Credentials credentials = new Credentials();
credentials.setApiKey("<API_KEY>"); // or setToken/setCredentialsString/setPath

VaultConfig vaultConfig = new VaultConfig();
vaultConfig.setVaultId("<VAULT_ID>");
vaultConfig.setClusterId("<CLUSTER_ID>"); // part of the vault URL, e.g. https://{clusterId}.vault.skyflowapis.com
vaultConfig.setEnv(Env.PROD);
vaultConfig.setCredentials(credentials);

Skyflow skyflowClient = Skyflow.builder()
        .addVaultConfig(vaultConfig)
        .build();

// Uses the default (first configured) vault
VaultController vault = skyflowClient.vault();

// Or a specific vault by ID, if multiple were configured
VaultController vault = skyflowClient.vault("<VAULT_ID>");
```

# VaultController — Bulk operations

`VaultController` is returned by `skyflowClient.vault()` and `skyflowClient.vault(vaultId)`. `flowvault` currently exposes these bulk vault operations:

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `bulkInsert(BulkInsertRequest)` | `BulkInsertRequest`, optional `InsertOptions` | `BulkInsertResponse` | Insert many records, optionally across multiple tables, in one call |
| `bulkInsertAsync(BulkInsertRequest)` | same | `CompletableFuture<BulkInsertResponse>` | Async variant of `bulkInsert` |
| `bulkTokenize(BulkTokenizeRequest)` | `BulkTokenizeRequest`, optional `TokenizeOptions` | `BulkTokenizeResponse` | Tokenize many values, each against one or more named token groups |
| `bulkTokenizeAsync(BulkTokenizeRequest)` | same | `CompletableFuture<BulkTokenizeResponse>` | Async variant of `bulkTokenize` |
| `bulkDetokenize(BulkDetokenizeRequest)` | `BulkDetokenizeRequest`, optional `DetokenizeOptions` | `BulkDetokenizeResponse` | Detokenize many tokens, optionally with a redaction override per token group |
| `bulkDetokenizeAsync(BulkDetokenizeRequest)` | same | `CompletableFuture<BulkDetokenizeResponse>` | Async variant of `bulkDetokenize` |
| `bulkDeleteTokens(BulkDeleteTokensRequest)` | `BulkDeleteTokensRequest`, optional `DeleteTokensOptions` | `BulkDeleteTokensResponse` | Delete many tokens in one call |
| `bulkDeleteTokensAsync(BulkDeleteTokensRequest)` | same | `CompletableFuture<BulkDeleteTokensResponse>` | Async variant of `bulkDeleteTokens` |

All methods throw `SkyflowException` on request-level errors. Each also accepts an optional options object (`InsertOptions`, `TokenizeOptions`, `DetokenizeOptions`, `DeleteTokensOptions`) built the same way:

```java
import com.skyflow.vault.data.InsertOptions;
import com.skyflow.vault.data.RequestContext;
import com.skyflow.enums.CustomHeaderKey;

InsertOptions options = InsertOptions.builder()
        .interceptor(context -> context.addHeader(CustomHeaderKey.RequestIDHeader, "<REQUEST_ID>"))
        .build();

vault.bulkInsert(insertRequest, options);
```

Every bulk response carries three parts:

- a **summary** — totals for the call (e.g. `totalRecords`/`totalInserted`/`totalFailed` for insert)
- a **success** list — one entry per record/token that succeeded, indexed to match the input order
- an **errors** list — one entry per record/token that failed, with `index`, `error`, `code`, and `requestId`

Responses also expose a retry helper — `getRecordsToRetry()` on `BulkInsertResponse`, `getTokensToRetry()` on `BulkDetokenizeResponse` — that filters `errors` down to the ones worth resubmitting: server-side failures (HTTP 500–599), excluding 529 (a permanent capacity-limit code).

# Bulk Insert

Insert many records — even across different tables — in a single call. Each record is a [`BulkInsertRecord`](#bulk-insert) specifying its own `table`, `data`, and optional per-record `upsert`/`upsertType`.

### Construct a bulk insert request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.enums.UpsertType;
import com.skyflow.vault.data.BulkInsertRecord;
import com.skyflow.vault.data.BulkInsertRequest;
import com.skyflow.vault.data.BulkInsertResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BulkInsertExample {
    public static void main(String[] args) throws SkyflowException {
        // Step 1: Build each record with its own table and data
        Map<String, Object> record1Data = new HashMap<>();
        record1Data.put("card_number", "4111111111111111");
        record1Data.put("cardholder_name", "john doe");

        BulkInsertRecord record1 = BulkInsertRecord.builder()
                .table("table1")
                .data(record1Data)
                .build();

        Map<String, Object> record2Data = new HashMap<>();
        record2Data.put("email", "jane.doe@example.com");

        BulkInsertRecord record2 = BulkInsertRecord.builder()
                .table("table2")
                .data(record2Data)
                .upsert(Collections.singletonList("email")) // upsert on this record only
                .upsertType(UpsertType.UPDATE)
                .build();

        ArrayList<BulkInsertRecord> records = new ArrayList<>();
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
  "success": [
    {
      "index": 0,
      "skyflow_id": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
      "table": "table1",
      "tokens": {
        "card_number": [{ "token": "5484-7829-1702-9110" }],
        "cardholder_name": [{ "token": "b2308e2a-c1f5-469b-97b7-1f193159399b" }]
      }
    }
  ],
  "errors": [
    { "index": 1, "error": "Insert failed. Column email is invalid.", "code": 400, "requestId": "..." }
  ]
}
```

Use `insertResponse.getRecordsToRetry()` to get back only the `BulkInsertRecord`s worth resubmitting.

# Bulk Tokenize

Tokenize many values in one call. Each value can be tokenized against one or more named token groups.

### Construct a bulk tokenize request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkTokenizeRecord;
import com.skyflow.vault.data.BulkTokenizeRequest;
import com.skyflow.vault.data.BulkTokenizeResponse;

import java.util.ArrayList;
import java.util.Arrays;

public class BulkTokenizeExample {
    public static void main(String[] args) throws SkyflowException {
        BulkTokenizeRecord record1 = BulkTokenizeRecord.builder()
                .value("4111111111111111")
                .tokenGroupNames(Arrays.asList("card_number_cg"))
                .build();

        BulkTokenizeRecord record2 = BulkTokenizeRecord.builder()
                .value("john.doe@example.com")
                .tokenGroupNames(Arrays.asList("email_cg"))
                .build();

        ArrayList<BulkTokenizeRecord> data = new ArrayList<>();
        data.add(record1);
        data.add(record2);

        BulkTokenizeRequest tokenizeRequest = BulkTokenizeRequest.builder()
                .data(data)
                .build();

        BulkTokenizeResponse tokenizeResponse = vault.bulkTokenize(tokenizeRequest);
        System.out.println(tokenizeResponse);
    }
}
```

### Async bulk tokenize

```java
CompletableFuture<BulkTokenizeResponse> future = vault.bulkTokenizeAsync(tokenizeRequest);
```

Sample response:

```json
{
  "summary": { "totalTokens": 2, "totalTokenized": 2, "totalPartial": 0, "totalFailed": 0 },
  "success": [
    { "index": 0, "value": "4111111111111111", "tokens": { "card_number_cg": "5479-4229-4622-1393" } },
    { "index": 1, "value": "john.doe@example.com", "tokens": { "email_cg": "a1b2c3d4-e5f6-7890-abcd-ef1234567890" } }
  ],
  "errors": []
}
```

Since one input value can map to multiple token groups, the summary distinguishes fully tokenized records (`totalTokenized`), partially tokenized ones — some but not all token groups succeeded (`totalPartial`) — and fully failed ones (`totalFailed`).

# Bulk Detokenize

Detokenize many tokens in one call, optionally overriding the redaction applied per token group via `tokenGroupRedactions`.

### Construct a bulk detokenize request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkDetokenizeRequest;
import com.skyflow.vault.data.BulkDetokenizeResponse;
import com.skyflow.vault.data.BulkTokenGroupRedactions;

import java.util.ArrayList;
import java.util.Arrays;

public class BulkDetokenizeExample {
    public static void main(String[] args) throws SkyflowException {
        ArrayList<String> tokens = new ArrayList<>(Arrays.asList(
                "5479-4229-4622-1393",
                "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        ));

        // redaction is a free-form string understood by the vault (e.g. "PLAIN_TEXT",
        // "MASKED", "REDACTED", "DEFAULT" — the same redaction types as skyvault's RedactionType enum)
        BulkTokenGroupRedactions redaction = BulkTokenGroupRedactions.builder()
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
  "success": [
    { "index": 0, "token": "5479-4229-4622-1393", "value": "4111111111111111", "tokenGroupName": "card_number_cg", "metadata": {} }
  ],
  "errors": [
    { "index": 1, "error": "Token Not Found", "code": 404, "requestId": "..." }
  ]
}
```

Use `detokenizeResponse.getTokensToRetry()` to get back only the tokens worth resubmitting.

# Bulk Delete Tokens

Delete many tokens in one call.

### Construct a bulk delete tokens request

```java
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.BulkDeleteTokensRequest;
import com.skyflow.vault.data.BulkDeleteTokensResponse;

import java.util.ArrayList;
import java.util.Arrays;

public class BulkDeleteTokensExample {
    public static void main(String[] args) throws SkyflowException {
        ArrayList<String> tokens = new ArrayList<>(Arrays.asList(
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
  "success": [
    { "index": 0, "token": "5479-4229-4622-1393" },
    { "index": 1, "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890" }
  ],
  "errors": []
}
```

# Error Handling

All bulk operations throw `SkyflowException` for request-level failures (invalid request shape, auth errors, and similar). Per-record/per-token failures within an otherwise-successful call are surfaced in the response's `errors` list instead — they don't throw. See [Error Handling](../skyvault/README.md#error-handling) in the skyvault README for `SkyflowException`'s properties, since `flowvault` shares the same exception type from `common`.
