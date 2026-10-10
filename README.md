# IdeaForge

SaaS idea generator: pick a field, get 5 researched, AI-powered SaaS ideas with evidence-backed scores; refine them in chat (full version history), then finalize to get a tech stack, MVP roadmap, launch checklist, and a copy-paste prompt for a coding agent. Usage is metered with in-app credits.

> Full spec: [`BUILD_PROMPT.md`](BUILD_PROMPT.md). Agent rules: [`AGENTS.md`](AGENTS.md).

## Stack

- **Backend:** Java 21, Spring Boot 4, PostgreSQL 16, Flyway, Spring Security (session cookies + CSRF), LangChain4j (provider-agnostic), Bucket4j, dnsjava, Testcontainers.
- **Frontend:** React 19 + TypeScript + Vite, React Router, TanStack Query, Tailwind CSS + shadcn/ui, react-hook-form + zod, generated API client, Vitest + Testing Library.
- **Infra:** Docker Compose (Caddy, backend, postgres, searxng; Mailpit in dev).

## Quick start (dev)

```bash
cp .env.example .env          # adjust if needed
docker compose up -d          # postgres, mailpit, searxng
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm install && npm run dev
```

- App: http://localhost:5173
- Mailpit (dev emails, incl. OTP codes): http://localhost:8025
- Backend health: http://localhost:8080/actuator/health
- Dev login: any email, OTP code `000000` (dev profile only). Test users `test1@ideaforge.test` / `test2@ideaforge.test` / `test3@ideaforge.test` (dev only).

## Commands

| Task | Command |
|---|---|
| Infra (dev) | `docker compose up -d` |
| Backend run | `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` |
| Backend tests | `cd backend && ./mvnw verify` |
| Frontend dev | `cd frontend && npm run dev` |
| Frontend tests / lint / build | `cd frontend && npm run test` / `npm run lint` / `npm run build` |
| Regenerate API client | `cd frontend && npm run gen:api` (backend must be running) |

## Configuration

All configuration is environment-driven (`backend/src/main/resources/application.yml` + `.env`). See [`.env.example`](.env.example) for the full list. In dev, the backend **imports `.env` directly** (repo root or `backend/.env`), so `cp .env.example .env`, pick a provider, and `./mvnw spring-boot:run` works without Docker. Model/provider selection is config only:

```bash
# OpenAI
IDEATION_PROVIDER=openai IDEATION_MODEL=<model> OPENAI_API_KEY=sk-...

# DeepSeek (first-class, OpenAI-compatible under the hood; base URL defaults)
IDEATION_PROVIDER=deepseek IDEATION_MODEL=deepseek-chat DEEPSEEK_API_KEY=sk-...

# Any other provider (anthropic | gemini | ollama | openai-compatible) works the same way
```

At startup the backend logs the resolved provider/model per purpose and warns about missing keys. Each purpose (`ideation`, `scoring`, `chat`, `finalize`) has its own profile, temperature, token cap, and timeout; a fallback profile can be configured per profile. Prompts live in `backend/src/main/resources/prompts/` and are versioned; the version is stored on runs and finalizations.

## Credits

Credits are integer tenths ("units"): 1 credit = 10 units, shown as `units / 10` with one decimal. Costs live in the `credit_costs` table (2 credits per generation, 0.2 per chat message, 0.5 per re-validation, 1 per finalization) and are editable without redeploying. New accounts get 5 credits after verification. Real payments are not implemented; the schema is payment-ready.

## Deployment

See [`docs/DEPLOY.md`](docs/DEPLOY.md) for the single-VPS Docker Compose setup (Caddy TLS + static SPA + `/api` proxy, Postgres, SearXNG), the environment reference, LLM provider switching, the email SPF/DKIM/DMARC checklist, and nightly `pg_dump` backups.

## Docs

- [`docs/PROGRESS.md`](docs/PROGRESS.md) — phase-by-phase status
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — assumptions and trade-offs
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — architecture and operational notes
- [`docs/DEPLOY.md`](docs/DEPLOY.md) — single-VPS production deployment
- [`docs/REMOTE_TESTING.md`](docs/REMOTE_TESTING.md) — share the app with a teammate while the backend stays local (`render.yaml` included)
