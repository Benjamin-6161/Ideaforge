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
