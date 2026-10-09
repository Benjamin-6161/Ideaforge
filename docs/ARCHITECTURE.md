# Architecture

> Living document. Expanded as phases land.

## Overview

Single-origin web app: Caddy serves the built React app and reverse-proxies `/api/*` to Spring Boot; PostgreSQL stores everything; SearXNG (internal only) provides web search for idea research. LLM access is provider-agnostic behind `LlmGateway`.

```
Browser ── Caddy (TLS, static, /api proxy) ──> Spring Boot ──> PostgreSQL
                                                   │              
                                                   ├─> SearXNG (internal)
                                                   ├─> HN Algolia API
                                                   └─> LLM provider (config-selected)
```

## Backend packages (package-by-feature)

`auth`, `user`, `credits`, `fields`, `generation`, `ideas`, `chat`, `finalization`, `research`, `llm`, `security`, `common`.

Rules: controller → service → repository; DTOs only across the API boundary; RFC 7807 problem+json with stable codes; constructor injection; UTC `timestamptz` and an injected `Clock`.

## Credit model

- Integer units (1 credit = 10 units); 1 decimal in the UI.
- Spend is one atomic conditional `UPDATE ... WHERE balance_units >= :cost`; zero rows → HTTP 402, no side effects.
- Every wallet change has a ledger row in the same transaction. Ledger is append-only.
- Paid endpoints are idempotent via `Idempotency-Key` (unique per user on the ledger).
- Failures refund once (`related_entry_id`, unique for REFUNDs).
- Nightly reconciliation asserts `balance = SUM(ledger amounts)`.

## Research pipeline

- Light check during generation: ≤2 searches per idea + 1 HN query, snippets only.
- Deep check at finalization / re-validation / chat tools: ≤8 searches, ≤5 page fetches (SSRF-safe `PageFetcher`), HN, optional Trends.
- SearXNG results cached in `search_cache` for 7 days (hash of provider + normalized query).
- Research failures never fail the request; scores carry `evidence_available=false` when no evidence was found.

## LLM layer

- `LlmGateway` is the only interface the rest of the app sees; LangChain4j types never leak outside `llm`.
- Four config profiles (`ideation`, `scoring`, `chat`, `finalize`), each with provider/model/temperature/tokens/timeout and optional fallback; all env-overridable.
- Structured JSON output, schema validation, one repair retry; usage logged to `ai_usage_logs`.
- Global daily LLM spend circuit breaker (estimated from usage logs).

## Security

- Passwordless email OTP (HMAC-hashed codes) + Google Identity Services ID tokens; opaque session tokens stored hashed; HttpOnly cookies; CSRF cookie tokens; Turnstile; disposable-email and MX checks; IP hashes only.
- Prompt-injection defense: input hygiene → heuristic pre-filter (before charging) → instruction hierarchy with delimited untrusted blocks and canary tokens → read-only tools (≤3 per chat message) → strict output schemas → scope guard.
- All user-data queries scoped by authenticated user; 404 for other users' resources.
- SSRF-safe fetching for all user/model-influenced URLs.

## Future / out of scope

- Real payments (tables `credit_packages`/`payments` and a `PaymentProvider` interface exist). Planned flow: provider checkout → signature-verified, idempotent webhook on `provider_payment_id` → `PURCHASE` ledger row. Merchant-of-record (Lemon Squeezy/Paddle) or Stripe both fit.
- GDPR account deletion/export.
- Streaming chat (SSE with delimiter-based idea update) as a possible stretch.
