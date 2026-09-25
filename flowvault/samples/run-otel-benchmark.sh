#!/usr/bin/env bash
# Builds and runs the OpenTelemetry SDK-latency benchmark against a local mock vault.
#
#   ./run-otel-benchmark.sh                 # insert, then detokenize, zero-latency backend
#   ./run-otel-benchmark.sh insert          # one operation only
#   MOCK_LATENCY_MS=40 ./run-otel-benchmark.sh insert    # simulate a realistic backend
#   VUS=8 ITERATIONS=8000 ./run-otel-benchmark.sh
#
# Everything the benchmark measures lives in src/main/java/com/example/otel/benchmark. The Skyflow SDK is
# used unmodified, through its public API.
set -euo pipefail
cd "$(dirname "$0")"

if [ "$#" -gt 0 ]; then
  OPERATIONS=("$@")
else
  OPERATIONS=(insert detokenize)
fi

MOCK_PORT="${MOCK_PORT:-3025}"
MOCK_LATENCY_MS="${MOCK_LATENCY_MS:-0}"
export CSV_DIR="${CSV_DIR:-$PWD/otel-out}"
CLASSES="target/otel-classes"
CP_FILE="target/otel-classpath.txt"

# Only this package is compiled, with javac against the Maven-resolved classpath, rather than
# `mvn compile` for the whole module. The samples module carries examples pinned to other SDK
# versions, and an unrelated one failing to compile should not block a benchmark that does not
# depend on it.
echo "==> resolving dependencies"
mvn -B -q dependency:build-classpath -Dmdep.outputFile="$CP_FILE"
CP="$(cat "$CP_FILE")"

echo "==> compiling com.example.otel.tracing + com.example.otel.benchmark"
mkdir -p "$CLASSES" "$CSV_DIR"
javac -nowarn -cp "$CP" -d "$CLASSES" src/main/java/com/example/otel/tracing/*.java src/main/java/com/example/otel/benchmark/*.java

# Refuse to start on an occupied port. If something else is already listening, the mock fails to
# bind, the readiness check below still succeeds against the squatter, and the benchmark silently
# measures a server nobody chose. That produces numbers that look plausible and mean nothing.
if (exec 3<>"/dev/tcp/127.0.0.1/$MOCK_PORT") 2>/dev/null; then
  exec 3<&-
  echo "port $MOCK_PORT is already in use; set MOCK_PORT to a free port" >&2
  exit 1
fi

echo "==> starting mock vault on port $MOCK_PORT (latency ${MOCK_LATENCY_MS}ms)"
MOCK_PORT="$MOCK_PORT" MOCK_LATENCY_MS="$MOCK_LATENCY_MS" \
  java -cp "$CLASSES:$CP" com.example.otel.benchmark.MockVaultServer &
MOCK_PID=$!
trap 'kill "$MOCK_PID" 2>/dev/null || true' EXIT

# Wait for the listener rather than sleeping a fixed amount, so a slow machine does not produce
# a run whose first iterations hit a connection refusal.
for _ in $(seq 1 50); do
  if ! kill -0 "$MOCK_PID" 2>/dev/null; then
    echo "mock vault exited during startup; see the output above" >&2
    exit 1
  fi
  if (exec 3<>"/dev/tcp/127.0.0.1/$MOCK_PORT") 2>/dev/null; then exec 3<&-; break; fi
  sleep 0.2
done

for operation in "${OPERATIONS[@]}"; do
  echo
  echo "==> benchmarking $operation"
  TARGET_URL="http://127.0.0.1:$MOCK_PORT" \
    java -cp "$CLASSES:$CP" com.example.otel.benchmark.SdkLatencyBenchmark "$operation"
done

echo
echo "CSV summaries in $CSV_DIR"
