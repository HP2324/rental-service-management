#!/usr/bin/env bash
# =============================================================================
# stop.sh — Stop all backend containers (frontend stops with Ctrl+C)
# =============================================================================
set -euo pipefail

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo -e "${YELLOW}→ Stopping backend containers…${NC}"
docker compose down

echo -e "${GREEN}✓ All containers stopped.${NC}"
echo "  MongoDB data is preserved in the 'mongo-data' Docker volume."
echo "  Run ./start.sh to start again."
