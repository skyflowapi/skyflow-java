# Skyflow Java SDK v3 — Load Testing

Measures SDK throughput, latency, and error-handling under concurrent load using [k6](https://k6.io/), without hitting a real Skyflow vault.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         k6 (Load Generator)                     │
│                                                                 │
│   VU 1 ──┐                                                      │
│   VU 2 ──┤                                                      │
│   VU 3 ──┼──── POST /insert        ─────────────────────────►  │
│   ...    ├──── POST /detokenize    ─────────────────────────►  │
│   VU N ──┘                                                      │
└─────────────────────────┬───────────────────────────────────────┘
                          │  concurrent HTTP  (default: 50 VUs)
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│               WrapperServer  (Java)  :8080                      │
│                                                                 │
│   Thread Pool (200 threads)                                     │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │  InsertHandler     → skyflowClient.vault().bulkInsert()  │  │
│   │  DetokenizeHandler → skyflowClient.vault().bulkDetokenize│  │
│   │  HealthHandler     → GET /health                         │  │
│   │  MetricsHandler    → GET /metrics  (JVM + counters)      │  │
│   └──────────────────────────────────────────────────────────┘  │
│                    │                                            │
│            Skyflow Java SDK v3                                  │
└────────────────────┬────────────────────────────────────────────┘
                     │  HTTP (SDK internal calls)
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│               EchoServer  (Java)  :3015                         │
│                         (fake Skyflow vault)                    │
│                                                                 │
│   POST /v2/records/insert      → echoes back mock response      │
│   POST /v2/tokens/detokenize   → echoes back mock response      │
│   GET  /health                 → liveness check                 │
│                                                                 │
│   Configurable:  ECHO_WAIT_MS (latency)  ECHO_ERR_PCT (errors) │
└─────────────────────────────────────────────────────────────────┘
```

### Request Flow

```
k6 VU
  │
  │  POST /insert  { table, num_records }
  ▼
WrapperServer
  │  builds InsertRequest
  │  calls skyflowClient.vault().bulkInsert()
  ▼
Skyflow SDK  (real SDK code under test)
  │  HTTP POST /v2/records/insert
  ▼
EchoServer  (mock vault — no real Skyflow needed)
  │  waits ECHO_WAIT_MS ms
  │  randomly fails ECHO_ERR_PCT % of requests
  │  returns mock JSON response
  ▼
Skyflow SDK  (parses response, builds InsertResponse)
  ▼
WrapperServer  (serializes response, increments counters)
  ▼
k6 VU  (checks status, records metrics)
```

### Startup Sequence (`run.sh`)

```
run.sh
  │
  ├─ 1. mvn install  (v3 SDK → local Maven repo)
  ├─ 2. mvn package  (build WrapperServer fat JAR)
  ├─ 3. javac        (compile EchoServer)
  ├─ 4. java EchoServer    :3015  ──► health check
  ├─ 5. java WrapperServer :8080  ──► health check
  └─ 6. k6 run  insert.js / detokenize.js / both
             │
             └─ on exit: kill EchoServer + WrapperServer
```

- **k6** — drives concurrent load (VUs)
- **WrapperServer** — Java HTTP server wrapping the Skyflow Java v3 SDK; handles up to 200 threads in parallel
- **EchoServer** — mock Skyflow vault; no real credentials needed

---

## Prerequisites

- Java 11+
- Maven 3.x
- [k6](https://k6.io/docs/get-started/installation/) installed (`brew install k6` on macOS)

---

## Quick Start

```bash
# From the repo root
./load-testing/run.sh all
```

This will:
1. Install the v3 SDK to your local Maven repo
2. Build the WrapperServer fat JAR
3. Compile and start the EchoServer
4. Start the WrapperServer
5. Run `insert.js` then `detokenize.js`
6. Print metrics and shut down both servers

---

## Usage

```bash
./load-testing/run.sh [insert|detokenize|all]  [extra k6 flags]
```

| Command | Description |
|---|---|
| `./load-testing/run.sh insert` | Run insert load test only |
| `./load-testing/run.sh detokenize` | Run detokenize load test only |
| `./load-testing/run.sh all` | Run both tests sequentially |

---

## Environment Variables

### Infrastructure (`run.sh`)

These control the EchoServer and WrapperServer. Set them as shell environment variables before running `run.sh`.

| Variable | Default | Description |
|---|---|---|
| `ECHO_PORT` | `3015` | Port the EchoServer (fake vault) listens on |
| `WRAPPER_PORT` | `8080` | Port the WrapperServer listens on |
| `ECHO_WAIT_MS` | `0` | Artificial latency added to every EchoServer response (ms). Use to simulate a slow vault. |
| `ECHO_ERR_PCT` | `0` | Percentage of requests the EchoServer randomly fails (0–100). Use to test SDK error handling. |
| `VAULT_ID` | `mock-vault-id` | Vault ID passed to the SDK. |

### `insert.js` (k6)

These control the insert load test. Pass them after the test name using `--env KEY=VALUE`.

| Variable | Default | Description |
|---|---|---|
| `WRAPPER_URL` | `http://localhost:8080` | WrapperServer base URL |
| `VUS` | `50` | Number of concurrent virtual users (concurrency level) |
| `DURATION` | `120` | Total test duration in seconds. Includes a fixed 30s ramp-up and 30s ramp-down, so minimum useful value is `61`. |
| `NUM_RECORDS` | `1` | Number of records per `bulkInsert()` SDK call. Increase to test batching. |
| `TABLE` | `load_test_table` | Vault table name sent in each insert request. |

### `detokenize.js` (k6)

These control the detokenize load test. Pass them after the test name using `--env KEY=VALUE`.

| Variable | Default | Description |
|---|---|---|
| `WRAPPER_URL` | `http://localhost:8080` | WrapperServer base URL |
| `VUS` | `50` | Number of concurrent virtual users |
| `DURATION` | `120` | Total test duration in seconds (min: 61) |
| `NUM_TOKENS` | `1` | Number of tokens per `bulkDetokenize()` SDK call. Increase to test batching. |
| `TOKEN` | `mock-token-0000-0000-0000-000000000001` | Base token string. An index suffix is appended for each token in a batch (e.g. `mock-token-...-0`, `mock-token-...-1`). |

---

## Examples

**Baseline run with defaults:**
```bash
./load-testing/run.sh all
```

**High concurrency insert test (200 VUs, 3 min):**
```bash
VUS=200 DURATION=180 ./load-testing/run.sh insert
```

**Simulate slow vault (100ms latency):**
```bash
ECHO_WAIT_MS=100 ./load-testing/run.sh all
```

**Simulate 5% vault error rate:**
```bash
ECHO_ERR_PCT=5 ./load-testing/run.sh detokenize
```

**Batch insert — 10 records per SDK call:**
```bash
./load-testing/run.sh insert --env NUM_RECORDS=10
```

**Batch detokenize — 5 tokens per SDK call, 100 VUs:**
```bash
./load-testing/run.sh detokenize --env NUM_TOKENS=5 --env VUS=100
```

**Combined stress test with latency and errors:**
```bash
ECHO_WAIT_MS=50 ECHO_ERR_PCT=2 VUS=150 DURATION=300 ./load-testing/run.sh all
```

---

## Thresholds

Both tests fail if any of these are breached:

| Metric | Threshold |
|---|---|
| `http_req_duration` p95 | < 500ms |
| `http_req_failed` rate | < 1% |
| `insert_errors` / `detokenize_errors` rate | < 1% |
| `insert_sdk_duration_ms` / `detokenize_sdk_duration_ms` p95 | < 400ms |

---

## Metrics Endpoint

While a test is running, query the WrapperServer for live JVM and SDK stats:

```bash
curl http://localhost:8080/metrics
```

**Response:**
```json
{
  "sdk_calls": { "total": 1200, "success": 1195, "error": 5 },
  "jvm": {
    "heap_used_mb": 112,
    "heap_total_mb": 256,
    "heap_max_mb": 512,
    "active_threads": 87,
    "gc_count": 14,
    "gc_time_ms": 320
  }
}
```

---

## Port Conflicts

If you see `Address already in use`, kill the stale processes:

```bash
lsof -ti :3015 | xargs kill -9
lsof -ti :8080 | xargs kill -9
```

---

## SDK Performance Testing

A dedicated performance testing layer on top of the existing setup, matching the SDK perf testing spec.

### Goals

| Objective | What is measured |
|---|---|
| Scalability | RPS vs. RAM / CPU consumption |
| Concurrency | Thread count growth under load |
| Stability | Memory leaks / death spirals under sustained high load |

### Workload Profile (Linear Step-Up)

Each step runs for `STEP_DURATION` seconds (default: 5 min) to let the JVM's heap settle.

```
RPS
1000 │                          ┌─────────────┐
     │                         /               \
 500 │               ┌────────┘                 └─────────┐
     │              /                                      \
 100 │    ┌────────┘                                        └──
     │   /
   0 │──┘
     └──────────────────────────────────────────────────────── time
          Baseline  Light    Medium      High       Ramp-down
           (1m)     (5m)     (5m)        (5m)         (5m)
```

### Resource Phases (Docker)

| Phase | CPU | RAM | Purpose |
|---|---|---|---|
| Phase 1 | 1 Core | 512 MB | Baseline — typical constrained environment |
| Phase 2 | 2 Cores | 512 MB | CPU scaling — same memory |
| Phase 3 | 4 Cores | 1 GB | Unconstrained — max throughput |

### Metrics Captured

**External (OS level, via `docker stats`)**

| Metric | Unit | Purpose |
|---|---|---|
| RAM (total physical) | MB | Primary scaling metric; detects leaks |
| CPU utilization | % per core | Identifies CPU-bound behaviour |

**Internal (JVM level, via `/metrics` polling every 5s)**

| Metric | Unit | Purpose |
|---|---|---|
| Heap used / total / max | MB | Detects managed memory leaks |
| Active threads | Count | Detects thread pool saturation |
| GC count & pause time | Count / ms | Measures GC pressure |
| SDK call counters | Count | Success vs. error ratio |

**k6 output**

| Metric | Unit | Purpose |
|---|---|---|
| `http_req_duration` p50/p95/p99 | ms | End-to-end latency distribution |
| `sdk_latency_ms` p95/p99 | ms | SDK-only latency |
| `http_req_failed` rate | % | HTTP error rate |
| `sdk_error_rate` | % | SDK-level error rate |
| `sdk_requests_total` | Count | Total requests processed |

### Running the Performance Test

**Prerequisites:** Docker, k6, Java, Maven.

**Option A — Full three-phase automated run:**

```bash
# From repo root — runs all 3 resource phases automatically
./load-testing/perf-run.sh insert 1000

# Detokenize at 500 RPS peak
./load-testing/perf-run.sh detokenize 500
```

Results are saved to `load-testing/results/<timestamp>/`:
```
phase1_1cpu_512mb_docker_stats.csv
phase1_1cpu_512mb_jvm_metrics.jsonl
phase1_1cpu_512mb_k6_output.json
phase1_1cpu_512mb_k6_summary.json
phase2_...
phase3_...
```

**Option B — Single phase with Docker Compose:**

```bash
cd <repo-root>

# Phase 1: 1 CPU / 512 MB
CPU_LIMIT=1 MEM_LIMIT=512m OP=insert MAX_RPS=1000 \
  docker compose -f load-testing/docker-compose.perf.yml up --build

# Phase 2: 2 CPUs / 512 MB
CPU_LIMIT=2 MEM_LIMIT=512m OP=insert MAX_RPS=1000 \
  docker compose -f load-testing/docker-compose.perf.yml up

# Phase 3: 4 CPUs / 1 GB
CPU_LIMIT=4 MEM_LIMIT=1g OP=insert MAX_RPS=1000 \
  docker compose -f load-testing/docker-compose.perf.yml up
```

**Option C — Local (no Docker), custom RPS:**

```bash
# Shorter step duration (60s) for quick validation
STEP_DURATION=60 ./load-testing/run.sh insert --env MAX_RPS=500
```

### Perf Test Env Vars

| Variable | Default | Description |
|---|---|---|
| `OP` | `insert` | Operation under test: `insert` or `detokenize` |
| `MAX_RPS` | `1000` | Peak RPS (stress target). Light=10%, Medium=50%, High=100% of this. |
| `STEP_DURATION` | `300` | Seconds spent at each RPS level |
| `CPU_LIMIT` | `1` | Docker CPU cores for SUT container |
| `MEM_LIMIT` | `512m` | Docker memory limit for SUT container |
| `NUM_RECORDS` | `1` | Records per `bulkInsert()` call |
| `NUM_TOKENS` | `1` | Tokens per `bulkDetokenize()` call |

### Thresholds (perf.js)

| Metric | p95 | p99 |
|---|---|---|
| `http_req_duration` | < 500ms | < 1000ms |
| `sdk_latency_ms` | < 400ms | < 800ms |
| `http_req_failed` | < 5% | — |
| `sdk_error_rate` | < 5% | — |

### File Structure

```
load-testing/
├── perf-run.sh                  # three-phase orchestrator (local + docker)
├── docker-compose.perf.yml      # two-tier Docker setup (SUT + k6)
├── Dockerfile                   # SUT image (WrapperServer + EchoServer)
├── docker-entrypoint.sh         # container startup script
├── k6/
│   ├── perf.js                  # RPS-based step-up perf script  ← NEW
│   ├── insert.js                # time-based insert test
│   └── detokenize.js            # time-based detokenize test
└── results/                     # output directory (auto-created)
    └── YYYY-MM-DD_HH-MM-SS/
        ├── phase1_*_docker_stats.csv
        ├── phase1_*_jvm_metrics.jsonl
        ├── phase1_*_k6_output.json
        └── phase1_*_k6_summary.json
```