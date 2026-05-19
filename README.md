# ServiceOS

Home Appliance Service Operating System for a shop in Banda, Uttar Pradesh.

Two operations under one roof:

1. **B2B parts shop** — sells parts to ~200 freelance technicians (shop earns parts margin only).
2. **B2C repair service** — 4 hired technicians managed by admin (shop earns labor + parts margin).

## Tech stack

| Layer | Choice |
|---|---|
| Language | Java 21 (Virtual Threads, Records, Sealed Interfaces, Pattern Matching) |
| Framework | Spring Boot 3.3.5 + Spring Cloud 2023.0.3 |
| Database | PostgreSQL (one DB per service) — hosted on Neon |
| Cache | Redis — hosted on Upstash |
| Messaging | Kafka — hosted on Confluent Cloud (async events only) |
| Frontend | React 18 + Tailwind CSS |
| Payments | Razorpay |
| WhatsApp | Meta Cloud API |
| SMS | Fast2SMS |
| Storage | Cloudinary |
| Hosting | Railway |
| CI/CD | GitHub Actions |

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

`job.created`, `job.status.changed`, `job.completed`, `inventory.reorder-alert`,
`parts.sold`, `customer.amc-opportunity`, `payment.received`, `credit.updated`.

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

Spin up Postgres, Redis, Kafka locally (these compose values are **only for local dev**; production uses Neon, Upstash, Confluent Cloud):

```bash
docker compose up -d
```

Create the per-service databases:

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

## Environment variables

Copy `.env.example` (when present) to `.env` and fill in:

- `JWT_SECRET` — 32+ random bytes
- `*_DB_URL`, `*_DB_USER`, `*_DB_PASSWORD` — per-service Postgres credentials
- `REDIS_URL` — Upstash Redis URL
- `KAFKA_BOOTSTRAP`, `KAFKA_*` — Confluent Cloud
- `RAZORPAY_*` — Razorpay key id / secret / webhook secret
- `WA_PHONE_NUMBER_ID`, `WA_ACCESS_TOKEN` — Meta WhatsApp Cloud API
- `FAST2SMS_API_KEY`, `FAST2SMS_SENDER` — Fast2SMS
- `CLOUDINARY_*` — Cloudinary credentials

## License

Proprietary — internal use only.
