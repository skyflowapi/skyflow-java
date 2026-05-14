Create a Skyflow Java SDK sample file demonstrating: $ARGUMENTS

## Requirements

### File placement
- Create under `samples/src/main/java/com/example/<feature>/`
- Name: `<FeatureName>Example.java`
- Package: `com.example.<feature>`

### Structure (follow this order)
1. Package declaration
2. Imports — only from `com.skyflow.*`, `java.*`; never from `com.skyflow.generated.*`
3. Public class with `main(String[] args) throws Exception`
4. Credentials setup using `Credentials` with `setPath()` pointing to `"credentials.json"`
5. `VaultConfig` with `setVaultId`, `setClusterId`, `setEnv(Env.PROD)`
6. `Skyflow` client via `Skyflow.builder().addVaultConfig(vaultConfig).build()`
7. Request object built via the appropriate `*Request.builder()` pattern
8. Options object if applicable (e.g. `InsertOptions`)
9. Call the vault method inside a try/catch for `SkyflowException`
10. Print the response using `System.out.println(response)`

### Rules
- All vault IDs / cluster IDs use placeholder strings: `"<your-vault-id>"`, `"<your-cluster-id>"`
- Credentials file path: `"credentials.json"` (relative — do not hardcode absolute paths)
- Always catch `SkyflowException` and print `e.getMessage()`
- Keep under 80 lines
- No business logic — just the minimal SDK usage pattern

### After creating the file
Run a compile check:
```bash
cd samples && mvn compile -q 2>&1 | tail -20
```

Report the file path and any compile errors.
