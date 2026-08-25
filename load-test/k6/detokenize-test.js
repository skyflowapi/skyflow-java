import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 0    },
    { duration: '5m', target: 100  },
    { duration: '5m', target: 500  },
    { duration: '5m', target: 1000 },
    { duration: '5m', target: 500  },
  ],
  thresholds: {
    http_req_duration: ['p99<2000'],
    http_req_failed:   ['rate<0.01'],
  },
};

const BASE_URL = __ENV.WRAPPER_URL || 'http://localhost:8080';

// Static tokens are fine — mock server doesn't validate them
const TOKENS = ['tok-mock-0', 'tok-mock-1', 'tok-mock-2'];

// Bulk detokenize — 3 tokens per request
export default function () {
  const res = http.post(
    `${BASE_URL}/detokenize`,
    JSON.stringify({ tokens: TOKENS }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(res, {
    'status 200':        (r) => r.status === 200,
    'got 3 values back': (r) => JSON.parse(r.body).records?.length === 3,
    'latency < 300ms':   (r) => r.timings.duration < 300,
  });
}
