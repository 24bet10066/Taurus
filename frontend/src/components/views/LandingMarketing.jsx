// LandingMarketing — below-the-fold content for the public booking page.
//
// Design principles for a Banda repair-shop customer:
//   - Booking CTA stays above the fold; this content is for the scroll-down
//     skeptic who wants to verify before submitting their phone number.
//   - Only honest claims. No fake testimonials. The framework is ready for
//     real reviews once the shop starts collecting them.
//   - Tier-3 customer concerns answered in order: pricing, area coverage,
//     timing, what-if-it-breaks-again, payment.

import {
  ShieldCheck, Clock, MapPin, IndianRupee, Wrench,
  RefreshCw, Phone, MessageSquare,
} from 'lucide-react';

// Banda mohallas / localities the shop serves.
// Edit this list as you start serving new areas.
const SERVICE_AREAS = [
  'Civil Lines',
  'Khaprail Mohalla',
  'Naraini Road',
  'Indira Nagar',
  'Babulal Chauraha',
  'Mahatma Gandhi Road',
  'Tindwari Road',
  'Khurahand',
  'Tendua',
  'Atarra Road',
  'Banda Cantonment',
  'Mahokhar',
];

// FAQs answer the questions a first-time Banda customer mutters under their breath.
const FAQS = [
  {
    q: 'Kitne paise lagenge?',
    en: 'How much will it cost?',
    a: 'Technician visit is ₹150 (adjusted in the final bill if you accept the repair). Spare parts are charged at MRP — no inflated rates. You only pay once the job is done.',
  },
  {
    q: 'Same-day service milegi?',
    en: 'Will I get same-day service?',
    a: 'For calls received before 4 PM, we aim to send a technician the same day. After 4 PM or for tomorrow morning preference, schedule it during booking.',
  },
  {
    q: 'Aap meri mohalla aate ho?',
    en: 'Do you cover my area?',
    a: 'We cover all of Banda city. See the service areas list above. If your locality is not listed, call the shop — we still try to send someone if a technician is nearby.',
  },
  {
    q: 'Repair ke baad warranty milti hai?',
    en: 'Is there a warranty on the repair?',
    a: 'Yes — every paid repair carries a 15-day workmanship warranty. If the same issue returns within 15 days, we come back free of charge.',
  },
  {
    q: 'Advance payment dena padega?',
    en: 'Do I have to pay an advance?',
    a: 'Never. No advance, no booking fee. Pay only after the work is done, in cash or online.',
  },
];

// ─── Section: Why book with us ─────────────────────────────────────────────
function WhyUs() {
  const items = [
    { icon: IndianRupee, title: 'Fair pricing',     hi: 'सही दाम',
      sub: '₹150 visit charge · parts at MRP · no advance' },
    { icon: Clock,       title: 'Same-day service', hi: 'उसी दिन',
      sub: 'Booked before 4 PM, fixed same day' },
    { icon: ShieldCheck, title: '15-day warranty',  hi: 'गारंटी',
      sub: 'Free re-visit if the same issue returns' },
    { icon: Wrench,      title: 'Trained technicians', hi: 'अनुभवी कारीगर',
      sub: '4 hired + 200 verified freelancers' },
    { icon: RefreshCw,   title: 'Genuine parts',    hi: 'असली पुर्जे',
      sub: 'OEM-grade, no local fakes' },
    { icon: MapPin,      title: '10 km radius',     hi: 'बांदा शहर',
      sub: 'Full Banda city coverage' },
  ];

  return (
    <section className="px-5 lg:px-8 py-10">
      <div className="max-w-3xl mx-auto">
        <h2 className="text-2xl font-display font-extrabold text-ink">Why book with us</h2>
        <p className="hi text-sm text-brand mt-0.5">हम क्यों?</p>

        <div className="grid grid-cols-2 md:grid-cols-3 gap-3 mt-5">
          {items.map(({ icon: Icon, title, hi, sub }) => (
            <div
              key={title}
              className="bg-paper border border-line rounded-2xl p-4 shadow-card"
            >
              <div className="w-9 h-9 rounded-xl bg-brand-tint text-brand flex items-center justify-center mb-2.5">
                <Icon size={18} strokeWidth={2.2} />
              </div>
              <p className="text-sm font-bold text-ink leading-tight">{title}</p>
              <p className="hi text-[11px] text-ink-3 mt-0.5">{hi}</p>
              <p className="text-xs text-ink-2 mt-1.5 leading-snug">{sub}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── Section: Service areas (mohalla coverage) ─────────────────────────────
function ServiceAreas() {
  return (
    <section className="px-5 lg:px-8 py-10 bg-paper-2">
      <div className="max-w-3xl mx-auto">
        <h2 className="text-2xl font-display font-extrabold text-ink">
          Areas we serve
        </h2>
        <p className="hi text-sm text-brand mt-0.5">हम कहाँ-कहाँ जाते हैं</p>
        <p className="text-sm text-ink-3 mt-2 leading-relaxed max-w-lg">
          Banda city and surrounding mohallas. Your area not listed? Call us —
          if a technician is nearby, we'll still come.
        </p>

        <div className="flex flex-wrap gap-2 mt-5">
          {SERVICE_AREAS.map(area => (
            <span
              key={area}
              className="inline-flex items-center gap-1.5 bg-paper border border-line rounded-full px-3 py-1.5 text-xs font-semibold text-ink-2"
            >
              <MapPin size={11} className="text-brand" />
              {area}
            </span>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── Section: Pricing transparency ─────────────────────────────────────────
function Pricing() {
  return (
    <section className="px-5 lg:px-8 py-10">
      <div className="max-w-3xl mx-auto">
        <h2 className="text-2xl font-display font-extrabold text-ink">
          Simple, honest pricing
        </h2>
        <p className="hi text-sm text-brand mt-0.5">साफ़ दाम</p>

        <div className="mt-5 bg-paper border border-line rounded-2xl shadow-card overflow-hidden">
          <PriceRow label="Visit charge"
                    hi="आने का ख़र्च"
                    value="₹150"
                    note="One-time, adjusted in your final bill if you proceed with the repair." />
          <PriceRow label="Diagnosis"
                    hi="जाँच"
                    value="Free"
                    tone="money"
                    note="Technician inspects and gives you an exact quote before any work starts." />
          <PriceRow label="Labour charge"
                    hi="मरम्मत मज़दूरी"
                    value="₹200 – ₹800"
                    note="Depends on appliance and complexity. Quoted upfront, no surprises." />
          <PriceRow label="Spare parts"
                    hi="पुर्जे"
                    value="At MRP"
                    note="OEM-grade parts. Shop bill shown to you." />
          <PriceRow label="Advance payment"
                    hi="अग्रिम भुगतान"
                    value="₹0"
                    tone="money"
                    note="You pay only after the job is done. Cash or online." />
        </div>

        <p className="text-xs text-ink-3 mt-3 leading-relaxed">
          If you're not satisfied, you don't pay the labour charge. We try once more
          or refund the visit. <span className="text-money font-semibold">Bharosa hai humpe.</span>
        </p>
      </div>
    </section>
  );
}

function PriceRow({ label, hi, value, note, tone }) {
  const fg = tone === 'money' ? 'text-money' : 'text-ink';
  return (
    <div className="px-4 py-3.5 border-b border-line last:border-b-0">
      <div className="flex items-baseline justify-between gap-3">
        <div>
          <p className="text-sm font-bold text-ink">{label}</p>
          <p className="hi text-[11px] text-ink-3">{hi}</p>
        </div>
        <p className={`text-base font-display font-bold num ${fg}`}>{value}</p>
      </div>
      {note && <p className="text-xs text-ink-3 mt-1.5 leading-snug">{note}</p>}
    </div>
  );
}

// ─── Section: FAQs ─────────────────────────────────────────────────────────
function FAQ() {
  return (
    <section className="px-5 lg:px-8 py-10 bg-paper-2">
      <div className="max-w-3xl mx-auto">
        <h2 className="text-2xl font-display font-extrabold text-ink">
          Common questions
        </h2>
        <p className="hi text-sm text-brand mt-0.5">अक्सर पूछे जाने वाले सवाल</p>

        <div className="mt-5 space-y-2">
          {FAQS.map((f, i) => (
            <details
              key={i}
              className="bg-paper border border-line rounded-2xl shadow-card overflow-hidden group"
            >
              <summary className="cursor-pointer px-4 py-3.5 flex items-start justify-between gap-3 list-none">
                <div className="min-w-0">
                  <p className="hi text-base font-bold text-ink leading-snug">{f.q}</p>
                  <p className="text-xs text-ink-3 mt-0.5">{f.en}</p>
                </div>
                <span className="text-brand text-lg font-bold transition-transform group-open:rotate-45 leading-none">+</span>
              </summary>
              <div className="px-4 pb-4 -mt-1 text-sm text-ink-2 leading-relaxed">
                {f.a}
              </div>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── Section: Address & shop info (the "real business" anchor) ─────────────
function ShopInfo() {
  return (
    <section className="px-5 lg:px-8 py-10">
      <div className="max-w-3xl mx-auto bg-paper border border-line rounded-2xl shadow-card p-6">
        <h2 className="text-xl font-display font-extrabold text-ink">SK Electronics</h2>
        <p className="hi text-sm text-brand mt-0.5">एसके इलेक्ट्रॉनिक्स · Banda</p>

        <div className="mt-4 space-y-2.5 text-sm text-ink-2 leading-relaxed">
          <div className="flex items-start gap-2">
            <MapPin size={15} className="text-brand mt-0.5 flex-shrink-0" />
            <span>Civil Lines, Banda, Uttar Pradesh 210001</span>
          </div>
          <div className="flex items-start gap-2">
            <Phone size={15} className="text-brand mt-0.5 flex-shrink-0" />
            <a href="tel:+918960245022" className="font-mono num text-ink hover:text-brand">+91 89602 45022</a>
          </div>
          <div className="flex items-start gap-2">
            <Clock size={15} className="text-brand mt-0.5 flex-shrink-0" />
            <span>Mon – Sat, 9:00 AM – 8:00 PM</span>
          </div>
        </div>

        <p className="text-xs text-ink-3 mt-4 leading-relaxed">
          Run by <span className="font-bold text-ink">Sushil Gupta</span> since 2010.
          A real Banda shop you can walk into. Not a call-centre middleman.
        </p>
      </div>
    </section>
  );
}

// ─── Sticky Call + WhatsApp float (always visible on mobile) ───────────────
//
// Customer arriving from Instagram often wants to CALL not fill a form.
// Sticky float on the right keeps both options one tap away at any scroll.
export function StickyContact({ phone = '8960245022' }) {
  const wa = `https://wa.me/91${phone}?text=${encodeURIComponent(
    'Namaste SK Electronics, mujhe ek service ke baare mein puchna tha.',
  )}`;
  return (
    <div className="fixed right-4 bottom-5 z-40 flex flex-col gap-2.5 pb-safe">
      <a
        href={wa}
        target="_blank"
        rel="noopener noreferrer"
        aria-label="WhatsApp"
        className="w-14 h-14 rounded-full bg-[#25D366] text-white shadow-panel flex items-center justify-center pressable focus-ring"
      >
        <MessageSquare size={24} strokeWidth={2.2} />
      </a>
      <a
        href={`tel:+91${phone}`}
        aria-label="Call shop"
        className="w-14 h-14 rounded-full bg-money text-white shadow-panel flex items-center justify-center pressable focus-ring"
      >
        <Phone size={24} strokeWidth={2.2} />
      </a>
    </div>
  );
}

// ─── Default export — all marketing sections in display order ──────────────
export default function LandingMarketing() {
  return (
    <>
      <WhyUs />
      <ServiceAreas />
      <Pricing />
      <FAQ />
      <ShopInfo />
    </>
  );
}
