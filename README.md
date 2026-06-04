# Rental Service Management

A production-grade rental property platform built with a Java microservices backend and a React frontend. Landlords list properties and manage their portfolio; tenants browse, search, and pay for rentals through Stripe or PayPal.

---

## Features

- JWT authentication with two distinct roles — Landlord and Tenant
- Role-based access control enforced at both the API gateway and service level
- Rental listing management with image uploads, city/price/bedroom search filters
- Stripe and PayPal payment integration with sandbox support
- Automatic listing deactivation after a successful payment
- MongoDB with compound indexes for optimised query performance
- Containerised with Docker Compose — single command to run the full stack

---

## Technologies Used

**Backend**
- Java 17 · Spring Boot 3 · Spring Cloud Gateway
- Spring Security (JWT · RBAC)
- MongoDB (database-per-service pattern)
- Stripe Java SDK · PayPal REST API
- Docker · Docker Compose

**Frontend**
- React 18 · Vite · React Router v6
- Tailwind CSS · shadcn/ui component library
- Stripe.js / Stripe Elements
- Axios

---

## Project Structure

```
rental-service-management/
├── api-gateway/               Spring Cloud Gateway — JWT validation & routing
│   └── src/main/java/com/rental/gateway/
│       └── filter/JwtGlobalFilter.java
├── user-service/              Auth, registration, user management
│   └── src/main/java/com/rental/user/
│       ├── controller/        AuthController, UserController
│       ├── service/           UserService, JwtService
│       └── model/             User, Role
├── listing-service/           Listings, search, image uploads
│   └── src/main/java/com/rental/listing/
│       ├── controller/        ListingController, FileUploadController
│       └── service/           ListingService
├── payment-service/           Stripe & PayPal payment processing
│   └── src/main/java/com/rental/payment/
│       ├── controller/        PaymentController
│       └── service/           PaymentService, StripePaymentService, PayPalPaymentService
├── shared/                    Common DTOs and exceptions
├── frontend/                  React + Vite application
│   └── src/
│       ├── api/               Typed API client functions
│       ├── components/        Shared UI (shadcn/ui primitives, Navbar)
│       ├── context/           AuthContext (JWT session management)
│       └── pages/             Login, Register, Listings, Dashboard, Payment
├── scripts/
│   ├── seed.sh                Populate DB with 15 sample listings across 7 cities
│   └── test-flow.sh           End-to-end Stripe payment test via curl
├── start.sh                   One-command startup script
├── stop.sh                    Stop all containers
└── docker-compose.yml         Full local stack definition
```

---

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Docker Desktop | 24+ | [docker.com](https://www.docker.com/products/docker-desktop/) |
| Node.js | 18+ | [nodejs.org](https://nodejs.org) |
| jq | any | `brew install jq` (only for seed and test scripts) |

---

## Installation & Setup

**1. Clone the repository**

```bash
git clone <your-repo-url>
cd rental-service-management
```

**2. Configure environment**

```bash
cp .env.example .env
cp frontend/.env.example frontend/.env
```

Open `frontend/.env` and fill in your Stripe publishable key:

```
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_your_key_here
```

**3. Start the full stack**

```bash
./start.sh
```

On first run, Docker builds all five images — this takes 3–5 minutes. You'll see:

```
✓ Docker is running
✓ Node.js v24.x.x
✓ Backend containers started
→ Waiting for backend to be ready…
✓ Backend is healthy
✓ All services running!

  Frontend  →  http://localhost:3000
  Backend   →  http://localhost:8080
  DB UI     →  http://localhost:8084
```

The browser opens automatically at `http://localhost:3000`.

**4. Seed sample data** (optional but recommended)

```bash
./scripts/seed.sh
```

Creates 3 landlord accounts and 15 listings across Austin, Dallas, New York, Chicago, San Francisco, Miami, and Seattle.

```
✓ Sarah's listings created
✓ James's listings created
✓ Priya's listings created

  15 listings across 7 cities
  Browse them at: http://localhost:3000/listings
```

**5. Stop**

```bash
./stop.sh
```

MongoDB data is preserved in a named Docker volume between restarts.

---

## Running with Docker only (no start.sh)

```bash
docker compose up --build       # start all services
docker compose down             # stop and remove containers
docker compose logs -f          # tail all service logs
docker compose logs listing-service --tail=50  # single service
```

---

## Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | React dev server (Vite) |
| API Gateway | 8080 | Single entry point — JWT validation and routing |
| User Service | 8081 | Registration, login, user management |
| Listing Service | 8082 | Listings CRUD, search, image uploads |
| Payment Service | 8083 | Stripe & PayPal payment processing |
| Mongo Express | 8084 | Database browser (dev only) |

All API traffic goes through the gateway at port 8080. The frontend proxies `/api/*` requests to the gateway via Vite's dev server proxy.

---

## API Endpoints

### Auth

#### `POST /api/auth/register`

**Request**
```json
{
  "firstName": "Alice",
  "lastName": "Smith",
  "email": "alice@example.com",
  "password": "password123",
  "role": "TENANT"
}
```
Role must be one of `TENANT` or `LANDLORD`.

**Response**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": "6657a1b2c3d4e5f6a7b8c9d0",
    "email": "alice@example.com",
    "role": "TENANT"
  }
}
```

---

#### `POST /api/auth/login`

**Request**
```json
{
  "email": "alice@example.com",
  "password": "password123"
}
```

**Response**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": "6657a1b2c3d4e5f6a7b8c9d0",
    "email": "alice@example.com",
    "role": "TENANT"
  }
}
```

---

### Listings

#### `GET /api/listings`

Returns all active listings. No authentication required.

**Response**
```json
{
  "success": true,
  "data": [
    {
      "id": "6657b1c2d3e4f5a6b7c8d9e0",
      "landlordId": "6657a1b2c3d4e5f6a7b8c9d0",
      "title": "Modern 1BR in East Austin",
      "city": "Austin",
      "state": "TX",
      "pricePerMonth": 1750,
      "bedrooms": 1,
      "bathrooms": 1,
      "status": "ACTIVE",
      "imageUrls": ["/uploads/abc123.jpg"]
    }
  ]
}
```

---

#### `GET /api/listings/search?city=Austin&maxPrice=2000&minBedrooms=1`

Returns active listings matching the filters. No authentication required.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `city` | string | yes | Case-insensitive city name |
| `maxPrice` | number | no | Maximum monthly rent |
| `minBedrooms` | number | no | Minimum bedroom count (default: 0) |

---

#### `POST /api/listings`

Creates a new listing. Requires `LANDLORD` role.

**Request**
```json
{
  "title": "Sunny Studio in South Congress",
  "address": "1204 S Congress Ave",
  "city": "Austin",
  "state": "TX",
  "zipCode": "78704",
  "pricePerMonth": 1350,
  "bedrooms": 0,
  "bathrooms": 1,
  "squareFeet": 480,
  "amenities": ["WiFi", "Laundry"],
  "imageUrls": ["/uploads/abc123.jpg"],
  "status": "ACTIVE"
}
```

---

#### `PATCH /api/listings/{id}/status`

Updates the status of a listing. Role-based rules apply:

| Role | Allowed values | Restriction |
|------|---------------|-------------|
| TENANT | `INACTIVE` only | After paying rent |
| LANDLORD | `ACTIVE`, `INACTIVE` | Own listings only |

**Request**
```json
{ "status": "INACTIVE" }
```

---

#### `POST /api/listings/upload`

Uploads a photo for a listing. Requires `LANDLORD` role. Returns the URL path to use in `imageUrls`.

**Request:** `multipart/form-data` with field `file` (JPEG, PNG, WebP — max 10 MB)

**Response**
```json
{
  "success": true,
  "data": "/uploads/f47ac10b-58cc-4372-a567-0e02b2c3d479.jpg"
}
```

---

### Payments

#### `POST /api/payments`

Initiates a payment via Stripe or PayPal. Requires `TENANT` role.

**Request**
```json
{
  "listingId": "6657b1c2d3e4f5a6b7c8d9e0",
  "landlordId": "6657a1b2c3d4e5f6a7b8c9d0",
  "amount": 1750.00,
  "currency": "USD",
  "provider": "STRIPE",
  "description": "Rent for Modern 1BR in East Austin"
}
```

**Response (Stripe)**
```json
{
  "success": true,
  "data": {
    "id": "6657c1d2e3f4a5b6c7d8e9f0",
    "status": "PROCESSING",
    "provider": "STRIPE",
    "providerPaymentId": "pi_3Ox4AbLkdIwHu7ix1234",
    "clientSecret": "pi_3Ox4AbLkdIwHu7ix1234_secret_abc",
    "amount": 1750.00,
    "currency": "USD"
  }
}
```

Use `clientSecret` with Stripe.js to confirm the payment on the frontend.

**Response (PayPal)**
```json
{
  "success": true,
  "data": {
    "id": "6657c1d2e3f4a5b6c7d8e9f0",
    "status": "PROCESSING",
    "provider": "PAYPAL",
    "providerPaymentId": "5O190127TN364715T",
    "approvalUrl": "https://www.sandbox.paypal.com/checkoutnow?token=5O190127TN364715T"
  }
}
```

Redirect the user to `approvalUrl` to complete the PayPal flow.

---

#### `POST /api/payments/{id}/sync-status`

Fetches the latest status from Stripe and syncs it to the database. Call this after confirming a PaymentIntent via Stripe.js.

**Response**
```json
{
  "success": true,
  "message": "Status synced",
  "data": {
    "id": "6657c1d2e3f4a5b6c7d8e9f0",
    "status": "COMPLETED",
    "amount": 1750.00,
    "provider": "STRIPE"
  }
}
```

---

## Payment Flow (Stripe)

1. Tenant clicks **Rent this property**
2. Frontend calls `POST /api/payments` → backend creates a Stripe PaymentIntent, returns `clientSecret`
3. Stripe Elements collects card details and calls `stripe.confirmCardPayment(clientSecret)`
4. On success, frontend calls `POST /api/payments/{id}/sync-status` → backend confirms status with Stripe and updates the database to `COMPLETED`
5. Frontend calls `PATCH /api/listings/{id}/status` with `INACTIVE` → listing is removed from browse
6. Landlord reopens the listing via the Edit form by setting status back to `ACTIVE`

Test card: `4242 4242 4242 4242` · any future date · any 3-digit CVC

---

## User Roles

| Role | Capabilities |
|------|-------------|
| **Tenant** | Browse listings, search by city/price/bedrooms, make payments, view payment history |
| **Landlord** | Create, edit, and delete own listings, upload listing photos, view received payments |

---

## Environment Variables

### Backend (`.env`)

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | HS256 signing key — must be at least 32 characters |
| `JWT_EXPIRATION` | Token lifetime in milliseconds (default: `86400000` = 24 hours) |
| `STRIPE_SECRET_KEY` | Stripe secret key (`sk_test_...`) |
| `PAYPAL_CLIENT_ID` | PayPal sandbox client ID |
| `PAYPAL_CLIENT_SECRET` | PayPal sandbox client secret |
| `PAYPAL_BASE_URL` | PayPal API base URL (default: `https://api-m.sandbox.paypal.com`) |

### Frontend (`frontend/.env`)

| Variable | Description |
|----------|-------------|
| `VITE_STRIPE_PUBLISHABLE_KEY` | Stripe publishable key (`pk_test_...`) |

---

## Running Tests

```bash
mvn test -pl shared,user-service,listing-service \
  -Dtest="UserServiceTest,ListingServiceTest"
```

26 unit tests covering registration, login, profile updates, listing CRUD, ownership enforcement, role-based access, and search behaviour.

---

## End-to-End Test Script

Tests the complete payment flow using the real Stripe sandbox API:

```bash
STRIPE_SECRET_KEY=sk_test_xxx ./scripts/test-flow.sh
```

Registers a landlord and tenant, creates a listing, initiates a Stripe PaymentIntent, confirms it with a test card via the Stripe API, syncs the status, and verifies the final payment record — 9 assertions in total.
