/**
 * K6 Fixed-Iteration Load Test: skyflow.vault().bulkInsert()  [v3 SDK]
 *
 * Runs exactly TOTAL_REQUESTS iterations across all VUs, then stops.
 * RPS = total_requests / total_time_taken  (no ramp-up/ramp-down noise)
 *
 * Run:
 *   k6 run load-testing/k6/insert-fixed.js
 *   k6 run --env TOTAL_REQUESTS=100000 --env VUS=200 load-testing/k6/insert-fixed.js
 *
 * Env vars:
 *   WRAPPER_URL      default: http://localhost:8080
 *   VUS              virtual users        default: 100
 *   TOTAL_REQUESTS   total SDK calls      default: 100000
 *   NUM_RECORDS      records per call     default: 1
 *   TABLE            vault table          default: load_test_table
 */

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL       = __ENV.WRAPPER_URL     || 'http://localhost:8080';
const VUS            = parseInt(__ENV.VUS            || '100');
const TOTAL_REQUESTS = parseInt(__ENV.TOTAL_REQUESTS || '100000');
const NUM_RECORDS    = parseInt(__ENV.NUM_RECORDS    || '1');
const TABLE          = __ENV.TABLE                   || 'load_test_table';

const errorRate   = new Rate('insert_errors');
const sdkDuration = new Trend('insert_sdk_duration_ms', true);

export const options = {
    vus:        VUS,
    iterations: TOTAL_REQUESTS,   // k6 stops exactly after this many calls
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

export default function () {
    const res = http.post(
        `${BASE_URL}/insert`,
        JSON.stringify({ table: TABLE, num_records: NUM_RECORDS }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    const ok = check(res, {
        'status 200': (r) => r.status === 200,
    });

    errorRate.add(!ok);
    sdkDuration.add(res.timings.duration);
}

export function setup() {
    http.post(`${BASE_URL}/reset`);  // clear counters so RPS is correct for this run
}

export function teardown() {
    const res = http.get(`${BASE_URL}/metrics`);
    if (res.status === 200) console.log('[WrapperMetrics]', res.body);
}

export function handleSummary(data) {
    const reqs    = data.metrics['http_reqs'];
    const dur     = data.metrics['http_req_duration'];
    const failed  = data.metrics['http_req_failed'];
    const sdkDur  = data.metrics['insert_sdk_duration_ms'];

    const totalReqs      = reqs?.values?.count ?? 0;
    const testDurationMs = data?.state?.testRunDurationMs ?? 0;
    const testDurationSec = (testDurationMs / 1000).toFixed(3);

    // RPS = total_requests / total_time_taken  (what the user asked for)
    const rpsRaw = totalReqs > 0 && testDurationMs > 0
        ? totalReqs / (testDurationMs / 1000)
        : 0;
    const rps = rpsRaw.toFixed(2);

    // k6's own rate (same formula, shown for cross-check)
    const k6RateRaw = reqs?.values?.rate ?? 0;
    const k6Rate    = k6RateRaw.toFixed(2);

    // Delta between the two — should be <5% if both are correct.
    // Divergence happens because testRunDurationMs includes teardown() time.
    const delta    = Math.abs(rpsRaw - k6RateRaw);
    const deltaPct = k6RateRaw > 0 ? ((delta / k6RateRaw) * 100).toFixed(1) : '0.0';
    const match    = parseFloat(deltaPct) < 5 ? 'OK  (within 5%)' : 'WARN (>5% gap — check teardown latency)';

    const p50      = (dur?.values?.['p(50)']  ?? 0).toFixed(1);
    const p95      = (dur?.values?.['p(95)']  ?? 0).toFixed(1);
    const p99      = (dur?.values?.['p(99)']  ?? 0).toFixed(1);
    const avgMs    = (dur?.values?.avg        ?? 0).toFixed(1);
    const minMs    = (dur?.values?.min        ?? 0).toFixed(1);
    const maxMs    = (dur?.values?.max        ?? 0).toFixed(1);
    const errRate  = ((failed?.values?.rate   ?? 0) * 100).toFixed(2);
    const sdkP50   = (sdkDur?.values?.['p(50)'] ?? 0).toFixed(1);
    const sdkP95   = (sdkDur?.values?.['p(95)'] ?? 0).toFixed(1);
    const sdkP99   = (sdkDur?.values?.['p(99)'] ?? 0).toFixed(1);

    const summary = `
╔══════════════════════════════════════════════════╗
║         FIXED-ITERATION INSERT SUMMARY           ║
╠══════════════════════════════════════════════════╣
║  Config                                          ║
║    VUs              : ${String(VUS).padStart(24)} ║
║    Target requests  : ${String(TOTAL_REQUESTS).padStart(24)} ║
║    Completed        : ${String(totalReqs).padStart(24)} ║
║    Test duration    : ${(testDurationSec + 's').padStart(24)} ║
╠══════════════════════════════════════════════════╣
║  Throughput                                      ║
║    RPS (total/time) : ${String(rps).padStart(24)} ║
║    RPS (k6 rate)    : ${String(k6Rate).padStart(24)} ║
║    Delta            : ${(delta.toFixed(2) + ' (' + deltaPct + '%)').padStart(24)} ║
║    Verdict          : ${String(match).padStart(24)} ║
║    Error rate       : ${(errRate + '%').padStart(24)} ║
╠══════════════════════════════════════════════════╣
║  HTTP round-trip latency (ms)                    ║
║    min              : ${String(minMs).padStart(24)} ║
║    avg              : ${String(avgMs).padStart(24)} ║
║    p50              : ${String(p50).padStart(24)} ║
║    p95              : ${String(p95).padStart(24)} ║
║    p99              : ${String(p99).padStart(24)} ║
║    max              : ${String(maxMs).padStart(24)} ║
╠══════════════════════════════════════════════════╣
║  SDK duration (ms)                               ║
║    p50              : ${String(sdkP50).padStart(24)} ║
║    p95              : ${String(sdkP95).padStart(24)} ║
║    p99              : ${String(sdkP99).padStart(24)} ║
╚══════════════════════════════════════════════════╝
`;
    console.log(summary);
    return { stdout: summary };
}
