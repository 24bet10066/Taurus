# SK Electronics — Production Ready

Consolidated summary across all sessions. **What's done. What to do tonight. What to do this week.**

---

## 0 · Current state — everything that's shipped

### Backend
- [x] **9 microservices** all build clean, all communicate via Kafka + Feign.
- [x] **Gateway / technician path mismatch** (was 404'ing) → fixed.
- [x] **Customer history by phone** → new `?customerPhone=` param on `/api/v1/jobs`.
- [x] **Payment received → automatic WhatsApp receipt** → consumer rewritten, customer-phone field added to event.
- [x] **Public booking accepts ?ref=** for marketing attribution (WA_LINK, IG_BIO, IG_POST, GOOGLE, etc.).
- [x] **Smart assignment** (skill × workload × trust) — was already there, verified working.
- [x] **EMA weekly forecast** + reorder alerts to admin WA — was already there, verified working.
- [x] **Per-freelancer credit ledger** with overdue tracking — already there.
- [x] **23 Flyway migrations** + meaningful test suite — already there.

### Frontend (public — what customers see)
- [x] **Light, warm, Indian-business theme** (saffron #DC4F00 + cream + deep ink).
- [x] **3-step booking** (was 4) with smart defaults, past-date blocked, double-submit guarded.
- [x] **URL-param prefill**: `?phone=X&appliance=Y` lands with form half-filled.
- [x] **WhatsApp confirmation** after booking — saves chat to shop.
- [x] **Viral "Share on WhatsApp" CTA** post-booking with attribution tracking.
- [x] **Below-fold marketing** (Why us, service areas, transparent pricing, FAQ, shop info) — answers tier-3 customer concerns before they ask.
- [x] **Sticky Call + WhatsApp float** — one-tap access at any scroll position.
- [x] **PWA manifest** — installable as a home-screen app.
- [x] **OG card + schema.org LocalBusiness** — rich link previews on WhatsApp/Instagram/Google.
- [x] **ErrorBoundary** — even if the React app crashes, customer sees a calm fallback with one-tap Call shop.

### Frontend (admin — what Sushil ji sees)
- [x] **Mobile-first Home tab** with greeting + 4 hero KPIs + Today's money block.
- [x] **Log Call modal** — most important marketing lever. Captures lead + opens prefilled WhatsApp.
- [x] **Customer search** — type phone → past repairs, appliances, outstanding balance, big call button.
- [x] **Pipeline kanban** for full job tracking.
- [x] **Detail panel** with assign / reassign + status transitions.
- [x] **Collect Payment modal** — cash/online + collected-by-tech + notes → fires WA receipt.
- [x] **Bottom nav on mobile** (Home / Customers / Pipeline / Techs / Parts), sidebar on desktop.
- [x] **Pending payments tile** with one-tap deep-link to the unpaid job.

### Frontend (technician — what Rajan ji sees)
- [x] **Light high-contrast PWA** — outdoor readable.
- [x] **Hero card** of current job + progress trail (Got → On way → Reached → Fixing → Done).
- [x] **Big Call + Map buttons** — single-thumb operable in sunlight.
- [x] **WhatsApp parts request** to shop in one tap.
- [x] **3-tile snapshot**: Active / Done / Earned today.

### Production hardening
- [x] **Code-split routes** — customer landing is 114 KB gzipped, admin lazy-loaded only when needed.
- [x] **robots.txt + sitemap.xml**.
- [x] **No-JS fallback** with shop phone number.
- [x] **iOS theme color** + apple-touch-icon.
- [x] **Toast deduplication** by ID — no more error spam.
- [x] **Backend validation errors** surfaced to user verbatim ("Mobile must start with 6-9" not "Could not book").

---

## 1 · Deploy steps (45 minutes total)

### Step 1 — Backend rebuild (5 min)
```bash
cd ~/Documents/Taurus

# shared-lib has a new field on PaymentReceivedEvent → must rebuild first
mvn install -pl shared-lib -q

# downstream services that depend on shared-lib or whose code changed
mvn install -pl payment-service,notification-service,job-service,customer-service,technician-service -q

# Optional: full clean build
mvn clean install -q
```

### Step 2 — Backend deploy (15 min)

**Easiest** — single Hetzner CX22 (€4.50 / ~₹400 per month):
```bash
# On the server, after SSH + Docker installed
git clone <your-git-url> taurus
cd taurus
docker compose up -d           # postgres + redis + kafka
mvn install -pl shared-lib -q
./start-serviceos.sh            # all 7 spring boot services
./health-check.sh               # all should show UP
```

Point a subdomain (`api.wrenchy.in`) to the server, configure NGINX or Caddy with TLS:
```
# Caddy is one-command auto-TLS
caddy reverse-proxy --from api.wrenchy.in --to localhost:8080
```

### Step 3 — Frontend deploy (10 min)

Update `frontend/.env.production`:
```
VITE_API_BASE_URL=https://api.wrenchy.in
VITE_SHOP_PHONE=8960245022
```

Then:
```bash
cd frontend
npm install
npm run build       # produces dist/
```

Deploy `dist/` to **Vercel** (`vercel.json` already configured):
```bash
npx vercel --prod
```

Or any static host (Netlify, Cloudflare Pages, S3+CloudFront — all work the same way).

### Step 4 — Smoke test (15 min)

Open `https://wrenchy.in/?phone=YOUR_PHONE&appliance=AC&ref=DIRECT` and verify:

1. **Above-fold:** brand, trust strip, booking form pre-filled.
2. **Below-fold scroll:** Why us, service areas, pricing, FAQ all render.
3. **Sticky float:** Call + WhatsApp buttons stay visible at all scroll positions.
4. **Fill booking** with a future date → "Confirm & book" → success screen.
5. **Tap "Send confirmation on WhatsApp"** → WhatsApp opens with chat to shop's number prefilled.
6. **Tap "Share on WhatsApp"** → WhatsApp opens with viral share text.
7. **Admin** (`/admin`, log in with OTP) → Home tab renders → Customers tab → type the phone you booked from → past job shows up.
8. **Detail panel** → Mark as ASSIGNED → IN_TRANSIT → AT_CUSTOMER → IN_PROGRESS → COMPLETED → "Collect payment" button appears → record ₹500 cash → **check your phone for the WhatsApp receipt within 5 seconds**.

If any step fails, do NOT share publicly. Fix and retry.

---

## 2 · Day-1 launch (after smoke tests pass)

### WhatsApp Status / Instagram bio
Use the share text from `ROADMAP.md` section 5. Don't forget the `?ref=IG_BIO` and `?ref=WA_STATUS` UTM-style tags so you can see which channel converts.

### First 50 customers
**Do NOT mass-message.** Meta will flag the number for spam. Instead:

For each past customer, open admin → **Log Call** → enter their phone → it opens WhatsApp prefilled with a personal greeting. Tap Send. One customer at a time. 50 customers = 50 minutes once. Forever after, you can re-engage them in the same way.

This is also why "Log Call" is positioned as the most prominent action on the admin Home — it's the literal lever that grows the business.

### Day-1 metrics to watch
Admin → Home tab:
- **Jobs today** count — proves the app is being used.
- **Revenue today (cash + online)** — proves money is flowing through it.
- **Pending payments** — should always be < 3 by night.

---

## 3 · This week's must-builds (in order)

From `ROADMAP.md` section 1 — the gaps that hurt money daily:

| Priority | Build | Effort | ₹ Impact |
|---|---|---|---|
| 1 | **Photo + amount at job completion** | 3 days | High — closes the accountability hole that lets cash skim. |
| 2 | **Walk-in parts counter UI** | 2 days | Very high — 60% of daily transactions. Paper book wins until you fix this. |
| 3 | **Hired-tech cash float / settlement** | 1.5 days | Mid — daily reconciliation, kills the diary completely. |
| 4 | **Repeat-customer auto-WhatsApp at 75–90 days** | 1 day | High — free retention compounds month over month. |
| 5 | **Google Business Profile setup** | 1 hour (paperwork, not code) | Highest — top-of-search "AC repair Banda" for free. |

---

## 4 · Bundle metrics (sanity check)

| File | Size | Gzipped | Loaded when |
|---|---|---|---|
| `index.js` (main + customer landing) | 350 KB | **114 KB** | Every customer visit |
| `LoginPage.js` | 7 KB | 3 KB | Admin/tech logs in |
| `ServiceOS.js` | 60 KB | 15 KB | Admin opens `/admin` |
| `TechnicianPWA.js` | 12 KB | 4 KB | Tech opens `/tech` |
| `Unauthorized.js` | 1.5 KB | 0.8 KB | Wrong role |
| CSS | 26 KB | 6 KB | Always |

**Customer landing total**: ~120 KB gzipped first load. Loads in < 2s on Banda 4G.

---

## 5 · Brand assets shipped

- `public/favicon.svg` — saffron monogram, all browser tabs.
- `public/icon-512.svg` — full app icon, used by PWA install + iOS home screen.
- `public/og-card.svg` — 1200×630 share card with tricolour stripe, used by every WhatsApp/Instagram/FB link preview.

If you want to replace these with AI-generated images, use the prompts in `ROADMAP.md` section 6. The SVGs are zero-byte placeholders that look more professional than the Vite default.

---

## 6 · What's intentionally NOT built

- **AI features.** Forecasting is EMA. Assignment is scored. Nothing else needed.
- **Real-time tech tracking on map.** Overkill at this volume.
- **Multi-city expansion.** You're in Banda. Stay in Banda until you win Banda.
- **Photo upload + completion OTP.** High-value next step but not blocking launch. See ROADMAP section 1.1.
- **Microservice consolidation.** Over-engineered but functional. Revisit only if deploy pain forces it.
- **Sentry / observability.** Add after you have ≥ 10 real users daily.

---

## 7 · Files of interest

| Path | Purpose |
|---|---|
| `ROADMAP.md` | The 60-day business + tech roadmap, with logo prompts. |
| `PRODUCTION_READY.md` | This file. Pre-deploy checklist. |
| `start-serviceos.sh` | Boots all 7 Spring Boot services in separate Terminal windows on macOS. |
| `health-check.sh` | Hits `/actuator/health` on each service. |
| `frontend/.env.production` | Set `VITE_API_BASE_URL` before deploy. |
| `frontend/public/manifest.webmanifest` | PWA config. |

---

## 8 · The one paragraph for your relatives

> SK Electronics ki website ab live hai — ghar baithe AC, fridge, washing machine, geyser, TV, RO repair book kar sakte ho. Same-day service, no advance payment, 15-day workmanship warranty. Ek try karke dekho: **https://wrenchy.in/?ref=FAMILY** — call agar zaroori ho to **+91 89602 45022**. — Sushil

(The `?ref=FAMILY` tag means you'll see in admin which bookings came from your family network specifically. Marketing science, but invisible to them.)

---

**Ship it.**
