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
const DURATION    = parseInt(__ENV.DURATION    || '120');
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

export function teardown() {
    const res = http.get(`${BASE_URL}/metrics`);
    if (res.status === 200) console.log('[WrapperMetrics]', res.body);
}
