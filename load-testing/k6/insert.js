/**
 * K6 Load Test: skyflow.vault().bulkInsert()  [v3 SDK]
 *
 * Flow: K6 → WrapperServer /insert → SDK bulkInsert() → EchoServer POST /v2/records/insert
 *
 * Run:
 *   k6 run load-testing/k6/insert.js
 *   k6 run --env VUS=100 --env DURATION=120 --env TABLE=persons load-testing/k6/insert.js
 *
 * Env vars:
 *   WRAPPER_URL    default: http://localhost:8080
 *   VUS            virtual users        default: 50
 *   DURATION       total seconds        default: 120
 *   NUM_RECORDS    records per call     default: 1
 *   TABLE          vault table          default: load_test_table
 */

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL    = __ENV.WRAPPER_URL  || 'http://localhost:8080';
const VUS         = parseInt(__ENV.VUS         || '50');
const DURATION    = Math.max(61, parseInt(__ENV.DURATION || '120'));  // must be > 60 (30s ramp-up + 30s ramp-down)
const NUM_RECORDS = parseInt(__ENV.NUM_RECORDS || '1');
const TABLE       = __ENV.TABLE                || 'load_test_table';

const errorRate   = new Rate('insert_errors');
const sdkDuration = new Trend('insert_sdk_duration_ms', true);

export const options = {
    stages: [
        { duration: '30s',               target: VUS },
        { duration: `${DURATION - 60}s`, target: VUS },
        { duration: '30s',               target: 0 },
    ],
    thresholds: {
        'http_req_duration':      ['p(95)<500'],
        'http_req_failed':        ['rate<0.01'],
        'insert_errors':          ['rate<0.01'],
        'insert_sdk_duration_ms': ['p(95)<400'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

export default function () {
    const res = http.post(
        `${BASE_URL}/insert`,
        JSON.stringify({ table: TABLE, num_records: NUM_RECORDS }),
        { headers: { 'Content-Type': 'application/json' }, tags: { op: 'insert' } }
    );

    const ok = check(res, {
        'insert: status 200':         (r) => r.status === 200,
        'insert: has success/errors':  (r) => {
            try {
                const b = JSON.parse(r.body);
                return b.success !== undefined || b.errors !== undefined || r.status === 200;
            } catch (_) { return false; }
        },
    });

    errorRate.add(!ok);
    sdkDuration.add(res.timings.duration);
}

// setup() runs once before any VU starts.
// Resets WrapperServer counters and records the exact start time.
// Return value is passed as `data` into teardown().
export function setup() {
    http.post(`${BASE_URL}/reset`);
    return { startTimeMs: Date.now() };
}

// teardown(data) runs once after all VUs finish.
// data.startTimeMs = timestamp from setup() — no ramp-up/teardown noise.
// RPS = total completed requests / (now - startTimeMs)
export function teardown(data) {
    const endTimeMs   = Date.now();
    const elapsedSec  = (endTimeMs - data.startTimeMs) / 1000;

    const metricsRes  = http.get(`${BASE_URL}/metrics`);
    if (metricsRes.status !== 200) {
        console.log('[teardown] Could not fetch metrics');
        return;
    }

    const m           = JSON.parse(metricsRes.body);
    const totalReqs   = m.insert.total;
    const rps         = (totalReqs / elapsedSec).toFixed(2);

    console.log('[WrapperMetrics]', metricsRes.body);
    console.log(`[RPS] total=${totalReqs}  elapsed=${elapsedSec.toFixed(1)}s  rps=${rps}`);
}

export function handleSummary(data) {
    const reqs    = data.metrics['http_reqs'];
    const dur     = data.metrics['http_req_duration'];
    const failed  = data.metrics['http_req_failed'];
    const sdkDur  = data.metrics['insert_sdk_duration_ms'];

    const totalReqs = reqs?.values?.count ?? 0;
    const k6Rps     = (reqs?.values?.rate ?? 0).toFixed(2);
    const p50       = (dur?.values?.['p(50)']  ?? 0).toFixed(1);
    const p95       = (dur?.values?.['p(95)']  ?? 0).toFixed(1);
    const p99       = (dur?.values?.['p(99)']  ?? 0).toFixed(1);
    const avgMs     = (dur?.values?.avg        ?? 0).toFixed(1);
    const errRate   = ((failed?.values?.rate   ?? 0) * 100).toFixed(2);
    const sdkP95    = (sdkDur?.values?.['p(95)'] ?? 0).toFixed(1);

    const summary = `
╔══════════════════════════════════════════╗
║         K6 INSERT SUMMARY                ║
╠══════════════════════════════════════════╣
║  Total requests : ${String(totalReqs).padStart(20)} ║
║  RPS (k6 rate)  : ${String(k6Rps).padStart(20)} ║
║  Error rate     : ${(errRate + '%').padStart(20)} ║
╠══════════════════════════════════════════╣
║  HTTP latency (ms)                       ║
║    avg          : ${String(avgMs).padStart(20)} ║
║    p50          : ${String(p50).padStart(20)} ║
║    p95          : ${String(p95).padStart(20)} ║
║    p99          : ${String(p99).padStart(20)} ║
╠══════════════════════════════════════════╣
║  SDK duration p95 (ms)                   ║
║    p95          : ${String(sdkP95).padStart(20)} ║
╚══════════════════════════════════════════╝
`;
    console.log(summary);
    return { stdout: summary };
}
