# Progress

Phase-by-phase status. One section per phase, appended after tests are green and the phase is committed.

## Phase 0 — Scaffold

- Repo layout: `backend/`, `frontend/` (flattened Vite skeleton), `infra/`, `docs/`, `compose.yml` (includes `infra/docker-compose.yml`).
- Dev infra via Docker Compose: PostgreSQL 16 (host port 5433 to avoid local clashes), Mailpit, SearXNG (JSON API enabled, internal use only).
- Backend: Spring Boot 4.1.1 on Java 21, project renamed to `IdeaForgeApplication` (`com.app.ideaforge`), all spec dependencies added, Flyway baseline (`V1__baseline.sql`), health endpoint via Actuator, sane `application.yml` (+ `dev`/`prod` profiles).
- Frontend: Vite + React + TypeScript skeleton moved to `frontend/`, Tailwind v4 + shadcn/ui, dev proxy to the backend, Vitest + Testing Library wiring.
- CI workflow: backend `./mvnw verify` (Testcontainers), frontend lint/test/build.
- `README.md`, `.env.example`, `docs/` skeletons.

## Phase 1 — Schema + auth

- All Flyway migrations (identity, fields, credits, generation/ideas, chat/finalization, ops, seed fields with 32 fields × 5 niches, credit costs/packages).
- Passwordless email OTP: SecureRandom codes stored as HMAC hashes, 10 min expiry, 5 attempts, 60 s resend cooldown, per-email (5/h) and per-IP (20/h) limits, constant-time compare, one-time use, generic 202 responses (no enumeration).
- Google sign-in via `GoogleIdTokenVerifier` (aud + `email_verified` enforced), multiple identities per user.
- Opaque session tokens (only SHA-256 stored), HttpOnly/Secure-in-prod/SameSite=Lax cookie, 14-day sliding / 30-day absolute expiry, logout revokes.
- CSRF via cookie token repository with the SPA request handler; `GET /api/auth/csrf` bootstraps the cookie.
- Turnstile verifier (disabled in dev/test), disposable-email blocklist (9k+ domains, weekly refresh with bundled fallback), MX/A checks (disabled in dev/test), IP hashing with signup throttling (3/24h), security events.
- Prod fail-fast checks (dev OTP, empty/default peppers, insecure cookie, Turnstile secret).
- `GET /api/fields` (with niches) and `GET /api/generation-options`.
- 28 tests green (Testcontainers): OTP happy path, disposable emails, enumeration, expiry, attempt exhaustion, suspension, signup throttling, Google login, session cookie flags, logout, field seeds.

## Phase 2 — Credits

- Wallet/ledger/costs/packages domain; costs cached 60 s from `credit_costs`; spends use the atomic conditional `UPDATE ... WHERE balance_units >= :cost` with wallet row locking, then write exactly one signed ledger row.
- Idempotent spends (`Idempotency-Key` unique per user on the ledger); refunds lock the spend row and are single-use via the unique `related_entry_id` constraint; signup grant (50 units) fires on the `UserVerifiedEvent`.
- Nightly reconciliation job compares every wallet to its ledger sum and logs security events + a Micrometer counter on mismatch.
- REST: `GET /api/credits`, `/ledger` (paged), `/packages`; dev seeder creates test1–test3 (test1 ADMIN) with 1000 units.
- Frontend: app shell (responsive top bar, nav, credit chip, account menu, light/dark theme), OTP login with 6-box code input + resend timer, Google/Turnstile hooks (hidden when unconfigured), dashboard, credits page (balance, cost table, ledger, packages with disabled buy buttons), typed client generated from the OpenAPI schema (`npm run gen:api`).
- 39 backend + 15 frontend tests green.

## Phase 3 — LLM + research layer

- `LlmGateway` is the only LLM surface (`generateJson` with one repair retry, `generateText`); `LangChain4jLlmGateway` is the only implementation and the only place LangChain4j is imported.
- Four config profiles (`ideation`, `scoring`, `chat`, `finalize`) resolved from `app.llm.profiles.*`; providers anthropic/openai/gemini/ollama/openai-compatible; optional fallback profile; model/provider switch is config-only (unit-tested).
- Versioned prompts in `backend/src/main/resources/prompts/*-vN.md` (ideation + scoring v1 with the quality rubric and scoring definitions); `PromptLibrary` picks the highest version and exposes the label.
- Canary token per call with leak detection (output dropped, exception raised for refund, security event logged); response length cap; per-call usage logging to `ai_usage_logs` with estimated costs; daily spend circuit breaker (503, cached 60 s).
- Injection pipeline: Unicode/zero-width/control sanitizing, delimiter stripping, heuristic pre-filter with 50+ attack payloads (direct, encoded, delimiter-breaking, tool abuse), `InputGuard` that logs security events and rejects before any charge.
- Research: SearXNG JSON provider, Hacker News Algolia provider, no-op trends provider behind a flag, 7-day DB-backed search cache, and an SSRF-safe `PageFetcher` (scheme allowlist, every hop re-validated, redirect cap, timeout, 1 MB cap, jsoup text extraction, 8k truncation).
- `FakeLlmGateway` for tests; gateway tests cover parsing, repair, canary leaks, fallback, and usage logging.
- 135 backend tests green.

## Phase 4 — Generation flow

- `POST /api/generations` (Idempotency-Key required): validates the field and free-text parameters through the injection guard, enforces the daily limit (10/day) and one active job per user (409), spends 2 credits atomically, creates the run, and schedules the async pipeline after commit. A transaction-scoped advisory lock per user serializes creation.
- Pipeline: ideation (5 ideas, schema-validated) → light research (≤2 searches + 1 community query per idea, cached snippets only) → one batched scoring call → a single replacement round for ideas with `competition >= 8` or `demand <= 3` (re-ideate, re-research, re-score). At most four logical LLM calls.
- Ideas, immutable version 1, and LIGHT validations are persisted atomically only when the whole pipeline succeeds; any failure marks the run FAILED, logs an error code, and refunds exactly once.
- Startup recovery marks stale PENDING/RUNNING runs FAILED (`APP_RESTART`) and refunds.
- Polling endpoint `GET /api/generations/{id}` returns status, progress stage, and scored idea summaries; owner-scoped (404 otherwise).
- Fix found by tests: `@Modifying(clearAutomatically = true)` on wallet updates detached managed entities mid-transaction, silently losing the failure status; removed and documented in DECISIONS.
- 146 backend tests green (11 new generation tests: happy path, idempotent retry, missing key, injection, insufficient credits, LLM failure + refund, replacement round, conflict, daily limit, IDOR, recovery).

## Phase 5 — Ideas UI

- `GET /api/ideas` with field/status/favorite/archived filters, title search, `newest|demand|profitability` sorting (lateral join to the latest validation), and pagination; latest scores attached per item.
- `GET /api/ideas/{id}` returns the current version content plus the latest validation with rationale, competitors, evidence, and a `stale` flag when the validated version is not current.
- `PATCH` favorite/archive, soft `DELETE`, version listing, single-version fetch, and restore (creates a new `RESTORE` version; finalized ideas return 409 `IDEA_LOCKED`). Everything owner-scoped with 404 for other users.
- Frontend: filter bar (field, status, sort, favorites, archived, search), idea cards with score chips, optimistic favorite toggles, archive and delete dialogs, pagination; detail page with segmented score meters (competition inverted, rationale tooltips), Light/Deep badge, evidence and competitor lists, all content sections, stale-scores banner, version history dialog with restore.
- Verified manually against the real SearXNG instance: light research returned real sources into the detail page.
- 154 backend + 25 frontend tests green.

## Phase 6 — Work on it (chat, versioning, re-validation)

- Chat system prompt with a strict scope guard, read-only tool policy, and a delimiter-based revision directive (`<idea_update>` stripped from the visible reply); a second structured call applies the revision as a new immutable `CHAT` version.
- Tools: `web_search` (cached SearXNG), `hacker_news`, `fetch_page` (SSRF-safe), capped at 3 calls per message by the gateway's manual tool loop.
- Context window: current version content + latest validation summary + last 10 messages only. Messages are sanitized and injection-filtered before any charge; per-idea (100) and per-user daily (200) limits run before charging.
- Short transactions around the LLM call (`prepare`, `complete`, `fail`); failed turns refund exactly once and record a failed assistant message; `Idempotency-Key` replays return the original turn.
- Deep re-validation (0.5 credits): `validation_runs` migration/entity, 4 research queries + HN + up to 5 fetched pages, scoring against the current version, async pipeline with single refund on failure, startup recovery, and status polling through the idea detail.
- Frontend: Work page split view (idea panel + chat, tabs on mobile), sanitized markdown rendering, suggestion chips, typing indicator, per-message "created version N" links, version history with restore; detail page Re-validate button with running/failed/already-fresh states.
- Bugs found by the E2E pass and covered by a regression test: concurrent evidence-list mutation and snippet/page dedupe order in deep research.
- 171 backend + 31 frontend tests green.

## Phase 7 — Finalize

- `POST /api/ideas/{id}/finalize` (Idempotency-Key, 1 credit, one active per user): deep validation at finalize time, then three calls — structured plan (tech stack, roadmap, pricing, checklist, next steps, branding, risks), a detailed build prompt, and a draft AGENTS.md — each schema/length validated.
- On success the output and both markdown documents are stored, a DEEP validation is saved, and the idea is locked (`FINALIZED`); chat, restore, and re-validation are rejected for locked ideas while favorites/archive still work.
- Failures refund exactly once; startup recovery fails and refunds stale RUNNING finalizations; replay-after-lock returns the original finalization.
- Frontend: confirm dialog explaining the cost and lock, progress state, and the finalized view in tabs (Tech stack, Roadmap, Build prompt, AGENTS.md, Launch checklist, Risks) with copy and download-as-.md for both documents.
- Verified end-to-end in the browser: fake-provider plan/documents, real deep research, locked idea, download buttons.
- 178 backend + 38 frontend tests green.
