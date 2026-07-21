# CUST-4311 — Configurable Timeouts & Retries (Java v3 SDK) — Design

**Date:** 2026-07-21
**Scope:** Java v3 SDK only.
**Supersedes/extends:** [`docs/CUST-4311-timeouts-retries-onepager.md`](../../CUST-4311-timeouts-retries-onepager.md) — see [Divergences](#divergences-from-the-cust-4311-one-pager) for what changed and why.

---

## 1. Problem

Two related defects in the v3 Java SDK:

1. **No configurability.** A customer cannot set any timeout or retry value. `VaultConfig`
   exposes only `vaultId`, `clusterId`, `vaultURL`, `env`, `credentials`. The knobs exist one
   layer down (Fern's `ApiClientBuilder.timeout()/maxRetries()/httpClient()`) but are walled off.

2. **A silent regression.** `VaultClient.updateExecutorInHTTP()` injects its own `OkHttpClient`
   (`sharedHttpClient`) so it can attach the auth interceptor. That puts `ClientOptions.build()`
   on the `httpClient != null` branch, which **does not set `callTimeout` and does not add the
   `RetryInterceptor`**. Net effect on the real path today:

   | Setting | Value on v3's injected-client path |
   |---|---|
   | `callTimeout` | **0 — unbounded** |
   | `connectTimeout` / `readTimeout` / `writeTimeout` | OkHttp defaults (10s) |
   | retries | **none** |

   A stuck response therefore blocks the calling thread indefinitely → thread pool exhausts →
   service becomes unresponsive. This is the customer's reported impact.

**Contrast with v2 (motivation):** v2 does *not* inject its own client (it passes auth as a
header), so it stays on the `else` branch and *does* get `callTimeout=60s` + active retries. The
bug is v3-specific, introduced by the shared-client injection.

## 2. Customer's ask (CUST-4311)

The customer explicitly listed **all four** timeouts plus lowered retry backoff:

| Setting | Requested value |
|---|---|
| `callTimeout` | 30s |
| `connectTimeout` | 5–10s |
| `writeTimeout` | 5–10s |
| `readTimeout` | 15–20s |
| retry 1 backoff | up to 0.5s |
| retry 2 backoff | up to 1s |

They observed current backoff as `retry 1: 0–1s`, `retry 2: 0–3s` — which is the Fern
`RetryInterceptor` formula (`1s × rand[0, 2ⁿ)`). "up to X" describes **full jitter** (uniform
`[0, cap]`), consistent with those observed ranges.

## 3. Requirements

- **R1 — Bounded blocking.** Every call has a finite, knowable worst-case wall-clock ceiling.
- **R2 — Fail fast on connect.** Connection establishment bounded by a short, independent limit.
- **R3 — Tolerate slow-but-healthy.** Response reads get a separate, larger budget.
- **R4 — Tunable retries nested in budget.** Retry count and full-jitter backoff configurable;
  worst-case total (attempts + backoff) bounded by the overall ceiling.
- **R5 — Safe by default.** Unconfigured SDK already satisfies R1–R3 (this fixes the regression).
- **R6 — Reachable config.** Knobs settable through the public API.
- **R7 — Backward compatible.** Existing integrations compile and run unchanged.

## 4. Key constraint: Fern cannot express connect/read/write

Fern models exactly one timeout (`timeout` → OkHttp `callTimeout`) and hardcodes
`connectTimeout(0)`, `writeTimeout(0)`, `readTimeout(0)` in its generator template — at every
version, at generation time and runtime (confirmed via the one-pager's generator-source review
and [fern #2922](https://github.com/fern-api/fern/issues/2922)).

Two consequences that drive the whole design:

1. **R2/R3 force a custom `OkHttpClient`.** Granular connect/read/write only exist on an
   OkHttp client we build ourselves. There is no Fern path to them.
2. **A custom client disables Fern's retries.** `RetryInterceptor` is only added on the
   no-custom-client branch. Since R2/R3 force a custom client, **we must supply our own retry
   interceptor** — Fern's is unreachable on our path. A Fern generator upgrade does **not**
   change this (its interceptor still isn't attached on the custom-client branch, and its
   proportional jitter can exceed the cap, failing R4's "up to X").

Therefore: **everything is implemented in hand-written code (`VaultClient` + new config
classes). No Fern generator upgrade. No edits to generated code.** This is regeneration-safe.

## 5. Public API

Two config value-objects in `com.skyflow.config` (alongside `VaultConfig`/`Credentials`).
**Units: milliseconds** (backoff of 0.5s requires sub-second precision — see
[Divergences](#divergences-from-the-cust-4311-one-pager)). Customers pass **values only**; they
never see or supply OkHttp types or interceptors.

```java
// com.skyflow.config.TimeoutConfig  (builder-style)
TimeoutConfig.builder()
    .connectTimeoutMs(5000)
    .readTimeoutMs(20000)
    .writeTimeoutMs(10000)
    .callTimeoutMs(30000)      // overall hard ceiling
    .build();

// com.skyflow.config.RetryConfig  (builder-style)
RetryConfig.builder()
    .maxRetries(2)
    .initialBackoffMs(500)     // retry 1 → [0, 500ms]
    .maxBackoffMs(1000)        // retry 2 → [0, 1000ms], capped here
    .build();
```

### Two-level configuration (client default + per-vault override)

Mirrors the existing **credentials** precedence (`prioritiseCredentials()` already picks
vault-level creds over the shared Skyflow creds), so it is a consistent mental model.

- **Client level (default for all vaults):** new setters on v3's `Skyflow.SkyflowClientBuilder`
  (NOT the shared `common/BaseSkyflowClientBuilder`, to keep this v3-only).
  ```java
  Skyflow.builder()
      .addSkyflowCredentials(creds)
      .setTimeoutConfig(defaultTimeouts)   // applies to all vaults
      .setRetryConfig(defaultRetries)
      .addVaultConfig(vaultA)              // inherits defaults
      .addVaultConfig(vaultB)
      .build();
  ```
- **Vault level (override):** `VaultConfig.setTimeoutConfig(...)` / `setRetryConfig(...)`.

**Precedence (highest → lowest): per-vault → client-wide → SDK default.**

**Propagation:** `addVaultConfig` builds the `VaultController` immediately. To avoid
order-dependent surprises, the client-level setters propagate to already-created controllers —
exactly as `addSkyflowCredentials` already loops and calls `setCommonCredentials`. New parallel:
`VaultController/VaultClient.setCommonHttpConfig(...)`.

## 6. Internal design

### 6.1 `SkyflowRetryInterceptor` (new, hand-written)

- Location: `com.skyflow.utils` (hand-written package, not generated).
- OkHttp `Interceptor`; constructed from `RetryConfig`.
- Retry triggers: **408 / 429 / 5xx only** (same as Fern today). Does **not** retry
  connection failures / `IOException` (out of scope — see §8).
- Full-jitter backoff: `delay(attempt n) = uniform[0, min(initialBackoffMs × 2^(n-1), maxBackoffMs)]`.
  With `initialBackoffMs=500, maxBackoffMs=1000`: attempt 1 → `[0,500ms]`, attempt 2 → `[0,1000ms]`.
- Closes each failed `Response` before retrying (avoids connection/body leak).

### 6.2 Wiring in `VaultClient.updateExecutorInHTTP()`

Resolve effective config (new `prioritiseHttpConfig()` mirroring `prioritiseCredentials()`),
then build the shared client:

```java
TimeoutConfig t = effectiveTimeoutConfig();   // vault ?? client ?? defaults
RetryConfig   r = effectiveRetryConfig();

sharedHttpClient = new OkHttpClient.Builder()
        .connectionPool(new ConnectionPool(10, 1, TimeUnit.MINUTES))
        .callTimeout(t.getCallTimeoutMs(),       TimeUnit.MILLISECONDS)
        .connectTimeout(t.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
        .readTimeout(t.getReadTimeoutMs(),       TimeUnit.MILLISECONDS)
        .writeTimeout(t.getWriteTimeoutMs(),     TimeUnit.MILLISECONDS)
        .addInterceptor(new SkyflowRetryInterceptor(r))  // OUTER: retries whole call
        .addInterceptor(chain -> {                       // INNER: existing auth, unchanged
            Request requestWithAuth = chain.request().newBuilder()
                    .header("Authorization", "Bearer " + this.token)
                    .build();
            return chain.proceed(requestWithAuth);
        })
        .build();
apiClientBuilder.httpClient(sharedHttpClient);
```

Critical wiring facts:

- **Do NOT call `apiClientBuilder.timeout(...)` or `.maxRetries(...)`.** On the custom-client
  branch, setting `timeout` triggers `ClientOptions.build()` to reset connect/write/read to `0`,
  clobbering our granular values. Keeping our timeouts on the client itself avoids this.
- **Interceptor order = retry outer, auth inner** → each retry re-enters auth and re-reads
  `this.token` (token refresh stays correct). The auth lambda is preserved byte-for-byte.
- **`callTimeout` bounds retries.** The retry loop and its backoff sleeps run inside the
  application-interceptor chain, so total wall-clock (attempts + backoff) is capped by
  `callTimeout` — R4's "nest inside the budget."

### 6.3 Semantics decisions (locked)

- **Milliseconds** throughout the public config.
- **`callTimeout` = total ceiling** (bounds the whole call including retries), not per-attempt.
- **Full jitter** (`uniform[0, cap]`), matching the customer's "up to X".
- **Retry triggers unchanged** (408/429/5xx).

Note on interaction: `callTimeout=30s` is a *total* bound while `readTimeout=20s` is *per
attempt*. With retries, three 20s reads cannot all fit in 30s — the 30s total wins and may abort
mid-retry. This is intended (fail at the budget), but customers should set values knowing the
total is the real bound.

## 7. Defaults (= customer's recommended production values, satisfies R5)

| Setting | Default |
|---|---|
| `connectTimeoutMs` | 5000 |
| `readTimeoutMs` | 20000 |
| `writeTimeoutMs` | 10000 |
| `callTimeoutMs` | 30000 |
| `maxRetries` | 2 |
| `initialBackoffMs` | 500 |
| `maxBackoffMs` | 1000 |

Unconfigured SDK is now bounded *and* matches the customer's target out of the box.

## 8. Out of scope

- **Custom `OkHttpClient` injection** by consumers (deferred; additive later, non-breaking).
- **Custom retry `Interceptor` injection** by consumers (not proposed).
- **Per-request timeout/retry overrides** (see divergence note — decision pending).
- **Retrying connection failures / `IOException`** (dead host). Triggers stay 408/429/5xx.
- **Per-operation timeouts** (one client-wide set per vault).
- **v2 SDK** (v2 is already bounded + retrying; different problem).
- **Python v3 SDK** (separate effort; not covered here).

## 9. Backward compatibility & release notes

- All new fields optional → existing code compiles and runs unchanged (R7).
- **Behavior change to announce:** calls that previously could hang unbounded are now capped at
  `callTimeout` (default 30s), and retries now actually fire (previously silently disabled on
  v3's path). This is the fix, but it is a behavior change from shipped beta behavior.

## 10. Testing

- **Unit:** the four timeouts are applied to the built client; `maxRetries` honored; backoff
  samples fall within `[0, cap_n]`; precedence resolves vault → client → default correctly.
- **Integration:** a deliberately-hanging mock server proves no thread blocks past `callTimeout`
  — the regression guard for the original thread-choke impact.

## 11. Divergences from the CUST-4311 one-pager

The earlier one-pager made narrower choices; this design widens them based on the customer's
explicit ask. **These need your confirmation:**

| Topic | One-pager | This design | Why it changed |
|---|---|---|---|
| connect/read/write | **Deferred** ("callTimeout alone resolves the impact") | **Included** | Customer explicitly listed all four values; deferring them leaves the ask unmet. |
| Fix approach | Prefer **Option 2** (drop httpClient override, use Fern natively) | **Option 1** (plumb into shared client) | Option 2 can only set `callTimeout`; including connect/read/write forces the custom-client path. |
| Retry interceptor | Reuse Fern's `RetryInterceptor` | Own `SkyflowRetryInterceptor` | Fern's backoff is fixed at 0–1s/0–3s; customer wants 0.5s/1s full jitter, which Fern can't express and (on the custom-client path) wouldn't even attach. |
| Units | **Seconds** (no `TimeUnit` in public API) | **Milliseconds** | 0.5s backoff needs sub-second precision; seconds can't express it. |
| Per-request timeout override | Included (3rd level, Fern-native) | Not included (client + vault only) | Per-request goes through Fern's `httpClientWithTimeout`, which re-zeros connect/read/write — conflicts with granular timeouts. Open question below. |
| SDKs | Java **and** Python v3 | **Java v3 only** | This effort targets the Java v3 SDK; Python is tracked separately. |

### Open questions for reviewer
1. **Per-request override:** worth adding despite the connect/read/write conflict, or is
   client+vault sufficient? (Customer did not ask for per-request.)
2. **Units:** confirm milliseconds (recommended) vs. the one-pager's seconds.
