# Decisions

Assumptions and trade-offs made while building. Newest last.

## D1 — Spring Boot 4.1.1 without the LangChain4j Spring Boot starter

The LangChain4j Spring Boot starter (`langchain4j-spring-boot-starter:1.22.0-beta32`) targets Spring Boot 3.5.x and is not compatible with Spring Boot 4.1.1. The spec requires the latest stable Spring Boot that the integration supports, and the provided skeleton (which we must not regenerate) is Spring Boot 4.1.1. We keep Spring Boot 4.1.1 and use the plain LangChain4j modules (`langchain4j`, provider modules) directly, wiring models inside the `llm` package only. This keeps the "only `llm` imports LangChain4j" rule and removes auto-configuration from the picture. Re-evaluate when the starter supports Boot 4.

## D2 — LangChain4j 1.22.0 direct provider modules

`langchain4j-anthropic`, `langchain4j-open-ai`, `langchain4j-google-ai-gemini`, `langchain4j-ollama`. OpenAI-compatible endpoints use `langchain4j-open-ai` with a custom `baseUrl`, so no extra module is needed.

## D3 — Credits stored as integer units

As specified: 1 credit = 10 units, integer only. `cost_units` in `credit_costs`, signed `amount_units` in `credit_ledger`, balance cache in `credit_wallets`.

## D4 — Root `compose.yml` includes `infra/docker-compose.yml`

`AGENTS.md` documents `docker compose up -d` from the repo root while the layout wants compose files under `infra/`. A two-line root `compose.yml` with `include:` satisfies both.

## D5 — Frontend skeleton flattened

The Vite skeleton was generated under `frontend/frontend/`. Contents were moved to `frontend/` so the documented commands (`cd frontend && npm run dev`) work. No files were regenerated.

## D6 — Google ID token verification via `google-api-client`

Spec says "Google API client (ID token verification)". We use `GoogleIdTokenVerifier` from `com.google.api-client:google-api-client:2.9.1` and enforce `email_verified=true` and the configured `aud` (client ID).

## D7 — Boot 4 uses Jackson 3; test starters are modular

Spring Boot 4 ships Jackson 3 (`tools.jackson.databind`) and splits test/JSON libraries into separate starters. The app explicitly depends on `spring-boot-starter-json` and `spring-boot-starter-webmvc-test`; `RestSecurityHandlers` uses the Jackson 3 `ObjectMapper`. `openapi-typescript` still peers on TypeScript 5, so the frontend sets `legacy-peer-deps=true` in `.npmrc` (codegen tool only; the app itself builds with TS 6).

## D8 — Dev Postgres on host port 5433

The dev Compose file maps Postgres to host port 5433 by default because local machines often already run Postgres on 5432. Backend dev profile and `.env.example` follow the same default.

## D9 — OTP verification returns an outcome instead of throwing

Attempt counters and challenge invalidation must survive failed verifications, but throwing a runtime exception inside `@Transactional` would roll them back. `OtpService.verifyCode` therefore returns a `Verification` result and the controller maps failures to 401/403 after the transaction commits.

## D10 — SPA CSRF bootstrap endpoint

With deferred cookie CSRF tokens, a 401 response does not necessarily set the `XSRF-TOKEN` cookie. `GET /api/auth/csrf` (permit-all) resolves the token, and the frontend client self-heals by calling it before any unsafe request when the cookie is missing.

## D11 — `dev-fixed-otp` allowed in dev and test, forbidden in prod

The spec allows a fixed dev OTP and requires startup failure under prod. Tests use the same mechanism (documented here), and `ProdSafetyValidator` rejects any non-blank value plus default secrets under the `prod` profile.

## D12 — Dev test users and roles

`DevDataSeeder` (dev profile only) creates `test1..3@ideaforge.test` with 1000 units each; `test1` is `ADMIN` so the admin usage endpoint can be exercised locally.

## D13 — Integration tests and Mailpit

Tests that log in without mocking `EmailSender` may deliver real OTP mail to a running Mailpit (localhost:1025). Delivery failures are swallowed by design, so tests pass in CI without Mailpit; no test asserts on delivered mail.

## D14 — Prompt-based JSON output instead of provider JSON-schema APIs

Provider structured-output APIs differ and several of our target setups (Ollama, OpenAI-compatible gateways) do not support them uniformly. The gateway therefore asks for JSON in the prompt, extracts the first JSON object/array, parses it, and allows exactly one repair retry. This keeps model/provider switching config-only. Revisit if a portable schema API emerges.

## D15 — Trends provider is a no-op by default

The spec allows a no-op default because free Google Trends access is unreliable. `TrendsProvider` exists with `NoopTrendsProvider` behind `app.research.trends.enabled`; a best-effort Google implementation is intentionally deferred until there is a stable free endpoint to target.

## D16 — jsoup for text extraction

`PageFetcher` needs HTML-to-text extraction. jsoup (MIT, actively maintained) was added for this; it only parses already-fetched, size-capped bytes and never fetches anything itself. Recorded per the dependency rule.

## D17 — Provider config key `openai-compatible`

The YAML map key under `app.llm.providers` is `openai-compatible` (matching the `provider` value used in profiles). The OpenAI-compatible client reuses `langchain4j-open-ai` with a custom base URL, so no extra module is needed.

## D18 — `DEEP_VALIDATION` logs map to the `scoring` profile

`ai_usage_logs.purpose` distinguishes DEEP_VALIDATION, but for model selection it uses the `scoring` profile (research + scoring workloads are the cheaper tier). A dedicated `deep-validation` profile can be added to config without code changes if needed.

## D19 — "At most 4 LLM calls" counts pipeline stages, not repairs

The hard cap applies to logical pipeline calls (ideation, scoring, replacement ideation, replacement scoring). The structured-output repair retry (at most one per call) is an exceptional path bounded by the gateway, so a pathological run could still exceed 4 physical calls. This is documented rather than traded away, because dropping repair would make JSON failures fatal.

## D20 — Generation creation is serialized per user

`pg_advisory_xact_lock(hashtext(user_id))` makes concurrent create requests for the same user strictly serial, so the active-job check, idempotency replay, and run creation cannot race. It also keeps the double-charge protection airtight together with the ledger idempotency key.

## D21 — Modifying queries must not clear the persistence context mid-business-transaction

`@Modifying(clearAutomatically = true)` on `credit_wallets` updates detached managed entities (e.g. the `GenerationRun` being marked failed) before the transaction committed, silently dropping those changes. The flags were removed; refunds now mark the run failed first and wallet balances are always read with scalar queries. A regression test covers refund + run failure in one transaction.

## D22 — Startup recovery is authoritative

Because a single app instance runs the pipeline, any PENDING/RUNNING generation at startup is stale by definition; the recovery runner fails and refunds them unconditionally. This also covers a crash between charge and run creation (orphan spends are reused by the idempotent create path).

## D23 — Score-based sorting uses a lateral join

Sorting by demand/profitability needs the latest validation per idea. A PostgreSQL `LEFT JOIN LATERAL (… ORDER BY created_at DESC LIMIT 1)` keeps sorting and pagination in the database with a matching `countQuery`; JPQL cannot express this cleanly.

## D24 — Favorite toggles are optimistic; archive/delete are not

Favorites are reversible, so the list updates immediately and reconciles on settle. Archive changes filtering (the item may leave the list) and delete is destructive, so both invalidate after the server confirms. The detail cache is refreshed for all three.

## D25 — Revisions use a two-call protocol with a delimiter directive

The chat call stays free-form (tools + natural reply) and may end with `<idea_update>{changeSummary,instructions}</idea_update>`, which is stripped before display. When present, a second structured call returns the full updated idea. This avoids asking one response to be both conversation and a large schema-validated document, and keeps the versioned content validated exactly like generation output.

## D26 — No transaction is held across an LLM call

Chat is split into `prepare` (validate, limit, charge, store the user message), the untracked LLM call, and `complete`/`fail`. This avoids long-lived database connections and lets failure refunds commit even though the request ends with an error response.

## D27 — Message limits count user messages

Both the per-idea (100) and per-user daily (200) limits count `USER` role messages; assistant replies are capped implicitly at one per user message. A failed reply still counts because it consumed a charge cycle (which is then refunded).

## D28 — Deep re-validation is a tracked async run

Re-validation adds the `validation_runs` table (with a partial unique index for one active run per idea) so the API can return 202, the UI can poll state through the idea detail, and stale RUNNING rows can be recovered and refunded at startup. "Up to 8 searches" is implemented as four queries with four results each plus a community query, which stays within budget.

## D29 — Admin usage aggregates are computed on demand

`GET /api/admin/usage` runs a handful of indexed aggregate queries (daily spend, purpose/model, signups, ledger sums) instead of maintaining materialized counters. Data volumes are tiny at this scale, and accuracy beats denormalization; the queries stay bounded by a 1–90 day window.

## D30 — Tool output is sanitized before it reaches the prompt

Beyond instruction-hierarchy framing, `GenerationPrompts` strips delimiter-like tags from every value it embeds, and chat tool results pass through `ToolText.sanitize` (tag stripping + length caps). Search snippets and fetched pages are attacker-influenced data, so this is treated as untrusted at every hop, not just at the boundary.

## D31 — Single origin in production via Caddy

The frontend image is a Caddy image with the built SPA in `/srv`; the same container also proxies `/api` to the backend. This keeps session cookies and CSRF first-party with zero CORS configuration, exactly like the Vite dev proxy does locally. The backend container exposes no host ports.

## D32 — Compose refuses to start without production secrets

The prod compose file marks `POSTGRES_PASSWORD`, the three peppers, `MAIL_HOST`, `MAIL_FROM`, the model names, and `SEARXNG_SECRET` as required (`:?`), complementing the application's own startup validation. Misconfiguration fails fast at `up` instead of leaking default credentials into a running stack.

## D33 — The dev profile imports `.env` and pins safety settings

Dev runs on the host, where Compose's `.env` means nothing to Spring. The dev profile therefore imports `optional:file:.env[.properties]` and `optional:file:../.env[.properties]`, so provider choices and keys work with `./mvnw spring-boot:run` too. Because `.env.example` ships values meant for both dev and prod, the dev profile **pins** two settings that must not leak in from the file: Turnstile stays disabled (an enabled Turnstile with an empty dev secret would silently block every login), and the OpenAPI docs stay enabled (needed by `npm run gen:api`).

## D34 — DeepSeek is a first-class provider

DeepSeek's API is OpenAI-compatible, so `provider: deepseek` reuses `OpenAiChatModel` with a default base URL of `https://api.deepseek.com/v1`; only `DEEPSEEK_API_KEY` (and optionally `DEEPSEEK_BASE_URL`) is required. No dedicated LangChain4j module exists for it, and `openai-compatible` remains available for anything else.

## D35 — LLM failures carry a stable taxonomy

Provider errors are classified into `LlmErrorCode` (NOT_CONFIGURED, UNAUTHORIZED, MODEL_NOT_FOUND, RATE_LIMITED, TIMEOUT, CONNECTION_FAILED, INVALID_REQUEST, INVALID_OUTPUT, CANARY_LEAK, UNKNOWN), persisted as run `error_code` values, logged with a root-cause summary (class + message, never keys or bodies), and shown in the UI with actionable text (e.g. "check `*_MODEL` in your .env"). The startup logger prints each resolved profile and warns about missing keys, so misconfiguration is visible before the first request.

## D36 — Validation failures repair once, then normalize; they never fail a run by themselves

Spec requires schema and length validation with at most one repair retry. That retry now covers Bean Validation failures, not only JSON parsing: the gateway reports the violations back to the model once. If the repaired output still violates only range limits, `LlmOutputNormalizer` truncates the offending strings (char-boundary safe) and trims arrays to the declared `max` instead of failing. Structural problems (missing/blank required fields, wrong types) still fail with `LLM_INVALID_OUTPUT`, because silently fabricating content would be worse than an honest refund. Prompts were also tightened (v2) with explicit per-field character budgets so the repair path is the exception.

## D37 — Prompts are versioned, v2 files supersede v1

`PromptLibrary` loads the highest `-vN.md` per name; adding `ideation-system-v2`, `scoring-system-v2`, `revision-system-v2`, and `finalize-system-v2` (hard length limits) automatically switches behavior while v1 stays on disk for reference. Runs and finalizations record the exact version label used.





