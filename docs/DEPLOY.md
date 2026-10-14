# Free test deployment

| Part | Host | Live address |
|---|---|---|
| Backend API (Spring Boot, Docker) | Render free web service, Frankfurt | `https://syrian-transit-api.onrender.com` |
| Rider app (PWA) | Cloudflare Workers static assets | `https://syrian-transit.syria-transit.workers.dev` |
| Admin console | Cloudflare Workers static assets | `https://syrian-transit-admin.syria-transit.workers.dev` |
| Database | MongoDB Atlas M0, Azure UAE North (Dubai) | — |
| E-mails (reset / verification) | Brevo free SMTP (optional, not set up yet) | — |

Cloudflare Pages is now part of Cloudflare Workers; each app has a `wrangler.jsonc` that uploads only its built `dist/` folder.

## 1. MongoDB Atlas — allow Render to connect
Render's free services have no fixed IP, so Atlas must accept connections from anywhere:
**Atlas → Security → Network Access → Add IP Address → `0.0.0.0/0`**. The database stays protected by its username/password, so keep that password strong. (Better for production: a dedicated database user for the app with read/write on `syrian_transit` only.)

## 2. Render — backend
1. Sign in at render.com (GitHub login is easiest).
2. **New → Web Service → Public Git Repository** → `https://github.com/ranamairey/Transit-Routing-project` (or **New → Blueprint** if the repo is connected to your Render account; it reads `render.yaml`).
3. Runtime **Docker**, plan **Free**, region **Frankfurt**, health check path `/api/v1/health`.
4. Environment variables (see `render.yaml` for the full list):
   - `MONGO_URI` — the Atlas connection string
   - `MONGO_DB` = `syrian_transit`
   - `JWT_SECRET` — long random string (Blueprint generates it)
   - `SEED_ADMIN_EMAIL`, `SEED_ADMIN_PASSWORD` — only used if the database is empty
   - `CORS_ORIGINS` — the two Cloudflare addresses, comma separated (fill after step 3)
   - `PUBLIC_URL` — the rider app address (fill after step 3)
   - `OSRM_URL` = `https://router.project-osrm.org` (demo walking routes; testing only)
   - `JAVA_OPTS` — copy from `render.yaml`
5. Deploy. The first build takes ~5–8 minutes. Check `https://<api>/api/v1/health` → `{"status":"UP","database":"UP"}`.

## 3. Cloudflare — the two web apps
```bash
npx wrangler login        # once; opens the browser to authorise
```
Rider app:
```bash
cd frontend
VITE_API_BASE=https://syrian-transit-api.onrender.com VITE_ADMIN_URL=https://syrian-transit-admin.syria-transit.workers.dev npm run build
npx wrangler deploy
```
Admin console:
```bash
cd admin-console
VITE_API_BASE=https://syrian-transit-api.onrender.com npm run build
npx wrangler deploy
```
The MapTiler key is read from each app's `.env.local` (see `tools/set-map-key.sh`). `not_found_handling: single-page-application` serves `index.html` for unknown paths, so app links like `/reset?token=…` work.

## 4. Connect the pieces
- Render → backend → Environment: set `CORS_ORIGINS` and `PUBLIC_URL` to the Pages addresses → **Save, rebuild and deploy**.
- MapTiler → Account → API keys → your key → **Allowed HTTP Origins**: add `syrian-transit.pages.dev` and `syrian-transit-admin.pages.dev` (one per line).

## 5. Optional
- **E-mails**: create a free Brevo account, verify a sender address, create an SMTP key, then set `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM` on Render. Without them, reset/verification links are written to the Render log.
- **Keep the API awake** (free plan sleeps after 15 min): GitHub → repository **Settings → Secrets and variables → Actions → Variables → New** `BACKEND_URL` = `https://<api>.onrender.com`. The `Keep API awake` workflow then pings it every 10 minutes from 05:00 to midnight Damascus time.
- **Android test build** against the live API: set `VITE_API_BASE=https://<api>.onrender.com` in `frontend/.env.android`, then `npm run android:apk`.

## Updating
- Backend: push to `main` → Render redeploys automatically (Blueprint / connected repo) or click **Manual Deploy**.
- Web apps: rebuild and run the same `npx wrangler deploy` commands.
