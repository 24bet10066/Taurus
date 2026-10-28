# ServiceOS / Taurus (wrenchy.in)

Home Appliance Service Operating System for a shop in Banda, Uttar Pradesh.

Two operations under one roof:

1. **B2C repair service** — 4 hired technicians managed by admin (shop earns labor + parts margin).
2. **B2B parts shop** — sells parts to ~200 freelance technicians (shop earns parts margin only).

## Tech Stack (Current State)

| Layer | Choice |
|---|---|
| Language | Java 21 (Virtual Threads, Records, Sealed Interfaces) |
| Framework | Spring Boot 3.3.5 + Spring Cloud 2023.0.3 |
| Database | PostgreSQL 16 (database-per-service: 7 databases on one server instance, each in its `public` schema) |
| Cache | Redis 7 |
| Messaging | Kafka 3.7 |
| Frontend | React 19 + Tailwind CSS |
| Payments | Razorpay |
| WhatsApp | Notification-service channel (mechanism unverified — see PROJECT_CONTEXT.md §10) |
| SMS | Notification-service channel (provider unverified — see PROJECT_CONTEXT.md §10) |
| Storage | Cloudinary |
| Hosting | Not currently decided — Postgres/Redis/Kafka run self-hosted via Docker Compose in local/dev |
| CI/CD | GitHub Actions (path-filtered per-service builds) |

## Services

| # | Module | Port | Responsibility |
|---|---|---|---|
| 1 | `api-gateway` | 8080 | Spring Cloud Gateway — single entry point |
| 2 | `auth-service` | 8081 | OTP login, JWT, roles, token blacklist |
| 3 | `job-service` | 8082 | Job FSM, smart assignment (priority queue) |
| 4 | `parts-service` | 8083 | Catalog, inventory, B2B sales, credit pages |
| 5 | `customer-service` | 8084 | Customer + appliance + AMC |
| 6 | `technician-service` | 8085 | Hired + freelance profiles, skills, availability |
| 7 | `notification-service` | 8086 | Kafka consumer — WhatsApp + SMS |
| 8 | `payment-service` | 8087 | Razorpay + cash receipts |
| 9 | `analytics-service` | 8088 | Admin dashboard data, EWMA forecasts |

Plus `shared-lib` — Kafka event records, sealed result interfaces, enums, exceptions, DTOs.

## Roles

`ADMIN`, `TECHNICIAN_HIRED`, `TECHNICIAN_FREE`, `CUSTOMER`

## Job State Machine

```
REQUESTED → ASSIGNED → IN_TRANSIT → AT_CUSTOMER → DIAGNOSING
              ↓                                          ↓
              ↓                                    PARTS_NEEDED
              ↓                                          ↓
              ↓                                     IN_PROGRESS
              ↓                                          ↓
            CANCELLED ← (any state)                 COMPLETED → REVISIT_NEEDED
```

## Key DSA Decisions

- **Job assignment**: Priority queue, score = skill 50% + workload 30% + trust 20%.
- **Parts search**: In-memory Trie (O(k) prefix) + PostgreSQL full-text fallback.
- **Inventory forecast**: Exponential Weighted Moving Average, α = 0.30.
- **Freelancer trust**: Composite (payment 40% + frequency 30% + tenure 20% + volume 10%) → credit limit.
- **Batch traceability**: SHA-256 hash chain.

## Build & Local Development

```bash
# Build everything
./mvnw clean install

# Spin up local Postgres, Redis, Kafka
docker compose up -d

# Run a service
./mvnw -pl auth-service spring-boot:run
```

## Environment Variables

Copy `.env.example` to `.env` and fill in:

- `JWT_SECRET` — 32+ random bytes
- `DATABASE_URL` — PostgreSQL connection string
- `REDIS_URL` — Redis connection string
- `KAFKA_BOOTSTRAP` — Kafka bootstrap server (self-hosted; `localhost:9092` in local dev)
- `RAZORPAY_*` — Razorpay credentials
- `WA_PHONE_NUMBER_ID`, `WA_ACCESS_TOKEN` — Meta WhatsApp Cloud API
- `FAST2SMS_API_KEY`, `FAST2SMS_SENDER` — Fast2SMS
- `CLOUDINARY_*` — Cloudinary credentials

## License

Proprietary — internal use only.
