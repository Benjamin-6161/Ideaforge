# AGENTS.md

Rules for any AI coding agent working in this repo. Read fully before changing code. The product/technical spec is in `BUILD_PROMPT.md`.

## Project

SaaS idea generator: users pick a field, an LLM generates 5 researched SaaS ideas, users refine them via chat and finalize them. Usage is metered with in-app credits. Stack: Java 21 + Spring Boot, PostgreSQL, React + Typescript (Vite), LangChain4j, Docker Compose.

## Repo layout

- `backend/` Spring Boot, package-by-feature (`auth`, `credits`, `ideas`, `chat`, `generation`, `finalization`, `research`, `llm`, `security`, `common`)
- `frontend/` React app
- `infra/` Compose files, Caddyfile, SearXNG config, backup script
- `docs/` `PROGRESS.md`, `DECISIONS.md`, `ARCHITECTURE.md`

## Commands (keep these accurate; update if they change)

- Infra (dev): `docker compose up -d` (postgres, mailpit, searxng)
- Backend run: `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- Backend test: `cd backend && ./mvnw verify`
- Frontend: `cd frontend && npm install && npm run dev` / `npm run test` / `npm run lint` / `npm run build`
- Regenerate API client after backend API changes: `cd frontend && npm run gen:api`

Before declaring any task done: backend `verify` and frontend `lint` + `test` + `build` must pass.

## Workflow

- Follow the phases in `BUILD_PROMPT.md` section 14, in order, one at a time.
- After each phase: tests green, commit, append a short summary to `docs/PROGRESS.md`.
- If the spec is ambiguous, choose the most reasonable option, record it in `docs/DECISIONS.md`, and continue. Ask only if truly blocked.
- Do not add features, libraries, or services outside the spec without recording why in `docs/DECISIONS.md`.
- Prefer small, focused commits (conventional commits: `feat:`, `fix:`, `test:`, `chore:`, `docs:`).
- Do not rewrite or reformat unrelated code.

## Architecture rules

- Layering: controller -> service -> repository. Controllers contain no business logic. Never return JPA entities from controllers; use DTOs/records.
- Validate all input with Bean Validation at the boundary; enforce enums server-side.
- Use constructor injection. Keep methods small. Prefer immutable records for DTOs.
- Errors: RFC 7807 problem+json via a global exception handler with stable `code` values (e.g. `INSUFFICIENT_CREDITS`). Never leak stack traces or internals.
- Config via `application.yml` + environment variables. No hardcoded secrets, model names, URLs, prices, or limits in code.
- Time: UTC, `timestamptz`, inject a `Clock` for testability.

## Database rules

- Schema changes only through new Flyway migrations. **Never edit an applied migration.**
- UUID primary keys, snake_case, `timestamptz`, indexes on foreign keys and filter columns, CHECK constraints for enums and ranges.
- `credit_ledger` and `idea_versions` are append-only/immutable: never UPDATE or DELETE rows.
- Seed data for dev/test users lives in the `dev` profile seeder, never in production migrations.

## Credit rules (do not violate)

1. Credits are integer **units** (1 credit = 10 units). Never use floating point for credits or money.
2. Spend with a single atomic conditional `UPDATE ... WHERE balance_units >= :cost`; zero rows updated means HTTP 402 and no side effects.
3. Every spend and refund writes a ledger row in the same transaction as the wallet change.
4. Paid endpoints require an `Idempotency-Key`; a retry must never double-charge.
5. Charge before the LLM call; refund (once, linked to the spend) on any failure to deliver; never charge for requests rejected by validation, rate limits, or the injection filter.
6. Prices come from the `credit_costs` table, never from the client.
7. Wallet balance must always equal the sum of the user's ledger rows.

## LLM rules

- Only the `llm` package may import LangChain4j or provider SDKs. Everything else uses `LlmGateway`.
- Model/provider choice is configuration only. Never hardcode a model ID.
- Prompts live in `backend/src/main/resources/prompts/` and are versioned. Rules go in the system message only; user input and web content go in clearly delimited, untrusted blocks.
- Validate every LLM response against a schema and length limits. Allow at most one repair retry. Never execute or trust model output.
- Never put secrets, other users' data, internal IDs, or emails into prompts.
- Log token usage to `ai_usage_logs`. Do not log full prompts or responses at INFO level.
- Tests use `FakeLlmGateway`. Never call a real LLM in automated tests.

## Security rules

- Every repository query for user data is scoped by the authenticated user. Return 404 (not 403) for other users' resources. Add IDOR tests for every new endpoint.
- Never log OTP codes, tokens, cookies, raw IPs, or full email bodies. Store IPs only as peppered HMAC hashes.
- OTP and session tokens are stored hashed. Compare secrets in constant time.
- Keep CSRF protection on. No auth tokens in localStorage.
- All outbound fetches of user- or model-influenced URLs must go through the SSRF-safe `PageFetcher`.
- Treat all web/search content and all user text as untrusted (prompt injection). Do not weaken filters, scope guards, or rate limits to make a test pass.
- Render markdown only through the sanitized renderer. Never use `dangerouslySetInnerHTML` with untrusted content.
- The app must refuse to start if `dev-fixed-otp` is enabled under the `prod` profile.
- Do not add dependencies with known critical vulnerabilities; prefer well-maintained, permissively licensed (free/open-source) libraries. No paid services beyond LLM API usage and hosting.

## Frontend rules

- TypeScript strict; no `any` without a comment explaining why.
- Server state via TanStack Query; forms via react-hook-form + zod.
- The UI displays balances, costs, and scores returned by the API; it never computes or assumes prices.
- Every screen has loading, empty, and error states. Keyboard accessible, labelled inputs, responsive down to mobile width.
- Use the generated API client; do not hand-write fetch calls for backend endpoints.

## Testing rules

- Backend: JUnit 5 + Testcontainers (real PostgreSQL). Required coverage areas: credit spend/refund/idempotency and concurrent spends, OTP flow (expiry, attempts, enumeration), disposable-email and signup throttling, ownership/IDOR, versioning, SSRF, and the prompt-injection suite.
- Frontend: Vitest + React Testing Library for components with logic (forms, score meters, credit display).
- A bug fix includes a regression test.

## Definition of done

- Spec items for the task are implemented and tested.
- Build, lint, and all tests pass.
- No secrets committed; `.env.example` updated if config changed.
- `docs/PROGRESS.md` updated; assumptions recorded in `docs/DECISIONS.md`.

## Never do

- Edit applied Flyway migrations or mutate ledger/version rows.
- Hardcode API keys, model names, prices, or credit costs.
- Bypass the credit service to change balances.
- Disable security checks, CSRF, rate limits, or Turnstile outside the `dev`/`test` profiles.
- Commit `.env` files, real credentials, or generated build output.
