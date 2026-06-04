#!/usr/bin/env bash
# =============================================================================
# test-flow.sh — End-to-end Stripe payment flow
#
# Prerequisites:
#   - docker compose up --build (all services healthy)
#   - jq installed  (brew install jq)
#   - STRIPE_SECRET_KEY set in your environment or .env
#
# Usage:
#   chmod +x scripts/test-flow.sh
#   STRIPE_SECRET_KEY=sk_test_xxx ./scripts/test-flow.sh
# =============================================================================

set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────
GATEWAY="http://localhost:8080"
STRIPE_API="https://api.stripe.com/v1"
STRIPE_KEY="${STRIPE_SECRET_KEY:-}"

# Colours
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

pass() { echo -e "${GREEN}✓ $1${NC}"; }
info() { echo -e "${YELLOW}→ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; exit 1; }

# ── Guards ────────────────────────────────────────────────────────────────────
command -v jq  >/dev/null 2>&1 || fail "jq is required: brew install jq"
command -v curl >/dev/null 2>&1 || fail "curl is required"
[ -n "$STRIPE_KEY" ] || fail "STRIPE_SECRET_KEY is not set"

echo ""
echo "============================================================"
echo " Rental Service Management — End-to-End Test"
echo "============================================================"

# ── 1. Health check ───────────────────────────────────────────────────────────
info "Checking services are up..."
curl -sf "$GATEWAY/actuator/health" >/dev/null 2>&1 || \
  fail "Gateway not reachable at $GATEWAY — run: docker compose up --build"
pass "Gateway is healthy"

# ── 2. Register a landlord ────────────────────────────────────────────────────
info "Registering landlord..."
LANDLORD_RESP=$(curl -sf -X POST "$GATEWAY/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName":  "Landlord",
    "email":     "landlord@test.com",
    "password":  "password123",
    "role":      "LANDLORD"
  }')

LANDLORD_TOKEN=$(echo "$LANDLORD_RESP" | jq -r '.data.token')
LANDLORD_ID=$(echo "$LANDLORD_RESP"   | jq -r '.data.userId')
[ "$LANDLORD_TOKEN" != "null" ] || fail "Landlord registration failed: $LANDLORD_RESP"
pass "Landlord registered  (id=$LANDLORD_ID)"

# ── 3. Create a rental listing ────────────────────────────────────────────────
info "Creating rental listing..."
LISTING_RESP=$(curl -sf -X POST "$GATEWAY/api/listings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $LANDLORD_TOKEN" \
  -d '{
    "title":          "Modern Studio in Austin",
    "description":    "Great natural light, walkable neighbourhood.",
    "address":        "100 Congress Ave",
    "city":           "Austin",
    "state":          "TX",
    "zipCode":        "78701",
    "pricePerMonth":  1500,
    "bedrooms":       1,
    "bathrooms":      1,
    "squareFeet":     650,
    "amenities":      ["WiFi", "Gym", "Parking"],
    "status":         "ACTIVE"
  }')

LISTING_ID=$(echo "$LISTING_RESP" | jq -r '.data.id')
[ "$LISTING_ID" != "null" ] || fail "Listing creation failed: $LISTING_RESP"
pass "Listing created  (id=$LISTING_ID)"

# ── 4. Register a tenant ──────────────────────────────────────────────────────
info "Registering tenant..."
TENANT_RESP=$(curl -sf -X POST "$GATEWAY/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Bob",
    "lastName":  "Tenant",
    "email":     "tenant@test.com",
    "password":  "password123",
    "role":      "TENANT"
  }')

TENANT_TOKEN=$(echo "$TENANT_RESP" | jq -r '.data.token')
TENANT_ID=$(echo "$TENANT_RESP"   | jq -r '.data.userId')
[ "$TENANT_TOKEN" != "null" ] || fail "Tenant registration failed: $TENANT_RESP"
pass "Tenant registered  (id=$TENANT_ID)"

# ── 5. Initiate a Stripe payment ──────────────────────────────────────────────
info "Initiating Stripe payment (\$1500.00)..."
PAYMENT_RESP=$(curl -sf -X POST "$GATEWAY/api/payments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -d "{
    \"listingId\":   \"$LISTING_ID\",
    \"landlordId\":  \"$LANDLORD_ID\",
    \"amount\":      1500.00,
    \"currency\":    \"USD\",
    \"provider\":    \"STRIPE\",
    \"description\": \"Rent for Modern Studio in Austin — January 2025\"
  }")

PAYMENT_ID=$(echo "$PAYMENT_RESP"       | jq -r '.data.id')
PAYMENT_INTENT_ID=$(echo "$PAYMENT_RESP" | jq -r '.data.providerPaymentId')
CLIENT_SECRET=$(echo "$PAYMENT_RESP"    | jq -r '.data.clientSecret')
PAYMENT_STATUS=$(echo "$PAYMENT_RESP"   | jq -r '.data.status')

[ "$PAYMENT_ID" != "null" ]       || fail "Payment initiation failed: $PAYMENT_RESP"
[ "$PAYMENT_INTENT_ID" != "null" ] || fail "No Stripe PaymentIntent ID in response"
[ "$CLIENT_SECRET" != "null" ]    || fail "No clientSecret in response"

pass "Stripe PaymentIntent created"
echo "   Payment ID:          $PAYMENT_ID"
echo "   PaymentIntent ID:    $PAYMENT_INTENT_ID"
echo "   Status:              $PAYMENT_STATUS"
echo "   Client Secret:       ${CLIENT_SECRET:0:30}..."

# ── 6. Confirm the PaymentIntent via Stripe API (test card) ───────────────────
info "Creating Stripe test PaymentMethod (card 4242...)..."
PM_RESP=$(curl -sf -X POST "$STRIPE_API/payment_methods" \
  -u "$STRIPE_KEY:" \
  -d "type=card" \
  -d "card[number]=4242424242424242" \
  -d "card[exp_month]=12" \
  -d "card[exp_year]=2026" \
  -d "card[cvc]=123")

PM_ID=$(echo "$PM_RESP" | jq -r '.id')
[ "$PM_ID" != "null" ] || fail "Failed to create Stripe test PaymentMethod: $PM_RESP"
pass "Test PaymentMethod created  (id=$PM_ID)"

info "Confirming PaymentIntent with test card..."
CONFIRM_RESP=$(curl -sf -X POST "$STRIPE_API/payment_intents/$PAYMENT_INTENT_ID/confirm" \
  -u "$STRIPE_KEY:" \
  -d "payment_method=$PM_ID" \
  -d "return_url=http://localhost:8080/return")

STRIPE_STATUS=$(echo "$CONFIRM_RESP" | jq -r '.status')
echo "   Stripe status: $STRIPE_STATUS"

if [ "$STRIPE_STATUS" = "succeeded" ]; then
  pass "PaymentIntent confirmed successfully"
elif [ "$STRIPE_STATUS" = "requires_action" ]; then
  # Some test cards trigger 3DS — use the bypass card (4242...) to avoid this
  fail "PaymentIntent requires additional action — check Stripe dashboard"
else
  fail "Unexpected Stripe status: $STRIPE_STATUS"
fi

# ── 7. Sync status back to our DB ─────────────────────────────────────────────
info "Syncing Stripe status to our DB..."
SYNC_RESP=$(curl -sf -X POST "$GATEWAY/api/payments/$PAYMENT_ID/sync-status" \
  -H "Authorization: Bearer $TENANT_TOKEN")

FINAL_STATUS=$(echo "$SYNC_RESP" | jq -r '.data.status')
[ "$FINAL_STATUS" = "COMPLETED" ] || fail "Expected COMPLETED, got: $FINAL_STATUS"
pass "DB status synced  →  $FINAL_STATUS"

# ── 8. Verify payment record is readable ──────────────────────────────────────
info "Fetching payment by ID..."
GET_RESP=$(curl -sf "$GATEWAY/api/payments/$PAYMENT_ID" \
  -H "Authorization: Bearer $TENANT_TOKEN")

VERIFIED_STATUS=$(echo "$GET_RESP" | jq -r '.data.status')
VERIFIED_AMOUNT=$(echo "$GET_RESP" | jq -r '.data.amount')
[ "$VERIFIED_STATUS" = "COMPLETED" ] || fail "Verification failed — status: $VERIFIED_STATUS"
pass "Payment verified  (status=$VERIFIED_STATUS, amount=\$$VERIFIED_AMOUNT)"

# ── 9. Check tenant's payment history ────────────────────────────────────────
info "Checking tenant payment history..."
HISTORY_RESP=$(curl -sf "$GATEWAY/api/payments/my-payments" \
  -H "Authorization: Bearer $TENANT_TOKEN")

COUNT=$(echo "$HISTORY_RESP" | jq '.data | length')
[ "$COUNT" -ge 1 ] || fail "Expected at least 1 payment in history, got $COUNT"
pass "Tenant payment history: $COUNT payment(s) found"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
echo -e "${GREEN} All checks passed!${NC}"
echo "============================================================"
echo "  Landlord token:  ${LANDLORD_TOKEN:0:40}..."
echo "  Tenant token:    ${TENANT_TOKEN:0:40}..."
echo "  Listing ID:      $LISTING_ID"
echo "  Payment ID:      $PAYMENT_ID"
echo "  Final status:    $FINAL_STATUS"
echo ""
echo "  Stripe dashboard: https://dashboard.stripe.com/test/payments"
echo "============================================================"
