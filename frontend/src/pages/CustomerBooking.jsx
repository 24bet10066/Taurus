// CustomerBooking — 3-step booking for SK Electronics, Banda.
// No registration. Trust strip up top. WhatsApp confirmation at the end.

import { useState, useRef } from 'react';
import toast from 'react-hot-toast';
import {
  CheckCircle2, ChevronLeft,
  Wrench, Phone, MapPin, Calendar, MessageSquare,
  Clock, ShieldCheck, Award, ArrowRight,
} from 'lucide-react';
import { jobsApi } from '../api/jobs';
import { Button } from '../components/ui/Button';
import { FieldInput, FieldTextArea } from '../components/ui/FieldInput';
import LandingMarketing, { StickyContact } from '../components/views/LandingMarketing';

// ─── Helpers ───────────────────────────────────────────────────────────────
// Backend rejects past datetimes; min for the datetime-local input.
const minSchedule = () => {
  const d = new Date();
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
};

// Human-readable label for a backend field key.
const FIELD_LABEL = {
  customerPhone:   'Mobile number',
  applianceType:   'Appliance',
  area:            'Address',
  issueDescription:'Problem description',
  customerName:    'Name',
};
const humanField = (k) => FIELD_LABEL[k] || k;

// ─── Constants ─────────────────────────────────────────────────────────────
const APPLIANCES = [
  { type: 'AC',        label: 'AC',          emoji: '❄️', hi: 'एसी' },
  { type: 'RF',        label: 'Fridge',      emoji: '🧊', hi: 'फ्रिज' },
  { type: 'WM',        label: 'Washing M/c', emoji: '🌊', hi: 'वॉशिंग' },
  { type: 'TV',        label: 'TV',          emoji: '📺', hi: 'टीवी' },
  { type: 'GEYSER',    label: 'Geyser',      emoji: '🔥', hi: 'गीज़र' },
  { type: 'MICROWAVE', label: 'Microwave',   emoji: '📡', hi: 'माइक्रो' },
  { type: 'RW',        label: 'RO / Purifier', emoji: '💧', hi: 'आर.ओ' },
];

const STEPS = [
  { key: 'problem', title: 'What is broken?',  hi: 'क्या खराब है?' },
  { key: 'contact', title: 'Where to come?',   hi: 'कहाँ आना है?' },
  { key: 'review',  title: 'Confirm booking',  hi: 'पक्का करें' },
];

// ─── Shop mark ─────────────────────────────────────────────────────────────
function ShopMark({ size = 40 }) {
  return (
    <div
      className="flex items-center justify-center rounded-xl bg-brand text-white font-display font-extrabold shadow-brand flex-shrink-0"
      style={{ width: size, height: size, fontSize: size * 0.42, letterSpacing: '-0.06em' }}
    >SK</div>
  );
}

// ─── Step rail ─────────────────────────────────────────────────────────────
function StepRail({ current }) {
  return (
    <div className="flex items-center gap-1.5">
      {STEPS.map((_, i) => (
        <div
          key={i}
          className="rounded-full transition-all duration-300"
          style={{
            width:  i === current ? 22 : 7,
            height: 7,
            background: i <= current ? '#DC4F00' : '#DDD2BE',
          }}
        />
      ))}
    </div>
  );
}

// ─── Main ──────────────────────────────────────────────────────────────────
// Read URL params once at module-evaluation time so the lazy useState
// initializers below can both start with the prefilled values without
// triggering effect-time setState.
const initialPrefill = (() => {
  if (typeof window === 'undefined') return { phone: '', name: '', appliance: '', ref: '' };
  const p = new URLSearchParams(window.location.search);
  const phone     = (p.get('phone') || '').replace(/\D/g, '').slice(-10);
  const name      = p.get('name') || '';
  const appliance = (p.get('appliance') || '').toUpperCase();
  // Marketing source: ?ref=ig_bio, ?ref=wa_link, ?ref=google etc.
  // Cached in sessionStorage so a customer who clicks IG → browses → books still gets attributed correctly.
  let ref = (p.get('ref') || '').toUpperCase().slice(0, 32);
  try {
    if (ref) sessionStorage.setItem('sk_ref', ref);
    else ref = sessionStorage.getItem('sk_ref') || '';
  } catch { /* sessionStorage may be unavailable in some embedded WebViews */ }
  return {
    phone,
    name,
    appliance: APPLIANCES.some(a => a.type === appliance) ? appliance : '',
    ref,
  };
})();

export default function CustomerBooking() {
  // If we landed with an appliance prefilled, jump straight to step 1.
  const [step, setStep] = useState(() => (initialPrefill.appliance ? 1 : 0));
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);
  const [jobId, setJobId] = useState(null);

  // Hard guard against double-submit even if React state hasn't re-rendered yet.
  const inFlight = useRef(false);

  // Prefill from URL params (admin shares wa.me link with
  // ?phone=…&appliance=…&name=…).
  const [form, setForm] = useState(() => ({
    applianceType: initialPrefill.appliance,
    description:   '',
    customerName:  initialPrefill.name,
    customerPhone: initialPrefill.phone,
    address:       '',
    area:          '',
    scheduledAt:   '',
  }));

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  // Step validation — kept strict so payload is always backend-valid.
  // Note: time-dependent checks (e.g. scheduledAt vs now) are validated on
  // submit + via the input's min attribute; render-time impurity not needed.
  const canNext = () => {
    if (step === 0) return !!form.applianceType && form.description.trim().length >= 3;
    if (step === 1) {
      const phoneOk    = /^[6-9]\d{9}$/.test(form.customerPhone);
      const nameOk     = form.customerName.trim().length >= 2;
      const addressOk  = form.address.trim().length >= 3;
      const areaOk     = form.area.trim().length >= 2;
      return phoneOk && nameOk && addressOk && areaOk;
    }
    return true;
  };

  const next = () => canNext() && setStep(s => s + 1);
  const back = () => setStep(s => s - 1);

  // Build the backend-shaped payload from the friendlier UI form.
  // Backend requires: customerPhone, applianceType, area, issueDescription.
  // We pack house-number + locality into `area` so the technician gets full address.
  const buildPayload = () => {
    const fullAddress = [form.address.trim(), form.area.trim()]
      .filter(Boolean).join(', ');
    let notes = '';
    if (form.scheduledAt) {
      const t = new Date(form.scheduledAt).toLocaleString('en-IN', {
        dateStyle: 'medium', timeStyle: 'short',
      });
      notes = `Preferred time: ${t}`;
    }
    return {
      customerPhone:    form.customerPhone,
      customerName:     form.customerName.trim() || null,
      applianceType:    form.applianceType,
      area:             fullAddress,
      issueDescription: form.description.trim(),
      customerNotes:    notes || null,
      source:           initialPrefill.ref || 'WEB',
    };
  };

  const submit = async () => {
    if (busy || inFlight.current) return;       // hard guard
    inFlight.current = true;
    setBusy(true);

    // Dedup toasts on the same id so spam clicks don't pile cards on top of each other.
    const TOAST_ID = 'booking-result';
    toast.dismiss(TOAST_ID);

    // Last-mile validation: future-date schedule (done here so render stays pure)
    if (form.scheduledAt && new Date(form.scheduledAt).getTime() <= Date.now()) {
      toast.error('Pick a future date & time, or leave it blank.', { id: TOAST_ID });
      setBusy(false);
      inFlight.current = false;
      return;
    }

    try {
      const { data: res } = await jobsApi.publicBook(buildPayload());
      setJobId(res.data?.id || res.data?.jobId || 'XXXX');
      setDone(true);
    } catch (err) {
      const r = err.response?.data;
      let msg = 'Could not book right now. Please try again in a minute.';
      if (r?.fieldErrors?.length) {
        const f = r.fieldErrors[0];
        msg = `${humanField(f.field)}: ${f.message || 'invalid'}`;
      } else if (r?.code === 'RATE_LIMIT_EXCEEDED') {
        msg = 'Too many booking attempts. Please wait a minute, then try again.';
      } else if (r?.message) {
        msg = r.message;
      } else if (err.code === 'ERR_NETWORK' || err.message?.includes('Network')) {
        msg = 'No internet connection. Please check and try again.';
      }
      toast.error(msg, { id: TOAST_ID, duration: 4500 });
    } finally {
      inFlight.current = false;
      setBusy(false);
    }
  };

  // ─── Success screen ──────────────────────────────────────────────────────
  if (done) {
    const appliance = APPLIANCES.find(a => a.type === form.applianceType);
    const shortId = String(jobId).slice(-6).toUpperCase();
    const waText = encodeURIComponent(
      `Hello SK Electronics,\nI just booked a service.\n\nBooking #${shortId}\nAppliance: ${appliance?.label || form.applianceType}\nProblem: ${form.description}\nName: ${form.customerName || '—'}\nAddress: ${form.address}${form.area ? ', ' + form.area : ''}\nPhone: +91 ${form.customerPhone}`,
    );
    const waLink = `https://wa.me/918960245022?text=${waText}`;

    return (
      <div className="min-h-dvh bg-canvas flex flex-col items-center px-5 py-8 sm:py-12">
        <div className="w-full max-w-md animate-fade-up text-center">

          {/* Big tick */}
          <div className="mx-auto w-20 h-20 rounded-full bg-money-tint flex items-center justify-center mb-5 animate-pop">
            <CheckCircle2 size={44} strokeWidth={2.2} className="text-money" />
          </div>

          <h1 className="text-2xl font-display font-extrabold text-ink">Booking confirmed</h1>
          <p className="hi text-base text-money mt-1">बुकिंग पक्की हो गई</p>
          <p className="text-sm text-ink-3 mt-2 max-w-xs mx-auto leading-relaxed">
            We've received your request. Our technician will call you within <span className="font-bold text-ink">30 minutes</span>.
          </p>

          {/* Job receipt */}
          <div className="bg-paper border border-line rounded-2xl mt-6 overflow-hidden text-left shadow-card">
            <div className="px-5 py-3 border-b border-line flex items-center justify-between bg-paper-2">
              <span className="text-xs text-ink-3 uppercase tracking-wide font-semibold">Booking</span>
              <span className="text-base font-mono font-bold text-brand num">#{shortId}</span>
            </div>
            <div className="px-5 py-4 space-y-2.5 text-sm">
              <Row label="Appliance" value={`${appliance?.emoji || ''} ${appliance?.label || form.applianceType}`} />
              <Row label="Problem"   value={form.description} />
              <Row label="Name"      value={form.customerName || '—'} />
              <Row label="Phone"     value={`+91 ${form.customerPhone}`} mono />
              <Row label="Address"   value={[form.address, form.area].filter(Boolean).join(', ')} />
              {form.scheduledAt && (
                <Row label="Schedule" value={new Date(form.scheduledAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })} />
              )}
            </div>
          </div>

          {/* WhatsApp confirmation — saves the booking to shop's chat */}
          <a
            href={waLink}
            target="_blank"
            rel="noopener noreferrer"
            className="mt-5 flex items-center justify-center gap-2.5 w-full rounded-2xl bg-[#25D366] text-white font-bold text-base pressable focus-ring shadow-card"
            style={{ height: '52px' }}
          >
            <MessageSquare size={18} />
            Send confirmation on WhatsApp
          </a>
          <p className="text-xs text-ink-3 mt-2 leading-snug">
            Saves the booking to your chat with SK Electronics.
          </p>

          {/* Viral share — recommend to friends/family ──────────────── */}
          <div className="mt-5 bg-brand-tint/60 border border-brand/15 rounded-2xl p-4 text-left">
            <p className="text-sm font-bold text-ink leading-snug">
              Help us help your neighbours
            </p>
            <p className="hi text-xs text-brand mt-0.5">पड़ोसी को बताएं</p>
            <p className="text-xs text-ink-2 mt-2 leading-relaxed">
              If we did well, share SK Electronics with one friend.
              For a small shop in Banda, word of mouth is everything.
            </p>
            <a
              href={`https://wa.me/?text=${encodeURIComponent(
                `Hum SK Electronics se ghar baithe appliance repair karwate hain. Same-day service, no advance payment.\n\nBook karein: ${typeof window !== 'undefined' ? window.location.origin : 'https://wrenchy.in'}/?ref=WA_SHARE\n\n+91 89602 45022`,
              )}`}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-3 inline-flex items-center justify-center gap-2 w-full h-11 rounded-xl bg-paper border border-brand text-brand font-bold text-sm pressable focus-ring"
            >
              <MessageSquare size={15} />
              Share on WhatsApp
            </a>
          </div>

          <button
            onClick={() => {
              setDone(false);
              setStep(0);
              setForm({
                applianceType: '', description: '', customerName: '', customerPhone: '',
                address: '', area: '', scheduledAt: '',
              });
            }}
            className="mt-5 text-sm text-brand font-semibold hover:underline"
          >
            Book another service
          </button>
        </div>
      </div>
    );
  }

  // ─── Main form ───────────────────────────────────────────────────────────
  return (
    <div className="min-h-dvh bg-canvas">
      {/* ── Top brand bar ───────────────────────────────────────────── */}
      <header className="px-5 pt-6 pb-4 max-w-md mx-auto">
        <div className="flex items-center gap-3">
          <ShopMark size={44} />
          <div className="min-w-0">
            <h1 className="text-lg font-display font-extrabold text-ink leading-none">SK Electronics</h1>
            <p className="text-xs text-ink-3 mt-1 flex items-center gap-1">
              <MapPin size={11} /> Banda, UP <span className="text-line-2">·</span> Since 2010
            </p>
          </div>
          <div className="ml-auto text-right">
            <p className="hi text-[11px] text-brand font-semibold leading-tight">घर बैठे मरम्मत</p>
            <p className="text-[10px] text-ink-3">Home service</p>
          </div>
        </div>

        {/* Trust strip */}
        <div className="grid grid-cols-3 gap-2 mt-5">
          <TrustChip icon={Award}       label="15+ years"  sub="in Banda" />
          <TrustChip icon={Clock}       label="~30 min"    sub="response" />
          <TrustChip icon={ShieldCheck} label="No advance" sub="payment" />
        </div>
      </header>

      {/* ── Booking card ────────────────────────────────────────────── */}
      <main className="px-5 pb-10 max-w-md mx-auto">
        <div className="bg-paper border border-line rounded-2xl shadow-card overflow-hidden">

          {/* Step header */}
          <div className="px-5 pt-5 pb-3 border-b border-line">
            <div className="flex items-center justify-between mb-2">
              <p className="text-[11px] uppercase tracking-wide font-semibold text-ink-3">
                Step {step + 1} of {STEPS.length}
              </p>
              <StepRail current={step} />
            </div>
            <h2 className="text-xl font-bold text-ink leading-tight">{STEPS[step].title}</h2>
            <p className="hi text-sm text-ink-3 mt-0.5">{STEPS[step].hi}</p>
          </div>

          {/* Step content */}
          <div className="p-5 space-y-4">

            {/* ── Step 0 — problem ───────────────────────────────── */}
            {step === 0 && (
              <>
                <div>
                  <p className="text-xs font-semibold text-ink-2 mb-2">Pick the appliance</p>
                  <div className="grid grid-cols-4 gap-2">
                    {APPLIANCES.map(a => {
                      const selected = form.applianceType === a.type;
                      return (
                        <button
                          key={a.type}
                          type="button"
                          onClick={() => set('applianceType', a.type)}
                          className={[
                            'flex flex-col items-center gap-1.5 px-1 py-3 rounded-xl border-2 transition-all pressable',
                            selected
                              ? 'border-brand bg-brand-tint'
                              : 'border-line bg-paper hover:border-line-2',
                          ].join(' ')}
                        >
                          <span className="text-xl">{a.emoji}</span>
                          <span className={`text-[11px] font-semibold leading-tight ${selected ? 'text-brand-3' : 'text-ink-2'}`}>
                            {a.label}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                <FieldTextArea
                  label="What is the problem?"
                  required
                  rows={3}
                  value={form.description}
                  onChange={e => set('description', e.target.value)}
                  placeholder="e.g. AC is not cooling at all since yesterday morning"
                  hint="Even one line helps the technician come prepared with the right parts."
                />
              </>
            )}

            {/* ── Step 1 — contact ───────────────────────────────── */}
            {step === 1 && (
              <>
                <FieldInput
                  label="Your name"
                  required
                  value={form.customerName}
                  onChange={e => set('customerName', e.target.value)}
                  placeholder="Ramesh Gupta"
                  autoFocus
                />
                <FieldInput
                  label="Mobile number"
                  required
                  type="tel"
                  inputMode="numeric"
                  prefix="+91"
                  value={form.customerPhone}
                  onChange={e => set('customerPhone', e.target.value.replace(/\D/g, '').slice(0, 10))}
                  placeholder="98765 43210"
                  maxLength={10}
                  error={
                    form.customerPhone.length === 10 && !/^[6-9]\d{9}$/.test(form.customerPhone)
                      ? 'Mobile must start with 6, 7, 8 or 9'
                      : undefined
                  }
                />
                <FieldInput
                  label="House number & street"
                  required
                  value={form.address}
                  onChange={e => set('address', e.target.value)}
                  placeholder="H. No. 12, near temple"
                />
                <FieldInput
                  label="Area / Mohalla"
                  required
                  value={form.area}
                  onChange={e => set('area', e.target.value)}
                  placeholder="Civil Lines, Khaprail Mohalla…"
                />
                <FieldInput
                  label="Preferred date & time (optional)"
                  type="datetime-local"
                  value={form.scheduledAt}
                  min={minSchedule()}
                  onChange={e => set('scheduledAt', e.target.value)}
                  hint="Leave blank if any time today is fine."
                />
              </>
            )}

            {/* ── Step 2 — review ───────────────────────────────── */}
            {step === 2 && (
              <div className="space-y-2.5">
                {[
                  { icon: Wrench,        label: 'Appliance',
                    value: APPLIANCES.find(a => a.type === form.applianceType)?.label || form.applianceType },
                  { icon: MessageSquare, label: 'Problem',  value: form.description },
                  { icon: Phone,         label: 'Phone',    value: `+91 ${form.customerPhone}`, mono: true },
                  { icon: MapPin,        label: 'Address',  value: [form.address, form.area].filter(Boolean).join(', ') },
                  form.scheduledAt && {
                    icon: Calendar, label: 'Schedule',
                    value: new Date(form.scheduledAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }),
                  },
                ].filter(Boolean).map(({ icon: Icon, label, value, mono }) => (
                  <div key={label} className="flex items-start gap-3 bg-paper-2 rounded-xl px-3.5 py-3 border border-line">
                    <div className="w-8 h-8 rounded-lg bg-paper flex items-center justify-center flex-shrink-0 border border-line">
                      <Icon size={14} className="text-ink-2" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-[11px] uppercase tracking-wide font-semibold text-ink-3 mb-0.5">{label}</p>
                      <p className={`text-sm text-ink break-words ${mono ? 'font-mono num' : ''}`}>{value || '—'}</p>
                    </div>
                  </div>
                ))}

                <div className="bg-money-tint border border-money/20 rounded-xl px-4 py-3 mt-4">
                  <p className="text-xs text-money font-semibold flex items-start gap-2">
                    <ShieldCheck size={14} className="flex-shrink-0 mt-0.5" />
                    <span>
                      No advance payment. Pay only after the job is done. Our technician will call you to confirm before visiting.
                    </span>
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* ── Footer nav ───────────────────────────────────────── */}
          <div className="px-5 pb-5 pt-2 flex items-center gap-3">
            {step > 0 && (
              <Button variant="outline" size="lg" icon={ChevronLeft} onClick={back}>
                Back
              </Button>
            )}
            {step < 2 ? (
              <Button size="lg" fullWidth={step === 0} className="flex-1" disabled={!canNext()} onClick={next} iconRight={ArrowRight}>
                Continue
              </Button>
            ) : (
              <Button size="lg" className="flex-1" onClick={submit} loading={busy} icon={CheckCircle2}>
                Confirm &amp; book
              </Button>
            )}
          </div>
        </div>

        {/* Quiet footer */}
        <p className="text-center text-[11px] text-ink-3 mt-5">
          By booking you agree to our service terms.
          <br />
          Call <span className="font-mono text-ink-2 num">+91 89602 45022</span> for any help.
        </p>
      </main>

      {/* ── Below-the-fold marketing: only for the scroll-down customer ─── */}
      <LandingMarketing />

      {/* Bottom copyright */}
      <footer className="px-5 lg:px-8 py-6 border-t border-line text-center">
        <p className="text-xs text-ink-3">
          © {new Date().getFullYear()} SK Electronics, Banda · All rights reserved
        </p>
        <p className="text-[10px] text-ink-3 mt-1">
          A trusted name in home appliance service since 2010
        </p>
      </footer>

      {/* ── Always-on Call + WhatsApp floats (mobile-critical) ─────────── */}
      <StickyContact />
    </div>
  );
}

// ─── Sub-components ────────────────────────────────────────────────────────
function Row({ label, value, mono }) {
  return (
    <div className="flex items-baseline justify-between gap-3">
      <span className="text-xs text-ink-3">{label}</span>
      <span className={`text-sm text-ink text-right max-w-[60%] break-words ${mono ? 'font-mono num' : ''}`}>{value}</span>
    </div>
  );
}

function TrustChip({ icon: Icon, label, sub }) {
  return (
    <div className="bg-paper border border-line rounded-xl px-3 py-2.5 text-center shadow-card">
      <Icon size={14} className="mx-auto mb-1 text-brand" strokeWidth={2.2} />
      <p className="text-xs font-bold text-ink leading-tight">{label}</p>
      <p className="text-[10px] text-ink-3 mt-0.5 leading-tight">{sub}</p>
    </div>
  );
}
