// CustomersView — the single most-used screen in a repair shop.
//
// Mrs. Sharma calls about her AC. Father types her phone → he sees:
//   - who she is, total spent, when she last called
//   - which appliances she has on file
//   - every past job (status, what was fixed, who fixed it, what it cost)
//
// One thumb. One screen. Two seconds.

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Search, Phone, MapPin, X, Wrench,
  ChevronRight, ClipboardList, MessageSquare, AlertCircle,
} from 'lucide-react';
import { customersApi } from '../../api/customers';
import { jobsApi } from '../../api/jobs';
import { StatusPill, AppliancePill } from '../ui/StatusPill';
import { EmptyState } from '../ui/EmptyState';
import { Button } from '../ui/Button';
import {
  inr, timeAgo, fmt, initials, applianceEmoji,
  applianceLabel, phone as fmtPhone, firstName,
} from '../../utils/formatters';

// ─── Normalise / validate phone ────────────────────────────────────────────
const cleanPhone = (s) => (s || '').replace(/\D/g, '').slice(-10);
const isValidPhone = (s) => /^[6-9]\d{9}$/.test(cleanPhone(s));

// ─── Main ──────────────────────────────────────────────────────────────────
export default function CustomersView({ onJobClick, recentJobs = [] }) {
  const [query, setQuery] = useState('');
  // Auto-derive "active" phone: as soon as 10 digits are typed, treat as submitted.
  // No effect needed — pure derivation avoids cascading renders.
  const phoneClean = cleanPhone(query);
  const isSearched = phoneClean.length === 10;

  const onSubmit = (e) => {
    e.preventDefault();
    // Submit handler is mostly for Enter key; the query state already drives the fetch.
    if (!isValidPhone(query)) return;
  };

  // ── Customer profile + jobs queries (only when phone is valid) ───────────
  const enabled = isSearched;

  const customerQ = useQuery({
    queryKey: ['customer', 'byPhone', phoneClean],
    queryFn: () =>
      customersApi.byPhone(phoneClean).then(r => r.data?.data || null).catch(() => null),
    enabled,
    staleTime: 60_000,
  });

  const customer = customerQ.data;

  const profileQ = useQuery({
    queryKey: ['customer', 'profile', customer?.id],
    queryFn: () => customersApi.profile(customer.id).then(r => r.data?.data || null),
    enabled: !!customer?.id,
    staleTime: 60_000,
  });

  const jobsQ = useQuery({
    queryKey: ['jobs', 'byPhone', phoneClean],
    queryFn: () =>
      jobsApi.byCustomerPhone(phoneClean)
        .then(r => r.data?.data?.content || r.data?.data || []),
    enabled,
    staleTime: 30_000,
  });

  const jobs = jobsQ.data || [];
  const appliances = profileQ.data?.appliances || [];
  const isLoading = customerQ.isLoading || jobsQ.isLoading || profileQ.isLoading;

  // Recent customers (from today's jobs) — dedupe by phone, keep first occurrence
  const recentCustomers = (() => {
    const seen = new Set();
    const out = [];
    for (const j of recentJobs) {
      const p = cleanPhone(j.customerPhone);
      if (!p || seen.has(p)) continue;
      seen.add(p);
      out.push({
        phone: p,
        name:  j.customerName,
        area:  j.area,
        applianceType: j.applianceType,
        when:  j.createdAt,
      });
      if (out.length >= 6) break;
    }
    return out;
  })();

  // ── Computed snapshot ────────────────────────────────────────────────────
  const outstanding = jobs
    .filter(j => j.status === 'COMPLETED' && (j.paymentStatus === 'PENDING' || !j.paymentStatus))
    .reduce((s, j) => s + Number(j.actualCharge || j.finalCharge || 0), 0);

  const totalSpent = jobs
    .filter(j => j.status === 'COMPLETED')
    .reduce((s, j) => s + Number(j.actualCharge || j.finalCharge || 0), 0);

  const completedCount = jobs.filter(j => j.status === 'COMPLETED').length;
  const lastJob        = jobs[0];

  // ── WhatsApp shortcut to customer ────────────────────────────────────────
  const waLink = phoneClean
    ? `https://wa.me/91${phoneClean}?text=${encodeURIComponent(
        `Namaste${customer?.name ? ' ' + firstName(customer.name) + ' ji' : ''}, SK Electronics se. `,
      )}`
    : null;

  return (
    <div className="flex-1 overflow-y-auto scrollbar-hide pb-28 lg:pb-6">
      <div className="px-5 pt-5 lg:px-8 lg:pt-6 max-w-3xl mx-auto">

        {/* ── Title row ────────────────────────────────────────────────── */}
        <div className="mb-4">
          <h2 className="text-2xl lg:text-3xl font-display font-extrabold text-ink leading-tight">
            Customer lookup
          </h2>
          <p className="hi text-sm text-ink-3 mt-0.5">ग्राहक का इतिहास देखें</p>
        </div>

        {/* ── Search bar ───────────────────────────────────────────────── */}
        <form onSubmit={onSubmit}>
          <div className="flex items-stretch bg-paper border-2 border-line-2 rounded-2xl overflow-hidden focus-within:border-brand focus-within:shadow-focus transition-all">
            <span className="px-4 flex items-center bg-paper-3 text-ink-2 text-base font-mono border-r border-line-2">+91</span>
            <input
              type="tel"
              inputMode="numeric"
              maxLength={10}
              value={query}
              onChange={e => setQuery(e.target.value.replace(/\D/g, ''))}
              placeholder="Enter customer's 10-digit mobile"
              className="flex-1 bg-transparent px-3.5 py-3.5 text-base text-ink placeholder:text-ink-4 outline-none num font-mono"
              autoFocus
            />
            {query && (
              <button type="button" onClick={() => setQuery('')}
                      className="px-4 text-ink-3 hover:text-ink" aria-label="Clear">
                <X size={18} />
              </button>
            )}
            <button type="submit" className="px-5 bg-brand text-white font-semibold pressable disabled:bg-brand/50"
                    disabled={!isValidPhone(query)}>
              <Search size={18} strokeWidth={2.4} />
            </button>
          </div>
          {query && !isValidPhone(query) && cleanPhone(query).length === 10 && (
            <p className="text-xs text-urgent mt-1.5 flex items-center gap-1">
              <AlertCircle size={11} /> Mobile must start with 6, 7, 8, or 9
            </p>
          )}
        </form>

        {/* ── Initial state — recent customers from today ──────────────── */}
        {!enabled && (
          <div className="mt-7">
            {recentCustomers.length > 0 ? (
              <>
                <div className="flex items-baseline justify-between mb-2 px-1">
                  <h3 className="text-base font-bold text-ink">Recent customers</h3>
                  <span className="hi text-xs text-ink-3">आज के ग्राहक</span>
                </div>
                <div className="space-y-2">
                  {recentCustomers.map(c => (
                    <button
                      key={c.phone}
                      onClick={() => setQuery(c.phone)}
                      className="w-full text-left bg-paper border border-line rounded-2xl p-4 active:scale-[0.99] transition-transform shadow-card card-hover"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-11 h-11 rounded-xl bg-brand-tint text-brand-3 font-bold flex items-center justify-center flex-shrink-0">
                          {initials(c.name || c.phone)}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="text-base font-bold text-ink truncate">{c.name || '—'}</p>
                          <p className="text-xs font-mono text-ink-3 num">{fmtPhone(c.phone)}</p>
                          {c.area && <p className="text-[11px] text-ink-3 mt-0.5"><MapPin size={9} className="inline -mt-px mr-1" />{c.area}</p>}
                        </div>
                        <ChevronRight size={16} className="text-ink-3 flex-shrink-0" />
                      </div>
                    </button>
                  ))}
                </div>
              </>
            ) : (
              <EmptyState
                icon={Search}
                tone="brand"
                title="Type a customer's mobile number"
                hint="Pull up their full history — past repairs, appliances on file, amount spent, and any outstanding payment."
              />
            )}
          </div>
        )}

        {/* ── No match ────────────────────────────────────────────────── */}
        {enabled && !customerQ.isLoading && !customer && !jobs.length && (
          <div className="mt-7">
            <EmptyState
              icon={ClipboardList}
              title="No customer found"
              hint={`No record matches +91 ${phoneClean.slice(0,5)} ${phoneClean.slice(5)}. They may be a new customer — book a job and they'll be added.`}
            />
          </div>
        )}

        {/* ── Loading ────────────────────────────────────────────────── */}
        {enabled && isLoading && (
          <div className="mt-7 space-y-3">
            <div className="skeleton h-32 rounded-2xl" />
            <div className="skeleton h-20 rounded-2xl" />
            <div className="skeleton h-20 rounded-2xl" />
          </div>
        )}

        {/* ── Customer profile ──────────────────────────────────────── */}
        {enabled && !isLoading && (customer || jobs.length > 0) && (
          <div className="mt-6 space-y-5">

            {/* Hero card */}
            <div className="bg-paper border border-line rounded-2xl p-5 shadow-card">
              <div className="flex items-start gap-3">
                <div className="w-14 h-14 rounded-2xl bg-brand text-white font-display font-extrabold text-xl flex items-center justify-center flex-shrink-0">
                  {initials(customer?.name || phoneClean)}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xl font-display font-bold text-ink leading-tight">
                    {customer?.name || 'Unnamed customer'}
                  </p>
                  <p className="text-sm font-mono text-ink-2 mt-0.5 num">{fmtPhone(phoneClean)}</p>
                  {(customer?.address || customer?.city) && (
                    <p className="text-xs text-ink-3 mt-1 flex items-start gap-1">
                      <MapPin size={11} className="mt-0.5 flex-shrink-0" />
                      {[customer?.address, customer?.city].filter(Boolean).join(', ')}
                    </p>
                  )}
                </div>
              </div>

              {/* Quick stats — total spent, jobs, last service, outstanding */}
              <div className="grid grid-cols-2 gap-2 mt-4">
                <Stat label="Total spent"   value={inr(totalSpent || customer?.totalSpent || 0)} tone="money" />
                <Stat label="Jobs"          value={completedCount || customer?.jobCount || 0}     tone="brand" />
                <Stat label="Last service"  value={lastJob ? timeAgo(lastJob.createdAt) : '—'}    tone="default" />
                {outstanding > 0 ? (
                  <Stat label="Outstanding"   value={inr(outstanding)} tone="urgent" emphasise />
                ) : (
                  <Stat label="Outstanding"   value="₹0"            tone="money" />
                )}
              </div>

              {/* Actions */}
              <div className="grid grid-cols-2 gap-2 mt-4">
                <Button
                  as="a" href={`tel:${phoneClean}`}
                  size="lg" variant="money" icon={Phone}
                >Call</Button>
                <Button
                  as="a" href={waLink} target="_blank" rel="noopener noreferrer"
                  size="lg" variant="outline" icon={MessageSquare}
                >WhatsApp</Button>
              </div>
            </div>

            {/* Appliances on file */}
            {appliances.length > 0 && (
              <section>
                <div className="flex items-baseline justify-between mb-2 px-1">
                  <h3 className="text-base font-bold text-ink">Appliances on file</h3>
                  <span className="text-xs text-ink-3">{appliances.length}</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  {appliances.map(a => (
                    <div key={a.id} className="inline-flex items-center gap-2 bg-paper border border-line rounded-xl px-3 py-2 shadow-card">
                      <span className="text-lg">{applianceEmoji(a.applianceType)}</span>
                      <div>
                        <p className="text-sm font-bold text-ink">{a.brand || applianceLabel(a.applianceType)}</p>
                        {a.model && <p className="text-[10px] text-ink-3 font-mono">{a.model}</p>}
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            )}

            {/* Past jobs */}
            <section>
              <div className="flex items-baseline justify-between mb-2 px-1">
                <h3 className="text-base font-bold text-ink">
                  Past repairs
                  <span className="text-ink-3 ml-2 num">{jobs.length}</span>
                </h3>
                <span className="hi text-xs text-ink-3">पुराने काम</span>
              </div>

              {jobs.length === 0 ? (
                <EmptyState
                  icon={Wrench}
                  tone="default"
                  title="First-time customer"
                  hint="No past repairs on record. Take their address while you have them on the line — saves time next call."
                />
              ) : (
                <div className="space-y-2">
                  {jobs.map(j => (
                    <button
                      key={j.id}
                      onClick={() => onJobClick?.(j)}
                      className="w-full text-left bg-paper border border-line rounded-2xl p-4 active:scale-[0.99] transition-transform shadow-card card-hover"
                    >
                      <div className="flex items-start justify-between gap-3 mb-1.5">
                        <div className="flex items-center gap-2 flex-wrap">
                          <AppliancePill type={j.applianceType} size="sm" />
                          <StatusPill status={j.status} size="sm" />
                        </div>
                        <span className="text-[11px] font-mono text-ink-3 num">#{j.id?.slice(-6).toUpperCase()}</span>
                      </div>
                      {j.issueDescription || j.description ? (
                        <p className="text-sm text-ink leading-snug line-clamp-2 mb-2">
                          {j.issueDescription || j.description}
                        </p>
                      ) : null}
                      <div className="flex items-center justify-between text-xs">
                        <span className="text-ink-3">{fmt(j.createdAt)}</span>
                        <div className="flex items-center gap-3">
                          {j.assignedTechName && (
                            <span className="text-ink-2"><Wrench size={10} className="inline -mt-px mr-1" />{firstName(j.assignedTechName)}</span>
                          )}
                          {j.actualCharge != null && (
                            <span className="font-mono font-bold text-money num">{inr(j.actualCharge)}</span>
                          )}
                        </div>
                      </div>
                      {j.status === 'COMPLETED' && j.paymentStatus !== 'COLLECTED' && j.actualCharge > 0 && (
                        <div className="mt-2 flex items-center gap-1.5 text-[11px] text-pending bg-pending-tint rounded-lg px-2 py-1 font-semibold w-fit">
                          <AlertCircle size={11} /> Payment pending
                        </div>
                      )}
                    </button>
                  ))}
                </div>
              )}
            </section>
          </div>
        )}

      </div>
    </div>
  );
}

// ─── Stat tile ─────────────────────────────────────────────────────────────
function Stat({ label, value, tone = 'default', emphasise }) {
  const fg =
    tone === 'money'  ? 'text-money'
  : tone === 'brand'  ? 'text-brand'
  : tone === 'urgent' ? 'text-urgent'
  :                     'text-ink';
  const bg =
    tone === 'urgent' && emphasise ? 'bg-urgent-tint border-urgent/30'
  :                                  'bg-paper-2 border-line';
  return (
    <div className={`border rounded-xl px-3 py-2.5 ${bg}`}>
      <p className={`text-base font-display font-bold num ${fg}`}>{value}</p>
      <p className="text-[10px] uppercase tracking-wide text-ink-3 mt-0.5">{label}</p>
    </div>
  );
}
