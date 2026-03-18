#!/usr/bin/env bash
# =============================================================================
# Skyflow Java SDK v3 Load Testing - Orchestration Script
#
# Usage:
#   ./load-testing/run.sh [insert|detokenize|all]  [extra k6 flags]
#
# Examples:
#   ./load-testing/run.sh insert
#   ./load-testing/run.sh detokenize --env VUS=100 --env NUM_TOKENS=5
#   ./load-testing/run.sh all --env DURATION=180
#
# Environment variables (override defaults):
#   ECHO_PORT     Echo server port          (default: 3015)
#   WRAPPER_PORT  Wrapper server port       (default: 8080)
#   ECHO_WAIT_MS  Simulated vault latency   (default: 0)
#   ECHO_ERR_PCT  Random error rate %       (default: 0)
#   VAULT_ID      Vault ID for SDK          (default: mock-vault-id)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

ECHO_PORT="${ECHO_PORT:-3015}"
WRAPPER_PORT="${WRAPPER_PORT:-8080}"
ECHO_WAIT_MS="${ECHO_WAIT_MS:-0}"
ECHO_ERR_PCT="${ECHO_ERR_PCT:-0}"
VAULT_ID="${VAULT_ID:-mock-vault-id}"
TEST="${1:-all}"
shift || true   # remaining args passed through to k6

ECHO_PID=""
WRAPPER_PID=""

cleanup() {
    echo ""
    echo "[run.sh] Stopping servers..."
    [ -n "$ECHO_PID" ]    && kill "$ECHO_PID"    2>/dev/null || true
    [ -n "$WRAPPER_PID" ] && kill "$WRAPPER_PID" 2>/dev/null || true
    wait 2>/dev/null || true
    echo "[run.sh] Done."
}
trap cleanup EXIT INT TERM

# ---------------------------------------------------------------------------
# 1. Install v3 SDK to local Maven repo (idempotent)
# ---------------------------------------------------------------------------
echo "[run.sh] Installing v3 SDK to local Maven repo..."
mvn clean install
echo "[run.sh] v3 SDK installed."

# ---------------------------------------------------------------------------
# 2. Build wrapper fat jar
# ---------------------------------------------------------------------------
echo "[run.sh] Building wrapper fat jar..."
mvn package -f "$SCRIPT_DIR/wrapper/pom.xml" -DskipTests -q
WRAPPER_JAR="$SCRIPT_DIR/wrapper/target/skyflow-load-test-wrapper-1.0.0.jar"
echo "[run.sh] Wrapper built: $WRAPPER_JAR"

# ---------------------------------------------------------------------------
# 3. Compile echo server (single-file, no Maven needed)
# ---------------------------------------------------------------------------
echo "[run.sh] Compiling echo server..."
javac -d "$SCRIPT_DIR/echo-server/" "$SCRIPT_DIR/echo-server/EchoServer.java"
echo "[run.sh] Echo server compiled."

# ---------------------------------------------------------------------------
# 4. Start echo server
# ---------------------------------------------------------------------------
echo "[run.sh] Starting EchoServer on port $ECHO_PORT (wait=${ECHO_WAIT_MS}ms, err=${ECHO_ERR_PCT}%)..."
java -cp "$SCRIPT_DIR/echo-server" EchoServer "$ECHO_PORT" "$ECHO_WAIT_MS" "$ECHO_ERR_PCT" &
ECHO_PID=$!
sleep 2

curl -sf "http://localhost:$ECHO_PORT/health" > /dev/null \
    || { echo "[run.sh] ERROR: Echo server did not start"; exit 1; }
echo "[run.sh] Echo server running (pid=$ECHO_PID)."

# ---------------------------------------------------------------------------
# 5. Start wrapper server
# ---------------------------------------------------------------------------
echo "[run.sh] Starting WrapperServer on port $WRAPPER_PORT..."
VAULT_ID="$VAULT_ID" \
VAULT_URL="http://localhost:$ECHO_PORT" \
WRAPPER_PORT="$WRAPPER_PORT" \
java -jar "$WRAPPER_JAR" &
WRAPPER_PID=$!
sleep 3

curl -sf "http://localhost:$WRAPPER_PORT/health" > /dev/null \
    || { echo "[run.sh] ERROR: Wrapper server did not start"; exit 1; }
echo "[run.sh] Wrapper server running (pid=$WRAPPER_PID)."

# ---------------------------------------------------------------------------
# 6. Run k6 test(s)
# ---------------------------------------------------------------------------
run_k6() {
    local script="$1"; shift
    echo ""
    echo "[run.sh] ===== Running k6: $script ====="
    k6 run \
        --env "WRAPPER_URL=http://localhost:$WRAPPER_PORT" \
        "$@" \
        "$SCRIPT_DIR/k6/$script"
}

case "$TEST" in
    insert)
        run_k6 insert.js "$@"
        ;;
    detokenize)
        run_k6 detokenize.js "$@"
        ;;
    all)
        run_k6 insert.js     "$@"
        run_k6 detokenize.js "$@"
        ;;
    *)
        echo "[run.sh] Unknown test '$TEST'. Use: insert | detokenize | all"
        exit 1
        ;;
esac

echo ""
echo "[run.sh] All tests completed."
