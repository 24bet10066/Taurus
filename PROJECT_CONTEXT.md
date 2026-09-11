# wrenchy.in / ServiceOS / Taurus — Full Project Context

Paste this entire document to any AI agent, or share with any person, in place of the actual codebase. It exists so nobody has to rediscover, from scratch, what took real time to establish.

**Labels used throughout:** `[FACT]` verified directly (live check, resume, or Raj's explicit statement) · `[INFERENCE]` logically derived from facts, not independently checked · `[ASSUMPTION]` reasonable but unconfirmed · `[VERIFY]` open question — don't assert as settled.

---

## 1. What this is

`[FACT]` wrenchy.in is a production home-appliance repair and parts platform digitizing SK Electronics — a third-generation, family-run appliance repair and spare-parts business in Banda, Uttar Pradesh, established 1991. Built solo, as both a real operating business tool and a primary portfolio project for Java/Spring backend engineering roles.

## 2. Current status — read this before anything else

`[FACT]` **The production backend is down.** Root cause: the Azure subscription ("Azure for Students") is disabled, marked read-only — confirmed directly via a failed VM-start attempt ("subscription is disabled and therefore marked as read only"). The VM (`serviceos-vm`) shows Stopped (deallocated). Hitting `api.wrenchy.in` returns Cloudflare Error 522 (Cloudflare reached its own edge fine; the origin server never answered) — independent third-party confirmation, not just an internal check.

`[INFERENCE]` Likely cause: the VM was created 18 May 2026 with no auto-shutdown configured, ran continuously, and the one-time $100 Azure for Students credit was exhausted (~2 months of always-on VM + Postgres + Redis + Kafka + bandwidth is a plausible amount to burn through that in).

`[FACT]` The frontend (static, Cloudflare Pages) still loads fine — it's independent of the VM. Anything that calls the API (booking, admin functions) fails.

**Fix, in order:** resolve the Azure subscription (Cost Management + Billing) → start the VM → SSH in and run `docker compose up -d` manually, because `[FACT]` systemd auto-restart was never configured — the stack does not come back on its own just because the VM does.

## 3. Naming — one codebase, three names

`[FACT]`
- **ServiceOS** — the original/internal name. Maven groupId `com.serviceos`. All infrastructure is prefixed `serviceos-*` (VM, resource group, databases).
- **Taurus** — the GitHub repo name (`github.com/24bet10066/Taurus`).
- **wrenchy.in** — the customer-facing brand. This is what's on the resume, the live domain, and what external presentation should lead with.

None of these are wrong — they're three layers of the same thing. A README or doc using only one without acknowledging the others isn't lying, just incomplete.

## 4. Business model — and why it's shaped this way

`[FACT]` Two integrated lines under one platform:

1. **B2C repair service** — a network of hired technicians dispatched to customer homes. Guarantee-backed job tracking, request through completion. Revenue: labor + parts margin. This is the consumer-facing, trust-building half of the business.
2. **B2B parts distribution** — wholesale spare-parts supply to roughly 200 independent freelance technicians across the region, extended on trust-scored credit. Revenue: parts margin only (no labor involved on this side, hence the different margin structure).

`[INFERENCE]` The reason this is one platform and not two separate products: both sides consume the same parts inventory and the same technician trust/reliability signal — a freelancer's payment history on the B2B side and a hired technician's job-completion record on the B2C side are the same underlying kind of data, just applied to different decisions (credit limit vs. job assignment). Splitting them would mean maintaining two versions of the same trust logic.

`[FACT]` The whole platform is grounded in an actual 35-year operating business, not a generic template — fault categorization, parts-to-job linking, and the guarantee mechanism were modeled from observed real operations.

## 5. Confirmed current architecture

`[FACT]`, confirmed via resume + Raj's direct statement ("just follow the resume, what resume is saying is valid") + independent verification:

| Layer | Actual current state |
|---|---|
| Language / framework | Java 21, Spring Boot 3.3.5, Spring Cloud 2023.0.3 |
| Database | PostgreSQL 16 — **one shared instance, schema-per-service** (not database-per-service) — self-hosted via Docker Compose on the Azure VM |
| Cache | Redis 7 — self-hosted via Docker Compose on the same Azure VM |
| Messaging | Kafka 3.7 — self-hosted via Docker Compose on the same Azure VM |
| Frontend | React 19 (confirmed from live `package.json`; not React 18 as an older doc claims) |
| Hosting | Microsoft Azure VM — `Standard_B2ls_v2` (2 vCPU, 4GB), Central India, public IP `40.80.91.204` — plus **Cloudflare Pages** for the frontend |
| Reverse proxy | Caddy, automatic Let's Encrypt HTTPS |
| Edge | Cloudflare also proxies `api.wrenchy.in` (confirmed by the 522 error's own diagnostic), not just the frontend domain |
| CI/CD | GitHub Actions, real and working — path-filtered per-service builds via `dorny/paths-filter` |
| Payments | Razorpay |

`[FACT]` `wrenchy.in` itself, independently verified live: real SEO/OG/Twitter meta tags, React SPA gated behind JS, theme color `#DC4F00`, business phone `+91 89602 45022` in the metadata (matches SK Electronics' public contact).

## 6. What's stale — ignore these claims wherever they appear

`[FACT]` The Taurus repo's own `README.md` (and its underlying tech-stack table) describes a **different, older, no-longer-accurate** setup: Postgres per-service on Neon, Redis on Upstash, Kafka on Confluent Cloud, hosting on Railway, WhatsApp via Meta Cloud API. This was true at some point but is not the current state. Same for the repo's GitHub "Website" link, which still points to an old Vercel URL (`taurus-mu.vercel.app`) instead of `wrenchy.in`.

**Do not treat repo README claims about infrastructure as current without cross-checking against Section 5 above.** This exact gap — docs saying one thing, reality saying another — is what caused a multi-week debugging detour on this project already. Don't repeat it.

## 7. Services — and the business reason each exists

`[FACT]` (ports/responsibilities), `[INFERENCE]` (the "why" column):

| Service | Port | Responsibility | Why it's a separate service |
|---|---|---|---|
| `api-gateway` | 8080 | Single entry point, routing | Centralizes auth so downstream services trust a validated header instead of re-checking JWTs everywhere |
| `auth-service` | 8081 | OTP login, JWT, roles | OTP (not passwords) matches how a phone-first, low-tech-literacy customer base actually operates |
| `job-service` | 8082 | Job FSM, smart assignment | This is the core product — everything else supports a job getting done |
| `parts-service` | 8083 | Catalog, inventory, B2B sales, credit | Serves both business lines — the same catalog is sold wholesale (B2B) and consumed internally (B2C jobs) |
| `customer-service` | 8084 | Customer + appliance + AMC | Appliance history matters for repeat business and AMC (annual maintenance contract) upsell |
| `technician-service` | 8085 | Hired + freelance profiles | Deliberately separate role types — hired techs are managed employees, freelancers are B2B customers who happen to also do the labor |
| `notification-service` | 8086 | WhatsApp + SMS via Kafka | Async by design — a slow notification shouldn't block a booking |
| `payment-service` | 8087 | Razorpay + cash | Cash matters because a large share of Indian home-service payments are still cash-on-completion |
| `analytics-service` | 8088 | Dashboard data, EWMA forecasts | Where the parts-forecasting work (Section 12) plugs in |

Plus `shared-lib` for cross-service DTOs, events, and exceptions.

## 8. Domain model — job lifecycle

`[FACT]`
```
REQUESTED → ASSIGNED → IN_TRANSIT → AT_CUSTOMER → DIAGNOSING
              ↓                                          ↓
              ↓                                    PARTS_NEEDED
              ↓                                          ↓
              ↓                                     IN_PROGRESS
              ↓                                          ↓
            CANCELLED ← (any state)                 COMPLETED → REVISIT_NEEDED
```

`[INFERENCE]` Why 10 states, not a simpler 4-5: appliance repair genuinely requires an on-site diagnosis step before knowing if parts are needed — `DIAGNOSING` and `PARTS_NEEDED` reflect that a technician often can't quote or fix on arrival. `REVISIT_NEEDED` exists because "completed" and "actually fixed" aren't always the same day in field service — parts sometimes need ordering after diagnosis. This models real field-service reality, not a generic CRUD status field.

## 9. Key engineering decisions — and the business reason for each

`[FACT]` (the decision), `[INFERENCE]` (the "why"):

- **Job assignment: priority queue**, score = skill 50% + workload 30% + trust 20%. *Why:* balances getting the right technician (skill) against not overloading your best one (workload) and rewarding reliability (trust) — a fairness and efficiency problem, not just a scheduling one.
- **Parts search: in-memory Trie** (O(k) prefix) + Postgres full-text fallback. *Why:* technicians search parts mid-call, often from memory of a partial name — prefix speed matters more than fuzzy matching here.
- **Inventory forecast: EWMA**, α = 0.30. *Why:* simple, explainable, doesn't need much history to produce a reasonable number — appropriate for a business without years of digitized sales data yet. (See Section 12 — this is being reconsidered, not replaced blindly.)
- **Freelancer trust → credit limit**: composite score (payment history 40% + order frequency 30% + tenure 20% + volume 10%). *Why:* this is a real money-risk decision (extending credit to ~200 people you don't employ) — an interpretable formula means you can explain to a freelancer exactly why their limit is what it is, which a black-box model can't do as easily.
- **Batch traceability: SHA-256 hash chain.** *Why:* parts provenance matters for warranty disputes and supplier accountability — a tamper-evident record protects the business, not just the code.

## 10. Open questions — genuinely unresolved, do not assert as fact

`[VERIFY]`
- **WhatsApp mechanism**: design docs describe manual `wa.me` deep links (chosen to avoid Cloud API cost + DLT registration); the stale README says Meta Cloud API. Never independently confirmed which is actually live.
- **Fast2SMS status**: may have been dropped for TRAI/DLT registration reasons for customer-facing marketing SMS — unclear if that's the same use as the admin-alert SMS this platform's notification-service sends, or a separate effort entirely.
- **Exact deployed repo**: `[INFERENCE]`, not SSH-verified — Taurus is almost certainly what's on `serviceos-vm`, based on the naming match (`com.serviceos` ↔ `serviceos-vm`/`serviceos-rg`) and direct confirmation it's "the one." A separate `BusinessController.java` found during this investigation (package `com.service_management.service_management`) does **not** match this naming and is likely a different, still-unidentified project — probably not what's live on this VM. Two other repos (`wrenchysk`, `wrenchy`) exist with unestablished relevance.
- **Database creation script**: the repo's local-dev setup script still creates seven separate per-service databases, matching the old Neon-era design. Needs rewriting to match the confirmed one-database, schema-per-service architecture — not yet done.
- **Exact env var names**: likely collapsed from per-service (`*_DB_URL` etc.) to a single shared set now that there's one database instance — exact current names unconfirmed.

**Definitive way to resolve all of these:** once the VM is back up, SSH in and check directly — `git remote -v` for the repo question, the actual `.env` for the var names, the actual notification-service code for WhatsApp/SMS. Nothing here should be treated as settled until then.

## 11. In progress: parts-demand forecasting (MLOps)

`[FACT]` — decisions made, not yet built, blocked on the same VM/subscription issue as everything else:

- **Problem**: current inventory forecasting is EWMA (α=0.30) — simple, but blind to seasonality (AC parts before summer, geysers before winter — real patterns this business has).
- **Data reality check first**: the platform has at most ~2-3 months of real transaction history once the VM is back — not enough for a seasonal model yet. Plan is scoped to that constraint, not around an idealized data set.
- **Python, not Java, for this one piece** — ecosystem fit (pandas/scikit-learn/statsmodels) and industry/coursework alignment (this maps to an active IBM MLOps course), run as a **scheduled batch job**, not a persistent service, to keep the operational cost low given this stack already has reliability history problems.
- **Architecture**: batch aggregator reads `parts.sold` (Kafka, already exists) → training table in the existing shared Postgres → weekly retrain via GitHub Actions → predictions table → existing Java `analytics-service` reads predictions and emits the same `inventory.reorder-alert` event as today. No new database, no new event contract.
- **Safety/ethics, concretely**: human approves reorders, model never auto-purchases; reorder alerts show *why* (recent trend, not a bare number); training data excludes customer PII, only aggregate part-level signals; any part with thin history falls back to EWMA rather than a confident-sounding guess; forecast error (MAPE) is tracked after the fact as a monitoring signal, not left unmeasured.
- **Definition of done**: the new model's MAPE must beat EWMA's MAPE on a held-out real window. If it doesn't, the honest outcome is reverting to EWMA — this isn't shipped just because it was built.
- **Build order**: fix subscription → check real data volume → confirm schema → build aggregator + simplest defensible model → wire fallback + monitoring **before** anything more sophisticated → only add seasonal features once enough calendar history actually exists.

## 12. How to use this document

If you're an AI reading this cold: treat `[FACT]` as safe to state directly, `[INFERENCE]` as reasonable but worth a caveat if it matters, and `[VERIFY]` as things to ask about rather than assert. If something you find in the live repo, database, or a conversation contradicts something here, the live/current source wins — **except** anything explicitly listed in Section 6 as stale, which should be treated as wrong until someone updates it deliberately.
