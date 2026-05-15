# Deploying SpendSmart Backend on Render (Free Tier)

This guide deploys **5 services** on Render's free tier:

| # | Service        | Render name           | Role                                |
|---|----------------|-----------------------|-------------------------------------|
| 1 | eureka-server  | `spendsmart-eureka`   | Service discovery                   |
| 2 | gateway        | `spendsmart-gateway`  | Public API entry point              |
| 3 | auth-service   | `spendsmart-auth`     | Login / register / JWT              |
| 4 | category       | `spendsmart-category` | Category CRUD                       |
| 5 | expense        | `spendsmart-expense`  | Expense CRUD                        |

The frontend deploys separately to **Vercel** (covered at the end).

---

## Prerequisites — 10 minutes

### 1. Push this repo to GitHub
Your Render account is already connected to GitHub. Make sure the repo is pushed and `render.yaml` is at the root.

### 2. Create a free MySQL host
Pick **one** of these (whichever you prefer):

| Provider       | Free tier              | Notes                              |
|----------------|------------------------|------------------------------------|
| **Aiven**      | 1 month trial          | Easiest setup, fast, expires       |
| **Clever Cloud** | 10 MB permanent free | Tiny, but never expires            |
| **Railway**    | $5 credit/month        | MySQL plugin, runs out fast        |

After signup, create one MySQL database and note these values:
- **Host**: e.g. `mysql-12345.aivencloud.com`
- **Port**: e.g. `12345`
- **Username**: e.g. `avnadmin`
- **Password**: `<long-string>`

### 3. Create the 3 schemas
Connect to MySQL with any client (MySQL Workbench, DBeaver, `mysql` CLI):
```sql
CREATE DATABASE spendsmart_auth;
CREATE DATABASE spendsmart_category;
CREATE DATABASE spendsmart_expense;
```

(If your provider only allows one database, you can put all 3 schemas inside it — but you'd then need to add `?currentSchema=` overrides. Easier to use a provider that allows multiple databases.)

---

## Phase 1 — Deploy via Blueprint (one click)

1. Go to **https://dashboard.render.com** → top-right **"New +"** → **"Blueprint"**.
2. Pick your GitHub repo. Render reads `render.yaml` and lists **5 services**.
3. Click **"Apply"**. Render starts building all 5. **DO NOT** click into any service yet — env vars are not set, builds will succeed but the services will crash on startup. That's expected.

## Phase 2 — Configure env vars in this exact order

### Step A — Wait for `spendsmart-eureka` to finish building (~5 min)
The eureka service has no external dependencies and should start successfully.

- Go to **spendsmart-eureka** → top of page shows the public URL, e.g.  
  `https://spendsmart-eureka.onrender.com`
- Visit that URL — you should see the Eureka dashboard. If yes, copy the full URL.

### Step B — Set env vars on each of the other 4 services

For each of `spendsmart-auth`, `spendsmart-category`, `spendsmart-expense`, `spendsmart-gateway`:

1. Open the service → **Environment** tab → click **Edit**
2. Fill in:

| Variable                 | Value                                                                             |
|--------------------------|-----------------------------------------------------------------------------------|
| `EUREKA_URL`             | `https://spendsmart-eureka.onrender.com/eureka/` (note the trailing slash)        |
| `DB_URL` (auth)          | `jdbc:mysql://<host>:<port>/spendsmart_auth?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_URL` (category)      | `jdbc:mysql://<host>:<port>/spendsmart_category?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_URL` (expense)       | `jdbc:mysql://<host>:<port>/spendsmart_expense?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME`            | `<your MySQL user>`                                                               |
| `DB_PASSWORD`            | `<your MySQL password>`                                                           |
| `JWT_SECRET`             | `SpendSmartSecretKey2026_Standard32Bytes!` (or any 32-char value — SAME on all 3) |
| `GOOGLE_CLIENT_ID` (auth)| Your Google OAuth client ID (optional; only needed for Google login)              |

The gateway only needs `EUREKA_URL` (no DB, no JWT).

3. Save. Render redeploys the service automatically.

### Step C — Verify boot order
Render rebuilds services in parallel after env var changes. Wait ~5-10 minutes. Check each service's **Logs** tab.

- Look for `Started <Service>Application in X seconds`
- Eureka registration message: `DiscoveryClient_AUTH-SERVICE/... registered with status: 204`

If a service fails:
- **Database connection refused** → wrong DB_URL, username, or password
- **Unable to register with Eureka** → wrong EUREKA_URL or trailing slash missing

## Phase 3 — Verify deployment

### Open Eureka dashboard
`https://spendsmart-eureka.onrender.com`

Under **Instances currently registered with Eureka** you should see 4 apps:
`AUTH-SERVICE`, `CATEGORY-SERVICE`, `EXPENSE-SERVICE`, `GATEWAY-SERVICE`.

### Test the gateway with curl
```bash
# Replace with your actual gateway URL
GATEWAY=https://spendsmart-gateway.onrender.com

# Register a test user via the gateway → auth-service
curl -X POST $GATEWAY/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@x.com","passwordHash":"pass1234","fullName":"Test User"}'

# Login
curl -X POST $GATEWAY/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@x.com","password":"pass1234"}'
```

You should get back a JWT. The 5-service deployment is now live.

---

## Phase 4 — Deploy the frontend (Vercel)

1. Go to **https://vercel.com** → **New Project** → import your frontend repo.
2. In the **Environment Variables** step, set:
   - `NEXT_PUBLIC_API_URL` (Next.js) or `API_URL` (Angular) → `https://spendsmart-gateway.onrender.com/api`
3. Deploy.

Update your Angular `environment.prod.ts`:
```ts
export const environment = {
  production: true,
  apiUrl: 'https://spendsmart-gateway.onrender.com/api'
};
```

---

## Free-tier gotchas you should know about

| Gotcha                          | Effect                          | Workaround                                                   |
|---------------------------------|---------------------------------|--------------------------------------------------------------|
| Services sleep after 15 min idle | First request takes ~50 s       | Use **UptimeRobot** (free) to ping every 14 minutes          |
| 750 free hours/month shared     | 5 always-on services use 3,600+ | Sleep behavior keeps you under the limit naturally for a demo |
| Cold start ~50 s                | Bad first impression            | Demo flow: wake up backend 1 min before demoing              |
| Aiven free MySQL expires        | DB vanishes in 30 days          | Migrate to Clever Cloud, or upgrade if Aiven offers a renewal|

---

## When you want to update code

Render auto-deploys on every push to `main`. No action needed beyond `git push`.

To redeploy without code changes: service → **Manual Deploy** → **Clear build cache & deploy**.
