#!/usr/bin/env bash
# =============================================================================
# Skyflow Java SDK v3 — Sample Runner
#
# Starts the EchoServer (mock vault), then runs one or all SDK samples,
# capturing metrics output to a timestamped results file.
#
# Usage:
#   ./load-testing/run-samples.sh [sample] [options]
#
# Samples:
#   all             Run all samples + print comparison table  (default)
#   insert          InsertSample          — sync bulk insert
#   detokenize      DetokenizeSample      — sync bulk detokenize
#   async-insert    AsyncInsertSample     — concurrent async insert
#   async-detokenize AsyncDetokenizeSample — concurrent async detokenize
#   concurrent      ConcurrentSample      — all 4 concurrent patterns
#   retry           RetryOnFailureSample  — retry on partial failure
#   benchmark       BenchmarkSample       — sustained load across 3 concurrency tiers
#
# Examples:
#   ./load-testing/run-samples.sh
#   ./load-testing/run-samples.sh async-insert
#   ./load-testing/run-samples.sh all
#   ECHO_WAIT_MS=50 ./load-testing/run-samples.sh concurrent
#   ./load-testing/run-samples.sh benchmark
#   BENCH_DURATION=60 BENCH_OP=detokenize ./load-testing/run-samples.sh benchmark
#
# Environment variables:
#   ECHO_PORT      EchoServer port            (default: 3015)
#   ECHO_WAIT_MS   Simulated vault latency ms  (default: 0)
#   ECHO_ERR_PCT   Random error rate %         (default: 0)
#   SAVE_RESULTS   Save output to file         (default: true)
#   BENCH_DURATION Benchmark seconds per tier  (default: 30)
#   BENCH_OP       Benchmark operation         (default: insert)
#   BENCH_BATCH    Records/tokens per SDK call (default: 1)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

ECHO_PORT="${ECHO_PORT:-3015}"
ECHO_WAIT_MS="${ECHO_WAIT_MS:-0}"
ECHO_ERR_PCT="${ECHO_ERR_PCT:-0}"
SAVE_RESULTS="${SAVE_RESULTS:-true}"
BENCH_DURATION="${BENCH_DURATION:-30}"
BENCH_OP="${BENCH_OP:-insert}"
BENCH_BATCH="${BENCH_BATCH:-1}"

SAMPLE="${1:-all}"

TIMESTAMP="$(date +%Y-%m-%d_%H-%M-%S)"
RESULTS_DIR="$SCRIPT_DIR/results"
mkdir -p "$RESULTS_DIR"
RESULTS_FILE="$RESULTS_DIR/samples-${SAMPLE}-${TIMESTAMP}.txt"

ECHO_PID=""

# ---------------------------------------------------------------------------
# Cleanup
# ---------------------------------------------------------------------------
cleanup() {
    echo ""
    echo "[run-samples] Stopping EchoServer..."
    [ -n "$ECHO_PID" ] && kill "$ECHO_PID" 2>/dev/null || true
    wait 2>/dev/null || true
    echo "[run-samples] Done."
}
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------------------
# Map sample name → main class
# ---------------------------------------------------------------------------
sample_class() {
    case "$1" in
        all)              echo "com.skyflow.loadtest.samples.RunAllSamples"         ;;
        insert)           echo "com.skyflow.loadtest.samples.InsertSample"          ;;
        detokenize)       echo "com.skyflow.loadtest.samples.DetokenizeSample"      ;;
        async-insert)     echo "com.skyflow.loadtest.samples.AsyncInsertSample"     ;;
        async-detokenize) echo "com.skyflow.loadtest.samples.AsyncDetokenizeSample" ;;
        concurrent)       echo "com.skyflow.loadtest.samples.ConcurrentSample"      ;;
        retry)            echo "com.skyflow.loadtest.samples.RetryOnFailureSample"  ;;
        benchmark)        echo "com.skyflow.loadtest.samples.BenchmarkSample"        ;;
        *)
            echo "[run-samples] Unknown sample '$1'." >&2
            echo "  Valid: all | insert | detokenize | async-insert | async-detokenize | concurrent | retry" >&2
            exit 1
            ;;
    esac
}

MAIN_CLASS="$(sample_class "$SAMPLE")"

# ---------------------------------------------------------------------------
# 1. Build wrapper (compile only — fast)
# ---------------------------------------------------------------------------
echo "[run-samples] Compiling wrapper..."
mvn compile \
    -f "$SCRIPT_DIR/wrapper/pom.xml" \
    -Dgpg.skip=true \
    -q
echo "[run-samples] Compile done."

# ---------------------------------------------------------------------------
# 2. Compile and start EchoServer
# ---------------------------------------------------------------------------
echo "[run-samples] Compiling EchoServer..."
javac -d "$SCRIPT_DIR/echo-server/" "$SCRIPT_DIR/echo-server/EchoServer.java"

# Kill any stale process on the port before starting
if lsof -ti :"$ECHO_PORT" > /dev/null 2>&1; then
    echo "[run-samples] Port $ECHO_PORT in use — killing stale process..."
    lsof -ti :"$ECHO_PORT" | xargs kill -9 2>/dev/null || true
    sleep 1
fi

echo "[run-samples] Starting EchoServer on :$ECHO_PORT (wait=${ECHO_WAIT_MS}ms err=${ECHO_ERR_PCT}%)..."
java -cp "$SCRIPT_DIR/echo-server" EchoServer "$ECHO_PORT" "$ECHO_WAIT_MS" "$ECHO_ERR_PCT" &
ECHO_PID=$!
sleep 2

curl -sf "http://localhost:$ECHO_PORT/health" > /dev/null \
    || { echo "[run-samples] ERROR: EchoServer did not start"; exit 1; }
echo "[run-samples] EchoServer running (pid=$ECHO_PID)."

# ---------------------------------------------------------------------------
# 3. Run sample
# ---------------------------------------------------------------------------
echo ""
echo "[run-samples] ===== Running: $SAMPLE ($MAIN_CLASS) ====="
echo ""

run_sample() {
    mvn exec:java \
        -f "$SCRIPT_DIR/wrapper/pom.xml" \
        -Dexec.mainClass="$MAIN_CLASS" \
        -Dbench.duration="$BENCH_DURATION" \
        -Dbench.op="$BENCH_OP" \
        -Dbench.batch="$BENCH_BATCH" \
        -Dgpg.skip=true \
        2>/dev/null
}

if [ "$SAVE_RESULTS" = "true" ]; then
    run_sample | tee "$RESULTS_FILE"
    echo ""
    echo "[run-samples] Results saved to: $RESULTS_FILE"
else
    run_sample
fi
