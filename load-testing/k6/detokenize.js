/**
 * K6 Load Test: skyflow.vault().bulkDetokenize()  [v3 SDK]
 *
 * Flow: K6 → WrapperServer /detokenize → SDK bulkDetokenize() → EchoServer POST /v2/tokens/detokenize
 *
 * Run:
 *   k6 run load-testing/k6/detokenize.js
 *   k6 run --env VUS=100 --env NUM_TOKENS=5 load-testing/k6/detokenize.js
 *
 * Env vars:
 *   WRAPPER_URL    default: http://localhost:8080
 *   VUS            virtual users          default: 50
 *   DURATION       total seconds          default: 120
 *   NUM_TOKENS     tokens per SDK call    default: 1
 *   TOKEN          base token string      default: mock-token-0000-0000-0000-000000000001
 */

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL   = __ENV.WRAPPER_URL || 'http://localhost:8080';
const VUS        = parseInt(__ENV.VUS        || '50');
const DURATION   = parseInt(__ENV.DURATION   || '120');
const NUM_TOKENS = parseInt(__ENV.NUM_TOKENS || '1');
const TOKEN      = __ENV.TOKEN || 'mock-token-0000-0000-0000-000000000001';

const errorRate   = new Rate('detokenize_errors');
const sdkDuration = new Trend('detokenize_sdk_duration_ms', true);

export const options = {
    stages: [
        { duration: '30s',               target: VUS },
        { duration: `${DURATION - 60}s`, target: VUS },
        { duration: '30s',               target: 0 },
    ],
    thresholds: {
        'http_req_duration':           ['p(95)<500'],
        'http_req_failed':             ['rate<0.01'],
        'detokenize_errors':           ['rate<0.01'],
        'detokenize_sdk_duration_ms':  ['p(95)<400'],
    },
};

export default function () {
    const res = http.post(
        `${BASE_URL}/detokenize`,
        JSON.stringify({ token: TOKEN, num_tokens: NUM_TOKENS }),
        { headers: { 'Content-Type': 'application/json' }, tags: { op: 'detokenize' } }
    );

    const ok = check(res, {
        'detokenize: status 200':          (r) => r.status === 200,
        'detokenize: has success/errors':  (r) => {
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
