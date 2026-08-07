# Deploying TuckZone — click by click

Follow top to bottom. Roughly 30 minutes of clicking, plus build queue time.

> **No real secrets are in this file** — it is committed to a public repository. Wherever a
> step says `<from backend/.env>`, open `backend/.env` on your laptop and copy the value
> from there. That file is gitignored and never leaves your machine.

Read the values you'll need now, so they're in front of you:

```bash
cd ~/school-canteen
grep -E "^JWT_SECRET=|^ADMIN_SIGNUP_CODE=" backend/.env
```

---

## What you are building

```
Neon (Postgres)  ◄──  Render (Spring Boot, Docker)  ◄──  Android phone
  free, permanent       free, sleeps unless pinged        APK or Expo Go
```

Accounts needed, all free: **Neon**, **Render**, **UptimeRobot**.

---

## STEP 1 · Database on Neon

**Time: 3 minutes**

1. Open <https://neon.tech> → **Sign up** → continue with GitHub
2. You land on **Create your first project**. Fill in:
   - **Project name:** `tuckzone`
   - **Postgres version:** leave the default
   - **Cloud service provider:** AWS
   - **Region:** **Asia Pacific (Mumbai)** — `ap-south-1`
3. Click **Create project**
4. A dialog appears titled **Connection string**. Make sure the dropdown says
   **Parameters only** or **Connection string**, then copy it. It looks like:

   ```
   postgresql://neondb_owner:npg_AbC123xyz@ep-cool-name-a1b2c3.ap-south-1.aws.neon.tech/neondb?sslmode=require
   ```

5. **Break it into three pieces** — Render needs them separately. Using the example above:

   | Piece | Where it is in the string | Example value |
   |---|---|---|
   | username | between `://` and `:` | `neondb_owner` |
   | password | between `:` and `@` | `npg_AbC123xyz` |
   | host + db | everything after `@` | `ep-cool-name-a1b2c3.ap-south-1.aws.neon.tech/neondb?sslmode=require` |

   So the JDBC URL you will paste into Render is the host part with `jdbc:postgresql://`
   in front:

   ```
   jdbc:postgresql://ep-cool-name-a1b2c3.ap-south-1.aws.neon.tech/neondb?sslmode=require
   ```

   ⚠️ Keep `?sslmode=require`. Neon rejects unencrypted connections, and the error it
   returns looks like a wrong password, which sends you debugging the wrong thing.

6. **Paste all four values into a scratch file** — you need them in Step 2 and Step 4.

---

## STEP 2 · Backend on Render

**Time: 10 minutes of setup, then a 10-minute build**

1. Open <https://render.com> → **Get Started** → sign in with GitHub
2. Top right: **New +** → **Web Service**
3. **Connect a repository** → find `prat999tech/Tuckzone_backend` → **Connect**
   - If it isn't listed: **Configure account** → grant Render access to the repo
4. You are now on the settings form. Set **exactly** these:

   | Field | Value |
   |---|---|
   | **Name** | `tuckzone` (this becomes `tuckzone.onrender.com`) |
   | **Project** | leave blank |
   | **Language** | **Docker** |
   | **Branch** | `main` |
   | **Region** | Singapore |
   | **Root Directory** | `backend` |
   | **Instance Type** | **Free** |

   ⚠️ **Root Directory must be `backend`.** The `Dockerfile` is inside that folder. Leave it
   blank and the build fails in the first few seconds with "no Dockerfile found".

   ⚠️ **Language must be Docker**, not Node. Render sometimes guesses Node because of the
   `mobile/` folder.

5. Scroll to **Environment Variables** → click **Add from .env** (or **Add Environment
   Variable** repeatedly). Paste this block, replacing the four placeholders:

   ```
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=jdbc:postgresql://<your-neon-host>/neondb?sslmode=require
   SPRING_DATASOURCE_USERNAME=<your-neon-username>
   SPRING_DATASOURCE_PASSWORD=<your-neon-password>
   JWT_SECRET=<from backend/.env>
   ADMIN_SIGNUP_CODE=<from backend/.env>
   APP_ALLOW_MOCK_TOPUP=true
   APP_OTP_DELIVERY=log
   APP_EMAIL_PROVIDER=log
   APP_TIMEZONE=Asia/Kolkata
   JAVA_TOOL_OPTIONS=-Xmx350m -XX:MaxMetaspaceSize=128m
   ```

   **Two of these are not optional:**

   - **`JAVA_TOOL_OPTIONS`** — the free instance has 512 MB. A JVM that sizes its own heap
     will grow past that and get OOM-killed. It usually happens *minutes into use*, not at
     startup, so it looks like the app randomly dies during your demo.
   - **`ADMIN_SIGNUP_CODE`** — `application.yml` is public on GitHub and carries the
     fallback `CANTEEN-SETUP-2026`. Skip this variable and anyone reading your repository
     can register themselves as canteen admin.

6. Click **Deploy Web Service**
7. The **Logs** tab opens. First build takes 8–12 minutes. You want to see, in order:
   ```
   ==> Building Docker image
   Successfully migrated schema "public" to version "10 - email otp"
   Tomcat started on port 10000
   Started CanteenApplication in 14.2 seconds
   ==> Your service is live 🎉
   ```
   `Successfully migrated schema ... version "10"` is Flyway building all ten migrations on
   the empty Neon database. That is the moment the deployment has really worked.

8. Copy your URL from the top of the page: `https://tuckzone.onrender.com`

### Verify before continuing

```bash
curl https://tuckzone.onrender.com/api/config
```

Expected — JSON starting with:
```json
{"currency":"INR","timezone":"Asia/Kolkata","otpLength":6,...}
```

If this doesn't work, **stop and fix it here.** Every later step depends on it.

---

## STEP 3 · Stop it falling asleep

**Time: 2 minutes. Do this tonight, not tomorrow morning.**

Render's free tier sleeps after 15 minutes idle and takes ~50 seconds to wake. Mid-pitch
that reads as a broken app.

1. <https://uptimerobot.com> → **Register for FREE** → verify your email
2. **+ New monitor**
3. Fill in:
   - **Monitor Type:** `HTTP(s)`
   - **Friendly Name:** `TuckZone backend`
   - **URL:** `https://tuckzone.onrender.com/api/config`
   - **Monitoring interval:** `every 5 minutes`
4. **Create monitor**

Within ~10 minutes it should show **Up** in green. Running 24/7 uses ~720 of Render's 750
free instance-hours per month, so you stay inside the free tier.

---

## STEP 4 · Load the demo menu

**Time: 1 minute**

An empty menu is a bad first impression, and adding items by hand through the admin UI at
midnight is a waste of your evening.

**Easiest route — Neon's browser SQL editor:**

1. Neon dashboard → your `tuckzone` project → **SQL Editor** in the left sidebar
2. Open `backend/demo-seed.sql` on your laptop, copy **all** of it
3. Paste into the editor → **Run**

**Or from your terminal, if you have `psql`:**

```bash
cd ~/school-canteen
psql "postgresql://<user>:<password>@<neon-host>/neondb?sslmode=require" -f backend/demo-seed.sql
```

Expected result:

```
 catalogue_items | on_menu_today | on_menu_tomorrow | active_slots
              13 |            13 |               13 |            2
```

That is 13 items across Meals / Snacks / Drinks / Combos, veg and non-veg, each with a cost
price so the **admin dashboard shows real profit numbers** rather than zeros — published for
**today and tomorrow**, so advance ordering demos either way.

Safe to run twice; a second run inserts nothing.

The ordering slot already exists — migrations V4, V12 and V13 leave a single active *Recess* slot, so there is nothing to configure here.

---

## STEP 5 · Point the app at your backend

**Time: 30 seconds**

```bash
cd ~/school-canteen
./configure-backend-url.sh https://tuckzone.onrender.com
```

This checks the backend actually responds, then updates `mobile/.env` **and** both build
profiles in `mobile/eas.json`. Doing it by hand is easy to get subtly wrong — a stray
trailing slash, a missing `/api`, one file updated and not the other — and with the APK you
only find out 40 minutes later when the build finishes.

Expected:
```
✓ backend responded 200 and looks like TuckZone
✓ mobile/.env updated
✓ mobile/eas.json updated
```

---

## STEP 6 · Get it onto a phone

**Do Step 5 first.** The API URL is compiled into the APK at build time — build before Step
5 and you ship an app pointing at a placeholder, and you wait out the queue twice.

### Option A — Expo Go (works in 2 minutes, needs your laptop)

```bash
cd ~/school-canteen/mobile
npx expo start
```

Install **Expo Go** from the Play Store on the phone, scan the QR in the terminal. The phone
must be on the same network as your laptop — **your phone's hotspot is fine**, connect the
laptop to it.

Only Metro (the JavaScript) comes from your laptop; the API calls go to Render. Push
notifications don't work in Expo Go — nothing else is affected.

### Option B — standalone APK (queue 30–90 min, no laptop needed)

```bash
cd ~/school-canteen/mobile
npx eas-cli build --profile preview --platform android
```

When it finishes you get a link. Open it on **any** Android phone → download the `.apk` →
allow "install from unknown sources" → install. No Play Store, no Expo Go, no laptop, works
anywhere with internet.

**Do both.** Start the APK build, and use Option A as the guaranteed fallback while it runs.

---

## STEP 7 · Create your admin account and rehearse

**Time: 5 minutes. Do not skip this.**

1. Open the app → **Register** → **Canteen** tab
2. Fill in name, email, mobile, password
3. **Canteen Signup Code:** the `ADMIN_SIGNUP_CODE` from `backend/.env`
4. **Register** → you land on **Verify your email**
5. The code is **already filled in** (`APP_OTP_DELIVERY=log` returns it in the response, so
   no mail server is needed) → **Verify & continue**
6. Sign in

Now walk the whole demo once, on the deployed app:

- [ ] Admin: **Menu Catalog** shows 13 items
- [ ] Admin: **Dashboard** shows sales/stock figures
- [ ] Register a second account as a **Student** or **Parent**
- [ ] Customer: **Wallet** → top up (it's mock money, it just works)
- [ ] Customer: add items → cart bar appears at the bottom → **View Cart** → place order
- [ ] Admin: **Orders Board** → mark it delivered
- [ ] Customer: order shows as delivered

**Rehearse on the Render URL, not on localhost.** Localhost working proves nothing about
the deployment.

---

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Build fails in seconds, "no Dockerfile" | **Root Directory** isn't `backend` |
| Render picked Node, build fails | **Language** must be `Docker` |
| `FATAL: password authentication failed` | `?sslmode=require` missing, or username left inside the URL |
| `502` or restarts every few minutes | `JAVA_TOOL_OPTIONS` missing — JVM being OOM-killed |
| `Flyway ... validate failed` | Database has partial schema. On a demo DB: Neon → drop the tables, redeploy |
| First request takes ~50s | Service slept — Step 3 not done |
| App: "Network error" | Step 5 not run, or APK built before Step 5 |
| Register gives 403 | Email not verified — the code is prefilled, just submit it |
| "Invalid admin signup code" | `ADMIN_SIGNUP_CODE` on Render doesn't match what you typed |
| Menu is empty | Step 4 not run, or you're viewing a date with no published menu |

**Render logs:** dashboard → your service → **Logs**. Nearly every failure above names
itself there.

---

## Timeline for tonight

| When | What |
|---|---|
| Now | Steps 1–4 — get `curl .../api/config` returning JSON |
| +30 min | Step 5, then start the APK build (Option B) so it queues overnight |
| While it builds | Step 7 via Expo Go — rehearse the full demo |
| Before bed | Confirm UptimeRobot shows **Up** |
| Morning | Install the APK, run through the demo once more |

The risk drops sharply once Step 2 verifies. After that, Expo Go over your hotspot is already
a complete working demo — the APK is polish, not a dependency.

---

## Before real users (not needed for the demo)

- [ ] `APP_ALLOW_MOCK_TOPUP=false` — wallet top-ups are currently **free money**, deliberately
- [ ] Real payment gateway
- [ ] `APP_OTP_DELIVERY=email`, `APP_EMAIL_PROVIDER=smtp`, real SMTP credentials, and
      `EMAIL_FROM_ADDRESS` on a domain you own with SPF + DKIM
- [ ] Rotate `JWT_SECRET` and `ADMIN_SIGNUP_CODE`
- [ ] Paid Render instance, so uptime doesn't depend on a pinger
- [ ] Neon backups enabled
