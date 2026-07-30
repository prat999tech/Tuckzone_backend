# TuckZone — Mobile App

React Native (Expo **SDK 54**, TypeScript) client for the TuckZone school canteen backend.

> Expo Go supports one SDK per release. Use the SDK 54 build:
> <https://github.com/expo/expo-go-releases/releases/download/Expo-Go-54.0.8/Expo-Go-54.0.8.apk>

## Setup

```bash
npm install
cp .env.example .env
# edit .env — see the comments inside for which host to use
```

### Connecting to the backend

`EXPO_PUBLIC_API_BASE_URL` in `.env` must point at a host your phone/emulator can actually
reach — `localhost` means "this device", not your computer:

| Running on | Set it to |
|---|---|
| Android emulator | `http://10.0.2.2:8080/api` (emulator's alias for your computer's localhost) |
| Physical phone, same wifi | `http://<your-computer's-LAN-IP>:8080/api` |
| Production | your deployed Render URL, e.g. `https://tuckzone.onrender.com/api` |

### Running

```bash
npx expo start
```

Scan the QR code with **Expo Go** for the fastest iteration on most screens. Real push
notifications will not work in Expo Go — see below.

## Push notifications need a development build

Expo Go uses Expo's own push infrastructure, which the backend's Firebase Admin SDK cannot
send to. To receive real pushes you need a **development build** with your own Firebase
project wired in:

1. Create a Firebase project (console.firebase.google.com), add an Android app with
   package name `com.schoolbite.canteen` (see `app.json`), download `google-services.json`
   into this folder, and add this line back to `app.json` under `expo.android`:
   ```json
   "googleServicesFile": "./google-services.json"
   ```
2. `npx expo prebuild` to generate the native Android project.
3. `npx expo run:android` (needs Android Studio) or `eas build --profile development`
   (builds in Expo's cloud, no local Android Studio needed).
4. The channel id the app registers (`canteen_orders` in
   `src/hooks/usePushRegistration.ts`) **must match** the backend's
   `FirebasePushSender.ANDROID_CHANNEL_ID` exactly, or Android 8+ silently drops every push.

## Architecture notes

- **`src/theme/`** — the only place colors/spacing/type sizes are defined. Every screen
  pulls from here; nothing hardcodes a hex value.
- **`src/api/client.ts`** — the axios instance with the token-refresh interceptor. Refreshes
  are single-flight: if several requests 401 at once, they all await one refresh call
  rather than racing separate ones.
- **`src/context/AuthContext.tsx`** / **`CartContext.tsx`** — global session and cart state.
- **`src/screens/customer/CheckoutScreen.tsx`** — note the idempotency key is generated
  **once per screen mount** (`useRef`), not per button tap. Regenerating it per-tap is
  exactly the bug that let a double-tap create two orders in the original web app.
- Admin screens live under `src/screens/admin/`, customer screens under
  `src/screens/customer/` — navigation switches between the two stacks entirely based on
  `user.role` (`src/navigation/RootNavigator.tsx`).

## Known gaps (by design, not oversight)

- Payment gateway is mocked on the backend (`walletApi.mockCompleteTopup`) until a real
  gateway is integrated — swap that one call when it's ready, everything else is unchanged.
- OTP delivery and email are backend-config flips (`app.otp.delivery`,
  `app.notification.email-provider`) requiring real MSG91/Resend accounts — not something
  the app needs to change for.
