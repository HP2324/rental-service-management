# Rental Service Management

A production-grade rental property platform built with a Java microservices backend and a React frontend. Tenants can browse and pay for rentals via Stripe; landlords manage their listings through a dedicated dashboard.

---

## Tech Stack

**Backend**
- Java 17 · Spring Boot 3 · Spring Cloud Gateway
- Spring Security (JWT · RBAC — Admin / Landlord / Tenant)
- MongoDB (database-per-service)
- Stripe & PayPal payment APIs
- Docker · Docker Compose

**Frontend**
- React 18 · Vite · React Router v6
- Tailwind CSS · shadcn/ui
- Stripe.js / Stripe Elements
- Axios

---

## Architecture

```
Browser (React · :3000)
        │
        ▼
  API Gateway (:8080)          ← JWT validation, routing
  ┌──────┬──────────┬────────┐
  │      │          │        │
User   Listing  Payment   /uploads
Service Service  Service   (static)
(:8081) (:8082)  (:8083)
  │       │          │
  └───────┴──────────┘
          │
       MongoDB
   (3 separate DBs)
```

All traffic enters through the API Gateway. Each microservice validates JWTs independently and enforces its own role rules via Spring Security method-level annotations.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Docker Desktop | 24+ |
| Node.js | 18+ |
| jq (optional, for test scripts) | any |

---

## Quick Start

```bash
# 1. Clone
git clone https://github.com/your-username/rental-service-management.git
cd rental-service-management

# 2. Configure environment
cp .env.example .env
cp frontend/.env.example frontend/.env
# Edit .env and set STRIPE_SECRET_KEY=sk_test_...
# Edit frontend/.env and set VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...

# 3. Start everything (builds Docker images on first run — ~3-5 min)
./start.sh
```

The script builds and starts all services, waits for them to be healthy, installs frontend dependencies, and opens `http://localhost:3000`.

```bash
# Seed the database with 15 sample listings across 7 cities
./scripts/seed.sh

# Stop all backend containers
./stop.sh
```

---

## Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| Frontend | 3000 | React dev server |
| API Gateway | 8080 | Single entry point for all API calls |
| User Service | 8081 | Auth, registration, user management |
| Listing Service | 8082 | Rental listings, search, image upload |
| Payment Service | 8083 | Stripe & PayPal payment processing |
| Mongo Express | 8084 | Database browser (dev only) |

---

## API Endpoints

### Auth
| Method | Path | Access |
|--------|------|--------|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |

### Users
| Method | Path | Access |
|--------|------|--------|
| GET | `/api/users/me` | Authenticated |
| PUT | `/api/users/me` | Authenticated |
| GET | `/api/users` | Admin |
| GET | `/api/users/{id}` | Admin |
| DELETE | `/api/users/{id}` | Admin |

### Listings
| Method | Path | Access |
|--------|------|--------|
| GET | `/api/listings` | Public (active only) |
| GET | `/api/listings/search` | Public |
| GET | `/api/listings/{id}` | Public |
| POST | `/api/listings` | Landlord |
| PUT | `/api/listings/{id}` | Landlord (owner) / Admin |
| PATCH | `/api/listings/{id}/status` | Authenticated |
| DELETE | `/api/listings/{id}` | Landlord (owner) / Admin |
| GET | `/api/listings/my-listings` | Landlord |
| POST | `/api/listings/upload` | Landlord |

### Payments
| Method | Path | Access |
|--------|------|--------|
| POST | `/api/payments` | Tenant |
| POST | `/api/payments/{id}/sync-status` | Authenticated |
| POST | `/api/payments/paypal/capture` | Public (redirect) |
| GET | `/api/payments/{id}` | Authenticated |
| GET | `/api/payments/my-payments` | Tenant |
| GET | `/api/payments/received` | Landlord |

---

## User Roles

| Role | Capabilities |
|------|-------------|
| **Tenant** | Browse listings, make payments, view payment history |
| **Landlord** | Create/edit/delete own listings, upload photos, view received payments |
| **Admin** | Full access to all users and listings |

---

## Payment Flow (Stripe)

1. Tenant clicks **Rent this property**
2. Frontend calls `POST /api/payments` → backend creates a Stripe PaymentIntent, returns `clientSecret`
3. Stripe Elements collects card details and confirms the PaymentIntent client-side
4. On success, frontend calls `POST /api/payments/{id}/sync-status` → backend fetches final status from Stripe and updates DB
5. Frontend calls `PATCH /api/listings/{id}/status` with `INACTIVE` → listing is removed from browse
6. Landlord can re-activate via the Edit listing form

Test card: `4242 4242 4242 4242` · any future date · any CVC

---

## Environment Variables

### Backend (`.env`)
| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | HS256 signing key — must be 32+ characters |
| `JWT_EXPIRATION` | Token lifetime in ms (default: 86400000 = 24h) |
| `STRIPE_SECRET_KEY` | Stripe secret key (`sk_test_...`) |
| `PAYPAL_CLIENT_ID` | PayPal sandbox client ID |
| `PAYPAL_CLIENT_SECRET` | PayPal sandbox client secret |

### Frontend (`frontend/.env`)
| Variable | Description |
|----------|-------------|
| `VITE_STRIPE_PUBLISHABLE_KEY` | Stripe publishable key (`pk_test_...`) |

---

## Running Tests

```bash
# Unit tests for user-service and listing-service
cd rental-service-management
mvn test -pl shared,user-service,listing-service -Dtest="UserServiceTest,ListingServiceTest"
```

26 unit tests covering service-layer logic, happy paths, error cases, and role-based ownership rules.

---

## Project Structure

```
rental-service-management/
├── api-gateway/          Spring Cloud Gateway — JWT validation & routing
├── user-service/         Auth, registration, RBAC
├── listing-service/      Listings CRUD, search, image upload
├── payment-service/      Stripe & PayPal integration
├── shared/               Common DTOs and exceptions
├── frontend/             React + Vite application
│   └── src/
│       ├── api/          Typed API client functions
│       ├── components/   Shared UI components (shadcn/ui)
│       ├── context/      AuthContext (JWT session)
│       └── pages/        Route-level page components
├── scripts/
│   ├── seed.sh           Populate DB with sample listings
│   └── test-flow.sh      End-to-end Stripe payment test
├── start.sh              One-command startup
├── stop.sh               Stop all containers
└── docker-compose.yml    Full local stack definition
```
