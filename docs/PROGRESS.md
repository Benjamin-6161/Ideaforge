# Progress

Phase-by-phase status. One section per phase, appended after tests are green and the phase is committed.

## Phase 0 — Scaffold

- Repo layout: `backend/`, `frontend/` (flattened Vite skeleton), `infra/`, `docs/`, `compose.yml` (includes `infra/docker-compose.yml`).
- Dev infra via Docker Compose: PostgreSQL 16, Mailpit, SearXNG (JSON API enabled, internal use only).
- Backend: Spring Boot 4.1.1 on Java 21, project renamed to `IdeaForgeApplication` (`com.app.ideaforge`), all spec dependencies added, Flyway baseline (`V1__baseline.sql`), health endpoint via Actuator, sane `application.yml` (+ `dev`/`prod` profiles).
- Frontend: Vite + React + TypeScript skeleton moved to `frontend/`, dev proxy to the backend, Vitest + Testing Library wiring.
- CI workflow: backend `./mvnw verify` (Testcontainers), frontend lint/test/build.
- `README.md`, `.env.example`, `docs/` skeletons.
