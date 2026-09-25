package com.example.otel.tracing;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.DetokenizeResponse;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.InsertRequestRecord;
import com.skyflow.vault.data.InsertResponse;
import com.skyflow.vault.data.InsertResponseRecord;
import com.skyflow.vault.data.Token;

import io.opentelemetry.sdk.OpenTelemetrySdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * End-to-end example of measuring Skyflow SDK latency from application code.
 *
 * <p>The whole integration is three lines: install OpenTelemetry, wrap the vault, call it. Run it
 * and every {@code insert}/{@code detokenize} produces a span with a prepare child and feeds the
 * duration histogram.
 *
 * <pre>
 *   VAULT_ID=... CLUSTER_ID=... API_KEY=... TABLE=... \
 *   OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
 *     java -cp ... com.example.otel.tracing.TracedVaultExample
 * </pre>
 * Leave {@code OTEL_EXPORTER_OTLP_ENDPOINT} unset to print spans and metrics to the console.
 */
public final class TracedVaultExample {

    private static final int ITERATIONS = Integer.parseInt(env("ITERATIONS", "10"));

    public static void main(String[] args) throws Exception {
        // 1. Install OpenTelemetry. Skip this if your application already has an SDK.
        OpenTelemetrySdk otel = SkyflowTracing.install();

        Skyflow skyflow = buildClient();
        // The SDK's logging setup clears every root JUL handler; put the console one back so the
        // logging exporters above are not silently muted. See SkyflowTracing.restoreConsoleLogging.
        SkyflowTracing.restoreConsoleLogging();

        // 2. Wrap the vault. Nothing else in your application changes.
        TracedVault vault = TracedVault.of(skyflow.vault(env("VAULT_ID", null)), otel);

        String table = env("TABLE", "table1");
        String column = env("COLUMN", "name");

        try {
            for (int i = 0; i < ITERATIONS; i++) {
                // 3. Call it exactly as you called the vault before.
                InsertResponse inserted = vault.insert(insertRequest(table, column, "value-" + i));

                List<String> tokens = tokensOf(inserted);
                if (!tokens.isEmpty()) {
                    DetokenizeResponse detokenized = vault.detokenize(
                            DetokenizeRequest.builder().tokens(tokens).build());
                    System.out.printf("iteration %d: inserted %d record(s), detokenized %d%n",
                            i, inserted.getRecords().size(), detokenized.getRecords().size());
                } else {
                    System.out.printf("iteration %d: inserted %d record(s)%n",
                            i, inserted.getRecords().size());
                }
            }
        } catch (SkyflowException e) {
            // The failure is already on the span: status ERROR, skyflow.http_status and, when the
            // vault returned one, skyflow.request_id.
            System.err.println("call failed: http " + e.getHttpCode() + " request " + e.getRequestId());
            throw e;
        } finally {
            SkyflowTracing.shutdown(otel);
        }
    }

    private static InsertRequest insertRequest(String table, String column, String value) {
        Map<String, Object> values = new HashMap<>();
        values.put(column, value);
        List<InsertRequestRecord> records = new ArrayList<>();
        records.add(InsertRequestRecord.builder().tableName(table).data(values).build());
        return InsertRequest.builder().records(records).build();
    }

    /** First token from the insert response, whichever column it came back under. */
    private static List<String> tokensOf(InsertResponse response) {
        for (InsertResponseRecord record : response.getRecords()) {
            Map<String, List<Token>> tokens = record.getTokens();
            if (tokens == null) {
                continue;
            }
            for (List<Token> forColumn : tokens.values()) {
                if (forColumn != null && !forColumn.isEmpty() && forColumn.get(0).getToken() != null) {
                    return Collections.singletonList(forColumn.get(0).getToken());
                }
            }
        }
        return Collections.emptyList();
    }

    private static Skyflow buildClient() throws SkyflowException {
        Credentials credentials = new Credentials();
        credentials.setApiKey(required("API_KEY"));

        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId(required("VAULT_ID"));
        vaultConfig.setClusterId(required("CLUSTER_ID"));
        vaultConfig.setEnv(Env.PROD);
        vaultConfig.setCredentials(credentials);

        String targetUrl = env("TARGET_URL", null);
        if (targetUrl != null) {
            vaultConfig.setVaultUrl(targetUrl);
        }

        return Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)
                .addVaultConfig(vaultConfig)
                .build();
    }

    private static String required(String name) {
        String value = env(name, null);
        if (value == null) {
            throw new IllegalStateException("environment variable " + name + " is required");
        }
        return value;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
