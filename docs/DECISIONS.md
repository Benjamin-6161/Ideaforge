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




