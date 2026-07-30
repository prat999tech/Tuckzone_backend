# Enabling real push notifications

Push needs three things wired together:

```
Firebase project  ──►  mobile/google-services.json   (app can receive)
       │
       └──────────►  backend FIREBASE_CREDENTIALS_JSON (server can send)

Development build ──►  replaces Expo Go (Expo Go cannot receive push at all)
```

Config files (`eas.json`, `app.json`) are already prepared. The steps below are the ones
that need your own Google and Expo accounts.

---

## Step 1 — Create the Firebase project

1. Go to <https://console.firebase.google.com> → **Add project** → name it `TuckZone`
   (Google Analytics is optional — skip it).
2. On the project dashboard click the **Android** icon to add an Android app.
3. **Android package name** — must be exactly:
   ```
   com.schoolbite.canteen
   ```
   This has to match `expo.android.package` in `mobile/app.json`, or push silently fails.
4. Register the app, then **Download `google-services.json`**.
5. Put it at:
   ```
   school-canteen/mobile/google-services.json
   ```
   (already in `.gitignore` — never commit it)

## Step 2 — Get the server credentials

This is a **different** file from step 1. Step 1 lets the app *receive*; this lets the
backend *send*.

1. Firebase console → ⚙️ **Project settings** → **Service accounts** tab
2. **Generate new private key** → confirm → a `.json` downloads
3. **This file does not go anywhere inside the project by hand.** Run the helper and point
   it at wherever the file downloaded:

   ```bash
   cd ~/school-canteen/backend
   ./add-firebase-creds.sh ~/Downloads/tuckzone-firebase-adminsdk-xxxxx.json
   ```

   It validates the file is genuinely a service-account key (it refuses
   `google-services.json`, which is the usual mix-up), copies it to
   `backend/firebase-service-account.json` with `chmod 600`, and appends to `backend/.env`:

   ```
   APP_PUSH_PROVIDER=firebase
   GOOGLE_APPLICATION_CREDENTIALS=/abs/path/to/backend/firebase-service-account.json
   ```

   Pointing at a *path* rather than inlining the JSON avoids fighting `.env` quoting — the
   private key contains newlines. Both the key file and `.env` are gitignored.

4. Restart the backend. On boot the `[DEV PUSH]` log lines stop appearing — that means
   `FirebasePushSender` took over from the logging one.

> **Deploying to Render/Railway later?** There is no filesystem for secrets there, so use
> the other supported form instead — set `FIREBASE_CREDENTIALS_JSON` to the base64 of the
> key file (`base64 -w0 backend/firebase-service-account.json`) and leave
> `GOOGLE_APPLICATION_CREDENTIALS` unset. The backend accepts raw JSON or base64.

## Step 3 — Build the development client

```bash
cd ~/school-canteen/mobile
npx eas-cli login          # free account — create one at expo.dev if needed
npx eas-cli build:configure # links the project, only needed once
npx eas-cli build --profile development --platform android
```

The build runs on Expo's servers — **no Android Studio needed**. Takes roughly 10–20
minutes (longer on the free queue). When it finishes you get a URL and a QR code.

Open that link on your **phone**, download the `.apk`, and install it (allow "install
unknown apps" when prompted). This app replaces Expo Go for this project.

## Step 4 — Run it

```bash
cd ~/school-canteen/mobile
npx expo start --dev-client
```

Open the **TuckZone** app you just installed (not Expo Go) and scan the QR.

> The development build loads its JavaScript from Metro on your laptop, so it reads
> `mobile/.env` exactly like Expo Go did — your LAN IP still applies, and code changes
> still hot-reload. Only the native shell changed.

## Step 5 — Verify push actually works

1. Sign in on the phone → the app requests notification permission → **Allow**
2. Check the device registered:
   ```bash
   docker exec canteen-dev-db psql -U canteen -d canteen -c \
     "select platform, left(token, 25) || '...' as token, last_seen_at from device_tokens;"
   ```
   A row here means the phone's FCM token reached the backend.
3. Trigger a notification — top up the wallet, or place an order.
4. The notification should appear on the phone **even with the app closed**.

---

## Troubleshooting

| Symptom | Cause |
|---|---|
| `eas build` fails: *google-services.json not found* | Step 1 file is missing or misnamed |
| Build log warns *"googleServicesFile … won't be uploaded to the builder"* | `google-services.json` is gitignored, and EAS uploads via git. `.easignore` at the **repo root** (`school-canteen/.easignore`, *not* `mobile/`) fixes it — eas-cli looks for it at the git root only. Note that once it exists, **every `.gitignore` in the repo is ignored**, so all secrets must be re-listed there. If the warning still appears, the APK will build fine but push will never work |
| Build succeeds, no `device_tokens` row | Permission denied on the phone, or the app was opened before signing in — sign out and back in |
| Row exists, no notification arrives | Package name mismatch, or backend still on `APP_PUSH_PROVIDER=log` — check backend logs for `[DEV PUSH]` |
| Notification sent, nothing shows on Android 8+ | Channel id mismatch. App uses `canteen_orders` (`usePushRegistration.ts`), backend stamps the same (`FirebasePushSender.ANDROID_CHANNEL_ID`) — they must be identical |

Backend-side delivery is visible in the database:

```sql
select channel, status, attempts, last_error
from notification_outbox
where channel = 'PUSH'
order by created_at desc limit 10;
```

`SENT` = FCM accepted it. `FAILED` with `last_error` = look at the message.
Dead tokens are pruned automatically, so a disappeared row usually means the install
was replaced.
