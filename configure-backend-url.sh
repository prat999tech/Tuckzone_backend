#!/usr/bin/env bash
# Points the mobile app at a deployed backend.
#
#   ./configure-backend-url.sh https://tuckzone.onrender.com
#
# Updates mobile/.env (used by Expo Go) and both build profiles in mobile/eas.json (baked
# into the APK at build time). Doing this by hand is easy to get subtly wrong — a trailing
# slash, a missing /api, or updating one file but not the other — and the APK failure only
# shows up 40 minutes later, after the build queue.

set -euo pipefail
cd "$(dirname "$0")"

GREEN=$'\033[32m'; RED=$'\033[31m'; YELLOW=$'\033[33m'; BOLD=$'\033[1m'; OFF=$'\033[0m'

RAW="${1:-}"
if [ -z "$RAW" ]; then
  echo "${RED}Usage:${OFF} ./configure-backend-url.sh <backend-url>"
  echo "Example: ./configure-backend-url.sh https://tuckzone.onrender.com"
  exit 1
fi

# Accept with or without a trailing slash, and with or without /api already on the end.
BASE="${RAW%/}"
BASE="${BASE%/api}"
API="$BASE/api"

echo "${BOLD}Backend base URL:${OFF} $BASE"
echo "${BOLD}API URL the app will use:${OFF} $API"
echo

# Check it actually answers before rewriting anything. A typo caught here costs seconds; the
# same typo caught after an EAS build costs the better part of an hour.
echo "Checking $API/config ..."
CODE=$(curl -s -o /tmp/tuckzone-config-check -w '%{http_code}' --max-time 25 "$API/config" || echo "000")
if [ "$CODE" = "200" ] && grep -q "otpLength" /tmp/tuckzone-config-check 2>/dev/null; then
  echo "${GREEN}✓${OFF} backend responded 200 and looks like TuckZone"
elif [ "$CODE" = "000" ]; then
  echo "${RED}✗${OFF} could not reach $API/config"
  echo "  On Render's free tier a sleeping service can take ~50s to wake — try again once."
  exit 1
else
  echo "${RED}✗${OFF} got HTTP $CODE from $API/config (expected 200)"
  echo "  Body: $(head -c 200 /tmp/tuckzone-config-check 2>/dev/null)"
  echo "  Check the URL, and that Render finished deploying."
  exit 1
fi
rm -f /tmp/tuckzone-config-check

# --- mobile/.env : used when running through Expo Go / a dev build -----------------------
touch mobile/.env
if grep -q '^EXPO_PUBLIC_API_BASE_URL=' mobile/.env; then
  sed -i "s|^EXPO_PUBLIC_API_BASE_URL=.*|EXPO_PUBLIC_API_BASE_URL=$API|" mobile/.env
else
  echo "EXPO_PUBLIC_API_BASE_URL=$API" >> mobile/.env
fi
echo "${GREEN}✓${OFF} mobile/.env updated"

# --- mobile/eas.json : compiled into preview and production builds -----------------------
python3 - "$API" <<'PY'
import json, sys
api = sys.argv[1]
path = 'mobile/eas.json'
with open(path) as fh:
    cfg = json.load(fh)
for profile in ('preview', 'production'):
    block = cfg.get('build', {}).get(profile)
    if block is None:
        continue
    block.setdefault('env', {})['EXPO_PUBLIC_API_BASE_URL'] = api
    print(f"  {profile}.env.EXPO_PUBLIC_API_BASE_URL = {api}")
with open(path, 'w') as fh:
    json.dump(cfg, fh, indent=2)
    fh.write('\n')
PY
echo "${GREEN}✓${OFF} mobile/eas.json updated"

echo
echo "${BOLD}Next:${OFF}"
echo "  Build the shareable APK   ${BOLD}cd mobile && npx eas-cli build --profile preview --platform android${OFF}"
echo "  Or run through Expo Go    ${BOLD}cd mobile && npx expo start${OFF}"
echo
echo "${YELLOW}Note${OFF} the APK compiles this URL in. Change the backend URL later and you must rebuild."
