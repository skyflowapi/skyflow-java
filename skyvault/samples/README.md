# Skyvault Java SDK samples

Runnable samples for the `skyvault` module (`com.skyflow:skyflow-java`) — vault CRUD, tokenize/detokenize,
query, file upload, Detect, Connections, and the shared service-account/bearer-token utilities.

See the [skyvault README](../README.md) for the full guide these samples exercise.

## Prerequisites

- A Skyflow account. If you don't have one, register on the [Try Skyflow](https://skyflow.com/try-skyflow) page.
- Java 8 or higher, and Maven.

### Create a vault

1. In a browser, sign in to Skyflow Studio.
2. Create a vault: **Create Vault** > **Start With a Template** > **Quickstart vault**.
3. Once the vault is ready, click the gear icon and select **Edit Vault Details**.
4. Note the **Vault ID** and **Vault URL**. The cluster ID that samples pass to `setClusterId(...)`
   is the subdomain of the vault URL — e.g. for `https://a1b2c3d4.vault.skyflowapis.com`, the
   cluster ID is `a1b2c3d4`.

### Create a service account

1. In the side navigation: **IAM** > **Service Accounts** > **New Service Account**.
2. Name it, and for **Roles** choose **Vault Editor**.
3. Click **Create**. Your browser downloads a `credentials.json` — keep it secure, you'll need its
   path or contents for most samples below.

## Running a sample

Each sample is a `public class X { public static void main(String[] args) ... }` under
`src/main/java/com/example/`. Fill in the placeholders (`<YOUR_VAULT_ID>`, `<YOUR_CLUSTER_ID>`,
`<YOUR_CREDENTIALS_FILE_PATH>`, etc.) with real values, then compile and run with Maven from
`skyvault/samples/`:

```bash
mvn compile
mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass="com.example.vault.InsertExample"
```

Swap `-Dexec.mainClass` for the fully-qualified class name of the sample you want to run.

## The samples

### Vault operations (`com.example.vault`)

| Sample | Demonstrates |
|---|---|
| [InsertExample.java](src/main/java/com/example/vault/InsertExample.java) | Inserting records with `TokenMode` enabled and with `upsert`. |
| [GetExample.java](src/main/java/com/example/vault/GetExample.java) | Retrieving records by Skyflow ID and by column name/value. |
| [UpdateExample.java](src/main/java/com/example/vault/UpdateExample.java) | Updating records with and without `TokenMode`. |
| [DeleteExample.java](src/main/java/com/example/vault/DeleteExample.java) | Deleting records from one or more vaults by Skyflow ID. |
| [DetokenizeExample.java](src/main/java/com/example/vault/DetokenizeExample.java) | Detokenizing data tokens. |
| [TokenizeExample.java](src/main/java/com/example/vault/TokenizeExample.java) | Tokenizing values against a column group. |
| [QueryExample.java](src/main/java/com/example/vault/QueryExample.java) | Running a SQL query against a vault. |
| [FileUploadExample.java](src/main/java/com/example/vault/FileUploadExample.java) | Uploading a file into a vault. |
| [ClientOperations.java](src/main/java/com/example/vault/ClientOperations.java) | Configuring a primary and secondary vault on one client, updating configs at runtime, and removing a vault config. |
| [CredentialsOptions.java](src/main/java/com/example/vault/CredentialsOptions.java) | Using the different `Credentials` authentication options across multiple vault configs. |

`vault/deprecated/` keeps pre-v2.1 versions of the Detokenize, Get, and Update samples. Each one
shows the deprecated API it replaces — for example `downloadURL()` vs. `downloadUrl()`, and the
`skyflow_id` vs. `skyflowId` keys. See [Migrate from v1 to v2](../../docs/migrate_to_v2.md) for the
full set of renames.

### Detect (`com.example.detect`)

Detect finds and de-identifies sensitive data in strings and files, re-identifies vault tokens, and
screens text against guardrails. See [Detect](../README.md#detect) in the skyvault README for the
request and response classes, [Detect errors](../README.md#detect-errors) for the failures to expect,
and [Migrate to the new Detect methods](../../docs/migrate_detect.md) if you used `deidentifyText`,
`reidentifyText` or `getDetectRun`.

| Sample | Demonstrates |
|---|---|
| [DeidentifyStringSample.java](src/main/java/com/example/detect/DeidentifyStringSample.java) | `deidentifyString` with a stored configuration id and with an inline `DetectConfiguration`. |
| [DeidentifyFileSample.java](src/main/java/com/example/detect/DeidentifyFileSample.java) | `deidentifyFile` for a local file: submit only, and submit-and-poll with `PollOptions` and `outputDirectory`. |
| [DeidentifyFileAsyncSample.java](src/main/java/com/example/detect/DeidentifyFileAsyncSample.java) | Polling several files on a bounded thread pool. |
| [GetRunSample.java](src/main/java/com/example/detect/GetRunSample.java) | `getRun` for a run id returned by a submit-only `deidentifyFile`. |
| [ReidentifyStringSample.java](src/main/java/com/example/detect/ReidentifyStringSample.java) | The round trip: de-identify with `VAULT_TOKEN`, then `reidentifyString` the processed text, plainly and per entity type with `RedactionLevel`. |
| [ReidentifyFileSample.java](src/main/java/com/example/detect/ReidentifyFileSample.java) | `reidentifyFile` for a de-identified text file sent as base64. |
| [CheckGuardrailsSample.java](src/main/java/com/example/detect/CheckGuardrailsSample.java) | `checkGuardrails` for toxicity and denied topics. |

In addition to the vault placeholders, replace `<CONFIGURATION_ID>` with a Detect configuration
created in Studio or through the management API (or keep the inline configuration), and replace
`<TABLE>.<COLUMN>` with a column of your vault that has tokenization enabled; `VAULT_TOKEN` values
are stored there.

### Connections (`com.example.connection`)

| Sample | Demonstrates |
|---|---|
| [InvokeConnectionExample.java](src/main/java/com/example/connection/InvokeConnectionExample.java) | Setting up a connection configuration and invoking it with a request body and headers. |

### Service account (`com.example.serviceaccount`)

These use the `BearerToken` / `SignedDataTokens` utilities directly, for callers who want to
manage their own bearer tokens rather than passing a credentials file/string straight to
`Credentials`. See [Authenticate with bearer tokens](../README.md#authenticate-with-bearer-tokens)
for when you'd do which.

| Sample | Demonstrates |
|---|---|
| [BearerTokenGenerationExample.java](src/main/java/com/example/serviceaccount/BearerTokenGenerationExample.java) | Basic token generation from a credentials file or credentials string. |
| [BearerTokenExpiryExample.java](src/main/java/com/example/serviceaccount/BearerTokenExpiryExample.java) | Retrying a detokenize call after a 401, letting the SDK's internal `Token.isExpired()` check mint a fresh bearer token on the retry. |
| [BearerTokenGenerationUsingThreadsExample.java](src/main/java/com/example/serviceaccount/BearerTokenGenerationUsingThreadsExample.java) | Sharing one `BearerToken` instance safely across multiple threads. |
| [BearerTokenGenerationWithContextExample.java](src/main/java/com/example/serviceaccount/BearerTokenGenerationWithContextExample.java) | Context-aware tokens via `setCtx(String)` and `setCtx(Map<String, Object>)`. |
| [ScopedTokenGenerationExample.java](src/main/java/com/example/serviceaccount/ScopedTokenGenerationExample.java) | Scoped tokens via `setRoles(...)`, restricting the token to specific role IDs. |
| [SignedTokenGenerationExample.java](src/main/java/com/example/serviceaccount/SignedTokenGenerationExample.java) | Signed data tokens via `SignedDataTokens`, with string and object context variants. |
