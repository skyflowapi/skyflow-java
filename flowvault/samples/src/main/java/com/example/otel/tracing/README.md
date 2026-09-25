# Measuring Skyflow Java SDK latency with OpenTelemetry

This package instruments the Skyflow vault SDK from **application code only**. Nothing in the SDK is
modified, patched, subclassed or reflected into. Everything here is something you can do in your own
service today, which also means the numbers it produces describe the SDK you actually run.

## Integration

Three lines. `TracedVault` mirrors `VaultController`'s signatures, so call sites do not change.

```java
// 1. If your service already has OpenTelemetry (Spring Boot starter, OTel agent, your own SDK
//    wiring), skip this and pass your existing instance in step 2.
OpenTelemetrySdk otel = SkyflowTracing.install();

// 2. Wrap the vault once, wherever you build it.
TracedVault vault = TracedVault.of(skyflow.vault(vaultId), otel);

// 3. Call it exactly as before.
InsertResponse response = vault.insert(request);
```

If your application logs through `java.util.logging`, call `SkyflowTracing.restoreConsoleLogging()`
after building the Skyflow client. See [Two things worth knowing](#two-things-worth-knowing).

Already passing a `RequestInterceptor` of your own? Keep doing it — pass your options object as
usual and `TracedVault` chains to your interceptor rather than replacing it.

## What you get

Each call produces one `CLIENT` span with a child:

```
skyflow.insert                     the full call, as your application experiences it
└── skyflow.insert prepare         validation + auth + request mapping, before any bytes go out
```

and three instruments:

| Instrument | Type | Attributes |
|---|---|---|
| `skyflow.client.operation.duration` | histogram, ms | `skyflow.operation`, `skyflow.outcome` |
| `skyflow.client.operation.prepare.duration` | histogram, ms | `skyflow.operation` |
| `skyflow.client.operation.errors` | counter | `skyflow.operation`, `error.type`, `skyflow.http_status` |

Span attributes: `skyflow.operation`, `skyflow.batches` (when the SDK batched the request), and on
failures `skyflow.http_status`, `skyflow.request_id` and `error.type`, with the span status set to
`ERROR` and the exception recorded. Batched requests also carry one `skyflow.batch.dispatched` event
per batch.

Covered operations: `insert`, `bulkInsert(+Async)`, `detokenize`, `bulkDetokenize(+Async)`,
`bulkTokenize(+Async)`, `bulkDeleteTokens(+Async)`, `get`, `update`, `delete`. Async variants end
their span when the future completes, not when it is returned. Anything not wrapped is reachable
through `vault.unwrap()`.

The default histogram boundaries in OpenTelemetry jump from 0ms straight to 5ms, which puts every
SDK-side measurement in a single bucket. `SkyflowTracing` registers boundaries that resolve the
sub-millisecond range instead. If you bring your own SDK, copy that view or your percentiles will be
meaningless at this scale.

## What this cannot show you, and why

This matters as much as what it measures. The SDK constructs its own `OkHttpClient` internally and
installs its retry and auth interceptors inside it. There is no supported way for application code to
reach into that client. So everything after the `prepare` span is **one opaque block**:

| Not visible from application code | Why |
|---|---|
| DNS, TCP connect, TLS handshake, time-to-first-byte | Requires an `okhttp3.EventListener` on the SDK's client; it cannot be injected |
| Retries — how many, how long each attempt took | `SkyflowRetryInterceptor` sits inside that same client; a retried call looks like one slow call |
| Connection reuse vs. a new connection | Same reason |
| Bearer token minting | Runs *before* the interceptor fires, so it is charged to `prepare` and cannot be separated from validation |
| Skyflow request id on success | The SDK populates it on error records only |

Practical consequence: if `skyflow.insert` is slow but `skyflow.insert prepare` is fast, this tells
you the time went somewhere between "request handed to OkHttp" and "response decoded" — but not
whether that was the network, the vault, or three retries. Narrowing that further needs either the
OpenTelemetry Java agent (which instruments OkHttp by bytecode, no SDK change required) or
instrumentation inside the SDK itself.

## Running the example

`TracedVaultExample` is a complete working program.

```bash
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
javac -cp "$(cat /tmp/cp.txt)" -d target/classes \
  src/main/java/com/example/otel/tracing/*.java

VAULT_ID=... CLUSTER_ID=... API_KEY=... TABLE=... ITERATIONS=20 \
  java -cp "target/classes:$(cat /tmp/cp.txt)" com.example.otel.tracing.TracedVaultExample
```

| Variable | Default | Meaning |
|---|---|---|
| `VAULT_ID`, `CLUSTER_ID`, `API_KEY` | — | required |
| `TABLE`, `COLUMN` | `table1`, `name` | where to write |
| `ITERATIONS` | `10` | calls to make |
| `TARGET_URL` | — | override the vault URL, e.g. to point at a local stand-in |
| `OTEL_SERVICE_NAME` | `skyflow-sdk-client` | `service.name` on every span |
| `OTEL_METRIC_INTERVAL_SECONDS` | `30` | how often metrics are printed |

Expect the first one or two calls to be far slower than the rest — that is JVM class loading and
connection setup, not the SDK. Measure a warm process.

## Exporting somewhere real

The default prints to the console, which is enough to answer "what does the SDK cost me" but is not
a monitoring setup. To send this to a collector, add one dependency:

```xml
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
  <version>1.35.0</version>
</dependency>
```

and in `SkyflowTracing.install()` swap the exporters:

```java
LoggingSpanExporter.create()
  -> OtlpHttpSpanExporter.builder().setEndpoint(endpoint + "/v1/traces").build()
LoggingMetricExporter.create()
  -> OtlpHttpMetricExporter.builder().setEndpoint(endpoint + "/v1/metrics").build()
```

wrapping the span exporter in `BatchSpanProcessor` rather than `SimpleSpanProcessor`, so export does
not sit on the calling thread. Nothing in `TracedVault` changes.

## Two things worth knowing

**Building a Skyflow client silences `java.util.logging`.** The SDK's logging setup begins with
`LogManager.getLogManager().reset()`, a global call that removes every handler on the root logger —
including ones your application installed. Anything logging through JUL goes quiet from that point,
with no error to explain it. `SkyflowTracing.restoreConsoleLogging()` puts a console handler back;
call it *after* building the client. Irrelevant if you export over OTLP or log through SLF4J.

**Instrumentation is not free, but it is small.** Each call creates two spans and records two
histogram points, on the order of a few microseconds. That is negligible against a network call and
noticeable against nothing at all — so if you benchmark this against a local mock, account for it.

## Files

| File | Purpose |
|---|---|
| `TracedVault.java` | The instrumentation. The only file you need to copy. |
| `SkyflowTracing.java` | OpenTelemetry bootstrap, for services that do not have one. |
| `TracedVaultExample.java` | A complete runnable example. |
