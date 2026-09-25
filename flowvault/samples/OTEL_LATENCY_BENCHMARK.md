# Measuring what the Java SDK adds on top of the vault API

A benchmark for settling the question "does the Skyflow Java SDK add latency over calling the API
directly?" with a number instead of an opinion.

It lives entirely in the sample application (`src/main/java/com/example/otel/benchmark`). The SDK is used
unmodified, through its public API, and captures its timings with OpenTelemetry.

## Running it

```bash
cd flowvault/samples
./run-otel-benchmark.sh                              # insert and detokenize, zero-latency backend
./run-otel-benchmark.sh insert                       # one operation
MOCK_LATENCY_MS=40 ./run-otel-benchmark.sh insert    # against a realistic backend
VUS=8 ITERATIONS=8000 ./run-otel-benchmark.sh        # under concurrency
```

The script resolves dependencies, compiles both `com.example.otel` packages, starts a local mock
vault, runs the benchmark against it and shuts the mock down. No collector, no Docker, no network.
Results print to the console and a percentile summary is written to `otel-out/*.csv`.

To run the pieces separately:

```bash
java -cp "$CP" com.example.otel.benchmark.MockVaultServer
TARGET_URL=http://127.0.0.1:3025 java -cp "$CP" com.example.otel.benchmark.SdkLatencyBenchmark insert
```

## What it measures

In one JVM, against one server, with the same payload, it runs two paths and interleaves them
iteration by iteration:

| path  | what runs                                                              |
|-------|------------------------------------------------------------------------|
| `sdk` | `vault.insert(...)` / `vault.detokenize(...)`                          |
| `raw` | the same JSON POSTed to the same endpoint with a bare OkHttp client     |

The difference between their distributions is the SDK's cost. Interleaving matters: JIT state, GC
pauses, CPU frequency scaling and server-side jitter then land on both columns instead of
penalising whichever ran first.

Each iteration produces OpenTelemetry spans:

```
skyflow.sdk.call                      total time inside the SDK call
├── skyflow.sdk.prepare               validation + credential resolution + request mapping
└── skyflow.sdk.transport_and_decode  serialisation, HTTP, deserialisation, response mapping

skyflow.raw.call                      total time for the equivalent bare HTTP call
├── skyflow.raw.acquire_connection    pool checkout (and connect, on a cold pool)
├── skyflow.raw.server_wait           request written → response headers back
└── skyflow.raw.read_response         reading the response body
```

The console table, the verdict block and the CSV are all computed from those span durations, so
the reported numbers are literally what OpenTelemetry recorded. A latency histogram
(`skyflow.client.operation.duration`, attributed by `skyflow.path` / `skyflow.operation` /
`skyflow.outcome`) is exported alongside them.

## Design decisions, and why they are what they are

**Nothing inside the SDK was changed.** Instrumentation compiled into the SDK would measure a
build the customer does not run, and invites the obvious objection. The one probe that reaches
inside a call is `RequestInterceptor`, a public SDK hook that fires immediately before the HTTP
request is issued. It costs one `System.nanoTime()` and splits an SDK call into work done before
the wire and everything from the wire onwards.

**The baseline is OkHttp, not HttpURLConnection.** The SDK's transport is OkHttp 4.x with a
10-connection pool. A baseline on a different HTTP client would measure "OkHttp vs
HttpURLConnection" as much as "SDK vs no SDK", and the result could be argued either way. Same
client, same pool settings, same timeouts leaves one variable: the SDK's own code. The SDK's retry
interceptor is deliberately *not* mirrored into the baseline, so any cost it carries is charged to
the SDK rather than hidden in both columns.

**The target is a local mock, not a real vault.** Against a real vault, network and server time are
two orders of magnitude larger than anything the SDK does and they move run to run, so a difference
of a few hundred microseconds is unmeasurable no matter how many samples you take. Against a
loopback server answering in ~100 µs, SDK overhead becomes most of the signal — the harshest
possible test, which is what you want when answering a complaint. `MOCK_LATENCY_MS` then shows the
same fixed overhead as a proportion of a realistic call.

**The instrumentation is outside every measured window.** Measured windows contain nothing but
`System.nanoTime()` calls; spans are materialised afterwards with explicit start/end timestamps.
Each run opens with a self-check reporting what that materialisation costs, so the claim is
checkable rather than asserted.

## Results on one developer machine

11th-gen laptop, loopback, `insert` of 10 records, 1500 measured iterations per path. Reproduce
before quoting — these are illustrative, not a specification.

Zero-latency backend, `VUS=1`:

| operation    | percentile | `sdk.call` | `raw.call` | overhead | share |
|--------------|------------|-----------|-----------|----------|-------|
| `insert`     | p50        | 225 µs    | 152 µs    | 73 µs    | 33 %  |
| `insert`     | p90        | 313 µs    | 200 µs    | 113 µs   | 36 %  |
| `insert`     | p99        | 401 µs    | 242 µs    | 159 µs   | 40 %  |
| `detokenize` | p50        | 202 µs    | 120 µs    | 82 µs    | 41 %  |
| `detokenize` | p90        | 308 µs    | 169 µs    | 138 µs   | 45 %  |
| `detokenize` | p99        | 380 µs    | 208 µs    | 172 µs   | 45 %  |

`insert` again with `MOCK_LATENCY_MS=40` and `VUS=8` — the same overhead against a backend that
behaves like a real one:

| percentile | `sdk.call` | `raw.call` | overhead | share |
|------------|-----------|-----------|----------|-------|
| p50        | 41.14 ms  | 40.92 ms  | 222 µs   | 0.5 % |
| p90        | 41.54 ms  | 41.30 ms  | 237 µs   | 0.6 % |
| p99        | 42.44 ms  | 42.02 ms  | 421 µs   | 1.0 % |

Validation and request mapping — everything before a byte is written — is 10–36 µs of that.

The share column is high in the first table and low in the second for the same reason: the overhead
is roughly constant, so it looks large next to a 150 µs loopback call and disappears next to a
41 ms one. The absolute number is the SDK's cost; the share depends entirely on what it is being
compared against.

## What this does and does not establish

It establishes that for these operations, on this hardware, the SDK's own code costs on the order
of a hundred microseconds per call, which is a fraction of a percent of a call to a real vault.

It does not establish anything about a customer's own numbers. If someone reports the SDK being
slow, this harness tells you the answer is somewhere other than SDK overhead, and the usual
suspects are worth checking directly:

- **Bearer token minting.** With a credentials file the SDK signs a JWT and fetches a token inside
  the first request, and `setBearerToken()` is `synchronized`, so every thread's first call queues
  behind it. This benchmark uses an API key to keep that out of the measurement — which also means
  it has not measured it. A customer seeing a slow first call per thread is seeing this.
- **Batching and concurrency settings.** The bulk operations split work into batches and run them
  with a concurrency limit that defaults to 1. A bulk call that looks slow may be serialising
  batches that could run in parallel.
- **Connection pool sizing.** The SDK's pool holds 10 idle connections. Above that concurrency,
  calls pay connection setup.
- **The network itself.** `skyflow.raw.server_wait` in the output above is the figure that
  dominates against a real vault.

## Two things found while building this

Both are incidental to the benchmark but worth knowing.

**The JDK's `com.sun.net.httpserver` is unusable for latency work.** It sends response headers and
body as separate TCP segments; against Nagle's algorithm and delayed ACK that produces a dead-on
40 ms stall between `responseHeadersEnd` and `responseBodyEnd` on keep-alive connections. The first
version of the mock used it and reported the SDK at 42 ms against 1.3 ms for the baseline —
entirely an artefact, and one that happened to land on the SDK. `MockVaultServer` is therefore
hand-rolled on plain sockets: `TCP_NODELAY` on every accepted connection, and status line, headers
and body written in a single `write()`. That alone moved the measurement from 42 ms to 0.7 ms.

**Building a Skyflow client removes every root `java.util.logging` handler.** `LogUtil.setupLogger`
opens with `LogManager.getLogManager().reset()`, which is global, not scoped to the SDK's own
logger. Anything else in the process that logs through JUL goes silent from that moment —
including OpenTelemetry's console exporters, which is how it was noticed here. The benchmark calls
`Telemetry.restoreConsoleLogging()` after building the client to work around it. Applications that
use JUL and build a Skyflow client will hit this too.

## Configuration

| variable | default | meaning |
|----------|---------|---------|
| `TARGET_URL` | — | required; the mock's base URL. Not `VAULT_URL`: the SDK reads that one from the environment ahead of its own config and rejects non-https values, which rules out a loopback mock. |
| `MODE` | `interleaved` | `interleaved`, `sdk` or `raw` |
| `VUS` | 1 | concurrent threads |
| `ITERATIONS` | 2000 | measured iterations, split across paths |
| `WARMUP_ITERATIONS` | 500 | discarded before measuring; raise it if `sdk.prepare` p50 is above ~20 µs, which means JIT had not settled |
| `ROWS` | 10 | records per insert |
| `BATCH_SIZE` | 15 | tokens per detokenize |
| `LOG_SPANS` | false | print every span as it ends; distorts the numbers at volume |
| `CSV_DIR` | `.` | where the summary CSV is written |
| `MOCK_LATENCY_MS` | 0 | artificial server-side latency (mock) |
| `MOCK_DEBUG` | false | print the first few requests' headers and server-side handling time (mock) |
