import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 0    },  // Baseline — cold memory snapshot
    { duration: '5m', target: 100  },  // Light load
    { duration: '5m', target: 500  },  // Medium load
    { duration: '5m', target: 1000 },  // High load
    { duration: '5m', target: 500  },  // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p99<2000'],
    http_req_failed:   ['rate<0.01'],
  },
};

const BASE_URL = __ENV.WRAPPER_URL || 'http://localhost:8080';

// Single insert — records array has 1 item
export default function () {
  const res = http.post(
    `${BASE_URL}/insert`,
    JSON.stringify({
      table: 'cards',
      records: [{ card_number: '4111111111111111', name: 'Jane Doe' }],
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    'status 200':      (r) => r.status === 200,
    'has skyflow_id':  (r) => JSON.parse(r.body).skyflow_id !== undefined,
    'latency < 500ms': (r) => r.timings.duration < 500,
  });
}
