/**
 * K6 SDK Performance Test — Ramping Arrival Rate (RPS-based)
 *
 * Workload profile (Linear Step-Up):
 *   Phase 0 — Baseline  :   0 RPS  for 1m  (cold start memory baseline)
 *   Phase 1 — Light     : 100 RPS  for 5m
 *   Phase 2 — Medium    : 500 RPS  for 5m
 *   Phase 3 — High      : 1000 RPS for 5m  (stress limit)
 *   Phase 4 — Ramp Down : 500 RPS  for 5m
 *
 * Usage:
 *   k6 run load-testing/k6/perf.js
 *   k6 run --env OP=detokenize --env MAX_RPS=500 load-testing/k6/perf.js
 *
 * Env vars:
 *   WRAPPER_URL   default: http://localhost:8080
 *   OP            operation: insert | detokenize   default: insert
 *   MAX_RPS       peak RPS target                 default: 1000
 *   STEP_DURATION step duration in seconds         default: 300  (5m)
 *   NUM_RECORDS   records per bulkInsert call      default: 1
 *   NUM_TOKENS    tokens per bulkDetokenize call   default: 1
 *   TABLE         vault table name                 default: load_test_table
 *   TOKEN         base token for detokenize        default: mock-token-0000-0000-0000-000000000001
 */

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// -- Config ------------------------------------------------------------------
const BASE_URL      = __ENV.WRAPPER_URL    || 'http://localhost:8080';
const OP            = __ENV.OP             || 'insert';
const MAX_RPS       = parseInt(__ENV.MAX_RPS       || '1000');
const STEP_DURATION = parseInt(__ENV.STEP_DURATION || '300');   // seconds per phase
const NUM_RECORDS   = parseInt(__ENV.NUM_RECORDS   || '1');
const NUM_TOKENS    = parseInt(__ENV.NUM_TOKENS    || '1');
const TABLE         = __ENV.TABLE  || 'load_test_table';
const TOKEN         = __ENV.TOKEN  || 'mock-token-0000-0000-0000-000000000001';

// Derived RPS targets per phase
const LIGHT_RPS  = Math.round(MAX_RPS * 0.10);   //  10% of peak
const MEDIUM_RPS = Math.round(MAX_RPS * 0.50);   //  50% of peak
const HIGH_RPS   = MAX_RPS;                       // 100% of peak (stress)
const DOWN_RPS   = Math.round(MAX_RPS * 0.50);   //  50% on ramp-down

// -- Custom Metrics ----------------------------------------------------------
const errorRate  = new Rate('sdk_error_rate');
const sdkLatency = new Trend('sdk_latency_ms', true);
const sdkRPS     = new Counter('sdk_requests_total');

// -- Workload Profile --------------------------------------------------------
export const options = {
    scenarios: {
        sdk_perf: {
            executor: 'ramping-arrival-rate',
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: 200,
            maxVUs: 1000,
            stages: [
                // Phase 0: Baseline — cold start, 0 RPS
                { target: 0,          duration: '1m'                    },
                // Ramp to Phase 1
                { target: LIGHT_RPS,  duration: '30s'                   },
                // Phase 1: Light Load — 10% of peak
                { target: LIGHT_RPS,  duration: `${STEP_DURATION}s`     },
                // Ramp to Phase 2
                { target: MEDIUM_RPS, duration: '1m'                    },
                // Phase 2: Medium Load — 50% of peak
                { target: MEDIUM_RPS, duration: `${STEP_DURATION}s`     },
                // Ramp to Phase 3
                { target: HIGH_RPS,   duration: '2m'                    },
                // Phase 3: High Load / Stress — 100% of peak
                { target: HIGH_RPS,   duration: `${STEP_DURATION}s`     },
                // Phase 4: Ramp Down — 50% of peak
                { target: DOWN_RPS,   duration: '1m'                    },
                { target: DOWN_RPS,   duration: `${STEP_DURATION}s`     },
                // Cool down
                { target: 0,          duration: '30s'                   },
            ],
        },
    },

    thresholds: {
        'http_req_duration':   ['p(95)<500', 'p(99)<1000'],
        'http_req_failed':     ['rate<0.05'],
        'sdk_error_rate':      ['rate<0.05'],
        'sdk_latency_ms':      ['p(95)<400', 'p(99)<800'],
    },
};

// -- Payload builders --------------------------------------------------------
function insertPayload() {
    return JSON.stringify({ table: TABLE, num_records: NUM_RECORDS });
}

function detokenizePayload() {
    return JSON.stringify({ token: TOKEN, num_tokens: NUM_TOKENS });
}

// -- Main test function ------------------------------------------------------
export default function () {
    const endpoint = OP === 'detokenize' ? '/detokenize' : '/insert';
    const body     = OP === 'detokenize' ? detokenizePayload() : insertPayload();

    const res = http.post(`${BASE_URL}${endpoint}`, body, {
        headers: { 'Content-Type': 'application/json' },
        tags:    { op: OP },
    });

    const ok = check(res, {
        [`${OP}: status 200`]:        (r) => r.status === 200,
        [`${OP}: response not empty`]: (r) => r.body && r.body.length > 0,
    });

    errorRate.add(!ok);
    sdkLatency.add(res.timings.duration);
    sdkRPS.add(1);
}

// -- Teardown: pull final JVM metrics ----------------------------------------
export function teardown() {
    const res = http.get(`${BASE_URL}/metrics`);
    if (res.status === 200) {
        console.log('\n[SDK JVM Metrics at teardown]');
        console.log(res.body);
    }
}
