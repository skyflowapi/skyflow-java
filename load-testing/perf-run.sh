#!/usr/bin/env bash
# =============================================================================
# Skyflow Java SDK v3 — SDK Performance Test Orchestrator
#
# Runs the full three-phase performance test (per the SDK perf testing spec):
#   Phase 1: 1 CPU  / 512 MB RAM
#   Phase 2: 2 CPUs / 512 MB RAM
#   Phase 3: 4 CPUs / 1 GB  RAM
#
# Each phase runs the full step-up workload:
#   Baseline (0 RPS) → Light (100) → Medium (500) → High (1000) → Ramp-down (500)
#
# Metrics collected per phase:
#   External:  CPU%, RAM (via docker stats → CSV)
#   Internal:  Heap, Threads, GC pause time, RPS (via /metrics polling → JSONL)
#   k6:        Latency p50/p95/p99, error rate, RPS (via k6 JSON output)
#
# Usage:
#   ./load-testing/perf-run.sh [insert|detokenize]  [max_rps]
#
# Examples:
#   ./load-testing/perf-run.sh insert 1000
#   ./load-testing/perf-run.sh detokenize 500
#
# Output: load-testing/results/YYYY-MM-DD_HH-MM-SS/
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.perf.yml"

OP="${1:-insert}"
MAX_RPS="${2:-1000}"
STEP_DURATION="${STEP_DURATION:-300}"   # seconds per phase (default 5m)
METRICS_POLL_INTERVAL=5                 # seconds between /metrics polls
WRAPPER_URL="http://localhost:8080"

TIMESTAMP="$(date +%Y-%m-%d_%H-%M-%S)"
RESULTS_DIR="$SCRIPT_DIR/results/$TIMESTAMP"
mkdir -p "$RESULTS_DIR"

# Resource configs per phase
declare -a CPU_LIMITS=("1"    "2"     "4")
declare -a MEM_LIMITS=("512m" "512m"  "1g")
declare -a PHASE_NAMES=("phase1_1cpu_512mb" "phase2_2cpu_512mb" "phase3_4cpu_1gb")

log()  { echo "[perf-run] $*"; }
fail() { echo "[perf-run] ERROR: $*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

start_docker_stats_collector() {
    local phase="$1"
    local out="$RESULTS_DIR/${phase}_docker_stats.csv"
    echo "timestamp,container,cpu_pct,mem_usage,mem_limit,mem_pct,net_in,net_out" > "$out"
    # docker stats streams continuously; we append timestamped rows
    docker stats --no-trunc --format \
        "{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}},{{.NetIO}}" \
        skyflow-sut 2>/dev/null | while IFS= read -r line; do
            echo "$(date +%s),$line" >> "$out"
        done &
    echo $!
}

start_jvm_metrics_collector() {
    local phase="$1"
    local out="$RESULTS_DIR/${phase}_jvm_metrics.jsonl"
    (
        while true; do
            ts="$(date +%s)"
            payload="$(curl -sf "$WRAPPER_URL/metrics" 2>/dev/null || echo '{}')"
            echo "{\"ts\":$ts,\"metrics\":$payload}" >> "$out"
            sleep "$METRICS_POLL_INTERVAL"
        done
    ) &
    echo $!
}

stop_collectors() {
    local stats_pid="$1"
    local jvm_pid="$2"
    kill "$stats_pid" "$jvm_pid" 2>/dev/null || true
}

run_phase() {
    local phase_idx="$1"
    local cpu="${CPU_LIMITS[$phase_idx]}"
    local mem="${MEM_LIMITS[$phase_idx]}"
    local phase_name="${PHASE_NAMES[$phase_idx]}"
    local k6_out="$RESULTS_DIR/${phase_name}_k6_output.json"
    local k6_summary="$RESULTS_DIR/${phase_name}_k6_summary.json"

    log "========================================================"
    log "Starting $phase_name  (CPU=$cpu  MEM=$mem  MAX_RPS=$MAX_RPS  OP=$OP)"
    log "========================================================"

    # Tear down any previous run
    docker compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
    sleep 2

    # Start SUT container with phase resource limits
    CPU_LIMIT="$cpu" MEM_LIMIT="$mem" \
    OP="$OP" MAX_RPS="$MAX_RPS" STEP_DURATION="$STEP_DURATION" \
    docker compose -f "$COMPOSE_FILE" up -d sut

    # Wait for SUT to be healthy
    log "Waiting for SUT to become healthy..."
    for i in $(seq 1 30); do
        curl -sf "$WRAPPER_URL/health" > /dev/null 2>&1 && break
        sleep 2
    done
    curl -sf "$WRAPPER_URL/health" > /dev/null || fail "SUT never became healthy"
    log "SUT is healthy."

    # Start background collectors
    STATS_PID="$(start_docker_stats_collector "$phase_name")"
    JVM_PID="$(start_jvm_metrics_collector "$phase_name")"
    log "Collectors started (docker-stats PID=$STATS_PID, jvm-poll PID=$JVM_PID)"

    # Run k6 directly (not via docker compose) so output lands locally
    log "Running k6 perf test..."
    k6 run \
        --env "WRAPPER_URL=$WRAPPER_URL" \
        --env "OP=$OP" \
        --env "MAX_RPS=$MAX_RPS" \
        --env "STEP_DURATION=$STEP_DURATION" \
        --out "json=$k6_out" \
        --summary-export "$k6_summary" \
        "$SCRIPT_DIR/k6/perf.js" || true   # don't abort on threshold breach

    stop_collectors "$STATS_PID" "$JVM_PID"
    log "$phase_name complete. Results saved to $RESULTS_DIR/"

    docker compose -f "$COMPOSE_FILE" down 2>/dev/null || true
    sleep 3
}

print_summary() {
    log ""
    log "============================================================"
    log "PERFORMANCE TEST COMPLETE — Results: $RESULTS_DIR"
    log "============================================================"
    log ""
    log "Files generated:"
    for f in "$RESULTS_DIR"/*; do
        log "  $(basename "$f")"
    done
    log ""
    log "Quick summary (p95 latency per phase):"
    for phase_name in "${PHASE_NAMES[@]}"; do
        local summary="$RESULTS_DIR/${phase_name}_k6_summary.json"
        if [[ -f "$summary" ]]; then
            p95=$(python3 -c "
import json, sys
d = json.load(open('$summary'))
v = d.get('metrics',{}).get('http_req_duration',{}).get('values',{}).get('p(95)', 'N/A')
print(v)
" 2>/dev/null || echo "N/A")
            log "  $phase_name  →  p95 = ${p95}ms"
        fi
    done
}

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
log "SDK Performance Test"
log "  Operation  : $OP"
log "  Max RPS    : $MAX_RPS"
log "  Step length: ${STEP_DURATION}s per phase"
log "  Results    : $RESULTS_DIR"
log ""

# Check prerequisites
command -v docker  > /dev/null || fail "docker not found"
command -v k6      > /dev/null || fail "k6 not found"

# Run all three phases
for i in 0 1 2; do
    run_phase "$i"
done

print_summary
