# Remote testing: fast static frontend, local backend

For letting someone test the app while the backend and PostgreSQL keep running on your machine. The pattern:

```
Teammate's browser
   │  https://ideaforge-demo.onrender.com        (static SPA, fast CDN)
   ▼
Render CDN ──serves assets──► dist/
   │  /api/*  rewritten server-side (proxy)
   ▼
https://<your-tunnel-host>  ──►  localhost:8080  (Spring Boot, dev profile)
   │
   ▼
localhost:5433 (Postgres) · localhost:8088 (SearXNG) · Mailpit
```

## Why the API must be proxied (not called directly)

If the Render page called `https://<tunnel>` directly, the browser would treat your session and CSRF cookies as **third-party cookies** — Safari blocks them outright and Chrome increasingly does too, so login would silently fail no matter how CORS is configured. Routing `/api/*` through the static host's server-side rewrite keeps every browser request same-origin to Render: cookies and CSRF work unchanged, and **no CORS or backend config changes are needed**.

Only API JSON travels over the tunnel; the heavy JS/CSS/fonts load from the CDN.

## 1. Local stack

```bash
cd /home/fat-joe/projects/ideaforge
docker compose up -d                            # postgres 5433, mailpit, searxng
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The teammate does not need the Vite server at all.

## 2. Expose the backend through a tunnel

Point the tunnel at **8080** (not 5173) for this pattern.

Preferred (usually faster than ngrok free):

```bash
cloudflared tunnel --url http://localhost:8080
# -> https://<random>.trycloudflare.com   (random per run; use a named tunnel for a stable host)
```

Or ngrok (use your free static domain so the URL survives restarts, and pick the region closest to you):

```bash
ngrok http 8080 --domain your-name.ngrok-free.app --region eu
```

A stable tunnel host matters: every tunnel restart with a random URL means updating the static-site rewrite.

## 3. Deploy the frontend with an API proxy rewrite

Build output is `frontend/dist`. Any static host that supports **proxying rewrites to external URLs** works.

### Render (as asked)

Create a **Static Site** from this repo:

- Build command: `cd frontend && npm ci && npm run build`
- Publish directory: `frontend/dist`
- In **Redirects/Rewrites**, add in this order:
  1. Rewrite: source `/api/*` → destination `https://<your-tunnel-host>/api/*` (full URL)
  2. Rewrite: source `/*` → destination `/index.html` (SPA client-side routing)

Or commit the included [`render.yaml`](../render.yaml) at the repo root, replace the tunnel host, and let Render create the site from the Blueprint.

**Verify the proxy before sharing:**

```bash
curl -i https://your-site.onrender.com/api/fields
# expect: HTTP 401 with a JSON body {"code":"UNAUTHORIZED",...}
# if you get a redirect or HTML instead, Render is not proxying; use a fallback below
```

### Fallbacks if your host won't proxy external URLs

- **Cloudflare Pages** (no Git repo needed): `npx wrangler pages deploy frontend/dist`, and include a `_redirects` file in `dist`:
  ```
  /api/*  https://<tunnel-host>/api/:splat  200
  /*      /index.html                       200
  ```
- **Netlify**: drag-and-drop `frontend/dist` (or connect the repo) with the same `_redirects` file at the publish root.
- **Vercel**: `vercel.json` with
  `{"rewrites":[{"source":"/api/(.*)","destination":"https://<tunnel-host>/api/$1"},{"source":"/(.*)","destination":"/index.html"}]}`.

If you rebuild after changing the tunnel host, remember the `_redirects` file is generated into `dist` — keep a template next to the source (e.g. `frontend/public/_redirects.example`) and copy it in, or set the rule in the host's dashboard.

## 4. What to give the teammate

- URL: the static site (assets load fast; API replies are the only tunneled part)
- Login: any email, but the simplest is a seeded account — `test2@ideaforge.test` or `test3@ideaforge.test` — with the **dev OTP `000000`** (Mailpit is on your machine, so they should not wait for an email; the dev profile honors the fixed code).
- Everything else works like local dev: generate (2 credits), chat (0.2/message), re-validate (0.5), finalize (1). Seeded test users start with 100 credits.

## Gotchas

- **403 FORBIDDEN on a POST (and the UI says "something went wrong")**: the CSRF cookie was stale or cleared. Refresh the page; if it persists, check `CORS_ALLOWED_ORIGINS` and redeploy the backend — see `D39` in `docs/DECISIONS.md` for the session/CSRF fix. Requests that reached the backend show up in the tunnel's request log (`http://127.0.0.1:4040` for ngrok), which is the fastest place to see the real status codes.
- **Use the seeded accounts.** Behind the tunnel every request arrives from one apparent IP, so the signup throttle (3 new accounts/IP/24h) applies to all teammates combined. The OTP rate limits are shared the same way (5/hour per email, 20/hour per apparent IP).
- **Limits still apply**: 10 generations/day and 200 chat messages/day per user, plus the global daily LLM budget circuit breaker (`LLM_DAILY_BUDGET_USD`). Fine for demos; tell them if a run returns 503.
- **ngrok warning page**: only shown for browser HTML requests. Because the tunnel is hit server-to-server with JSON, it does not appear. If you ever call the tunnel directly from a browser, add the `ngrok-skip-browser-warning: true` header.
- **Tunnel restarts** change random hostnames; update the rewrite (or use a static domain) and the teammate keeps the same Render URL.
- **Dev-only settings** (fixed OTP `000000`, Turnstile disabled) only exist in the `dev` profile. Never tunnel a `prod`-profile backend with these.
- **Never expose Mailpit/SearXNG/Postgres tunnels publicly**; only the backend port is needed.
