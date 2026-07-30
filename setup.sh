#!/usr/bin/env bash
# First-time setup for TuckZone. Safe to re-run — it never overwrites an existing .env.
#
#   chmod +x setup.sh && ./setup.sh
#
# Linux/macOS. On Windows use WSL or Git Bash.

set -euo pipefail
cd "$(dirname "$0")"

BOLD=$'\033[1m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; RED=$'\033[31m'; OFF=$'\033[0m'
ok()   { echo "  ${GREEN}✓${OFF} $1"; }
warn() { echo "  ${YELLOW}!${OFF} $1"; }
fail() { echo "  ${RED}✗${OFF} $1"; }

echo
echo "${BOLD}TuckZone — first-time setup${OFF}"
echo

# ─── 1. Prerequisites ────────────────────────────────────────────────────────
echo "${BOLD}1. Checking prerequisites${OFF}"
missing=0

if command -v docker >/dev/null 2>&1; then
  ok "docker $(docker --version | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)"
else
  fail "docker not found — install Docker Desktop: https://docker.com/products/docker-desktop"; missing=1
fi

if command -v java >/dev/null 2>&1; then
  java_major=$(java -version 2>&1 | head -1 | grep -oE '"[0-9]+' | tr -d '"')
  if [ "${java_major:-0}" -ge 21 ]; then
    ok "java $java_major"
  else
    fail "java $java_major found, but 21+ is required — https://adoptium.net"; missing=1
  fi
else
  fail "java not found — install JDK 21+: https://adoptium.net"; missing=1
fi

if command -v node >/dev/null 2>&1; then
  node_major=$(node -v | grep -oE '[0-9]+' | head -1)
  if [ "${node_major:-0}" -ge 18 ]; then
    ok "node $(node -v)"
  else
    fail "node $(node -v) found, but 18+ is required — https://nodejs.org"; missing=1
  fi
else
  fail "node not found — install Node 18+: https://nodejs.org"; missing=1
fi

if [ "$missing" -ne 0 ]; then
  echo
  echo "${RED}Install the missing tools above, then re-run ./setup.sh${OFF}"
  exit 1
fi

# ─── 2. Environment files ────────────────────────────────────────────────────
echo
echo "${BOLD}2. Creating environment files${OFF}"

if [ -f backend/.env ]; then
  warn "backend/.env already exists — leaving it alone"
else
  cp backend/.env.example backend/.env
  ok "backend/.env created (all channels in safe 'log' mode)"
fi

# Find this machine's LAN address so a physical phone can reach the backend.
# 'localhost' on a phone means the phone itself, which is why this matters.
detect_lan_ip() {
  if command -v ip >/dev/null 2>&1; then
    ip -4 route get 1.1.1.1 2>/dev/null | grep -oE 'src [0-9.]+' | awk '{print $2}' | head -1
  elif command -v ipconfig >/dev/null 2>&1; then
    ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null
  fi
}
LAN_IP="$(detect_lan_ip || true)"

if [ -f mobile/.env ]; then
  warn "mobile/.env already exists — leaving it alone"
else
  cp mobile/.env.example mobile/.env
  if [ -n "${LAN_IP:-}" ]; then
    sed -i.bak "s|^EXPO_PUBLIC_API_BASE_URL=.*|EXPO_PUBLIC_API_BASE_URL=http://${LAN_IP}:8080/api|" mobile/.env
    rm -f mobile/.env.bak
    ok "mobile/.env created, pointing at http://${LAN_IP}:8080/api"
  else
    warn "mobile/.env created, but the LAN IP could not be detected — edit it by hand"
  fi
fi

# ─── 3. Database ─────────────────────────────────────────────────────────────
echo
echo "${BOLD}3. Starting PostgreSQL${OFF}"
if ! docker info >/dev/null 2>&1; then
  fail "Docker is installed but not running — start Docker Desktop, then re-run"
  exit 1
fi

# Errors are captured and shown rather than swallowed: a silent failure here would leave
# the next step failing for a reason nobody can see.
if ! compose_output=$(docker compose -f docker-compose.dev.yml up -d 2>&1); then
  fail "Could not start the database:"
  echo "$compose_output" | sed 's/^/      /'
  case "$compose_output" in
    *"already in use"*)
      echo
      echo "      A container named canteen-dev-db already exists (another copy of this"
      echo "      project, perhaps). Remove it and re-run:"
      echo "        ${BOLD}docker rm -f canteen-dev-db${OFF}" ;;
    *"port is already allocated"*|*"address already in use"*)
      echo
      echo "      Port 5434 is taken. Stop whatever is using it, or change the port in"
      echo "      docker-compose.dev.yml and in backend/.env (SPRING_DATASOURCE_URL)." ;;
  esac
  exit 1
fi

printf "  waiting for the database"
db_ready=0
for _ in $(seq 1 30); do
  if [ "$(docker inspect -f '{{.State.Health.Status}}' canteen-dev-db 2>/dev/null)" = "healthy" ]; then
    db_ready=1; break
  fi
  printf "."; sleep 2
done
echo
if [ "$db_ready" -eq 1 ]; then
  ok "database healthy on port 5434"
else
  fail "The database did not become healthy in 60s. Check: docker logs canteen-dev-db"
  exit 1
fi

# ─── 4. Mobile dependencies ──────────────────────────────────────────────────
echo
echo "${BOLD}4. Installing mobile app dependencies${OFF} (this takes a minute)"
if (cd mobile && npm install --no-fund --no-audit >/dev/null 2>&1); then
  ok "mobile dependencies installed"
else
  fail "npm install failed. Run it directly to see why:  cd mobile && npm install"
  exit 1
fi

# ─── Done ────────────────────────────────────────────────────────────────────
cat <<EOF

${BOLD}${GREEN}Setup complete.${OFF} Now open two terminals:

  ${BOLD}Terminal 1 — backend${OFF}
    cd backend
    set -a && source .env && set +a
    ./mvnw spring-boot:run

  ${BOLD}Terminal 2 — mobile app${OFF}
    cd mobile
    npx expo start

Then scan the QR code with the ${BOLD}Expo Go${OFF} app on your phone.
Your phone must be on the ${BOLD}same wifi${OFF} as this computer.

Full details, test accounts and troubleshooting: ${BOLD}RUNNING_LOCALLY.md${OFF}
EOF
