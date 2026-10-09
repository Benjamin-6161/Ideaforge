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

All configuration is environment-driven (`backend/src/main/resources/application.yml` + `.env`). See [`.env.example`](.env.example) for the full list. Model/provider selection is config only — switching from Ollama to Anthropic/OpenAI/Gemini is an env change, no code change.

## Credits

Credits are integer tenths ("units"): 1 credit = 10 units, shown as `units / 10` with one decimal. Costs live in the `credit_costs` table (2 credits per generation, 0.2 per chat message, 1 per finalization, 0.5 per re-validation). New accounts get 5 credits after verification. Real payments are not implemented; the schema is payment-ready.

## Docs

- [`docs/PROGRESS.md`](docs/PROGRESS.md) — phase-by-phase status
- [`docs/DECISIONS.md`](docs/DECISIONS.md) — assumptions and trade-offs
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — architecture and operational notes
