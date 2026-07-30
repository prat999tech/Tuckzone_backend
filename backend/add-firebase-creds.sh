#!/usr/bin/env bash
# Wires a Firebase service-account JSON into backend/.env so the backend can SEND pushes.
#
#   ./add-firebase-creds.sh ~/Downloads/tuckzone-firebase-adminsdk-xxxxx.json
#
# This is the SERVER credential (Firebase console → Project settings → Service accounts).
# It is NOT google-services.json — that one is a file inside mobile/ and lets the APP
# RECEIVE. Two different files, two different jobs.

set -euo pipefail
cd "$(dirname "$0")"

GREEN=$'\033[32m'; RED=$'\033[31m'; YELLOW=$'\033[33m'; BOLD=$'\033[1m'; OFF=$'\033[0m'

SRC="${1:-}"
if [ -z "$SRC" ]; then
  echo "${RED}Usage:${OFF} ./add-firebase-creds.sh <path-to-service-account.json>"
  echo "Example: ./add-firebase-creds.sh ~/Downloads/tuckzone-firebase-adminsdk-a1b2c.json"
  exit 1
fi
if [ ! -f "$SRC" ]; then
  echo "${RED}✗${OFF} No such file: $SRC"
  exit 1
fi

# Validate it is genuinely a service-account key and not google-services.json, which is
# the single most common mix-up and produces a baffling runtime error otherwise.
if ! python3 - "$SRC" <<'PY'
import json, sys
try:
    d = json.load(open(sys.argv[1]))
except Exception as e:
    print(f"  not valid JSON: {e}"); sys.exit(1)
if d.get("type") != "service_account":
    if "project_info" in d or "client" in d:
        print("  This looks like google-services.json (the MOBILE file).")
        print("  You need: Firebase console -> Project settings -> Service accounts")
        print("            -> Generate new private key")
    else:
        print("  Missing \"type\": \"service_account\" — not a service-account key.")
    sys.exit(1)
for field in ("project_id", "private_key", "client_email"):
    if not d.get(field):
        print(f"  Missing required field: {field}"); sys.exit(1)
print(f"  project_id:   {d['project_id']}")
print(f"  client_email: {d['client_email']}")
PY
then
  echo "${RED}✗${OFF} That file is not a Firebase service-account key."
  exit 1
fi

DEST="firebase-service-account.json"
cp "$SRC" "$DEST"
chmod 600 "$DEST"   # readable only by you — it is a private key
echo "${GREEN}✓${OFF} copied to backend/$DEST (chmod 600, gitignored)"

touch .env
# Drop any previous Firebase settings so re-running does not stack duplicates.
grep -v -E '^(APP_PUSH_PROVIDER|GOOGLE_APPLICATION_CREDENTIALS|FIREBASE_CREDENTIALS_JSON)=' .env > .env.tmp || true
mv .env.tmp .env

{
  echo ""
  echo "# ── Firebase push (added by add-firebase-creds.sh) ──"
  echo "APP_PUSH_PROVIDER=firebase"
  # Point at the file rather than inlining the JSON: a private key containing newlines is
  # painful to quote correctly in a .env, and the SDK reads this variable natively.
  echo "GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/$DEST"
} >> .env

echo "${GREEN}✓${OFF} backend/.env updated:"
echo "     APP_PUSH_PROVIDER=firebase"
echo "     GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/$DEST"
echo
echo "${BOLD}Restart the backend to pick it up:${OFF}"
echo "     cd backend && set -a && source .env && set +a && ./mvnw spring-boot:run"
echo
echo "${YELLOW}Note${OFF} for deploying to Render/Railway (no filesystem for secrets):"
echo "     use FIREBASE_CREDENTIALS_JSON instead. Get the one-line value with:"
echo "     base64 -w0 backend/$DEST"
