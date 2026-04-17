/**
 * Skyflow SDK Load Testing - Echo/Mock Server (v3 SDK) — Node.js port
 *
 * Simulates only the Vault v3 API endpoints that VaultController actually calls:
 *   POST /v2/records/insert     <- bulkInsert() / bulkInsertAsync()
 *   POST /v2/tokens/detokenize  <- bulkDetokenize() / bulkDetokenizeAsync()
 *
 * Usage:
 *   node EchoServer.js [port] [wait_time_ms] [error_rate_percent]
 *
 * Examples:
 *   node EchoServer.js 3015              # defaults: port=3015, wait=0ms, error=0%
 *   node EchoServer.js 3015 50           # 50 ms simulated latency per request
 *   node EchoServer.js 3015 50 10        # 50 ms latency + 10 % random 5xx
 *
 * wait_time_ms / expected_response_code can also be passed per-request:
 *   - as JSON fields in the request body  (e.g. {"wait_time_ms":50,...})
 *   - or as query params                  (?wait_time_ms=50&expected_response_code=500)
 */

'use strict';

const http = require('http');
const { randomUUID } = require('crypto');
const { URL } = require('url');

// ─── Configuration (set from CLI args) ───────────────────────────────────────
const PORT          = parseInt(process.argv[2]) || 3015;
const DEFAULT_WAIT  = parseInt(process.argv[3]) || 0;
const ERROR_RATE    = parseInt(process.argv[4]) || 0;

// ─── Counters ─────────────────────────────────────────────────────────────────
let totalRequests = 0n;
let totalErrors   = 0n;

// ─── Helpers ──────────────────────────────────────────────────────────────────

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function extractQueryParam(query, key) {
  if (!query) return null;
  const params = new URLSearchParams(query);
  return params.has(key) ? params.get(key) : null;
}

function extractJsonNumber(json, key) {
  const m = new RegExp(`"${key}"\\s*:\\s*(\\d+)`).exec(json);
  return m ? m[1] : null;
}

function extractJsonString(json, key) {
  const m = new RegExp(`"${key}"\\s*:\\s*"([^"]+)"`).exec(json);
  return m ? m[1] : null;
}

function countOccurrences(text, literal) {
  let count = 0;
  let idx = 0;
  while ((idx = text.indexOf(literal, idx)) !== -1) { count++; idx += literal.length; }
  return count;
}

async function simulateLatency(body, query) {
  let ms = DEFAULT_WAIT;
  const qv = extractQueryParam(query, 'wait_time_ms');
  const bv = extractJsonNumber(body, 'wait_time_ms');
  if (qv !== null) ms = parseInt(qv);
  else if (bv !== null) ms = parseInt(bv);
  if (ms > 0) await sleep(ms);
}

function resolveExpectedCode(body, query) {
  if (ERROR_RATE > 0 && Math.random() * 100 < ERROR_RATE) return 500;
  const qv = extractQueryParam(query, 'expected_response_code');
  const bv = extractJsonNumber(body, 'expected_response_code');
  if (qv !== null) return parseInt(qv);
  if (bv !== null) return parseInt(bv);
  return 200;
}

function errorBody(code) {
  return JSON.stringify({ error: { http_code: code, message: 'Simulated server error' } });
}

function sendJson(res, code, body) {
  const bytes = Buffer.from(body, 'utf8');
  res.writeHead(code, {
    'Content-Type': 'application/json',
    'Content-Length': bytes.length,
  });
  res.end(bytes);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', chunk => chunks.push(chunk));
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

// ─── Route handlers ───────────────────────────────────────────────────────────

/**
 * POST /v2/records/insert
 *
 * SDK request body:
 *   {"vaultId":"...","tableName":"...","records":[{"data":{"col":"val"},...}],"upsert":{...}}
 *
 * Expected response:
 *   {"records":[{"skyflowID":"uuid","tokens":{"mock_field":"tok-XXXXXXXX"},"tableName":"tbl","httpCode":200}]}
 */
async function handleInsert(req, res, body, query) {
  await simulateLatency(body, query);

  const code = resolveExpectedCode(body, query);
  if (code !== 200) {
    totalErrors++;
    sendJson(res, code, errorBody(code));
    return;
  }

  let count = countOccurrences(body, '"data"');
  if (count === 0) count = 1;
  const table = extractJsonString(body, 'tableName') || 'load_test_table';

  const records = [];
  for (let i = 0; i < count; i++) {
    const id = randomUUID();
    records.push({
      skyflowID: id,
      tokens: { mock_field: `tok-${id.slice(0, 8)}` },
      tableName: table,
      httpCode: 200,
    });
  }
  sendJson(res, 200, JSON.stringify({ records }));
}

/**
 * POST /v2/tokens/detokenize
 *
 * SDK request body:
 *   {"vaultId":"...","tokens":["tok1","tok2"],"tokenGroupRedactions":[...]}
 *
 * Expected response:
 *   {"response":[{"token":"tok1","value":"plain-tok1xx","httpCode":200},...]}
 */
async function handleDetokenize(req, res, body, query) {
  await simulateLatency(body, query);

  const code = resolveExpectedCode(body, query);
  if (code !== 200) {
    totalErrors++;
    sendJson(res, code, errorBody(code));
    return;
  }

  const tokensMatch = /"tokens"\s*:\s*\[([^\]]+)\]/.exec(body);
  const response = [];

  if (tokensMatch) {
    const tokenStr = tokensMatch[1];
    const tokenRe = /"([^"]+)"/g;
    let m;
    while ((m = tokenRe.exec(tokenStr)) !== null) {
      const tok = m[1];
      response.push({
        token: tok,
        value: `plain-${tok.slice(0, 6)}`,
        httpCode: 200,
      });
    }
  }

  if (response.length === 0) {
    response.push({ token: 'mock-token', value: 'mock-plain-value', httpCode: 200 });
  }

  sendJson(res, 200, JSON.stringify({ response }));
}

/**
 * GET /metrics — request counters + process memory stats
 */
function handleMetrics(req, res) {
  const mem = process.memoryUsage();
  const usedMb = Math.round(mem.heapUsed / 1024 / 1024);
  const maxMb  = Math.round(mem.heapTotal / 1024 / 1024);
  sendJson(res, 200, JSON.stringify({
    total_requests: Number(totalRequests),
    total_errors:   Number(totalErrors),
    heap_used_mb:   usedMb,
    heap_total_mb:  maxMb,
  }));
}

/**
 * GET /health
 */
function handleHealth(req, res) {
  sendJson(res, 200, JSON.stringify({ status: 'ok', api: 'v3' }));
}

// ─── Server ───────────────────────────────────────────────────────────────────

const server = http.createServer(async (req, res) => {
  totalRequests++;

  const parsed = new URL(req.url, `http://localhost:${PORT}`);
  const path   = parsed.pathname;
  const query  = parsed.search ? parsed.search.slice(1) : null; // strip leading '?'

  try {
    if (path === '/v2/records/insert' && req.method === 'POST') {
      const body = await readBody(req);
      await handleInsert(req, res, body, query);
    } else if (path === '/v2/tokens/detokenize' && req.method === 'POST') {
      const body = await readBody(req);
      await handleDetokenize(req, res, body, query);

    } else if (path === '/metrics') {
      handleMetrics(req, res);

    } else if (path === '/health') {
      handleHealth(req, res);

    } else {
       const body = await readBody(req);
       await handleInsert(req, res, body, query);

    }
  } catch (err) {
    console.error('[EchoServer] Error:', err);
    sendJson(res, 500, errorBody(500));
  }
});

server.listen(PORT, () => {
  console.log(`[EchoServer-v3] port=${PORT}  wait=${DEFAULT_WAIT}ms  error_rate=${ERROR_RATE}%`);
});
