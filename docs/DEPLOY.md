# Deploying IdeaForge

Target: one small VPS (2–4 GB RAM) running everything through Docker Compose. The only recurring costs should be the VPS and LLM API usage.

## 1. Prerequisites

- A VPS with Ubuntu 22.04+ (2 vCPU / 4 GB is comfortable; 2 GB works for light use).
- A domain with an `A` record pointing at the server (Caddy provisions TLS automatically).
- A free SMTP account (Brevo, Resend, Postmark free tier, ...) for login codes.
- An LLM API key (Anthropic recommended for quality; OpenAI, Gemini, or a local/Ollama or OpenAI-compatible endpoint also work).

## 2. Server setup

```bash
# Docker Engine + Compose plugin
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER" && newgrp docker

sudo mkdir -p /opt/ideaforge && sudo chown "$USER" /opt/ideaforge
git clone <your-repo-url> /opt/ideaforge
cd /opt/ideaforge
```

## 3. Configure

```bash
cp .env.example .env
# Required for prod (compose refuses to start without them):
#   DOMAIN, POSTGRES_PASSWORD, SESSION_PEPPER, OTP_PEPPER, IP_PEPPER,
#   MAIL_HOST, MAIL_FROM, model + provider + API key, SEARXNG_SECRET
openssl rand -hex 32   # generate each pepper with this
```

The app refuses to start under the `prod` profile when:

- `SESSION_PEPPER`, `OTP_PEPPER`, or `IP_PEPPER` are missing/default,
- `DEV_FIXED_OTP` is set,
- session cookies are not `Secure`,
- Turnstile is enabled without a secret key,
- the SSRF page fetcher's test-only `allow-private-hosts` flag is on.

## 4. Build and start

```bash
docker compose -f infra/docker-compose.prod.yml --env-file .env up -d --build
docker compose -f infra/docker-compose.prod.yml ps
curl -fsS https://$DOMAIN/actuator/health
```

Flyway migrations run automatically on backend startup.

## 5. First admin

Promote your account after signing in once:

```bash
docker compose -f infra/docker-compose.prod.yml exec postgres \
  psql -U ideaforge -d ideaforge \
  -c "update users set role='ADMIN' where email_normalized='you@example.com';"
```

`GET /api/admin/usage` then reports LLM spend, signups, and credit flows.

## 6. Environment reference

| Variable | Purpose |
|---|---|
| `DOMAIN` | Public hostname (Caddy TLS + base URL) |
| `ACME_EMAIL` | Let's Encrypt account email |
| `POSTGRES_DB/USER/PASSWORD` | Database credentials (internal network only) |
| `SESSION_PEPPER/OTP_PEPPER/IP_PEPPER` | HMAC secrets; rotate only by invalidating sessions/codes |
| `MAIL_*`, `MAIL_FROM` | SMTP for login codes |
| `GOOGLE_CLIENT_ID` | Google Identity Services client id (optional) |
| `TURNSTILE_ENABLED/SITE_KEY/SECRET_KEY` | Cloudflare Turnstile (optional but recommended) |
| `*_PROVIDER`, `*_MODEL`, `*_API_KEY` | LLM profiles: `ideation`, `scoring`, `chat`, `finalize` |
| `LLM_DAILY_BUDGET_USD` | Global circuit breaker on estimated spend |
| `SEARXNG_SECRET` | SearXNG internal secret |
| `CREDIT_RECONCILIATION_CRON` | Nightly wallet-vs-ledger check |

See `.env.example` for the full annotated list. Everything has a sensible default except the required secrets above.

## 7. Switching LLM providers (config only)

Each purpose has its own profile in `backend/src/main/resources/application.yml`, overridable per environment:

```bash
# Anthropic
IDEATION_PROVIDER=anthropic IDEATION_MODEL=claude-sonnet-4-5 ANTHROPIC_API_KEY=sk-ant-...

# OpenAI
IDEATION_PROVIDER=openai IDEATION_MODEL=gpt-5.1 OPENAI_API_KEY=sk-...

# Gemini
IDEATION_PROVIDER=gemini IDEATION_MODEL=gemini-2.5-pro GEMINI_API_KEY=...

# Local Ollama (dev or a beefy VPS)
IDEATION_PROVIDER=ollama IDEATION_MODEL=llama3.1 OLLAMA_BASE_URL=http://host.docker.internal:11434

# Any OpenAI-compatible gateway
IDEATION_PROVIDER=openai-compatible IDEATION_MODEL=my-model \
  OPENAI_COMPATIBLE_BASE_URL=https://llm.example/v1 OPENAI_COMPATIBLE_API_KEY=...
```

Use a cheaper model for `SCORING_PROVIDER`/`SCORING_MODEL` (and therefore deep validation) to cut cost. A `fallback-profile` can be added per profile in `application.yml`; it is tried once when the primary provider errors.

## 8. Email DNS checklist

For reliable delivery from your own domain:

1. **SPF** — TXT on the root: `v=spf1 include:<provider-spf-include> ~all`.
2. **DKIM** — add the provider's DKIM keys (often two CNAMEs or a TXT record).
3. **DMARC** — TXT at `_dmarc`: `v=DMARC1; p=quarantine; rua=mailto:dmarc@yourdomain`.
4. Align the `MAIL_FROM` domain with the authenticated domain.
5. Test with a Gmail account before launch; check `mail-tester.com` once.

## 9. Backups

```bash
sudo mkdir -p /var/backups/ideaforge
# Nightly at 03:15:
( crontab -l 2>/dev/null; echo "15 3 * * * /opt/ideaforge/infra/backup.sh >> /var/log/ideaforge-backup.log 2>&1" ) | crontab -
```

Restore:

```bash
gunzip -c /var/backups/ideaforge/ideaforge-<stamp>.sql.gz | \
  docker compose -f infra/docker-compose.prod.yml exec -T postgres \
  psql -U ideaforge -d ideaforge
```

Consider copying backups off the VPS (e.g. `rclone` to any free object storage).

## 10. Updates and operations

```bash
git pull
docker compose -f infra/docker-compose.prod.yml --env-file .env up -d --build
docker compose -f infra/docker-compose.prod.yml logs -f backend
```

- Health: `GET /actuator/health` (probes enabled).
- Usage: `GET /api/admin/usage` as an admin.
- Credit invariants: the nightly reconciliation job logs security events and a Micrometer counter on any wallet/ledger mismatch.
- Interrupted generation, re-validation, or finalization runs refund automatically on restart.
