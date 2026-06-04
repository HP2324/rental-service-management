#!/usr/bin/env bash
# =============================================================================
# start.sh — Start the full rental-service-management stack
#
# Usage:  ./start.sh
# Stop:   ./stop.sh
# =============================================================================
set -euo pipefail

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; BOLD='\033[1m'; NC='\033[0m'

pass() { echo -e "${GREEN}✓ $1${NC}"; }
info() { echo -e "${YELLOW}→ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; exit 1; }
header() { echo -e "\n${BOLD}$1${NC}"; }

ROOT="$(cd "$(dirname "$0")" && pwd)"

echo ""
echo -e "${BOLD}============================================================${NC}"
echo -e "${BOLD}  Rental Service Management — Startup${NC}"
echo -e "${BOLD}============================================================${NC}"

# ── 1. Check prerequisites ─────────────────────────────────────────────────────
header "Checking prerequisites…"

command -v docker >/dev/null 2>&1 || fail "Docker not found. Install Docker Desktop: https://www.docker.com/products/docker-desktop"
docker info >/dev/null 2>&1      || fail "Docker Desktop is not running. Please start it and try again."
pass "Docker is running"

command -v node >/dev/null 2>&1  || fail "Node.js not found. Install it: https://nodejs.org (v18 or later)"
NODE_VER=$(node -e "process.stdout.write(process.versions.node)")
NODE_MAJOR=$(echo "$NODE_VER" | cut -d. -f1)
[ "$NODE_MAJOR" -ge 18 ]         || fail "Node.js v18+ required (you have v$NODE_VER). Update at https://nodejs.org"
pass "Node.js v$NODE_VER"

command -v npm >/dev/null 2>&1   || fail "npm not found (should come with Node.js)"
pass "npm $(npm --version)"

# ── 2. Set up backend .env ─────────────────────────────────────────────────────
header "Setting up environment…"

if [ ! -f "$ROOT/.env" ]; then
  cp "$ROOT/.env.example" "$ROOT/.env"
  info "Created .env from .env.example"
  echo ""
  echo -e "  ${YELLOW}Optional: add your Stripe/PayPal test keys to .env before paying.${NC}"
  echo -e "  The app works fine without them — just skip the payment flow for now."
  echo ""
else
  pass "Backend .env exists"
fi

# ── 3. Set up frontend .env ────────────────────────────────────────────────────
if [ ! -f "$ROOT/frontend/.env" ]; then
  cp "$ROOT/frontend/.env.example" "$ROOT/frontend/.env"
  info "Created frontend/.env from .env.example"
else
  pass "Frontend .env exists"
fi

# ── 4. Install frontend dependencies ──────────────────────────────────────────
header "Installing frontend dependencies…"

if [ ! -d "$ROOT/frontend/node_modules" ]; then
  info "Running npm install (first time — takes ~30s)…"
  cd "$ROOT/frontend" && npm install --silent
  pass "Dependencies installed"
else
  pass "node_modules already present"
fi

# ── 5. Start backend (docker compose) ─────────────────────────────────────────
header "Starting backend services…"
cd "$ROOT"

# Pull fresh images only on first run (speeds up subsequent starts)
if ! docker compose ps --quiet 2>/dev/null | grep -q .; then
  info "Building Docker images (first run takes 3–5 min)…"
  docker compose up -d --build
else
  info "Restarting existing containers…"
  docker compose up -d
fi

pass "Backend containers started"
echo ""
echo "  Services:"
echo "    API Gateway    →  http://localhost:8080"
echo "    User Service   →  http://localhost:8081"
echo "    Listing Service→  http://localhost:8082"
echo "    Payment Service→  http://localhost:8083"
echo "    Mongo Express  →  http://localhost:8084  (DB browser)"

# ── 6. Wait for gateway to be ready ───────────────────────────────────────────
header "Waiting for backend to be ready…"
info "This takes ~60s on first boot (JVM startup + MongoDB init)"

ATTEMPTS=0
MAX=30
until curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [ $ATTEMPTS -ge $MAX ]; then
    echo ""
    echo -e "${YELLOW}Gateway health check timed out after ${MAX}×5s.${NC}"
    echo "  The services might still be starting. Check with:"
    echo "    docker compose logs -f"
    break
  fi
  printf "."
  sleep 5
done

if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
  echo ""
  pass "Backend is healthy"
fi

# ── 7. Start frontend dev server ───────────────────────────────────────────────
header "Starting frontend…"
cd "$ROOT/frontend"

echo ""
echo -e "${GREEN}${BOLD}============================================================${NC}"
echo -e "${GREEN}${BOLD}  All services running!${NC}"
echo -e "${GREEN}${BOLD}============================================================${NC}"
echo ""
echo "  Frontend  →  http://localhost:3000   (opening now)"
echo "  Backend   →  http://localhost:8080"
echo "  DB UI     →  http://localhost:8084"
echo ""
echo "  Press Ctrl+C to stop the frontend."
echo "  Run ./stop.sh to stop the backend containers."
echo ""

# Open browser automatically (macOS)
sleep 2 && open http://localhost:3000 &

npm run dev
