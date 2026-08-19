# FlowVault Java SDK samples

Runnable samples for the `flowvault` module (`com.skyflow:skyflow-flowvault-java`) — bulk vault
operations against a Flow DB vault, plus the shared service-account/bearer-token utilities.

See the [flowvault README](../README.md) for the full API reference these samples exercise.

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
`flowvault/samples/`:

```bash
mvn compile
mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass="com.example.vault.BulkInsertSync"
```

Swap `-Dexec.mainClass` for the fully-qualified class name of the sample you want to run.

## The samples

### Service account (`com.example.serviceaccount`)

These use the `BearerToken` / `SignedDataTokens` utilities directly, for callers who want to
manage their own bearer tokens rather than passing a credentials file/string straight to
`Credentials` (see [Authenticate](../README.md#authenticate) for when you'd do which).

| Sample | Demonstrates |
|---|---|
| [BearerTokenGenerationExample.java](src/main/java/com/example/serviceaccount/BearerTokenGenerationExample.java) | Basic token generation from a credentials file or credentials string, using `Token.isExpired()` to avoid re-minting a still-valid token. |
| [BearerTokenGenerationUsingThreadsExample.java](src/main/java/com/example/serviceaccount/BearerTokenGenerationUsingThreadsExample.java) | Sharing one `BearerToken` instance safely across multiple threads. |
| [BearerTokenGenerationWithContextExample.java](src/main/java/com/example/serviceaccount/BearerTokenGenerationWithContextExample.java) | Context-aware tokens via `setCtx(String)` and `setCtx(Map<String, Object>)`, for CEL policies keyed on request context. |
| [ScopedTokenGenerationExample.java](src/main/java/com/example/serviceaccount/ScopedTokenGenerationExample.java) | Scoped tokens via `setRoles(...)`, restricting the token to specific role IDs. |
| [SignedTokenGenerationExample.java](src/main/java/com/example/serviceaccount/SignedTokenGenerationExample.java) | Signed data tokens via `SignedDataTokens`, with string and object context variants. |

### Vault operations (`com.example.vault`)

| Sample | Demonstrates |
|---|---|
| [BulkInsertSync.java](src/main/java/com/example/vault/BulkInsertSync.java) / [BulkInsertAsync.java](src/main/java/com/example/vault/BulkInsertAsync.java) | Bulk insert into one table (`tableName` at the request level), reading the summary, walking per-record results, and retrying failed records. |
| [BulkMultiTableInsertSync.java](src/main/java/com/example/vault/BulkMultiTableInsertSync.java) / [BulkMultiTableInsertAsync.java](src/main/java/com/example/vault/BulkMultiTableInsertAsync.java) | Bulk insert across multiple tables in one call (`tableName` set per record instead). |
| [BulkTokenizeSync.java](src/main/java/com/example/vault/BulkTokenizeSync.java) / [BulkTokenizeAsync.java](src/main/java/com/example/vault/BulkTokenizeAsync.java) | Tokenizing values against one or more token groups, reading the per-group outcome (a value can partially succeed), and retrying. |
| [BulkDetokenizeSync.java](src/main/java/com/example/vault/BulkDetokenizeSync.java) / [BulkDetokenizeAsync.java](src/main/java/com/example/vault/BulkDetokenizeAsync.java) | Detokenizing tokens with a per-token-group redaction override, reading per-token results, and retrying. |
| [BulkDeleteTokensSync.java](src/main/java/com/example/vault/BulkDeleteTokensSync.java) / [BulkDeleteTokensAsync.java](src/main/java/com/example/vault/BulkDeleteTokensAsync.java) | Deleting tokens, reading per-token results, and retrying. |
| [CustomHeaderExample.java](src/main/java/com/example/vault/CustomHeaderExample.java) | Attaching a custom HTTP header (e.g. a request id) to outgoing requests via a `RequestInterceptor` on the operation's options object. |
| [TimeoutAndRetryConfigExample.java](src/main/java/com/example/vault/TimeoutAndRetryConfigExample.java) | Configuring HTTP timeouts and transport-level retry behavior, per-vault and client-wide, and how the two levels resolve. |

Every `*Sync`/`*Async` pair above shows the same request-building and response-handling logic; the
`Async` variant just wraps it in `CompletableFuture` callbacks instead of a direct call. Response
handling follows the same shape everywhere: read `getSummary()` for totals, then walk `getRecords()`
checking `getError() == null` per entry — see [Error Handling](../README.md#error-handling) for the
full model these samples are built on.