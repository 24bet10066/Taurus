# 6-Month Job-Ready Roadmap

## From "I feel like I know nothing" to internship-ready, viva-ready, interview-ready.

| Field | Value |
|---|---|
| Owner | Raj |
| Horizon | 6 months from start date |
| Anchor Project | wrenchy.in |
| Targets | Java/Spring Backend · Full-Stack · SDE (Indian campus + off-campus) |
| Total Effort | ~25–30 hrs/week (recommended) |
| Last Updated | [ADD: YYYY-MM-DD] |

---

## 0. Brutal Audit — Where You Actually Stand

Honest snapshot before any plan. **[FACT]** Pulled from our SRS, System Design, DB Design, and API documentation.

**What you already have (the good news):**

- Nine Spring Boot microservices running in production with Postgres, Redis, Kafka, Docker, Caddy, Cloudflare — on a real Azure VM. Most fresher portfolios do not have this.
- A real domain (a 35-year-old shop), not a tutorial clone. **This is your single biggest asset.**
- Working JWT auth + gateway + multi-channel notifications + Razorpay integration scaffold.
- Four substantial design documents that read like real engineering work.

**What you actually lack (the gaps that block jobs):**

- **DSA depth.** You list patterns; you need 250+ solved + 5+ contest attempts. **[INFERENCE]** This is the single biggest blocker for placements at any tier-1 product company.
- **Java fundamentals you can defend in viva** — collections internals, equals/hashCode, immutability, generics with bounded types, streams with intent, concurrency primitives. **[FACT]** Indian interviewers grill these.
- **Testing.** wrenchy.in has near-zero automated tests right now. Recruiters and hiring managers ask. So do evaluators.
- **Articulation.** You built it; can you walk through the booking flow end-to-end in 3 minutes, then defend every choice in the next 12? Not yet.
- **System design interview fluency.** You designed your own system; you have not yet designed someone else's system out loud under time pressure.
- **Behavioral / story.** The wrenchy.in narrative needs to be a tight 90-second pitch and a 6-minute deep-dive that you can deliver cold.

**Brutal verdict:** you are NOT a fresher who needs to "build more." You are a fresher who needs to **defend, deepen, and articulate** what you already built, while filling fundamentals and DSA.

This roadmap is shaped accordingly. **Less new construction. More depth, testing, articulation, DSA.**

---

## 1. Two Tracks Running in Parallel

### Track A — DSA (daily, continuous, all 6 months)

- **Time:** 1.0–1.5 hrs/day, every day
- **Volume target:** 250 LeetCode problems by month 4, 320 by month 6
- **Pattern coverage:** 25 patterns (listed in Module 1)
- **Contest cadence:** 1 LeetCode Weekly Contest per week from week 6
- **Output:** a public GitHub repo of pattern-tagged solutions, your own notes

### Track B — Module Sequence (focused, one module at a time)

- **Time:** 3–4 hrs/day on weekdays, 4–6 hrs Sat/Sun
- **Cadence:** one module at a time, in the order below
- **Output:** an artifact per module (test suite, doc, working code, a viva re-explanation)

**Why parallel:** DSA needs daily reps to compound. Modules need focused blocks. Both lose value if interleaved sloppily.

---

## 2. The Six Phases

| Phase | Weeks | Theme | Outcome |
|---|---|---|---|
| Phase 0 | 1 | Setup and self-audit | Honest baseline + DSA habit + study environment |
| Phase 1 | 2–5 | Foundations refresh | Java core, OOP, SQL, concurrency basics that you can defend in a viva |
| Phase 2 | 6–10 | Spring depth + wrenchy.in defense | You can defend every Spring annotation in your code |
| Phase 3 | 11–14 | Microservices + security depth | You can re-architect wrenchy.in from a whiteboard |
| Phase 4 | 15–17 | Async + caching by need | Honest grasp of Kafka and Redis with reasons to use, not buzzwords |
| Phase 5 | 18–21 | Deployment, observability, frontend | Production discipline + enough full-stack to qualify for full-stack roles |
| Phase 6 | 22–24 | System design + behavioral + polish | Interview-ready end to end |

---

## 3. Module Catalog

Each module declares: **status** (core / optional / overkill-for-now), **what**, **why**, **wrenchy.in connection**, **time**.

---

### Module 0 — Setup, Self-Audit, and DSA Habit Bootstrap
**Status:** core
**Weeks:** 1
**Time:** ~12 hrs
**What:** set up a study workspace (notes repo, LeetCode account, GitHub for solutions, study timer), audit your real Java + Spring level honestly with a fixed checklist, do 7 LeetCode easy problems across patterns to establish the daily habit.
**Why:** without a baseline you can't measure progress and without a habit DSA never compounds.
**wrenchy.in connection:** none yet. This module is meta — set the conditions for the next 6 months.

---

### Module 1 — DSA Pattern Map (parallel daily track, defined here)
**Status:** core
**Weeks:** 1–24 (continuous)
**Time:** 1.0–1.5 hrs/day
**What:** 25 patterns drilled in sequence. Two Easy + one Medium per pattern minimum, then move on; revisit on a weekly cycle.

The 25 patterns, in the order I recommend:

1. Arrays and two pointers
2. Sliding window
3. Hashing for frequency and pairs
4. Binary search on answer
5. Prefix sums and difference arrays
6. Strings — sliding window and pattern search
7. Linked list — fast / slow pointer
8. Stack — monotonic and valid sequences
9. Queue — sliding maxima, level traversal
10. Recursion and backtracking
11. Trees — DFS / BFS
12. Trees — depth / diameter / LCA
13. Heaps and priority queues
14. Greedy patterns
15. Intervals — merge, insert, scheduling
16. Sorting tricks (custom comparators, bucket sort)
17. Graphs — BFS / DFS and topological sort
18. Graphs — Dijkstra and Union-Find
19. Dynamic programming — 1D
20. Dynamic programming — 2D
21. Dynamic programming — knapsack and partition
22. Bit manipulation basics
23. Tries
24. Segment trees and Fenwick (optional — only for top-tier product company prep)
25. Math + number theory basics

**Why:** Indian product-company assessments lean medium-graph, medium-DP, medium-array. Pattern coverage beats random grinding.
**wrenchy.in connection:** trust-score ranking (heap), parts auto-assign (greedy + ranked), notification cooldown (interval logic), state-machine validation (graph). When relevant patterns surface in modules, you connect them.

---

### Module 2 — Java Core Refresh and OOP Depth
**Status:** core
**Weeks:** 2–3
**Time:** ~30 hrs
**What:** Java language features you can defend in a viva — primitive vs reference types, autoboxing pitfalls, String / StringBuilder, equals / hashCode contract, immutability, final / static, generics with bounded types, varargs, enums as first-class types, exception hierarchy and best practices, Streams with intent (not just `forEach`), Optional, records, sealed classes. OOP depth — SOLID principles with real examples from your wrenchy.in code, composition over inheritance, common GoF patterns that actually matter (Strategy, Factory, Builder, Adapter, Observer, Template Method).
**Why:** Indian interviews live here. "Why is String immutable?" "Explain equals and hashCode." "When would you use composition over inheritance?" Failing these costs offers.
**wrenchy.in connection:** find one example in your code for every concept. If you don't have one, refactor a small piece to use it (e.g., add a Strategy interface for notification channels).

---

### Module 3 — Java Concurrency Basics
**Status:** core
**Weeks:** 4
**Time:** ~15 hrs
**What:** threads vs virtual threads (Java 21), synchronized vs ReentrantLock, volatile and happens-before, ExecutorService, CompletableFuture, the Java memory model in plain language. Common pitfalls — race conditions, deadlocks, the publication problem.
**Why:** asked at any product-company interview. Even web-app freshers get "what does volatile do" thrown at them.
**wrenchy.in connection:** your notification-service uses virtual threads under Spring Boot 3.3.5. You should be able to explain why that matters (fewer pinned carrier threads on blocking I/O).

---

### Module 4 — SQL Deep + Indexing + Transactions
**Status:** core
**Weeks:** 5
**Time:** ~20 hrs
**What:** beyond the basics — JOINs (inner / left / right / cross / self) with execution plans, GROUP BY with HAVING, window functions (ROW_NUMBER, RANK, LAG, LEAD, partitioned aggregates), CTEs (recursive included), subqueries vs joins, B-tree vs hash vs GIN indexes, partial and composite indexes (you already use them — explain them), EXPLAIN ANALYZE reading, transactions and isolation levels (Read Committed vs Repeatable Read), MVCC basics in Postgres.
**Why:** SQL is asked in 80% of backend interviews. Your DB Design doc is strong; this module turns it into spoken fluency.
**wrenchy.in connection:** walk through every index in your `DATABASE_DESIGN.md` and explain the query each one accelerates. Write 10 representative SQL queries against your real schema.

---

### Module 5 — Spring Boot Internals: How It Actually Works
**Status:** core
**Weeks:** 6–7
**Time:** ~30 hrs
**What:** IoC container internals (bean lifecycle, scopes, BeanPostProcessor), `@Configuration` vs `@Component`, dependency injection — constructor over field, autoconfiguration in depth (how `spring.factories` and `AutoConfiguration` triggers work), `@ConditionalOnX` annotations, profiles and `application.yml` precedence, externalised configuration patterns, embedded servers, `CommandLineRunner` vs `ApplicationRunner`. Reading the Spring source for one auto-config to demystify the magic.
**Why:** "How does Spring autoconfigure my DataSource?" is the highest-rejection interview question for Java freshers because most can't answer it.
**wrenchy.in connection:** trace the bean creation order in your `auth-service` startup logs. Identify one custom `@Configuration` you wrote and one you depend on by autoconfig.

---

### Module 6 — Spring Data JPA and Hibernate Gotchas
**Status:** core
**Weeks:** 8
**Time:** ~20 hrs
**What:** Repository abstractions, derived query methods, `@Query` JPQL and native, projection interfaces and DTOs, fetch types (LAZY vs EAGER) and the N+1 problem, `@EntityGraph`, transactional propagation modes (REQUIRED, REQUIRES_NEW, NESTED, etc.) with real failure cases, optimistic vs pessimistic locking, second-level cache (and when not to enable it).
**Why:** N+1 is the most common backend bug shipped by freshers, and it's a guaranteed interview question.
**wrenchy.in connection:** audit your job-service repository methods for N+1. Find at least one and fix it with `@EntityGraph` or a projection. Document the before/after query count.

---

### Module 7 — REST and Spring MVC Depth
**Status:** core
**Weeks:** 9
**Time:** ~15 hrs
**What:** the Servlet vs WebFlux models, request lifecycle in Spring MVC, controllers vs `@RestController`, content negotiation, exception handlers (`@ControllerAdvice`), validation (`@Valid`, custom constraints), HATEOAS (skip — overkill for our scale), versioning strategies (URL vs header), idempotency for unsafe verbs, OpenAPI generation via springdoc.
**Why:** half of any Spring interview is "explain how a request flows through your app".
**wrenchy.in connection:** walk through `POST /public` end-to-end — DispatcherServlet → HandlerMapping → HandlerAdapter → your controller → service → repository → response. Make the diagram. Defend each annotation.

---

### Module 8 — Spring Security and JWT Done Right
**Status:** core
**Weeks:** 10
**Time:** ~20 hrs
**What:** SecurityFilterChain (the modern bean-based config), filter ordering, UsernamePasswordAuthenticationFilter mental model, custom filters, method-level security (`@PreAuthorize`), CSRF (when needed, when not), CORS done correctly, JWT signing and validation, refresh token rotation, why never store JWTs in localStorage. OWASP API Top 10 — discuss each against wrenchy.in.
**Why:** every backend interview will ask "how does JWT work in your project?" Many freshers fumble. You can shine.
**wrenchy.in connection:** explain why JWT validation lives at the gateway, not in every service. Defend the choice of HS256 vs RS256 at your scale. Walk through OTP send → verify → JWT issue → refresh → logout.

---

### Module 9 — Defend wrenchy.in (Architecture Viva Drill)
**Status:** core
**Weeks:** 11
**Time:** ~20 hrs
**What:** structured rehearsal. Take your `SYSTEM_DESIGN.md` and answer 40 likely viva questions out loud, recording yourself. Sample: "Why microservices and not a monolith at this scale?" "Why Kafka and not RabbitMQ?" "How does a booking survive Kafka being down?" "What's the trade-off of shared Postgres?" "Walk me through a real failure mode."
**Why:** university viva at Chandigarh University will hit this hard. So will any final-round interview.
**wrenchy.in connection:** this IS the wrenchy module. Output: a 6-minute video of you defending your architecture.

---

### Module 10 — Microservices Patterns (Depth)
**Status:** core
**Weeks:** 12
**Time:** ~15 hrs
**What:** bounded contexts (domain-driven design, lightly), database-per-service vs shared DB (you already chose; deepen the why), API gateway pattern (you have it; understand the alternatives — backends-for-frontends, service mesh), service discovery, distributed tracing fundamentals, the saga pattern (compensating transactions), CAP theorem honestly explained.
**Why:** "microservices" appears in 90% of mid-tier Indian backend job descriptions. Interviewers want depth, not buzzwords.
**wrenchy.in connection:** identify one place in your code where a saga would be appropriate (payment → job mark-paid is close). Decide whether to implement it (probably not yet) and document why.

---

### Module 11 — API Gateway + Inter-Service Communication
**Status:** core
**Weeks:** 13
**Time:** ~15 hrs
**What:** Spring Cloud Gateway internals, route predicates and filters, rate-limiting (you should add this — RequestRateLimiter with Redis), circuit breakers (Resilience4j), Feign clients vs WebClient, retries with exponential backoff and idempotency, bulkhead and timeout patterns.
**Why:** distributed-systems failure modes are a guaranteed interview topic at any product company.
**wrenchy.in connection:** add Resilience4j to your job → parts service Feign call. Implement a circuit breaker. Document the configuration and reasoning.

---

### Module 12 — Async Messaging with Kafka (Defending What You Built)
**Status:** core (depth — you already have Kafka)
**Weeks:** 14
**Time:** ~15 hrs
**What:** Kafka mental model — topics, partitions, consumer groups, offsets, rebalances. Idempotent producers, exactly-once semantics honestly (you don't have it; don't claim it), consumer parallelism limits (partition count is the ceiling), dead-letter topics, schema evolution, when Kafka is overkill and you should have used a database outbox.
**Why:** the line "we use Kafka in production" works only if you can answer "why not SQS, why not Postgres LISTEN/NOTIFY, why not RabbitMQ?". You will be asked.
**wrenchy.in connection:** brutally honest re-examination of whether Kafka is right for your scale. It's not — you wrote that in `SYSTEM_DESIGN.md` Section 4.3. Be ready to defend the call as "for learning + future scale" without flinching.

---

### Module 13 — Redis and Caching Patterns (Caution)
**Status:** core (but watch for gold-plating)
**Weeks:** 15
**Time:** ~10 hrs
**What:** when to cache, what to cache, cache-aside vs read-through vs write-through, eviction policies, TTL discipline, cache stampedes and how to prevent them, Redis as a primary store (only for ephemeral data — OTP, dedup keys), Redis as a job queue (don't — use Kafka if you must async).
**Why:** caching questions ("design a URL shortener") are interview staples.
**wrenchy.in connection:** your current Redis use is correct and minimal (OTP, dedup, refresh-token blacklist). Resist adding aggressive caching to "look advanced". Defend the minimal use.

**Honest verdict:** if your project didn't need Redis, you wouldn't add it. It needs Redis. Don't add more.

---

### Module 14 — Testing in Spring (The Real Gap)
**Status:** core (most impactful module after DSA)
**Weeks:** 16–17
**Time:** ~30 hrs
**What:** the testing pyramid — unit / slice / integration / end-to-end. JUnit 5 idioms, Mockito patterns (when to mock, when not), AssertJ fluency, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`, Testcontainers for real Postgres in CI, MockMvc and WebTestClient, contract testing very briefly (Pact — overkill for now, but know the term).
**Why:** every job interview asks "how do you test your code?" Right now you cannot answer convincingly. After this module, you can.
**wrenchy.in connection:** build a real test suite — minimum 25 unit tests, 5 slice tests, 3 integration tests with Testcontainers. Target 60% coverage on the booking service alone. **Output:** a green CI pipeline.

---

### Module 15 — Docker, Linux Discipline, and Deployment
**Status:** core
**Weeks:** 18
**Time:** ~15 hrs
**What:** Dockerfile best practices (layer caching, multi-stage builds, distroless images, image size discipline), `docker compose` you already use, systemd unit files (you still need to ship these), basic Linux ops (journalctl, ss, lsof, htop), Azure VM ops (you have it) and the equivalent AWS deploy story (so you can claim AWS familiarity honestly).
**Why:** "deploy your project" is a viva question and a take-home question. You've done it — but you need to articulate the steps cleanly.
**wrenchy.in connection:** ship the systemd unit files (still pending per `SYSTEM_DESIGN.md` Section 7.3). Reduce one service's Docker image size as an artifact for show.

---

### Module 16 — Observability: Logs, Metrics, Tracing
**Status:** core
**Weeks:** 19
**Time:** ~12 hrs
**What:** structured JSON logs (you have this in Spring), `traceId` / `spanId` correlation, Spring Boot Actuator metrics, Prometheus + Grafana basics (run locally), OpenTelemetry basic concepts, log levels discipline. Skip ELK / Splunk at your scale.
**Why:** "how do you debug a 5xx in production?" answered with "I look at logs" is a fresher answer. Answered with "I correlate the request id across services via traceId, then check the consumer-lag metric" is the answer that lands offers.
**wrenchy.in connection:** add `X-Request-Id` propagation through your gateway and services (per NFR-OBSERV-03 in your SRS — currently aspirational). Stand up local Prometheus scraping your `/actuator/prometheus`. Take screenshots for your portfolio.

---

### Module 17 — Frontend Basics for Full-Stack Eligibility
**Status:** optional-but-strongly-recommended (skip if targeting backend-only roles)
**Weeks:** 20–21
**Time:** ~25 hrs
**What:** JavaScript ES2020+ essentials (you'll fill gaps), React hooks deeply (useState, useEffect, useMemo, useCallback, useReducer, custom hooks), TypeScript basics (let's add it to wrenchy.in frontend — small win), Tailwind discipline, fetch / Axios, react-query (you use it — deepen), accessibility basics, performance basics (lighthouse, lazy loading, route splitting).
**Why:** full-stack roles double your job pool. Backend-only freshers are filtered out of half the openings.
**wrenchy.in connection:** migrate the booking flow to TypeScript and add 5 unit tests in Vitest. Document the migration as a portfolio artifact.

**Skip this module if:** you have already decided to target backend-only and are confident in interviews.

---

### Module 18 — System Design Interview Prep
**Status:** core
**Weeks:** 22–23
**Time:** ~30 hrs
**What:** the system-design interview script — clarify requirements, estimate scale, propose API, propose data model, propose architecture, identify bottlenecks, propose scaling moves, identify trade-offs. Cover ~12 canonical problems: URL shortener, rate limiter, news feed, chat, ride hailing, file storage, search autocomplete, payment system, notification system, leaderboards, social graph, ticket booking. **Then design wrenchy.in as if you'd never seen it.**
**Why:** any product-company final round will be system design.
**wrenchy.in connection:** the final exercise IS wrenchy.in. You should be able to design it cleanly in 45 minutes on a whiteboard cold.

---

### Module 19 — Behavioral, HR, and the wrenchy.in Narrative
**Status:** core
**Weeks:** 24
**Time:** ~10 hrs
**What:** the STAR format (Situation, Task, Action, Result) drilled until automatic. Your wrenchy.in story compressed to 90 seconds and expanded to 6 minutes. Standard HR questions ("greatest weakness", "why our company", "where in 5 years") answered honestly without clichés. The salary-negotiation script for ₹6-12 lakh fresher offers in India.
**Why:** strong technical candidates fail at HR rounds. This module prevents that.
**wrenchy.in connection:** rehearse the story in front of a camera. Critique it. Re-record.

---

## 4. Honest Reality Check — Gold-Plating Watchlist

**Things you have built that you do NOT need to build more of.** When a module touches them, the verdict is "defend, don't extend."

| Thing | Already Have | Verdict |
|---|---|---|
| Microservices count (9) | Yes | **Defend the choice.** Do not add a 10th unless load demands. |
| Kafka | Running | **Defend with the SD doc's honest "for learning + future-readiness" rationale.** |
| Redis | Running, minimally | **Keep minimal.** Resist adding more caching to "look advanced". |
| JWT auth | Working | **Deepen articulation.** No need for OAuth 2.0 / OIDC yet. |
| Multi-channel notifications | Working | **Ship the systemd auto-restart** and the rate-limit; don't add a third channel. |

**Things you might be tempted to add that are overkill-for-now:**

- Kubernetes — explicit anti-goal in your SD doc.
- Service mesh (Istio, Linkerd) — overkill.
- Event sourcing / CQRS — not needed.
- gRPC — not needed.
- Multi-region deployment — not needed.
- Native mobile apps — defer.
- AI fault diagnosis — defer until 6 months of labelled complaint data.

**Things you SHOULD add (gaps from your own SRS):**

- Real test suite (Module 14)
- systemd auto-restart (Module 15)
- Gateway-level rate limiting with Redis (Module 11)
- `X-Request-Id` propagation (Module 16)
- 2Factor.in OTP delivery (you flagged this earlier — wire it up in Module 8)

---

## 5. Time Budget at a Glance

| Phase | Weeks | Module Track (hrs) | DSA Track (hrs) | Total |
|---|---|---|---|---|
| 0 | 1 | 12 | 10 | 22 |
| 1 | 2–5 | 65 | 35 | 100 |
| 2 | 6–10 | 85 | 50 | 135 |
| 3 | 11–14 | 65 | 40 | 105 |
| 4 | 15–17 | 25 | 30 | 55 |
| 5 | 18–21 | 52 | 40 | 92 |
| 6 | 22–24 | 40 | 30 | 70 |
| **Total** | **24** | **~344** | **~235** | **~580 hrs over 6 months** |

**[INFERENCE]** That's ~24 hrs/week average — heavy but realistic if you have 4 active hours a day on weekdays and 5 each on weekends. Adjust the per-module weeks if your weekly hours are different.

---

## 6. Output Per Module (For Your CV and Viva)

Each module ends with one of these tangible outputs:

| Module | Output |
|---|---|
| 0 | Audit doc + LeetCode profile linked |
| 2 | One refactor in wrenchy.in demonstrating an OOP principle |
| 3 | A short write-up: "Why my notification-service uses virtual threads" |
| 4 | Annotated `DATABASE_DESIGN.md` with EXPLAIN ANALYZE for 5 queries |
| 5 | A blog post: "How Spring Boot autoconfigures my data source" |
| 6 | A measurable N+1 fix in the job service |
| 7 | A request-lifecycle diagram for `POST /public` |
| 8 | OTP delivery wired (2Factor); JWT flow documented |
| 9 | A 6-minute video of you defending the architecture |
| 11 | A circuit breaker around job → parts call |
| 12 | A short post: "Why we use Kafka (and why it might be overkill)" |
| 14 | Green CI with 25 unit + 5 slice + 3 integration tests |
| 15 | systemd unit files shipped; smaller Docker image |
| 16 | Local Prometheus + Grafana dashboard screenshots |
| 17 | TypeScript booking flow + 5 Vitest tests |
| 18 | 12 system-design write-ups in a public repo |
| 19 | Recorded 90-second + 6-minute wrenchy.in story |

---

## 7. Calendar Sketch

Below is the default sequence. Adjust if you have semester exams in any week.

| Weeks | Focus |
|---|---|
| 1 | Module 0 |
| 2–3 | Module 2 |
| 4 | Module 3 |
| 5 | Module 4 |
| 6–7 | Module 5 |
| 8 | Module 6 |
| 9 | Module 7 |
| 10 | Module 8 |
| 11 | Module 9 |
| 12 | Module 10 |
| 13 | Module 11 |
| 14 | Module 12 |
| 15 | Module 13 |
| 16–17 | Module 14 |
| 18 | Module 15 |
| 19 | Module 16 |
| 20–21 | Module 17 (skip if backend-only) |
| 22–23 | Module 18 |
| 24 | Module 19 |
| All | Module 1 (DSA, daily) |

---

## 8. Progress-Tracking Conventions

Going forward, each time we converse on the roadmap, the assistant will:

1. State which module is current.
2. State which modules are complete.
3. State which artifacts are still pending from previous modules.
4. Surface one item from the revision pile (last week's concepts).

You answer with: what you actually did, what you got stuck on, and what you want to do next.

---

## 9. Document Change Log

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | [ADD: YYYY-MM-DD] | [ADD: Author] | Initial roadmap. |

---

*End of roadmap. No teaching yet — that begins when you say "start module N".*
