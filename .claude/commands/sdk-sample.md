Create a Skyflow Java SDK sample file demonstrating: $ARGUMENTS

## File placement

| Feature type | Package | Directory |
|---|---|---|
| Vault ops (insert/get/update/delete/query/tokenize) | `com.example.vault` | `samples/src/main/java/com/example/vault/` |
| Service account auth | `com.example.serviceaccount` | `samples/src/main/java/com/example/serviceaccount/` |
| Connection | `com.example.connection` | `samples/src/main/java/com/example/connection/` |
| Detect | `com.example.detect` | `samples/src/main/java/com/example/detect/` |

File name: `<FeatureName>Example.java`

## Structure (follow this order)

1. Package declaration
2. Imports — only from `com.skyflow.*`, `java.*`; never from `com.skyflow.generated.*`
3. Public class with `main(String[] args) throws SkyflowException`
4. Credentials setup — choose based on feature:
   - **Vault ops:** `credentials.setApiKey("<YOUR_API_KEY>")` or `credentials.setCredentialsString("<YOUR_CREDENTIALS_STRING>")`
   - **Service account:** `credentials.setPath("credentials.json")` (path to the service account JSON file)
5. `VaultConfig` with `setVaultId`, `setClusterId`, `setEnv(Env.PROD)`, `setCredentials(credentials)`
6. Build the Skyflow client:
   ```java
   Skyflow skyflowClient = Skyflow.builder()
       .setLogLevel(LogLevel.DEBUG)
       .addVaultConfig(vaultConfig)
       .build();
   ```
7. Request object via `*Request.builder()` — options go directly on the builder (no separate Options class):
   ```java
   // Example: InsertRequest with tokenMode
   InsertRequest request = InsertRequest.builder()
       .table("...")
       .values(records)
       .tokenMode(TokenMode.ENABLE)
       .build();
   ```
8. Call the vault method inside a try/catch for `SkyflowException`:
   ```java
   InsertResponse response = skyflowClient.vault().insert(request);
   System.out.println(response);
   ```

## Rules

- Vault IDs / cluster IDs use placeholders: `"<YOUR_VAULT_ID>"`, `"<YOUR_CLUSTER_ID>"`
- Credential values use placeholders: `"<YOUR_API_KEY>"`, `"<YOUR_CREDENTIALS_STRING>"`
- Credentials file path: `"credentials.json"` (relative — no absolute paths)
- Always catch `SkyflowException` and print `e.getMessage()`
- No separate `*Options` classes — they don't exist in this SDK; use request builder methods
- Keep under 80 lines

## After creating the file

```bash
cd samples && mvn compile -q 2>&1 | tail -20
```

Report the file path and any compile errors.
