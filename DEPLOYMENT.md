# Deploying TuckZone

End-to-end, roughly 45 minutes of work plus queue time for the Android build.

> **Secrets are not written in this file.** It is committed to a public repository. Every
> real value lives in `backend/.env`, which is gitignored — read them from there when a step
> below says `<from backend/.env>`.

---

## What you are deploying

```
Neon (Postgres)  ◄────  Render (Spring Boot in Docker)  ◄────  Android app
   free, managed          free tier, sleeps unless pinged        APK or Expo Go
```

Three accounts, all free: **Neon**, **Render**, **UptimeRobot**. GitHub you already have.

The backend is already deployment-ready — multi-stage `Dockerfile`, `PORT` from the
environment, externalised datasource, and Flyway migrations that build the whole schema on
first boot. There is nothing to change in the code.

---

## Step 1 — Database on Neon (~3 min)

1. <https://neon.tech> → sign up → **Create project**
   - Name: `tuckzone`
   - Region: **AWS Asia Pacific (Mumbai)** — closest to Render's Singapore region and to
     your users
2. Copy the connection string it shows you:
   ```
   postgresql://USER:PASSWORD@ep-xxxx-123.ap-south-1.aws.neon.tech/neondb?sslmode=require
   ```
3. Rewrite it into the three values Render needs. Note the `jdbc:` prefix and that the
   username and password come **out** of the URL:
   ```
   SPRING_DATASOURCE_URL      = jdbc:postgresql://ep-xxxx-123.ap-south-1.aws.neon.tech/neondb?sslmode=require
   SPRING_DATASOURCE_USERNAME = USER
   SPRING_DATASOURCE_PASSWORD = PASSWORD
   ```

`?sslmode=require` is mandatory — Neon refuses plaintext connections.

The free tier is permanent (0.5 GB). It idles after five minutes and wakes in under a
second, which is invisible in use.

---

## Step 2 — Backend on Render (~10 min setup, ~10 min first build)

1. <https://render.com> → **New → Web Service** → connect GitHub → pick `Tuckzone_backend`
2. Configure:

   | Setting | Value |
   |---|---|
   | Root Directory | `backend` |
   | Language | `Docker` |
   | Instance Type | `Free` |
   | Region | Singapore |

   **Root Directory must be `backend`** — the `Dockerfile` lives there, not at the repo root.

3. Add environment variables:

   ```bash
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=<from step 1>
   SPRING_DATASOURCE_USERNAME=<from step 1>
   SPRING_DATASOURCE_PASSWORD=<from step 1>
   JWT_SECRET=<from backend/.env>
   ADMIN_SIGNUP_CODE=<from backend/.env>
   APP_ALLOW_MOCK_TOPUP=true
   APP_OTP_DELIVERY=log
   APP_EMAIL_PROVIDER=log
   APP_TIMEZONE=Asia/Kolkata
   JAVA_TOOL_OPTIONS=-Xmx350m -XX:MaxMetaspaceSize=128m
   ```

   **`JAVA_TOOL_OPTIONS` is not optional.** Render's free instance has 512 MB. A JVM left to
   size its own heap will grow past that and be OOM-killed — typically minutes into a demo,
   not at startup, which makes it look like a random crash.

   **`ADMIN_SIGNUP_CODE` is not optional either.** `application.yml` carries a public
   fallback (`CANTEEN-SETUP-2026`) that anyone reading the repository can see. Without this
   variable set, a stranger can register themselves as canteen admin.

4. **Create Web Service.** The first Docker build takes 8–12 minutes.

### Verify before going further

```bash
curl https://<your-app>.onrender.com/api/config
```

Expect JSON containing `"otpLength":6`. If you get HTML or a 502, open the Render logs —
look for `Started CanteenApplication`, which means Flyway ran and the app is live.

---

## Step 3 — Keep it awake (~2 min)

Render's free tier sleeps after 15 minutes idle and takes ~50 seconds to wake. During a
demo that reads as a broken app.

<https://uptimerobot.com> → free account → **Add New Monitor**

| Field | Value |
|---|---|
| Monitor Type | HTTP(s) |
| URL | `https://<your-app>.onrender.com/api/config` |
| Interval | 5 minutes |

Running 24/7 costs about 720 of Render's 750 free instance-hours per month, so this stays
inside the free tier. **Set this up the night before, not an hour before.**

---

## Step 4 — Load demo data (~1 min)

An empty menu is a bad first impression, and clicking items in one by one at midnight is a
poor use of the evening.

```bash
psql "postgresql://USER:PASSWORD@ep-xxxx.ap-south-1.aws.neon.tech/neondb?sslmode=require" \
     -f backend/demo-seed.sql
```

No `psql` locally? Paste the file's contents into Neon's **SQL Editor** in the browser.

It creates 13 menu items with cost prices (so the admin dashboard shows real
profit figures) and publishes all of them for **today and tomorrow**. It is safe to re-run —
a second run inserts nothing.

Expected output:

```
 catalogue_items | on_menu_today | on_menu_tomorrow | active_slots
              13 |            13 |               13 |            2
```

Delivery slots (Morning Recess, Lunch Recess) already exist — migration V4 inserts them.

---

## Step 5 — Create the admin account (~2 min)

In the app: **Register → Canteen** tab → fill in details → enter `ADMIN_SIGNUP_CODE`.

Because `APP_OTP_DELIVERY=log`, the verification code comes back in the API response and the
app fills it in for you — no email needed. Enter it and you are in.

Do this **before** the pitch and place one test order end-to-end.

---

## Step 6 — Point the app at the backend

```jsonc
// mobile/eas.json — the "preview" profile
"env": { "EXPO_PUBLIC_API_BASE_URL": "https://<your-app>.onrender.com/api" }
```

```bash
# mobile/.env — for running via Expo Go
EXPO_PUBLIC_API_BASE_URL=https://<your-app>.onrender.com/api
```

Trailing `/api` matters. No trailing slash after it.

---

## Step 7 — Build the Android app (queue: 30–90 min)

```bash
cd mobile
npx eas-cli build --profile preview --platform android
```

**The API URL is compiled in at build time.** Build before Step 6 and you ship an app
pointing at a placeholder. If the Render URL ever changes, you must rebuild.

You get a link; open it on any Android phone, download the `.apk`, allow "install unknown
apps". No Play Store, no Expo Go, no laptop.

### Faster alternative, no build

Expo Go works with a deployed backend and needs no APK:

```bash
cd mobile && npx expo start
```

Your phone must share a network with the laptop running Metro — a phone hotspot is fine.
`--tunnel` removes even that requirement but depends on ngrok's anonymous tier, which fails
intermittently; do not rely on it for a live demo.

---

## Troubleshooting

| Symptom | Cause |
|---|---|
| Render build fails immediately | Root Directory is not `backend` |
| `502` / app restarts every few minutes | `JAVA_TOOL_OPTIONS` missing — the JVM is being OOM-killed |
| `FATAL: password authentication failed` | `?sslmode=require` missing, or username left inside the URL |
| First request takes ~50 s | Service slept; UptimeRobot not configured |
| `Flyway ... validate failed` | Database has partial schema. On a demo DB, drop and let migrations rerun |
| App shows "Network error" | `EXPO_PUBLIC_API_BASE_URL` wrong, or APK built before Step 6 |
| Registration gives 403 | Email not verified — enter the code, it is prefilled |
| Menu is empty | Step 4 not run, or you are viewing a date with no published menu |

---

## Before real users (not needed for a demo)

- [ ] `APP_ALLOW_MOCK_TOPUP=false` — wallet top-ups are currently **free money**, deliberately
- [ ] Real payment gateway wired in
- [ ] `APP_OTP_DELIVERY=email` + `APP_EMAIL_PROVIDER=smtp` with real SMTP credentials, and
      `EMAIL_FROM_ADDRESS` on a domain you own with SPF and DKIM configured
- [ ] Rotate `JWT_SECRET` and `ADMIN_SIGNUP_CODE`
- [ ] Paid Render instance, so the service does not depend on a pinger
- [ ] Neon backups enabled
