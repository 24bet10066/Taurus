# SK Electronics — Business & Tech Roadmap

Prepared for: **Sushil Gupta (owner) + Raj (operator + CSE student)**
Last updated: 17 May 2026

This is the playbook from "ship today" to "still running in 5 years."
Brutally prioritized by ₹ impact, not by what's fun to build.

---

## 0 · What just shipped (today's deliverables)

- **Booking error fixed.** Frontend was sending `description`/`address`, backend wants `issueDescription`/`area`. Now packs full address into `area` and dedupes toasts so one click = one outcome.
- **Past-date validation.** Past datetime now blocked at input level + submit-time check.
- **Log-call flow.** Home tab → tap "Log call" → enter customer details → opens WhatsApp prefilled with shop intro + booking deeplink (`/?phone=…&appliance=…`). Customer taps the link → booking page is half-filled. Saves the customer to the DB for the Customers search.
- **Customer page.** Search by phone → past repairs, appliances on file, outstanding balance, big Call + WhatsApp buttons.
- **Day-end money tile.** Cash / online / total + alert for pending collections.
- **Collect payment.** From the job sheet, one modal records cash/online, fires the WhatsApp receipt automatically.
- **Production hardening.** Web manifest (installable as PWA), OG/Twitter card for WhatsApp/Instagram link previews, robots.txt + sitemap, ErrorBoundary fallback (calls shop directly), updated favicon/icons, schema.org LocalBusiness JSON-LD for Google.
- **Gateway 404 fix.** Technician calls were broken in prod; now routed correctly.
- **Payment-received WhatsApp consumer.** Was dead logging code, now actually sends.

---

## 1 · Critical gaps that still hurt money (next 2–3 weeks)

These are the items the audit flagged that are NOT yet built. In order of how much each one costs you per week today:

### 1.1  Photo + amount capture on job completion *(₹₹₹)*
**Today:** Any TECHNICIAN_HIRED can mark a job COMPLETED. No proof. No invoice photo. Easy to skim cash.
**Build:** When tech taps "Done" → one screen → required: photo of fixed appliance + parts used + amount collected. Photo uploaded to S3-compatible store (Cloudflare R2 — ₹0/month for this scale). Job moves to COMPLETED only after the photo + amount land.
**Why now:** Single biggest accountability hole. Will repay itself in one prevented skim.
**Effort:** ~3 days. Needs object storage signup + multipart endpoint + frontend camera input.

### 1.2  Walk-in parts sale at the counter *(₹₹₹)*
**Today:** 200 freelancers walk in daily to buy parts. The backend `POST /api/v1/parts/sales` exists. **There is no UI.** Father uses a paper book.
**Build:** "Parts counter" view inside admin. Type freelancer name (autocomplete) → pick part (Trie search already exists!) → quantity → cash/credit toggle → print/WhatsApp receipt. Atomic stock deduction is already wired in `SalesService`.
**Why now:** Highest-frequency operational event of the day. Right now the paper book is faster than the app — that means the app is being IGNORED.
**Effort:** ~2 days. Pure UI.

### 1.3  Hired-tech cash float / settlement *(₹₹)*
**Today:** `PaymentRecord.collectedBy` records who took cash. No daily reconciliation. Father still does it on paper.
**Build:** New view "Tech settlements" → for each hired tech: today's cash collected vs deposited. Single button: "Tech deposited ₹X to shop" → records, settles. End-of-day pasta-easy.
**Effort:** ~1.5 days. New endpoint + new view.

### 1.4  Repeat-customer auto-WhatsApp *(₹₹₹ for retention)*
**Today:** Customer's AC was fixed in May. They forget you exist by August.
**Build:** Cron job (Spring scheduler — already used in `AmcScheduler`). Every Monday 9am IST, find COMPLETED jobs from 75-90 days ago. Send WhatsApp: *"Namaste {name} ji, 3 months ho gaye {appliance} fix kiye hue. Sab theek chal raha hai? Free check-up chahiye to ye link daba dijiye."*
**Why now:** Free retention. Costs ₹0. Generates repeat business worth lakhs per year.
**Effort:** 1 day. New consumer in notification-service.

### 1.5  Google Business Profile sync *(₹₹₹ for new customer acquisition)*
**Not a code feature, but critical.** Right now if someone in Banda searches "AC repair near me", you don't appear.
**Action:** Sign up at google.com/business. Add SK Electronics. Photo of shop. Hours. Phone. Link to your deployed booking site. Ask 10 happy customers to leave 5-star reviews (one-tap link).
**Why now:** Free top-of-search placement for "AC repair Banda", "fridge repair Banda" etc. The single highest-leverage marketing action.
**Effort:** 1 hour of paperwork from your side.

---

## 2 · The next layer (4–8 weeks)

### 2.1  Customer reviews & ratings
After a job is marked COMPLETED + paid, fire a WhatsApp 24h later: *"Namaste, kaam kaisa raha? Reply 5 for excellent, 4 for good, 3 for OK."* Store the score. Showcase the average rating ("4.7★ from 800+ customers") on your public landing page.
**Why:** Trust signal. The single biggest lift for booking conversion in a tier-3 city.

### 2.2  AMC (Annual Maintenance Contract) subscriptions
**Big-ticket idea.** Sell "AMC packages" — 4 service visits/year for ₹1,500. Locks in revenue. The customer-service module already has `amcStartDate/amcEndDate` fields and an `AmcScheduler` that fires opportunities. Just needs a frontend purchase flow + Razorpay-subscription wiring.
**₹ math:** 100 AMC customers × ₹1,500 = ₹1.5 lakh recurring annual revenue, mostly margin.

### 2.3  Per-freelancer pricing tiers
Schema supports it (`internalPrice` field on `SparePart`). Logic doesn't. Build a rule: freelancers with trust score > 80 get internal price; trust 50-80 get sell price; < 50 get sell price + 5%. Auto-pricing rewards loyalty.
**Effort:** 1 day backend.

### 2.4  Inventory auto-reorder to suppliers
Today, `inventory.reorder-alert` fires to admin WhatsApp. Next step: a "suppliers" table + one-tap "Order from supplier" button that drafts a WhatsApp/email to the supplier with the part list and quantities. Father reviews + sends.
**Why:** Slashes out-of-stock minutes per week (currently invisible cost).

### 2.5  Daily KPI WhatsApp digest to owner
Every night at 9pm: send Sushil ji a one-message summary. *"Aaj 14 jobs, ₹18,500 kamai (₹12,200 cash + ₹6,300 online), 2 pending payment, parts low: AC capacitor (3 left)."* He doesn't even open the app — the report opens itself.
**Effort:** Half a day, reuses notification infra.

### 2.6  Telephony integration (Exotel / Knowlarity)
**The "automatic WhatsApp on call" you asked about.** A virtual number that forwards to Sushil's phone AND posts a webhook to your backend. Backend fires a WhatsApp greeting automatically — no manual "Log call" tap needed.
**Cost:** Exotel ~₹500/mo + 30 paise/call. Worth it once volume > 50 calls/day.
**Effort:** 2-3 days incl. account approval. Defer until you've validated booking conversion from manual log-call.

---

## 3 · Things to STOP doing or DELETE

These look productive but cost you time:

- **Don't add AI features.** Smart assignment, EMA forecasting — done, working. Anything else is theater.
- **Don't ship the 9-microservice topology to a free-tier deploy.** Each service = its own container = idle cost. For the first 6 months: deploy as a single Docker compose stack on one Hetzner CX22 (€4.50/month, ~₹400). Survives the entire SK Electronics order book.
- **Don't fix the microservice split until it hurts.** It is over-engineered for your scale, but rebuilding now = a week of broken state. Revisit only when deploy time > 5 minutes or you have 3+ regressions/month traceable to cross-service drift.

---

## 4 · Deploy checklist (do these in order)

### Backend
1. **`cd ~/Documents/Taurus && mvn install -pl shared-lib -q`** — shared event added a field, must rebuild first.
2. `mvn install -pl payment-service,notification-service,job-service,customer-service,technician-service -q`
3. Run all 7 services + infra: `docker compose up -d && ./start-serviceos.sh`
4. `./health-check.sh` — all 7 should report UP.

### Frontend
1. `cd frontend`
2. Update `.env.production` → `VITE_API_BASE_URL=https://<your-api-gateway-domain>`
3. `npm run build` → produces `dist/`
4. Deploy `dist/` to **Vercel** (already configured via `vercel.json`) or any static host.
5. Open `https://<your-frontend-domain>/?phone=9876543210&appliance=AC` to test deeplink prefill.

### Backend deploy options (in order of cost/complexity)
| Option | Cost/mo | Setup time | When to pick |
|---|---|---|---|
| Single Hetzner CX22 VPS, docker-compose | ~₹400 | 2 hours | **Start here.** Sufficient up to 500 jobs/day. |
| Railway, all services | ~₹1500 | 1 hour | If you don't want to SSH. |
| Render free tier | ₹0 | 1 hour | Demo only; cold-start latency hurts UX. |
| AWS ECS Fargate | ~₹4000 | 1 day | When you have ≥ 2 paying staff users. |

### Pre-launch smoke tests (do all 7 in order)
1. Open landing page on phone → trust strip renders, "Book Service" works → fill 3 steps → confirms.
2. Confirmation page → tap "Send confirmation on WhatsApp" → WhatsApp opens with the prefilled message to shop's number.
3. Admin login → OTP works → Home tab opens → "Today's money" section appears (will be ₹0 fresh start, that's fine).
4. Customers tab → enter the phone from step 1 → past job appears in the history.
5. Pipeline tab → newly-booked job is in "New requests" column → tap → assign to a technician.
6. Mark job COMPLETED → "Collect payment" button appears → record ₹500 cash → confirm the customer's phone receives the WhatsApp receipt.
7. Tech PWA — log in as a hired tech → see the assigned job → tap "Call" → phone dials.

If any step fails, do NOT share the link publicly. Fix and retry.

---

## 5 · Sharing the link (Instagram, WhatsApp, family)

### Instagram bio
```
SK Electronics — Banda
Ghar baithe appliance repair
AC · Fridge · WM · Geyser · TV · RO
📞 +91 89602 45022
👇 Book online
[your deployed URL]
```

### WhatsApp status (first post)
Image: the OG-card.svg from the public folder, or take a real photo of the shop.
Text:
```
🎉 SK Electronics ab online bhi available hai.
Ghar baithe appliance repair book karein:
[your deployed URL]
✓ Same-day service
✓ No advance payment
✓ 15+ years bharosa
+91 89602 45022
```

### Family/relatives WhatsApp
Keep it human:
```
Namaste,
Hamari SK Electronics ki website online ho gayi hai.
Ghar par appliance theek karwana ho to is link se book kar sakte ho:
[your deployed URL]
Koi bhi sawaal ho to sidha call/WhatsApp karein.
— Sushil
```

### First 50 customers
Hand-pick from existing customer phone numbers. WhatsApp them one-by-one (no spam blast — Meta will flag the number). The Log Call modal does this for you, one customer at a time, naturally.

---

## 6 · Logo & app-icon prompts (Indian tricolour + psychology)

You asked for prompts. Drop these into any AI image tool (Midjourney, DALL-E, Ideogram, Adobe Firefly). I've encoded the psychology in the prompt itself.

### Logo prompt — flat, professional, scalable
```
Minimalist flat logo for "SK Electronics", a trusted home appliance repair
shop in Banda, Uttar Pradesh. Wordmark "SK" inside a rounded square.
Saffron-to-amber gradient (#FF9933 → #DC4F00) on the square.
The "K" subtly forms a wrench or tool silhouette in negative space.
Below the square: clean sans-serif word "SK ELECTRONICS" in deep ink.
Tagline under, smaller: "Since 2010 · Banda".
Thin horizontal tricolour bar at the bottom (saffron, white, green).
Style: modern Indian small-business branding, like Khatabook or PhonePe
for Business, NOT generic SaaS. Flat, no shadows, vector-friendly.
Square 1:1 ratio. White background. No text outside the logo.
```

### App icon prompt — bold, recognisable at 48×48
```
App icon for an Indian appliance repair shop. Rounded square,
gradient from warm saffron (#FF7A1A top-left) to deep amber
(#DC4F00 bottom-right). Centered: bold white letters "SK"
in an extra-bold sans-serif, with tight negative letter-spacing.
Subtle small "tools" mark — a wrench + screwdriver crossed —
faintly visible in the bottom-right corner at 30% opacity.
No tagline, no text outside. Must be readable at 24×24 pixels.
Style: confident, premium, warm, NOT cute. iOS-style corner radius.
```

### Hero banner / Instagram post prompt
```
Editorial banner image: an Indian repair technician in a clean blue
uniform, smiling, kneeling next to a split AC unit in a tier-3 Indian
home. Warm afternoon light through a window. Subtle saffron-amber
color grading. Hindi+English overlay text: "Ghar baithe appliance
service" / "Same-day repair". Bottom: thin saffron-white-green tricolour
bar. Logo "SK Electronics — Banda · Since 2010" top-right.
Style: warm, trustworthy, real (not stock-photo glossy).
Aspect ratio 4:5 (Instagram portrait).
```

### Psychology behind the choices (so you can iterate)

| Element | Why |
|---|---|
| **Saffron-amber gradient (#DC4F00)** | Saffron = Indian commercial heritage + warmth + energy. Strong on white. Not generic SaaS blue. Stands out in any city's Google search. |
| **Deep ink type (#1A1209) on warm cream (#FFF8F0)** | Higher contrast than pure black-on-white, easier on bright-sunlight phone screens (your customers are Banda, not Mumbai air-conditioned offices). |
| **"SK" monogram** | Father is the brand. Personal recognition. People in Banda know "Sushil ji ka shop" — the monogram reinforces that. |
| **Tricolour bar (saffron-white-green)** | Subtle Indian patriotic signal. Builds trust with older customers. Don't overuse — once at the bottom is enough. |
| **Green for "money" (#047857) and "done"** | Money colour worldwide. Reassures "kaam pakka ho gaya, paisa receive ho gaya". |
| **No emojis as logo elements** | Local businesses get judged on professionalism. Emojis in marketing text is fine; emojis IN the logo says "I made this in Canva in 5 minutes". |
| **Hindi sub-labels, not full bilingual** | Bilingual = clutter. Hindi micro-labels = warmth + accessibility for non-English readers without slowing down anyone. |

---

## 7 · Honest closing — what to do this week

Day 1 (today):
- Re-build backend, deploy frontend.
- Run all 7 smoke tests above.
- Post the link to your own status only — not relatives yet.

Day 2:
- Have 3 trusted family members book a test service. Real flow. See what they get stuck on.
- Fix any obvious confusion. Don't add features.

Day 3-5:
- Share with first 20 actual past customers via Log Call flow (one-by-one).
- Watch what happens in admin's Home tab the next morning.

Day 6-10:
- Add the **photo capture + amount on completion** (Section 1.1). This is the single biggest risk before scaling.

Day 11-30:
- Build the **walk-in parts counter UI** (Section 1.2). This is where 60% of daily transactions happen.

Day 30-60:
- Google Business Profile setup. AMC purchase flow. Reviews.

This is not a 6-month plan. It's a 6-week plan that decides whether the app survives or the diary wins.

The diary doesn't crash. Don't give it a reason to come back.
