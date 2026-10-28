# Wrenchy / ServiceOS — Business & Engineering Roadmap

**Purpose:** Make the software reliable enough to support the real daily operations of a home-appliance repair service and a spare-parts shop.

**Source note:** This roadmap reorganizes planning notes dated 17 May 2026, plus a review of the current `Taurus` README, `docker-compose.yml`, `scripts/init-databases.sql`, and `.env.example`. Because the original status snapshot is several months old and the deployment descriptions conflict, every `reported shipped` or `gap` below must be verified against the current branch and live environment before being treated as current.

## Decision summary

**Accept the business-first direction; reject the unverified claims, arbitrary promises of revenue, and unnecessary architecture changes.** First prevent lost cash, untraceable job completion, and stock inconsistencies. Then improve repeat business and reporting. Add no new AI layer until a measured problem justifies it.

## Operating model

The product must support two distinct flows:

1. **B2C repair:** request → validation → triage/assignment → technician visit → diagnosis → parts used or shortage → evidence and completion → payment/receipt → guarantee/revisit/follow-up.
2. **B2B parts counter:** identify freelance technician → search part → quantity/price → cash or approved credit → sale record and atomic stock deduction → receipt → account balance/repayment.

Both flows feed shared inventory and reports, but repair income, parts-sale margin, customer receivables, freelance credit, and cash held by hired technicians must remain distinct ledger concepts.

## Phase 0 — Verify the truth before shipping more

**Priority: release blocker.**

- [ ] Run the current live deployment through the full smoke-test path and record the results/date.
- [ ] Confirm the actual frontend host, backend host, database provider/topology, Redis provider, Kafka provider, and how secrets are configured.
- [ ] Resolve conflicting documentation: current README says Azure/self-hosted/shared schema-per-service; local compose says it is local-only, initializes separate databases, and describes Neon/Upstash/Confluent Cloud for production; `.env.example` contains per-service database URLs and Confluent Cloud variables.
- [ ] Confirm whether Wrenchy is the public product name, ServiceOS the system name, and Taurus the repository name; use the same explanation everywhere.
- [ ] Verify all integrations actually enabled in production (OTP provider, WhatsApp method, SMS fallback, payment provider, image storage). Remove old comments and env examples that don't match reality.
- [ ] Verify the public repository contains no populated secrets, customer information, private phone lists, or internal operating notes. Decide whether the repository's proprietary/internal-use status matches the intended public visibility.

**Acceptance criteria:** one current deployment diagram and one correct setup path; all public claims match a test or configuration; launch smoke tests pass; no secret/customer data is in Git.

## Phase 1 — Protect service-job revenue and accountability

### P0.1 — Completion evidence and charge capture

**Why:** The May planning snapshot said a hired technician could complete a job without proof of repair or a structured amount/parts record.

- [ ] Require job result, parts used, final charge, amount collected, payment method, and any outstanding amount at completion.
- [ ] Require a repair/evidence photo where shop policy calls for it; store media in controlled object storage and link it to the job.
- [ ] Prevent transition to `COMPLETED` until required fields have saved successfully.
- [ ] Record actor, timestamp, and immutable job-history event for status and financial changes.
- [ ] Provide an admin correction path with a reason and audit entry instead of silently overwriting a closed job.

**Acceptance criteria:** the API rejects incomplete completion; retries do not duplicate parts or payments; an admin can retrieve evidence/history by job ID; technician permissions are tested.

### P0.2 — Walk-in parts-counter interface

**Why:** The planning snapshot said `POST /api/v1/parts/sales` existed but counter staff still used a paper book because there was no UI.

- [ ] Add an admin/counter screen to select the freelance technician, search a part, set quantity, show the applicable price, choose cash or policy-approved credit, and confirm the sale.
- [ ] In one safe operation, record sale lines, deduct stock, record payment/receivable, and issue a receipt.
- [ ] Make duplicate clicks, network retries, and concurrent sales idempotent/transaction-safe.
- [ ] Show sale history and outstanding balance for the selected technician.
- [ ] Add receipt print/share flow only after the sale record is saved.

**Acceptance criteria:** a test sale changes stock and the technician ledger exactly once; a failed transaction leaves no partial stock/payment mutation; the receipt is reproducible from stored data.

### P0.3 — Inventory movement ledger

- [ ] Record supplier receipts, B2B sales, parts consumed on repair jobs, returns, damage, stock counts, and approved corrections.
- [ ] Track every movement by SKU/part, signed quantity, source transaction, actor, time, and reason.
- [ ] Add low-stock thresholds and surface discrepancies for a human review.
- [ ] Define whether batch/cost tracking is required before calculating gross margin; do not infer profit from gross sales.

**Acceptance criteria:** opening stock + movements = system stock; a physical count can be reconciled; every correction has a reason and actor.

### P0.4 — Hired-technician cash settlement

- [ ] Show cash collected by each hired technician over a selected shift/day.
- [ ] Allow the owner/admin to record an actual deposit/settlement against the collected amount.
- [ ] Show unsettled balance, overage/shortage, and historical adjustments.
- [ ] Keep the settlement separate from the payment record: “collected by technician” does not mean “deposited with shop.”

**Acceptance criteria:** daily totals reconcile to individual job payments and actual deposits; a partial settlement remains outstanding and visible.

### P0.5 — Job status history and exception handling

### Frontend
1. `cd frontend`
2. Update `.env.production` → `VITE_API_BASE_URL=https://<your-api-gateway-domain>`
3. `npm run build` → produces `dist/`
4. Deploy `dist/` to **Cloudflare Pages** or any static host.
5. Open `https://<your-frontend-domain>/?phone=9876543210&appliance=AC` to test deeplink prefill.

### Backend deploy options (in order of cost/complexity)
| Option | Cost/mo | Setup time | When to pick |
|---|---|---|---|
| Render (Web Services) | Scalable | 1 hour | **Start here.** Fully managed, zero ops. |
| Single Hetzner CX22 VPS, docker-compose | ~₹400 | 2 hours | If Render gets too expensive at scale. |
| AWS ECS Fargate | ~₹4000 | 1 day | When you have ≥ 2 paying staff users and need VPC peering. |

<!-- GitHub alternative section retained for later consolidation -->

---

- [ ] Record each status transition, actor, timestamp, previous and new state, and reason where necessary.
- [ ] Prevent invalid transitions and handle simultaneous admin/technician updates.
- [ ] Make unassigned, stalled, cancellation, parts-needed, revisit, and failed-notification work visible in the admin view.

**Acceptance criteria:** the timeline explains what happened without relying on WhatsApp history or memory.

<!-- End of retained GitHub alternative section -->

## Phase 2 — Improve service quality and repeat business

### P1.1 — Guarantee and revisit workflow

- [ ] Link a revisit to the original completed job.
- [ ] Store complaint/revisit reason, diagnosis, evidence, owner decision, and whether it is covered or chargeable.
- [ ] Preserve original payment/parts history and prevent the revisit from masquerading as a new unrelated repair.

### P1.2 — Customer follow-up and reviews

- [ ] Add configurable post-service and repeat-customer reminders only after contact data and completion dates are reliable.
- [ ] Respect customer communication preferences; support opt-out and manual follow-up.
- [ ] Record send/delivery/failure status and avoid sending duplicate messages after retries.
- [ ] Request a review after a completed service, without inventing ratings or publishing unverified review counts.

### P1.3 — Daily owner digest

- [ ] Report jobs opened/completed/pending, cash vs online collections, unpaid balances, parts sold, credit outstanding, technician settlements, and low-stock items.
- [ ] Derive figures from ledger records and expose the report date/time.
- [ ] Provide a way to inspect underlying transactions if a number looks wrong.

**Acceptance criteria:** digest totals reconcile to the admin screen and transaction records for the same business day.

### P1.4 — Supplier replenishment workflow

## 7 · Honest closing — what to do this week

<!-- GitHub alternative section retained for later consolidation -->

---

- [ ] Maintain supplier contacts and purchase history.
- [ ] Let the system draft suggested quantities based on low stock and consumption history.
- [ ] Require owner review/approval before sending an order or committing business money.
- [ ] Record ordered quantity, received quantity, purchase cost, discrepancies, and stock movement.

### P1.5 — Annual Maintenance Contract (AMC)

- [ ] Define package terms, price, included visits, validity, cancellation/refund policy, and service area.
- [ ] Implement purchase/payment only after the terms are clear and subscription/provider flow is tested.
- [ ] Track visits consumed, remaining visits, expiry, and renewal reminders.

Do not forecast recurring revenue until pricing, margin, conversion, capacity, and retention assumptions have been validated with actual customers.

### P1.6 — Freelance-technician credit and pricing policy

- [ ] Document the business policy for credit eligibility, limits, repayments, overdue accounts, and manual overrides.
- [ ] Only introduce automatic price tiers when the owner confirms the rules and customers can see the applicable price before purchase.
- [ ] Explain trust/credit decisions and retain an audit trail; keep a manual review path for unusual cases.

## Phase 3 — Integrations and analytics only when justified

### P2.1 — Telephony integration

Defer until the manual Log call flow has been measured and the benefit of call-triggered automation is clear. Before buying a phone integration, measure call volume, missed calls, booking conversion, monthly cost, and reliability requirements.

<!-- End of retained GitHub alternative section -->

### P2.2 — Forecast quality before ML expansion

The repository README documents an EWMA inventory forecast with alpha `0.30`. Keep it as a baseline until there is enough clean history.

- [ ] Capture actual parts consumption, sales, stockouts, supplier lead times, and seasonality where available.
- [ ] Compare EWMA with a simple baseline on a time-based holdout; record MAE/WAPE or another suitable error metric.
- [ ] Monitor data freshness and forecast error.
- [ ] Show uncertainty and let the owner review reorder quantities; do not automatically purchase stock from a forecast alone.
- [ ] Build a fuller MLOps pipeline only if the measured baseline leaves a material business problem to solve.

**Decision:** do not add a generative-AI feature just for the résumé. Improve data quality, workflow reliability, and evaluated forecasting first.

## Deployment and architecture decisions

- Keep the existing codebase as a monorepo while it is useful; avoid a costly service split/rewrite without measured evidence that the present design blocks delivery.
- The current Docker Compose file is labelled **local-development only** and starts infrastructure containers, not every Spring Boot application. Keep local and production instructions separate.
- Do not mix providers from different planning revisions. Verify the actual deployment, then update the README, `.env.example`, diagrams, and deployment scripts to tell one consistent story.
- Use managed database/queue/cache services or self-hosted equivalents based on real cost, recovery, operational load, latency, and support constraints—not a guessed “cheap” monthly price.

## Launch gates

Before inviting real customers to rely on the system, verify:

- [ ] Booking create → admin assignment → technician progress → completion → receipt works on an actual phone.
- [ ] Parts used on a job and parts sold at the counter update stock exactly once.
- [ ] Cash, online payment, credit, repayments, and technician settlements reconcile separately.
- [ ] Duplicate clicks/webhooks and notification retries are safe.
- [ ] Every critical action is role-protected and has a traceable history.
- [ ] A backup has been restored successfully in a test environment.
- [ ] The business owner knows how to handle a failed notification, payment, job, or inventory correction manually.
- [ ] All launch smoke tests pass on the actual deployment before promoting the link.

## How to use this roadmap

Treat priorities as order of attention, not promises about calendar dates. Update status only when acceptance criteria pass in the current code and deployed environment. Record the test and any unresolved limitation. The goal is to make the physical business more dependable, not to maximize the number of features in a GitHub repository.
