#!/usr/bin/env bash
# =============================================================================
# seed.sh — Populate the database with landlords and rental listings
#
# Usage:  ./scripts/seed.sh
# Requires: backend running (./start.sh), jq installed (brew install jq)
# =============================================================================
set -euo pipefail

GATEWAY="http://localhost:8080"
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'

pass() { echo -e "${GREEN}✓ $1${NC}"; }
info() { echo -e "${YELLOW}→ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; exit 1; }

command -v jq >/dev/null 2>&1 || fail "jq is required: brew install jq"
curl -sf "$GATEWAY/actuator/health" >/dev/null 2>&1 || fail "Backend not reachable — run ./start.sh first"

echo ""
echo "============================================================"
echo " Seeding database with listings…"
echo "============================================================"

# ── Helper: register or login a landlord ──────────────────────────────────────
get_token() {
  local email="$1" password="$2" first="$3" last="$4"

  # Try register first; if already exists, fall back to login
  RESP=$(curl -sf -X POST "$GATEWAY/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"firstName\":\"$first\",\"lastName\":\"$last\",\"email\":\"$email\",\"password\":\"$password\",\"role\":\"LANDLORD\"}" 2>/dev/null || true)

  TOKEN=$(echo "$RESP" | jq -r '.data.token // empty' 2>/dev/null || true)

  if [ -z "$TOKEN" ]; then
    RESP=$(curl -sf -X POST "$GATEWAY/api/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"$email\",\"password\":\"$password\"}")
    TOKEN=$(echo "$RESP" | jq -r '.data.token')
  fi

  echo "$TOKEN"
}

# ── Helper: create listing ────────────────────────────────────────────────────
create_listing() {
  local TOKEN="$1"
  curl -sf -X POST "$GATEWAY/api/listings" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "$2" | jq -r '.data.title' 2>/dev/null || echo "(created)"
}

# picsum.photos gives free, consistent placeholder images by seed
img() { echo "https://picsum.photos/seed/$1/800/600"; }

# ── Landlord 1: Sarah Mitchell (Austin & Dallas) ──────────────────────────────
info "Registering Sarah Mitchell (Austin / Dallas)…"
T1=$(get_token "sarah.mitchell@seed.com" "Password123" "Sarah" "Mitchell")
pass "Token obtained"

info "Creating Austin listings…"

create_listing "$T1" "{
  \"title\": \"Sunny Studio in South Congress\",
  \"description\": \"Bright studio steps from SoCo's best cafes and bars. Hardwood floors, exposed brick, and great natural light. Walk to Whole Foods and Lady Bird Lake.\",
  \"address\": \"1204 S Congress Ave\", \"city\": \"Austin\", \"state\": \"TX\", \"zipCode\": \"78704\",
  \"pricePerMonth\": 1350, \"bedrooms\": 0, \"bathrooms\": 1, \"squareFeet\": 480,
  \"amenities\": [\"WiFi\", \"Laundry in building\", \"Bike storage\"],
  \"imageUrls\": [\"$(img apt1)\", \"$(img apt2)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T1" "{
  \"title\": \"Modern 1BR in East Austin\",
  \"description\": \"Renovated 1-bedroom in the heart of East Austin. Open kitchen, quartz countertops, private patio. Minutes to Mueller, UT, and downtown.\",
  \"address\": \"3301 E 5th St\", \"city\": \"Austin\", \"state\": \"TX\", \"zipCode\": \"78702\",
  \"pricePerMonth\": 1750, \"bedrooms\": 1, \"bathrooms\": 1, \"squareFeet\": 720,
  \"amenities\": [\"WiFi\", \"Parking\", \"Patio\", \"AC\"],
  \"imageUrls\": [\"$(img apt3)\", \"$(img apt4)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T1" "{
  \"title\": \"Spacious 2BR Near Domain\",
  \"description\": \"Large two-bedroom with in-unit washer/dryer, community pool, and gym. Close to Apple Campus, The Domain shopping, and Q2 Stadium.\",
  \"address\": \"11600 Rock Rose Ave\", \"city\": \"Austin\", \"state\": \"TX\", \"zipCode\": \"78758\",
  \"pricePerMonth\": 2400, \"bedrooms\": 2, \"bathrooms\": 2, \"squareFeet\": 1100,
  \"amenities\": [\"Pool\", \"Gym\", \"Parking\", \"In-unit laundry\", \"Pet friendly\"],
  \"imageUrls\": [\"$(img apt5)\", \"$(img apt6)\", \"$(img apt7)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T1" "{
  \"title\": \"Cozy Studio — UT Area\",
  \"description\": \"Affordable studio a 5-min walk from UT Austin main campus. Fully furnished option available. All utilities included.\",
  \"address\": \"2504 Guadalupe St\", \"city\": \"Austin\", \"state\": \"TX\", \"zipCode\": \"78705\",
  \"pricePerMonth\": 1050, \"bedrooms\": 0, \"bathrooms\": 1, \"squareFeet\": 380,
  \"amenities\": [\"WiFi\", \"Utilities included\", \"Furnished option\"],
  \"imageUrls\": [\"$(img apt8)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T1" "{
  \"title\": \"Luxury 3BR in Uptown Dallas\",
  \"description\": \"Stunning three-bedroom with skyline views, chef's kitchen, and concierge service. Steps from Klyde Warren Park and the Arts District.\",
  \"address\": \"2323 N Akard St\", \"city\": \"Dallas\", \"state\": \"TX\", \"zipCode\": \"75201\",
  \"pricePerMonth\": 4200, \"bedrooms\": 3, \"bathrooms\": 2, \"squareFeet\": 1850,
  \"amenities\": [\"Concierge\", \"Rooftop pool\", \"Valet parking\", \"Gym\", \"City views\"],
  \"imageUrls\": [\"$(img apt9)\", \"$(img apt10)\", \"$(img apt11)\"],
  \"status\": \"ACTIVE\"
}"

pass "Sarah's listings created"

# ── Landlord 2: James Okafor (New York & Chicago) ────────────────────────────
info "Registering James Okafor (New York / Chicago)…"
T2=$(get_token "james.okafor@seed.com" "Password123" "James" "Okafor")
pass "Token obtained"

create_listing "$T2" "{
  \"title\": \"Manhattan Studio — Financial District\",
  \"description\": \"Sleek studio in the heart of FiDi. Floor-to-ceiling windows with East River views. Doorman building, gym, and rooftop terrace.\",
  \"address\": \"75 Wall St\", \"city\": \"New York\", \"state\": \"NY\", \"zipCode\": \"10005\",
  \"pricePerMonth\": 3200, \"bedrooms\": 0, \"bathrooms\": 1, \"squareFeet\": 520,
  \"amenities\": [\"Doorman\", \"Gym\", \"Rooftop\", \"City views\", \"Laundry\"],
  \"imageUrls\": [\"$(img nyc1)\", \"$(img nyc2)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T2" "{
  \"title\": \"Brooklyn 1BR — Williamsburg\",
  \"description\": \"Bright one-bedroom in prime Williamsburg. Exposed brick, original hardwood floors, private deck. Steps from L train and the waterfront.\",
  \"address\": \"245 Bedford Ave\", \"city\": \"New York\", \"state\": \"NY\", \"zipCode\": \"11211\",
  \"pricePerMonth\": 2800, \"bedrooms\": 1, \"bathrooms\": 1, \"squareFeet\": 680,
  \"amenities\": [\"Private deck\", \"Laundry in building\", \"Bike storage\", \"Pet friendly\"],
  \"imageUrls\": [\"$(img nyc3)\", \"$(img nyc4)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T2" "{
  \"title\": \"Spacious 2BR — Upper West Side\",
  \"description\": \"Classic pre-war two-bedroom with high ceilings and hardwood floors. Steps from Central Park, multiple subway lines, and top-rated restaurants.\",
  \"address\": \"340 W 86th St\", \"city\": \"New York\", \"state\": \"NY\", \"zipCode\": \"10024\",
  \"pricePerMonth\": 4500, \"bedrooms\": 2, \"bathrooms\": 1, \"squareFeet\": 950,
  \"amenities\": [\"Laundry\", \"Storage unit\", \"Live-in super\"],
  \"imageUrls\": [\"$(img nyc5)\", \"$(img nyc6)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T2" "{
  \"title\": \"Chicago Studio — River North\",
  \"description\": \"Modern studio in buzzing River North. Floor-to-ceiling windows, smart home features, and a building rooftop with lake views.\",
  \"address\": \"400 N Lasalle Dr\", \"city\": \"Chicago\", \"state\": \"IL\", \"zipCode\": \"60654\",
  \"pricePerMonth\": 1800, \"bedrooms\": 0, \"bathrooms\": 1, \"squareFeet\": 560,
  \"amenities\": [\"Rooftop\", \"Gym\", \"Doorman\", \"Smart home\", \"Bike room\"],
  \"imageUrls\": [\"$(img chi1)\", \"$(img chi2)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T2" "{
  \"title\": \"Lincoln Park 2BR — Chicago\",
  \"description\": \"Charming two-bedroom in Lincoln Park, one of Chicago's most sought-after neighborhoods. Gourmet kitchen, private parking, and steps from the lakefront trail.\",
  \"address\": \"2148 N Halsted St\", \"city\": \"Chicago\", \"state\": \"IL\", \"zipCode\": \"60614\",
  \"pricePerMonth\": 2900, \"bedrooms\": 2, \"bathrooms\": 2, \"squareFeet\": 1050,
  \"amenities\": [\"Parking\", \"In-unit laundry\", \"Private deck\", \"Pet friendly\"],
  \"imageUrls\": [\"$(img chi3)\", \"$(img chi4)\"],
  \"status\": \"ACTIVE\"
}"

pass "James's listings created"

# ── Landlord 3: Priya Sharma (San Francisco, Miami, Seattle) ──────────────────
info "Registering Priya Sharma (SF / Miami / Seattle)…"
T3=$(get_token "priya.sharma@seed.com" "Password123" "Priya" "Sharma")
pass "Token obtained"

create_listing "$T3" "{
  \"title\": \"SoMa Loft — San Francisco\",
  \"description\": \"Industrial-chic loft in SoMa with polished concrete floors, 14-foot ceilings, and an open mezzanine bedroom. Walk to BART, Oracle Park, and dozens of restaurants.\",
  \"address\": \"888 Brannan St\", \"city\": \"San Francisco\", \"state\": \"CA\", \"zipCode\": \"94103\",
  \"pricePerMonth\": 3800, \"bedrooms\": 1, \"bathrooms\": 1, \"squareFeet\": 900,
  \"amenities\": [\"Parking\", \"Bike storage\", \"Concierge\", \"Gym\"],
  \"imageUrls\": [\"$(img sf1)\", \"$(img sf2)\", \"$(img sf3)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T3" "{
  \"title\": \"Mission District 1BR\",
  \"description\": \"Sunny one-bedroom in the vibrant Mission. Victorian charm meets modern updates — new kitchen, clawfoot tub, and a sunny backyard shared with one other unit.\",
  \"address\": \"3250 21st St\", \"city\": \"San Francisco\", \"state\": \"CA\", \"zipCode\": \"94110\",
  \"pricePerMonth\": 3100, \"bedrooms\": 1, \"bathrooms\": 1, \"squareFeet\": 750,
  \"amenities\": [\"Backyard\", \"Laundry\", \"Pet friendly\"],
  \"imageUrls\": [\"$(img sf4)\", \"$(img sf5)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T3" "{
  \"title\": \"Brickell Studio — Miami\",
  \"description\": \"High-rise studio with Biscayne Bay views in Miami's financial hub. Resort-style pool, gym, and sky lounge. Walking distance to Metromover.\",
  \"address\": \"1010 Brickell Ave\", \"city\": \"Miami\", \"state\": \"FL\", \"zipCode\": \"33131\",
  \"pricePerMonth\": 2200, \"bedrooms\": 0, \"bathrooms\": 1, \"squareFeet\": 510,
  \"amenities\": [\"Pool\", \"Gym\", \"Sky lounge\", \"Valet\", \"Bay views\"],
  \"imageUrls\": [\"$(img mia1)\", \"$(img mia2)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T3" "{
  \"title\": \"Wynwood 2BR Loft — Miami\",
  \"description\": \"Artsy two-bedroom loft in Wynwood, Miami's world-famous street art district. Open floor plan, private rooftop terrace, and two blocks from the Wynwood Walls.\",
  \"address\": \"255 NW 26th St\", \"city\": \"Miami\", \"state\": \"FL\", \"zipCode\": \"33127\",
  \"pricePerMonth\": 3400, \"bedrooms\": 2, \"bathrooms\": 2, \"squareFeet\": 1200,
  \"amenities\": [\"Rooftop terrace\", \"Parking\", \"In-unit laundry\", \"Pet friendly\"],
  \"imageUrls\": [\"$(img mia3)\", \"$(img mia4)\"],
  \"status\": \"ACTIVE\"
}"

create_listing "$T3" "{
  \"title\": \"Capitol Hill 1BR — Seattle\",
  \"description\": \"Stylish one-bedroom on Seattle's lively Capitol Hill. Updated kitchen, in-unit laundry, and a private Juliet balcony with city views. Walk to light rail.\",
  \"address\": \"1525 E Olive Way\", \"city\": \"Seattle\", \"state\": \"WA\", \"zipCode\": \"98122\",
  \"pricePerMonth\": 2100, \"bedrooms\": 1, \"bathrooms\": 1, \"squareFeet\": 680,
  \"amenities\": [\"In-unit laundry\", \"Balcony\", \"Bike storage\", \"Pet friendly\"],
  \"imageUrls\": [\"$(img sea1)\", \"$(img sea2)\"],
  \"status\": \"ACTIVE\"
}"

pass "Priya's listings created"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
echo -e "${GREEN} Seeding complete!${NC}"
echo "============================================================"
echo "  3 landlords created (password: Password123 for all)"
echo "  15 listings across Austin, Dallas, New York, Chicago,"
echo "     San Francisco, Miami, and Seattle"
echo ""
echo "  Browse them at: http://localhost:3000/listings"
echo "============================================================"
