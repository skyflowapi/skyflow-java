import http from 'k6/http';
import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// Custom metrics — show up as separate series in Grafana
const insertDuration   = new Trend('sdk_insert_duration',     true); // true = percentiles
const detokDuration    = new Trend('sdk_detokenize_duration', true);
const insertErrors     = new Counter('sdk_insert_errors');
const detokErrors      = new Counter('sdk_detokenize_errors');

export const options = {
  stages: [
    { duration: '1m', target: 0    },
    { duration: '5m', target: 100  },
    { duration: '5m', target: 500  },
    { duration: '5m', target: 1000 },
    { duration: '5m', target: 500  },
  ],
  thresholds: {
    http_req_duration: ['p99<3000'],
    http_req_failed:   ['rate<0.01'],
  },
};

const BASE_URL = __ENV.WRAPPER_URL || 'http://localhost:8080';

export default function () {
  // ── Step 1: Insert ────────────────────────────────────────────────────────
  const insertRes = http.post(
    `${BASE_URL}/insert`,
    JSON.stringify({
      table: 'cards',
      records: [{ card_number: '4111111111111111', name: 'Jane Doe' }],
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  insertDuration.add(insertRes.timings.duration);
  const insertOk = check(insertRes, { 'insert status 200': (r) => r.status === 200 });
  if (!insertOk) {
    insertErrors.add(1);
    return;
  }

  // Extract the token the SDK returned from the insert response
  const token = JSON.parse(insertRes.body).tokens?.card_number;
  if (!token) return;

  // ── Step 2: Detokenize using the token from step 1 ────────────────────────
  const detokRes = http.post(
    `${BASE_URL}/detokenize`,
    JSON.stringify({ tokens: [token] }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  detokDuration.add(detokRes.timings.duration);
  const detokOk = check(detokRes, {
    'detok status 200':   (r) => r.status === 200,
    'value is correct':   (r) => JSON.parse(r.body).records?.[0]?.value === '4111111111111111',
  });
  if (!detokOk) detokErrors.add(1);
}
