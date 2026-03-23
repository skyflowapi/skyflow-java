'use strict';

const express = require('express');
const app = express();
app.use(express.json());

const PORT = process.env.PORT || 8181;

// Fallback latency/error-code when the request body doesn't specify them.
// Set via docker-compose environment: MOCK_WAIT_MS=50, MOCK_RESPONSE_CODE=200
const DEFAULT_WAIT_MS    = parseInt(process.env.MOCK_WAIT_MS      ?? 0,   10);
const DEFAULT_RESP_CODE  = parseInt(process.env.MOCK_RESPONSE_CODE ?? 200, 10);

// ─── Metrics ────────────────────────────────────────────────────────────────

const metrics = {
  totalRequests: 0,
  insertRequests: 0,
  detokenizeRequests: 0,
  errorResponses: 0,
  totalDelayMs: 0,
};

// ─── Helpers ─────────────────────────────────────────────────────────────────

/**
 * Pull __wait_time_ms and __expected_response_code out of the request body.
 * Falls back to env-var defaults (MOCK_WAIT_MS / MOCK_RESPONSE_CODE).
 */
function extractParams(body) {
  return {
    waitMs: parseInt(body.__wait_time_ms ?? DEFAULT_WAIT_MS, 10),
    responseCode: parseInt(body.__expected_response_code ?? DEFAULT_RESP_CODE, 10),
  };
}

/**
 * Sleep for ms milliseconds.
 */
function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Send a mock error shaped like a real Skyflow error.
 */
function sendError(res, code) {
  metrics.errorResponses++;
  res.status(code).json({
    error: {
      http_code: code,
      message: `Mock server returning error ${code}`,
      request_id: `mock-req-${Date.now()}`,
      details: [],
    },
  });
}

// ─── Auth ─────────────────────────────────────────────────────────────────────
// SDK calls this first. Always return a token so the SDK doesn't block.

app.post('/v1/auth/sa/oauth/token', (_req, res) => {
  res.json({
    accessToken: 'mock-bearer-token',
    tokenType: 'Bearer',
  });
});

// ─── Insert ───────────────────────────────────────────────────────────────────
// POST /v1/vaults/:vaultId/:table
// Works for single (records.length=1) and bulk (records.length=N) — same endpoint.

app.post('/v1/vaults/:vaultId/:table', async (req, res) => {
  metrics.totalRequests++;
  metrics.insertRequests++;

  const { waitMs, responseCode } = extractParams(req.body);

  if (waitMs > 0) {
    metrics.totalDelayMs += waitMs;
    await sleep(waitMs);
  }

  if (responseCode !== 200) {
    return sendError(res, responseCode);
  }

  const incomingRecords = req.body.records ?? [{}];

  const records = incomingRecords.map((_, i) => ({
    skyflow_id: `mock-id-${Date.now()}-${i}`,
    tokens: {
      card_number: `tok-${Date.now()}-${i}`,
    },
  }));

  res.json({ records });
});

// ─── Detokenize ───────────────────────────────────────────────────────────────
// POST /v1/vaults/:vaultId/detokenize
// Works for single and bulk — array size in detokenizationParameters.

app.post('/v1/vaults/:vaultId/detokenize', async (req, res) => {
  metrics.totalRequests++;
  metrics.detokenizeRequests++;

  const { waitMs, responseCode } = extractParams(req.body);

  if (waitMs > 0) {
    metrics.totalDelayMs += waitMs;
    await sleep(waitMs);
  }

  if (responseCode !== 200) {
    return sendError(res, responseCode);
  }

  const params = req.body.detokenizationParameters ?? [];

  const records = params.map((p) => ({
    token: p.token,
    value: '4111111111111111', // mock plain-text value
    valueType: 'STRING',
  }));

  res.json({ records });
});

// ─── Metrics ──────────────────────────────────────────────────────────────────
// Prometheus-style text — Prometheus can scrape this, or you can curl it.

app.get('/metrics', (_req, res) => {
  res.type('text/plain').send(
    [
      `mock_total_requests ${metrics.totalRequests}`,
      `mock_insert_requests ${metrics.insertRequests}`,
      `mock_detokenize_requests ${metrics.detokenizeRequests}`,
      `mock_error_responses ${metrics.errorResponses}`,
      `mock_total_delay_ms ${metrics.totalDelayMs}`,
    ].join('\n') + '\n'
  );
});

// ─── Health ───────────────────────────────────────────────────────────────────

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', uptime: process.uptime() });
});

// ─── Start ────────────────────────────────────────────────────────────────────

app.listen(PORT, () => {
  console.log(`Mock Skyflow server running on port ${PORT}`);
  console.log('Routes:');
  console.log('  POST /v1/auth/sa/oauth/token');
  console.log('  POST /v1/vaults/:vaultId/:table       (insert)');
  console.log('  POST /v1/vaults/:vaultId/detokenize   (detokenize)');
  console.log('  GET  /metrics');
  console.log('  GET  /health');
});
