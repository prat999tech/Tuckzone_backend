# 🍽️ TuckZone — School Canteen Pre-Order System

Students, parents and teachers pre-order meals from the school canteen and get them
delivered to their classroom (or collect them at the counter). The canteen runs the whole
operation — menu, stock, orders, and finances — from an admin app.

---

## Quick start

```bash
chmod +x setup.sh && ./setup.sh
```

That checks your tools, creates the config files, starts the database and installs
dependencies. Then follow the two commands it prints at the end.

Full details, test logins and troubleshooting: **[RUNNING_LOCALLY.md](RUNNING_LOCALLY.md)**

To turn on real push notifications: **[PUSH_SETUP.md](PUSH_SETUP.md)**

---

## What you need installed

| Tool | Version | Where |
|---|---|---|
| Docker Desktop | any recent | https://docker.com/products/docker-desktop |
| Java JDK | 21 or newer | https://adoptium.net |
| Node.js | 18 or newer | https://nodejs.org |
| **Expo Go** (on your phone) | **SDK 54 build** | See note below |

Windows users: run `setup.sh` from **WSL** or **Git Bash**.

> **Expo Go version matters.** Expo Go supports exactly one SDK per release, and this
> project targets **SDK 54**. If you see *"Project is incompatible with this version of
> Expo Go"*, install the matching build directly:
> <https://github.com/expo/expo-go-releases/releases/download/Expo-Go-54.0.8/Expo-Go-54.0.8.apk>
> (uninstall any existing Expo Go first, then allow "install unknown apps" for your browser).

---

## Project layout

```
school-canteen/
├── backend/     Spring Boot 4 API (Java 21, PostgreSQL, Flyway)
├── mobile/      React Native app (Expo + TypeScript) — the app people actually use
├── frontend/    Older React web app — superseded by mobile/, kept for reference only
├── docs/        openapi.json — the generated API contract (44 endpoints)
└── setup.sh     One-shot first-time setup
```

> `frontend/` predates the mobile app and is **no longer maintained** — parts of it no
> longer match the current API. Ignore it; `mobile/` is the real client.

---

## Tech

| Layer | Choice | Why |
|---|---|---|
| API | Spring Boot 4, Java 21 | — |
| Database | PostgreSQL + Flyway | Money and stock need real transactions and row locks |
| Auth | JWT access + refresh, revocable server-side | Logout/password change actually kill a session |
| Auth (alt.) | Firebase Authentication (phone OTP + email) | Additive alongside the JWT login above — see `RUNNING_LOCALLY.md` |
| OTP | 6-digit codes **by email** | No SMS gateway, no India DLT registration, no per-message cost |
| Email | **Plain SMTP** (JavaMail) | Any provider works — Gmail, Zoho, Amazon SES's SMTP interface, self-hosted. No vendor lock-in |
| Push | Firebase Cloud Messaging | Free, and the only way to get real-time order updates on a phone |
| Mobile | React Native (Expo) + TypeScript | — |

---

## Features

**Students / Parents / Teachers**
- Register and sign in with a password **or** an emailed one-time code
- Browse the daily menu, filter by veg/non-veg and category, order for a future date
- Prepaid wallet — top up once, orders debit instantly; cancellations refund immediately
- Parents link their children (admission number + registered mobile) and order for them
- Teachers can choose **takeaway** and collect with a pickup code
- Live order tracking: placed → accepted → preparing → packed → on the way → delivered

**Canteen admin**
- Dashboard: today's orders, revenue, cost of goods, expenses, **net profit**, low stock
- Kitchen board: accept/reject, advance through each stage, assign a delivery person
- Menu catalog + per-day stock scheduling
- **Advance ordering control**: stop/reopen ordering per slot, see aggregated demand so you
  know what to cook and when to add stock
- Sales reports, expense tracking, account enable/disable

---

## Notes for whoever runs this

- Out of the box, **OTP codes, emails and push all print to the backend terminal** instead
  of being sent for real. That makes the whole app usable with zero external accounts. See
  RUNNING_LOCALLY.md to switch on real email.
- **Payments are mocked.** Wallet top-ups succeed without real money, which is deliberate
  until a payment gateway is integrated (one config flag turns it off).
- Real push notifications need a Firebase project and a development build — Expo Go cannot
  receive them. Everything else works in Expo Go.
