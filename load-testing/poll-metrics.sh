#!/usr/bin/env bash
# =============================================================================
# poll-metrics.sh — Continuously poll WrapperServer /metrics and save to file
#
# Usage:
#   ./load-testing/poll-metrics.sh                      # defaults
#   INTERVAL=2 ./load-testing/poll-metrics.sh           # poll every 2s
#   WRAPPER_PORT=9090 ./load-testing/poll-metrics.sh    # different port
#
# Environment variables:
#   WRAPPER_PORT   WrapperServer port     (default: 8080)
#   INTERVAL       Seconds between polls  (default: 5)
#   RESULTS_DIR    Output directory       (default: load-testing/results)
# =============================================================================
set -euo pipefail

WRAPPER_PORT="${WRAPPER_PORT:-8080}"
INTERVAL="${INTERVAL:-5}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULTS_DIR="${RESULTS_DIR:-$SCRIPT_DIR/results}"

mkdir -p "$RESULTS_DIR"

TIMESTAMP="$(date '+%Y-%m-%d_%H-%M-%S')"
OUTPUT_FILE="$RESULTS_DIR/metrics-${TIMESTAMP}.txt"

# Track peak values across all snapshots
PEAK_INSERT_RPS="0"
PEAK_DETOKENIZE_RPS="0"
PEAK_THREADS="0"
SNAPSHOT_COUNT=0

echo "[poll-metrics] Polling http://localhost:${WRAPPER_PORT}/metrics every ${INTERVAL}s"
echo "[poll-metrics] Saving to: $OUTPUT_FILE"
echo "[poll-metrics] Press Ctrl-C to stop."
echo ""

# Extract a numeric field from JSON using python3
extract() {
    local json="$1" field="$2"
    python3 -c "
import json, sys
try:
    d = json.loads('''$json''')
    keys = '$field'.split('.')
    v = d
    for k in keys: v = v[k]
    print(v)
except: print(0)
" 2>/dev/null || echo "0"
}

# Return the larger of two numbers (supports decimals)
max_of() {
    python3 -c "a,b=float('$1'),float('$2'); print(a if a>b else b)" 2>/dev/null || echo "$1"
}

pretty_print() {
    if command -v python3 &>/dev/null; then
        python3 -m json.tool 2>/dev/null || cat
    else
        cat
    fi
}

cleanup() {
    echo ""
    echo "================================================================"
    echo " PEAK METRICS SUMMARY  ($OUTPUT_FILE)"
    echo "================================================================"
    echo " Snapshots collected : $SNAPSHOT_COUNT"
    echo " Peak insert RPS     : $PEAK_INSERT_RPS"
    echo " Peak detokenize RPS : $PEAK_DETOKENIZE_RPS"
    echo " Peak threads        : $PEAK_THREADS"
    echo "================================================================"
    echo ""

    # Also append the summary to the results file
    {
        echo ""
        echo "================================================================"
        echo " PEAK METRICS SUMMARY"
        echo "================================================================"
        echo " Snapshots collected : $SNAPSHOT_COUNT"
        echo " Peak insert RPS     : $PEAK_INSERT_RPS"
        echo " Peak detokenize RPS : $PEAK_DETOKENIZE_RPS"
        echo " Peak threads        : $PEAK_THREADS"
        echo "================================================================"
    } >> "$OUTPUT_FILE"

    echo "[poll-metrics] Results saved to: $OUTPUT_FILE"
}
trap cleanup EXIT INT TERM

while true; do
    TS="$(date '+%Y-%m-%d %H:%M:%S')"
    SNAPSHOT="$(curl -sf "http://localhost:${WRAPPER_PORT}/metrics" 2>/dev/null || echo '{"error":"metrics endpoint unreachable"}')"
    PRETTY="$(echo "$SNAPSHOT" | pretty_print)"

    # Extract current values
    CUR_INSERT_RPS="$(extract "$SNAPSHOT" 'insert.rps')"
    CUR_DETOKENIZE_RPS="$(extract "$SNAPSHOT" 'detokenize.rps')"
    CUR_THREADS="$(extract "$SNAPSHOT" 'jvm.threads_current')"

    # Update peaks
    PEAK_INSERT_RPS="$(max_of "$PEAK_INSERT_RPS" "$CUR_INSERT_RPS")"
    PEAK_DETOKENIZE_RPS="$(max_of "$PEAK_DETOKENIZE_RPS" "$CUR_DETOKENIZE_RPS")"
    PEAK_THREADS="$(max_of "$PEAK_THREADS" "$CUR_THREADS")"

    SNAPSHOT_COUNT=$((SNAPSHOT_COUNT + 1))

    ENTRY="[${TS}]  insert_rps=${CUR_INSERT_RPS}  threads=${CUR_THREADS}
${PRETTY}
"

    # Append to file
    printf '%s\n' "$ENTRY" >> "$OUTPUT_FILE"

    # Print to stdout
    printf '%s\n' "$ENTRY"

    sleep "$INTERVAL"
done
