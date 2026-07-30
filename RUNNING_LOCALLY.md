# Running TuckZone locally

Everything below is verified working on this machine.

## Prerequisites (already installed here)
Docker, Java 21+, Node 18+.

## First-time setup (once)

```bash
cd ~/school-canteen
cp backend/.env.example backend/.env
cp mobile/.env.example  mobile/.env
```

Then set the API URL in `mobile/.env` to match how you'll test:

| Testing on | EXPO_PUBLIC_API_BASE_URL |
|---|---|
| Physical Android phone (same wifi) | `http://<YOUR-LAN-IP>:8080/api` |
| Android Studio emulator | `http://10.0.2.2:8080/api` |

Find your LAN IP with `ip -4 addr show wlan0 | grep -oP 'inet \K[\d.]+'`.
It changes when you reconnect to wifi — re-check if the phone can't connect.

## Every time you work (3 terminals)

**Terminal 1 — database**
```bash
cd ~/school-canteen
docker compose -f docker-compose.dev.yml up -d
```

**Terminal 2 — backend**
```bash
cd ~/school-canteen/backend
set -a && source .env && set +a      # Spring Boot does NOT auto-read .env
./mvnw spring-boot:run
```
Wait for `Started CanteenApplication`. Check: <http://localhost:8080/api/health>
Browse the API: <http://localhost:8080/swagger-ui.html>

**Terminal 3 — mobile app**
```bash
cd ~/school-canteen/mobile
npx expo start
```
Scan the QR with Expo Go. Everything works except real push (needs a dev build).

**Expo Go must match the project's SDK (54).** If it says *"Project is incompatible with
this version of Expo Go"*, install the matching build:
<https://github.com/expo/expo-go-releases/releases/download/Expo-Go-54.0.8/Expo-Go-54.0.8.apk>

## Test accounts (created on a fresh DB)

| Role | Email | Password |
|---|---|---|
| Canteen Admin | admin@school.local | Admin@12345 |
| Parent | parent@school.local | Parent@123 |
| Student | student@school.local | Student@123 |
| Teacher | teacher@school.local | Teacher@123 |

Student admission number `ADM-001`, parent mobile `9876500001` — use these on the
parent's "Link a Child" screen.

Register more via the app, or the admin invite code `CANTEEN-SETUP-2026`.

## Where do OTPs and emails go?

In the default `log` mode they are **printed to the backend terminal**, and the OTP is
also returned in the API response so the app fills it in automatically. Look for:

```
[DEV OTP]   purpose=LOGIN email=... code=123456
[DEV EMAIL] to=... | subject=New sign-in to your account | ...
[DEV PUSH]  to=... | Order placed | ...
```

## Turning on real email (optional)

Edit `backend/.env`:

```bash
APP_OTP_DELIVERY=email
APP_EMAIL_PROVIDER=smtp
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=<16-char Google App Password>
EMAIL_FROM_ADDRESS=you@gmail.com
```

The Gmail password must be an **App Password** (Google Account → Security → 2-Step
Verification → App passwords), not your normal password. Restart the backend.
Wrong/missing credentials make the app refuse to start with a clear message — that is
deliberate, so a misconfiguration can't silently swallow every email.

Any SMTP provider works (Zoho `smtp.zoho.in`, AWS SES, your own domain) — only these
four values change.

## Reset to a clean database

```bash
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d
```
Wipes all data; Flyway rebuilds the schema on next backend start.

## Troubleshooting

| Problem | Fix |
|---|---|
| Phone can't reach backend | Use LAN IP not `localhost`; same wifi; check IP hasn't changed |
| `Port 8080 already in use` | An old backend is running — `pkill -f spring-boot:run` |
| Backend won't start, DB error | Is the container up? `docker ps` |
| App shows old data | Pull to refresh, or restart Expo with `npx expo start -c` |
