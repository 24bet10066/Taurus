# ServiceOS

Home Appliance Service Operating System for a shop in Banda, Uttar Pradesh.

Two operations under one roof:
- **B2B parts shop** — sells parts to ~200 freelance technicians (shop earns parts margin only).
- **B2C repair service** — 4 hired technicians managed by admin (shop earns labor + parts margin).

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 21 (Virtual Threads, Records, Sealed Interfaces, Pattern Matching) |
| Framework | Spring Boot 3.3.5 + Spring Cloud 2023.0.3 |
| Database | PostgreSQL 16 — one shared instance, **schema-per-service** (not DB-per-service) — self-hosted via Docker Compose on an Azure VM |
| Cache | Redis 7 — self-hosted via Docker Compose on the same Azure VM |
| Messaging | Kafka 3.7 — self-hosted via Docker Compose on the same Azure VM (async events only) |
| Frontend | React 19 + Tailwind CSS |
| Payments | Razorpay |
| WhatsApp | <!-- VERIFY: design docs say manual wa.me deep links, not Cloud API. Confirm which is actually live before publishing. --> Meta Cloud API |
| SMS | <!-- VERIFY: Fast2SMS marketing outreach was dropped for TRAI/DLT reasons — confirm this admin-alert use is still active, separate from that. --> Fast2SMS |
| Storage | Cloudinary |
| Reverse proxy | Caddy — automatic Let's Encrypt HTTPS |
| Hosting | **Microsoft Azure** (backend VM, Docker Compose) + **Cloudflare Pages** (frontend) |
| CI/CD | GitHub Actions |

## Services

| # | Module | Port | Responsibility |
|---|---|---|---|
| 1 | api-gateway | 8080 | Spring Cloud Gateway — single entry point |
| 2 | auth-service | 8081 | OTP login, JWT, roles, token blacklist |
| 3 | job-service | 8082 | Job FSM, smart assignment (priority queue) |
| 4 | parts-service | 8083 | Catalog, inventory, B2B sales, credit pages |
| 5 | customer-service | 8084 | Customer + appliance + AMC |
| 6 | technician-service | 8085 | Hired + freelance profiles, skills, availability |
| 7 | notification-service | 8086 | Kafka consumer — WhatsApp + SMS |
| 8 | payment-service | 8087 | Razorpay + cash receipts |
| 9 | analytics-service | 8088 | Admin dashboard data, EWMA forecasts |

Plus `shared-lib` — Kafka event records, sealed result interfaces, enums, exceptions, DTOs.

## Roles

`ADMIN`, `TECHNICIAN_HIRED`, `TECHNICIAN_FREE`, `CUSTOMER`

## Job state machine

```
REQUESTED → ASSIGNED → IN_TRANSIT → AT_CUSTOMER → DIAGNOSING
              ↓                                          ↓
              ↓                                    PARTS_NEEDED
              ↓                                          ↓
              ↓                                     IN_PROGRESS
              ↓                                          ↓
            CANCELLED ← (any state)                 COMPLETED → REVISIT_NEEDED
```

## Kafka topics

`job.created`, `job.status.changed`, `job.completed`, `inventory.reorder-alert`, `parts.sold`, `customer.amc-opportunity`, `payment.received`, `credit.updated`.

## Key DSA decisions

- **Job assignment**: priority queue, score = skill 50% + workload 30% + trust 20%.
- **Parts search**: in-memory Trie (O(k) prefix) + PostgreSQL full-text fallback.
- **Inventory forecast**: Exponential Weighted Moving Average, α = 0.30.
- **Freelancer trust**: composite (payment 40% + frequency 30% + tenure 20% + volume 10%) → credit limit.
- **Batch traceability**: SHA-256 hash chain.

## Build

```bash
# Build everything
./mvnw clean install

# Build a single service (and its dependencies)
./mvnw -pl auth-service -am clean install

# Run a single service
./mvnw -pl auth-service spring-boot:run
```

## Local development

Spin up Postgres, Redis, Kafka locally. These same values now also describe **production** — prod runs the identical Docker Compose stack, on the Azure VM instead of your machine:

```bash
docker compose up -d
```

<!-- VERIFY: production is ONE shared database with schema-per-service (see tech stack table). This block below still creates seven separate databases, matching the old Neon setup. Replace with something like the block underneath, checked against your actual Flyway/datasource config — do not trust this verbatim. -->

Old (per-service databases — likely stale):
```bash
docker exec -i serviceos-postgres psql -U postgres <<'SQL'
CREATE DATABASE serviceos_auth;
CREATE DATABASE serviceos_jobs;
CREATE DATABASE serviceos_parts;
CREATE DATABASE serviceos_customers;
CREATE DATABASE serviceos_technicians;
CREATE DATABASE serviceos_payments;
CREATE DATABASE serviceos_analytics;
SQL
```

Likely correct now (one database, schema-per-service — verify before using):
```bash
docker exec -i serviceos-postgres psql -U postgres <<'SQL'
CREATE DATABASE serviceos;
\c serviceos
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS jobs;
CREATE SCHEMA IF NOT EXISTS parts;
CREATE SCHEMA IF NOT EXISTS customers;
CREATE SCHEMA IF NOT EXISTS technicians;
CREATE SCHEMA IF NOT EXISTS payments;
CREATE SCHEMA IF NOT EXISTS analytics;
SQL
```

## Environment variables

Copy `.env.example` (when present) to `.env` and fill in:

- `JWT_SECRET` — 32+ random bytes
- <!-- VERIFY: was per-service (*_DB_URL / *_DB_USER / *_DB_PASSWORD). With one shared instance this is probably now a single DB_URL / DB_USER / DB_PASSWORD, schema selected per service. Confirm actual var names. --> Database credentials for the shared Postgres instance
- `REDIS_URL` — self-hosted Redis on the Azure VM (not Upstash)
- `KAFKA_BOOTSTRAP`, `KAFKA_*` — self-hosted Kafka on the Azure VM (not Confluent Cloud)
- `RAZORPAY_*` — Razorpay key id / secret / webhook secret
- <!-- VERIFY: matches the WhatsApp row above — confirm mechanism before confirming these var names --> `WA_PHONE_NUMBER_ID`, `WA_ACCESS_TOKEN` — WhatsApp integration
- `FAST2SMS_API_KEY`, `FAST2SMS_SENDER` — Fast2SMS
- `CLOUDINARY_*` — Cloudinary credentials

## License

Proprietary — internal use only.
