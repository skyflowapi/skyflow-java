# Skyflow Java SDK — V1 to V2 Migration Guide

This guide covers the steps to migrate the Skyflow Java SDK from v1 to v2.

---

## Authentication options

In V2, multiple authentication options are available. You can now provide credentials in the following ways:

- Environment variable (`SKYFLOW_CREDENTIALS`) _(Recommended)_
- API Key
- Path to credentials JSON file
- Stringified JSON of credentials
- Bearer token

**V1 (Old)**

```java
static class DemoTokenProvider implements TokenProvider {
    @Override
    public String getBearerToken() throws Exception {
        ResponseToken res = null;
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_HERE>";
            res = Token.generateBearerToken(filePath);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
        return res.getAccessToken();
    }
}
```

**V2 (New): Choose one of the following:**

```java
// Option 1: API Key (Recommended)
Credentials skyflowCredentials = new Credentials();
skyflowCredentials.setApiKey("<YOUR_API_KEY>");

// Option 2: Environment Variable (Recommended)
// Set SKYFLOW_CREDENTIALS in your environment

// Option 3: Credentials File
skyflowCredentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>");

// Option 4: Stringified JSON
skyflowCredentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>");

// Option 5: Bearer Token
skyflowCredentials.setToken("<BEARER_TOKEN>");
```

> **Notes:**
> - Use only ONE authentication method per credentials object.
> - API Key or environment variable are recommended for production.
> - For priority order see [Quickstart — Initialize the client](https://github.com/skyflowapi/skyflow-java/blob/v1/README.md#vault-apis).

---

## Initializing the client

V2 introduces a builder pattern for client initialization with multi-vault support.

**Key changes:**
- `vaultUrl` replaced with `clusterId` (derived from vault URL)
- Added `env` specification (e.g. `Env.PROD`, `Env.SANDBOX`)
- Log level is now per-client-instance

**V1 (Old)**

```java
DemoTokenProvider demoTokenProvider = new DemoTokenProvider();
SkyflowConfiguration skyflowConfig = new SkyflowConfiguration(
    "<VAULT_ID>", "<VAULT_URL>", demoTokenProvider
);
Skyflow skyflowClient = Skyflow.init(skyflowConfig);
```

**V2 (New)**

```java
Credentials credentials = new Credentials();
credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>");

VaultConfig config = new VaultConfig();
config.setVaultId("<YOUR_VAULT_ID>");
config.setClusterId("<YOUR_CLUSTER_ID>");
config.setEnv(Env.PROD);
config.setCredentials(credentials);

Skyflow skyflowClient = Skyflow.builder()
    .setLogLevel(LogLevel.DEBUG)
    .addVaultConfig(config)
    .build();
```

---

## Request and response structure

V2 removes third-party JSON objects in favour of native `ArrayList` and `HashMap` with a builder pattern for requests.

**V1 (Old) — Request**

```java
JSONObject recordsJson = new JSONObject();
JSONArray recordsArrayJson = new JSONArray();
JSONObject recordJson = new JSONObject();
recordJson.put("table", "cards");
JSONObject fieldsJson = new JSONObject();
fieldsJson.put("cardNumber", "41111111111");
fieldsJson.put("cvv", "123");
recordJson.put("fields", fieldsJson);
recordsArrayJson.add(recordJson);
recordsJson.put("records", recordsArrayJson);
try {
    JSONObject insertResponse = skyflowClient.insert(records);
} catch (SkyflowException e) {
    System.out.println(e);
}
```

**V2 (New) — Request**

```java
HashMap<String, Object> value = new HashMap<>();
value.put("<COLUMN_NAME_1>", "<COLUMN_VALUE_1>");
value.put("<COLUMN_NAME_2>", "<COLUMN_VALUE_2>");
ArrayList<HashMap<String, Object>> values = new ArrayList<>();
values.add(value);

InsertRequest insertRequest = InsertRequest.builder()
    .table("<TABLE_NAME>")
    .values(values)
    .returnTokens(true)
    .build();

InsertResponse response = skyflowClient.vault().insert(insertRequest);
```

**V1 (Old) — Response**

```json
{
  "records": [
    {
      "table": "cards",
      "fields": {
        "skyflow_id": "16419435-aa63-4823-aae7-19c6a2d6a19f",
        "cardNumber": "f3907186-e7e2-466f-91e5-48e12c2bcbc1",
        "cvv": "1989cb56-63da-4482-a2df-1f74cd0dd1a5"
      }
    }
  ]
}
```

**V2 (New) — Response**

```json
{
  "insertedFields": [
    {
      "skyflowId": "9fac9201-7b8a-4446-93f8-5244e1213bd1",
      "card_number": "5484-7829-1702-9110",
      "cardholder_name": "b2308e2a-c1f5-469b-97b7-1f193159399b"
    }
  ],
  "errors": null
}
```

---

## Request options

V2 builder pattern replaces V1 options objects.

**V1 (Old)**

```java
InsertOptions insertOptions = new InsertOptions(true);
```

**V2 (New)**

```java
InsertRequest request = InsertRequest.builder()
    .table("<TABLE_NAME>")
    .values(values)
    .continueOnError(false)
    .tokenMode(TokenMode.DISABLE)
    .returnTokens(false)
    .upsert("<UPSERT_COLUMN>")
    .build();
```

---

## Error structure

V2 provides richer error details for easier debugging.

**V1 (Old)**

```json
{
  "code": "<http_code>",
  "description": "<description>"
}
```

**V2 (New)**

```json
{
  "httpStatus": "<http_status>",
  "grpcCode": "<grpc_code>",
  "httpCode": "<http_code>",
  "message": "<message>",
  "requestId": "<request_id>",
  "details": ["<details>"]
}
```

---

## Credential field names (v2.1+)

The credentials JSON file field names are updated to follow Java camelCase conventions. Both old and new forms are permanently accepted.

| Old form (still accepted) | New form (preferred) |
|---|---|
| `clientID` | `clientId` |
| `keyID` | `keyId` |
| `tokenURI` | `tokenUri` |

---

## Response field names (v2.1+)

Response maps now return `skyflowId` (camelCase). The legacy `skyflow_id` key is still present for backward compatibility but is deprecated.

| Deprecated (still returned) | Preferred |
|---|---|
| `skyflow_id` | `skyflowId` |

---

## Update request data key (v2.1+)

When calling `update()`, use `skyflowId` (camelCase) as the key in the data map to identify the record. Using `skyflow_id` still works but emits a deprecation warning. If both keys are present, `skyflowId` takes precedence.

```java
HashMap<String, Object> data = new HashMap<>();
data.put("skyflowId", "<SKYFLOW_ID>");   // preferred
data.put("card_number", "<NEW_VALUE>");

UpdateRequest request = UpdateRequest.builder()
    .table("<TABLE_NAME>")
    .data(data)
    .returnTokens(true)
    .build();

skyflowClient.vault().update(request);
```

---

## Method renames (v2.1+)

The following instance methods have been renamed for consistency. The old names still work but emit deprecation warnings.

| Deprecated | Preferred |
|---|---|
| `skyflowClient.updateLogLevel(logLevel)` | `skyflowClient.setLogLevel(logLevel)` |
| `TokenMode.getBYOT()` | `TokenMode.getByot()` |
| `DetokenizeRequest.builder().downloadURL(b)` | `DetokenizeRequest.builder().downloadUrl(b)` |

---

For the full list of changes see [CHANGELOG.md](../CHANGELOG.md).
