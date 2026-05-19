// ServiceOS — Admin command center for Sushil ji.
// Mobile-first (he checks 30x/day on his phone). Light, warm, Indian-business.
// Default landing: "Home" — at-a-glance day.
//
// Tabs:
//   home       — father's morning glance: KPIs + urgent attention list
//   pipeline   — full kanban (horizontal on phone, side-by-side on desktop)
//   technicians
//   parts
//   analytics
//
// All data fetching kept identical to the original (react-query). Only the
// visual layer changes.

import { useState, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import {
  Home, LayoutGrid, Users, Package, BarChart2, Plus, Search,
  RefreshCw, ChevronRight, X, Phone, MapPin, Wrench,
  AlertTriangle, CheckCircle2, ArrowRight, User, UserCheck,
  TrendingUp, IndianRupee, LogOut, Bell, Star, UserSearch,
  Banknote, Smartphone, ClipboardCheck, MessageSquare, PhoneIncoming,
} from 'lucide-react';

import { useAuth } from '../context/AuthContext';
import { jobsApi, JOB_STATUS_FLOW, PIPELINE_COLUMNS } from '../api/jobs';
import { analyticsApi } from '../api/analytics';
import { techApi } from '../api/technicians';
import { partsApi } from '../api/parts';
import { paymentsApi } from '../api/payments';
import { customersApi } from '../api/customers';
import { configApi } from '../api/config';
import { Modal } from '../components/ui/Modal';
import { StatusPill, AppliancePill } from '../components/ui/StatusPill';
import { JobCardSkeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { Button, IconButton } from '../components/ui/Button';
import { StatCard } from '../components/ui/StatCard';
import { FieldInput, FieldTextArea, FieldSelect } from '../components/ui/FieldInput';
import CustomersView from '../components/views/CustomersView';
import {
  inr, inrShort, timeAgo, fmt, initials, applianceEmoji,
  applianceLabel, statusMeta, phone as fmtPhone, firstName,
} from '../utils/formatters';

// ─── Constants ─────────────────────────────────────────────────────────────
const APPLIANCE_TYPES = ['AC', 'WM', 'RW', 'RF', 'TV', 'GEYSER', 'MICROWAVE'];

// All tabs — used by the desktop sidebar.
const TABS = [
  { id: 'home',        icon: Home,        label: 'Home',      hi: 'मुख्य' },
  { id: 'customers',   icon: UserSearch,  label: 'Customers', hi: 'ग्राहक' },
  { id: 'pipeline',    icon: LayoutGrid,  label: 'Pipeline',  hi: 'काम' },
  { id: 'technicians', icon: Users,       label: 'Techs',     hi: 'तकनीशियन' },
  { id: 'parts',       icon: Package,     label: 'Parts',     hi: 'पुर्जे' },
  { id: 'analytics',   icon: BarChart2,   label: 'Insights',  hi: 'विश्लेषण' },
];

// Mobile bottom-nav — 5 most-used tabs. Insights stays desktop-only since
// father glances at insights weekly at best, but uses Customers many times a day.
const MOBILE_TABS = TABS.filter(t => t.id !== 'analytics');

const COLUMN_LABELS = {
  REQUESTED:    'New requests',
  ASSIGNED:     'Assigned',
  IN_TRANSIT:   'On the way',
  AT_CUSTOMER:  'At customer',
  IN_PROGRESS:  'Repairing',
  PARTS_NEEDED: 'Parts needed',
  COMPLETED:    'Done',
};

// ─── Shop monogram ─────────────────────────────────────────────────────────
function ShopMark({ size = 36 }) {
  return (
    <div
      className="flex items-center justify-center rounded-xl bg-brand text-white font-display font-extrabold shadow-brand flex-shrink-0"
      style={{ width: size, height: size, fontSize: size * 0.42, letterSpacing: '-0.06em' }}
    >SK</div>
  );
}

// ─── Hero job card (Home: needs attention) ────────────────────────────────
function AttentionCard({ job, onOpen, reason }) {
  return (
    <button
      onClick={() => onOpen(job)}
      className="w-full text-left bg-paper border border-line rounded-2xl p-4 active:scale-[0.99] transition-transform shadow-card"
    >
      <div className="flex items-start justify-between gap-2 mb-2">
        <div className="flex items-center gap-2 flex-wrap">
          <AppliancePill type={job.applianceType} size="sm" />
          <StatusPill status={job.status} size="sm" />
        </div>
        <span className="text-[11px] font-mono text-ink-3 num">#{job.id?.slice(-6).toUpperCase()}</span>
      </div>

      <p className="text-base font-bold text-ink leading-snug">
        {job.customerName || job.customerPhone}
      </p>
      {job.area && (
        <p className="text-xs text-ink-3 flex items-center gap-1 mt-0.5">
          <MapPin size={11} /> {job.area}
        </p>
      )}

      {reason && (
        <div className="mt-3 flex items-center gap-2 bg-pending-tint text-pending rounded-xl px-3 py-2 text-xs font-semibold">
          <AlertTriangle size={12} />
          {reason}
        </div>
      )}

      <div className="flex items-center justify-between mt-3 pt-3 border-t border-line">
        <span className="text-[11px] text-ink-3">{timeAgo(job.createdAt)}</span>
        <span className="text-[11px] text-brand font-bold flex items-center gap-1">
          Open <ChevronRight size={12} />
        </span>
      </div>
    </button>
  );
}

// ─── Pipeline job card (kanban column) ─────────────────────────────────────
function JobCard({ job, onClick }) {
  return (
    <button
      onClick={() => onClick(job)}
      className="w-full text-left bg-paper border border-line rounded-xl p-3 space-y-2 card-hover focus-ring"
    >
      <div className="flex items-start justify-between gap-1">
        <span className="text-[10px] font-mono text-ink-3 num">#{job.id?.slice(-6).toUpperCase()}</span>
        <AppliancePill type={job.applianceType} size="sm" />
      </div>

      <div>
        <p className="text-sm font-bold text-ink leading-snug line-clamp-1">
          {job.customerName || job.customerPhone}
        </p>
        {job.area && (
          <p className="text-[11px] text-ink-3 flex items-center gap-1 mt-0.5">
            <MapPin size={9} /> {job.area}
          </p>
        )}
      </div>

      <div className="flex items-center justify-between">
        <span className="text-[10px] text-ink-3">{timeAgo(job.createdAt)}</span>
        {job.assignedTechName
          ? <span className="text-[10px] text-brand font-bold">{firstName(job.assignedTechName)}</span>
          : <span className="text-[10px] text-ink-3 italic">unassigned</span>}
      </div>

      {job.status === 'PARTS_NEEDED' && (
        <div className="flex items-center gap-1 text-[10px] text-pending bg-pending-tint rounded-lg px-2 py-1 font-semibold">
          <AlertTriangle size={9} /> parts required
        </div>
      )}
    </button>
  );
}

// ─── Pipeline column ───────────────────────────────────────────────────────
function PipelineColumn({ status, jobs = [], onJobClick }) {
  const m = statusMeta(status);
  return (
    <div className="flex flex-col h-full min-w-[230px] max-w-[260px] flex-shrink-0">
      <div className="flex items-center justify-between mb-2 px-1">
        <div className="flex items-center gap-2 min-w-0">
          <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: m.fg }} />
          <span className="text-xs font-bold text-ink truncate">{COLUMN_LABELS[status]}</span>
        </div>
        <span
          className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full"
          style={{ background: m.bg, color: m.fg }}
        >
          {jobs.length}
        </span>
      </div>

      <div className="h-px mb-2" style={{ background: m.fg, opacity: 0.4 }} />

      <div className="flex-1 space-y-2 overflow-y-auto scrollbar-hide pr-0.5">
        {jobs.length === 0 ? (
          <div className="flex items-center justify-center h-20 rounded-xl border border-dashed border-line">
            <span className="text-[11px] text-ink-3">empty</span>
          </div>
        ) : (
          jobs.map(job => <JobCard key={job.id} job={job} onClick={onJobClick} />)
        )}
      </div>
    </div>
  );
}

// ─── Detail panel (right drawer on desktop, full sheet on mobile) ──────────
function DetailPanel({ job, onClose, technicians, onAssign, onStatusChange, onCollectPayment, mutating }) {
  const [assigning, setAssigning] = useState(false);
  const [primaryTech, setPrimaryTech] = useState('');
  const [assistantTech, setAssistantTech] = useState('');
  const flow = JOB_STATUS_FLOW[job.status] || { next: [] };
  const hireTechs = technicians?.filter(t => t.status === 'HIRED' || t.approved) || [];

  const needsPayment =
    job.status === 'COMPLETED' &&
    (job.paymentStatus === 'PENDING' || !job.paymentStatus) &&
    Number(job.actualCharge || job.finalCharge || 0) > 0;

  const handleAssign = () => {
    if (!primaryTech) { toast.error('Select a primary technician first'); return; }
    onAssign(job.id, primaryTech, assistantTech || null);
    setAssigning(false);
  };

  return (
    <div className="fixed inset-0 z-40 lg:relative lg:flex-shrink-0">
      {/* Mobile backdrop */}
      <div className="absolute inset-0 bg-ink/40 backdrop-blur-sm lg:hidden" onClick={onClose} />

      <aside className="absolute right-0 top-0 bottom-0 w-full sm:w-[420px] bg-paper border-l border-line shadow-panel lg:shadow-none flex flex-col animate-slide-in lg:relative lg:w-[380px]">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-3 border-b border-line flex-shrink-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs font-mono text-ink-3 num">#{job.id?.slice(-6).toUpperCase()}</span>
            <StatusPill status={job.status} size="md" />
          </div>
          <IconButton icon={X} label="Close" onClick={onClose} />
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto scrollbar-hide p-5 space-y-5">

          {/* Customer */}
          <section>
            <p className="text-[11px] uppercase tracking-wide font-semibold text-ink-3 mb-2">Customer</p>
            <div className="bg-paper-2 border border-line rounded-2xl p-4">
              <div className="flex items-center gap-3">
                <div className="w-11 h-11 rounded-xl bg-brand-tint text-brand font-bold flex items-center justify-center flex-shrink-0">
                  {initials(job.customerName || job.customerPhone)}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-base font-bold text-ink">{job.customerName || '—'}</p>
                  <p className="text-xs font-mono text-ink-2 num">{fmtPhone(job.customerPhone)}</p>
                </div>
                <a
                  href={`tel:${job.customerPhone}`}
                  className="w-10 h-10 rounded-xl bg-money text-white flex items-center justify-center pressable"
                  aria-label="Call customer"
                >
                  <Phone size={16} />
                </a>
              </div>
              {job.address && (
                <p className="text-sm text-ink-2 mt-3 flex items-start gap-1.5 leading-snug">
                  <MapPin size={13} className="mt-0.5 flex-shrink-0 text-ink-3" /> {job.address}
                </p>
              )}
            </div>
          </section>

          {/* Job details */}
          <section>
            <p className="text-[11px] uppercase tracking-wide font-semibold text-ink-3 mb-2">Job details</p>
            <div className="bg-paper-2 border border-line rounded-2xl p-4 space-y-2.5">
              <Row label="Appliance" value={`${applianceEmoji(job.applianceType)} ${applianceLabel(job.applianceType)}`} />
              {job.description && (
                <div>
                  <p className="text-[11px] text-ink-3 mb-1">Problem</p>
                  <p className="text-sm text-ink bg-paper rounded-xl p-3 border border-line leading-snug">{job.description}</p>
                </div>
              )}
              <Row label="Received"  value={fmt(job.createdAt)} mono />
              {job.scheduledAt && <Row label="Scheduled" value={fmt(job.scheduledAt)} mono highlight />}
              {job.completedAt && <Row label="Completed" value={fmt(job.completedAt)} mono done />}
            </div>
          </section>

          {/* Financials */}
          {(job.laborCharge || job.partsCharge || job.totalAmount) && (
            <section>
              <p className="text-[11px] uppercase tracking-wide font-semibold text-ink-3 mb-2">Money</p>
              <div className="bg-paper-2 border border-line rounded-2xl p-4 space-y-2">
                {job.laborCharge != null && <Row label="Labour"  value={inr(job.laborCharge)} mono />}
                {job.partsCharge != null && <Row label="Parts"   value={inr(job.partsCharge)} mono />}
                {job.totalAmount != null && (
                  <div className="flex items-baseline justify-between gap-3 pt-2 border-t border-line">
                    <span className="text-sm font-bold text-ink">Total</span>
                    <span className="text-base font-display font-bold text-money num">{inr(job.totalAmount)}</span>
                  </div>
                )}
              </div>
            </section>
          )}

          {/* Technician */}
          <section>
            <div className="flex items-center justify-between mb-2">
              <p className="text-[11px] uppercase tracking-wide font-semibold text-ink-3">Technician</p>
              {!['COMPLETED', 'CANCELLED'].includes(job.status) && (
                <button
                  onClick={() => setAssigning(v => !v)}
                  className="text-xs text-brand font-bold hover:underline"
                >
                  {assigning ? 'Cancel' : job.assignedTechId ? 'Reassign' : 'Assign'}
                </button>
              )}
            </div>

            {assigning ? (
              <div className="bg-paper-2 border border-line rounded-2xl p-4 space-y-3">
                <FieldSelect
                  label="Primary technician"
                  required
                  placeholder="Select"
                  value={primaryTech}
                  onChange={e => setPrimaryTech(e.target.value)}
                  options={hireTechs.map(t => ({ value: t.id, label: `${t.name} — ${t.phone}` }))}
                />
                <FieldSelect
                  label="Assistant (optional)"
                  placeholder="None"
                  value={assistantTech}
                  onChange={e => setAssistantTech(e.target.value)}
                  options={hireTechs.filter(t => t.id !== primaryTech).map(t => ({ value: t.id, label: t.name }))}
                />
                <Button size="md" fullWidth onClick={handleAssign}>Confirm assignment</Button>
              </div>
            ) : job.assignedTechId ? (
              <div className="bg-paper-2 border border-line rounded-2xl p-4 flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-brand text-white font-bold flex items-center justify-center">
                  {initials(job.assignedTechName)}
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-bold text-ink">{job.assignedTechName}</p>
                  <p className="text-xs text-money flex items-center gap-1">
                    <UserCheck size={11} /> Assigned
                  </p>
                </div>
              </div>
            ) : (
              <div className="bg-paper-2 border border-dashed border-line-2 rounded-2xl p-4 flex items-center gap-2 text-ink-3">
                <User size={16} />
                <span className="text-sm">Not yet assigned</span>
              </div>
            )}
          </section>
        </div>

        {/* Footer — collect payment OR status actions */}
        {needsPayment ? (
          <div className="border-t border-line p-4 bg-money-tint/40 flex-shrink-0 space-y-2">
            <p className="text-xs font-bold text-money flex items-center gap-1.5">
              <IndianRupee size={13} /> Payment pending — {inr(job.actualCharge || job.finalCharge)}
            </p>
            <Button
              size="lg"
              variant="money"
              fullWidth
              icon={Banknote}
              onClick={() => onCollectPayment?.(job)}
            >
              Collect payment
            </Button>
            <p className="text-[11px] text-ink-3 leading-snug">
              Records cash/online · customer auto-receives WhatsApp receipt
            </p>
          </div>
        ) : flow.next.length > 0 ? (
          <div className="border-t border-line p-4 flex flex-col gap-2 flex-shrink-0">
            <p className="text-[11px] uppercase tracking-wide font-semibold text-ink-3">Move to</p>
            <div className="flex flex-wrap gap-2">
              {flow.next.map(nextStatus => {
                const m = statusMeta(nextStatus);
                const isCancel = nextStatus === 'CANCELLED';
                const isDone = nextStatus === 'COMPLETED';
                return (
                  <button
                    key={nextStatus}
                    onClick={() => onStatusChange(job.id, nextStatus)}
                    disabled={mutating}
                    className={[
                      'flex items-center gap-1.5 text-sm font-bold px-3.5 py-2.5 rounded-xl pressable focus-ring',
                      'disabled:opacity-50',
                    ].join(' ')}
                    style={{
                      background: m.bg,
                      color: m.fg,
                      border: `1px solid ${m.fg}33`,
                    }}
                  >
                    {isDone && <CheckCircle2 size={13} />}
                    {isCancel && <X size={13} />}
                    {m.label}
                    {!isDone && !isCancel && <ArrowRight size={12} />}
                  </button>
                );
              })}
            </div>
          </div>
        ) : null}
      </aside>
    </div>
  );
}

function Row({ label, value, mono, highlight, done }) {
  return (
    <div className="flex items-baseline justify-between gap-3">
      <span className="text-xs text-ink-3">{label}</span>
      <span className={[
        'text-sm text-right break-words',
        done ? 'text-money font-semibold' : highlight ? 'text-brand font-semibold' : 'text-ink',
        mono ? 'font-mono num' : '',
      ].join(' ')}>{value}</span>
    </div>
  );
}

// ─── MoneyTile — used by HomeView "Today's money" ──────────────────────────
function MoneyTile({ icon: Icon, label, value, tone = 'default' }) {
  const fg =
    tone === 'money'  ? 'text-money'
  : tone === 'brand'  ? 'text-brand'
  : tone === 'urgent' ? 'text-urgent'
  :                     'text-ink';
  return (
    <div className="text-center px-1">
      <Icon size={16} className={`mx-auto mb-1 ${fg}`} strokeWidth={2.2} />
      <p className={`text-lg font-display font-bold num ${fg}`}>{value ?? '—'}</p>
      <p className="text-[10px] uppercase tracking-wide text-ink-3 mt-0.5">{label}</p>
    </div>
  );
}

// ─── CollectPaymentModal — confirms cash/online collection on a completed job
function CollectPaymentModal({ open, job, onClose, technicians }) {
  const qc = useQueryClient();
  const defaultAmount = String(job?.actualCharge ?? job?.finalCharge ?? '');
  const [amount, setAmount] = useState(defaultAmount);
  const [method, setMethod] = useState('CASH');
  const [collectedBy, setCollectedBy] = useState(job?.assignedTechId || '');
  const [notes, setNotes] = useState('');

  // Parent re-mounts this component with a fresh key when job changes,
  // so initial state above is always correct.

  const collectMut = useMutation({
    mutationFn: () => paymentsApi.collectCash(
      job.id,
      Number(amount),
      notes || null,
      collectedBy || null,
    ),
    onSuccess: () => {
      toast.success('Payment recorded · customer gets WhatsApp receipt');
      qc.invalidateQueries({ queryKey: ['jobs'] });
      qc.invalidateQueries({ queryKey: ['payments', 'daily-summary'] });
      qc.invalidateQueries({ queryKey: ['dashboard'] });
      onClose();
    },
    onError: (err) =>
      toast.error(err.response?.data?.message || 'Could not record payment. Try again.'),
  });

  if (!open || !job) return null;

  const hireTechs = (technicians || []).filter(t => t.status === 'HIRED' || t.approved);

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`Collect payment for #${job.id?.slice(-6).toUpperCase()}`}
      subtitle={job.customerName || fmtPhone(job.customerPhone)}
      width="max-w-md"
    >
      <div className="space-y-4">
        <FieldInput
          label="Amount"
          required
          type="number"
          inputMode="numeric"
          prefix="₹"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          hint={job.actualCharge ? `Invoice: ${inr(job.actualCharge)}` : undefined}
        />

        <div>
          <p className="text-xs font-semibold text-ink-2 mb-1.5">Method</p>
          <div className="grid grid-cols-2 gap-2">
            <MethodTile
              icon={Banknote}
              label="Cash"
              hi="नकद"
              active={method === 'CASH'}
              onClick={() => setMethod('CASH')}
            />
            <MethodTile
              icon={Smartphone}
              label="Online / UPI"
              hi="ऑनलाइन"
              active={method === 'ONLINE'}
              onClick={() => setMethod('ONLINE')}
            />
          </div>
          {method === 'ONLINE' && (
            <p className="text-[11px] text-ink-3 mt-2 leading-snug">
              For online: ask the customer to pay via UPI/Razorpay link, then record it here once you confirm it landed.
            </p>
          )}
        </div>

        {hireTechs.length > 0 && (
          <FieldSelect
            label="Collected by"
            value={collectedBy}
            onChange={(e) => setCollectedBy(e.target.value)}
            placeholder="Pick technician (or leave blank for self)"
            options={hireTechs.map(t => ({ value: t.id, label: t.name }))}
            hint="Who physically took the money — for the cash kitty reconciliation."
          />
        )}

        <FieldTextArea
          label="Notes (optional)"
          rows={2}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="e.g. ₹100 discount given, customer paid via PhonePe"
        />

        <div className="flex gap-3 pt-1">
          <Button variant="outline" size="lg" fullWidth onClick={onClose}>Cancel</Button>
          <Button
            size="lg"
            fullWidth
            variant="money"
            icon={CheckCircle2}
            loading={collectMut.isPending}
            disabled={!amount || Number(amount) <= 0}
            onClick={() => collectMut.mutate()}
          >
            Record payment
          </Button>
        </div>
      </div>
    </Modal>
  );
}

function MethodTile({ icon: Icon, label, hi, active, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={[
        'flex flex-col items-center gap-1 py-3 px-3 rounded-xl border-2 transition-all pressable',
        active ? 'border-brand bg-brand-tint' : 'border-line bg-paper hover:border-line-2',
      ].join(' ')}
    >
      <Icon size={20} strokeWidth={2.2} className={active ? 'text-brand' : 'text-ink-2'} />
      <p className={`text-sm font-bold ${active ? 'text-brand-3' : 'text-ink'}`}>{label}</p>
      <p className={`hi text-[11px] ${active ? 'text-brand-3' : 'text-ink-3'}`}>{hi}</p>
    </button>
  );
}

// ─── LogCallModal — capture an incoming phone call as a lead ───────────────
// Father picks up a call. Customer asks about AC repair. Father logs it here.
// On submit:
//   1) Upserts the customer record (so they show up in Customers search)
//   2) Opens WhatsApp on father's phone, prefilled with a friendly greeting
//      that contains a booking deeplink with the customer's phone+appliance
//      pre-filled. Father reviews, taps Send. Customer gets the message.
//
// Why not auto-send via Meta Cloud API? Outbound greetings to numbers that
// haven't messaged the shop in 24h require *approved templates* (1-3 day
// Meta approval). wa.me works on every phone, today, with zero quota risk.
function LogCallModal({ open, onClose, defaultAppliance = '' }) {
  const qc = useQueryClient();
  const [phone, setPhone] = useState('');
  const [name, setName]   = useState('');
  const [appliance, setAppliance] = useState(defaultAppliance);
  const [issue, setIssue] = useState('');

  const upsertMut = useMutation({
    mutationFn: () => customersApi.upsert({
      phone,
      name: name.trim() || null,
    }),
    onError: () => { /* ignore — non-blocking; the WA send is what matters */ },
  });

  if (!open) return null;

  const phoneOk = /^[6-9]\d{9}$/.test(phone);

  const onSubmit = async () => {
    if (!phoneOk) { toast.error('Enter a valid 10-digit mobile'); return; }

    // 1. Save customer (non-blocking)
    upsertMut.mutate();

    // 2. Build pre-filled WhatsApp greeting + deeplink (with ref tracking)
    const origin = window.location.origin;
    const params = new URLSearchParams();
    if (phone)     params.set('phone', phone);
    if (name)      params.set('name', name);
    if (appliance) params.set('appliance', appliance);
    params.set('ref', 'WA_LINK');     // attribution: this booking came from the WhatsApp call-log flow
    const deeplink = `${origin}/?${params.toString()}`;

    const greeting =
`Namaste${name ? ' ' + firstName(name) + ' ji' : ''} 🙏

SK Electronics, Banda se. Aapne abhi ${appliance ? applianceLabel(appliance) : 'service'} ke baare mein call kiya tha.

Ghar baithe service book karne ke liye yeh link daba dijiye:
${deeplink}

Ya hume +91 89602 45022 par call/WhatsApp kar sakte hain.

Hum 30 minute mein contact karenge. No advance payment.
— SK Electronics, since 2010`;

    const waLink = `https://wa.me/91${phone}?text=${encodeURIComponent(greeting)}`;

    // 3. Open WhatsApp
    window.open(waLink, '_blank', 'noopener,noreferrer');

    toast.success(
      'WhatsApp opened — review and tap Send. Customer saved to your list.',
      { duration: 4000 },
    );

    qc.invalidateQueries({ queryKey: ['customer'] });
    onClose();

    // Reset for the next call (which is usually within minutes in summer)
    setPhone(''); setName(''); setAppliance(defaultAppliance); setIssue('');
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Log a customer call"
      subtitle="कॉल का रिकॉर्ड + WhatsApp greeting"
      width="max-w-md"
    >
      <div className="space-y-3.5">
        <FieldInput
          label="Customer's mobile"
          required
          type="tel"
          inputMode="numeric"
          prefix="+91"
          value={phone}
          onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
          placeholder="98765 43210"
          maxLength={10}
          autoFocus
          error={phone.length === 10 && !phoneOk ? 'Must start with 6, 7, 8 or 9' : undefined}
        />

        <FieldInput
          label="Customer name (if they told you)"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Ramesh Gupta"
        />

        <FieldSelect
          label="What's the appliance?"
          value={appliance}
          onChange={(e) => setAppliance(e.target.value)}
          placeholder="— Pick one —"
          options={APPLIANCE_TYPES.map(t => ({ value: t, label: applianceLabel(t) }))}
          hint="Goes into the WhatsApp message so the customer knows you remember."
        />

        <FieldTextArea
          label="What did they say? (optional)"
          rows={2}
          value={issue}
          onChange={(e) => setIssue(e.target.value)}
          placeholder="AC not cooling, called twice last week…"
        />

        <div className="bg-paper-3 border border-line rounded-xl p-3 text-xs text-ink-2 leading-snug">
          <p className="font-bold text-ink mb-1">After tap, WhatsApp opens with:</p>
          <p className="text-[11px] text-ink-3">
            Namaste {name ? firstName(name) + ' ji' : '{name}'} 🙏 SK Electronics, Banda se.
            Aapne abhi {appliance ? applianceLabel(appliance) : '{appliance}'} ke baare mein call kiya tha.
            <br />Ghar baithe service book karne ke liye: <span className="text-brand">{window.location.origin}/?phone=…</span>
          </p>
        </div>

        <div className="flex gap-3 pt-1">
          <Button variant="outline" size="lg" fullWidth onClick={onClose}>Cancel</Button>
          <Button
            size="lg"
            fullWidth
            icon={MessageSquare}
            onClick={onSubmit}
            disabled={!phoneOk}
          >
            Send WhatsApp
          </Button>
        </div>
      </div>
    </Modal>
  );
}

// ─── New Job Modal ─────────────────────────────────────────────────────────
function NewJobModal({ open, onClose, onCreated }) {
  const [form, setForm] = useState({
    customerName: '', customerPhone: '', address: '', area: '',
    applianceType: 'AC', description: '', scheduledAt: '',
  });

  const createMut = useMutation({
    mutationFn: () => jobsApi.create(form),
    onSuccess: () => {
      toast.success('Job created');
      onCreated();
      onClose();
      setForm({ customerName:'', customerPhone:'', address:'', area:'', applianceType:'AC', description:'', scheduledAt:'' });
    },
    onError: err => toast.error(err.response?.data?.message || 'Could not create. Please try again.'),
  });

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  return (
    <Modal open={open} onClose={onClose} title="New service job" subtitle="नया काम जोड़ें" width="max-w-md">
      <div className="space-y-3.5">
        <div className="grid grid-cols-2 gap-3">
          <FieldInput
            label="Customer name"
            value={form.customerName}
            onChange={e => set('customerName', e.target.value)}
            placeholder="Ramesh Gupta"
          />
          <FieldInput
            label="Phone"
            required
            prefix="+91"
            value={form.customerPhone}
            onChange={e => set('customerPhone', e.target.value.replace(/\D/g,'').slice(0, 10))}
            placeholder="98765 43210"
            maxLength={10}
          />
        </div>

        <FieldInput
          label="Address"
          value={form.address}
          onChange={e => set('address', e.target.value)}
          placeholder="House no, street, landmark"
        />

        <div className="grid grid-cols-2 gap-3">
          <FieldInput
            label="Area / Mohalla"
            value={form.area}
            onChange={e => set('area', e.target.value)}
            placeholder="Civil Lines"
          />
          <FieldSelect
            label="Appliance"
            value={form.applianceType}
            onChange={e => set('applianceType', e.target.value)}
            options={APPLIANCE_TYPES.map(t => ({ value: t, label: t }))}
          />
        </div>

        <FieldTextArea
          label="Problem description"
          rows={3}
          value={form.description}
          onChange={e => set('description', e.target.value)}
          placeholder="Not cooling / noisy compressor / not starting…"
        />

        <FieldInput
          label="Schedule (optional)"
          type="datetime-local"
          value={form.scheduledAt}
          onChange={e => set('scheduledAt', e.target.value)}
        />

        <div className="flex gap-3 pt-2">
          <Button variant="outline" size="lg" fullWidth onClick={onClose}>Cancel</Button>
          <Button
            size="lg"
            fullWidth
            icon={Plus}
            loading={createMut.isPending}
            disabled={!form.customerPhone || form.customerPhone.length < 10}
            onClick={() => createMut.mutate()}
          >
            Create job
          </Button>
        </div>
      </div>
    </Modal>
  );
}

// ─── HOME VIEW — father's morning glance ───────────────────────────────────
function HomeView({ adminName = 'Sushil', dashboard, dashLoading, jobs, technicians, onJobClick, onNewJob, onLogCall }) {
  const today      = new Date().toDateString();
  const jobsToday  = jobs.filter(j => new Date(j.createdAt).toDateString() === today);
  const unassigned = jobs.filter(j => !j.assignedTechId && !['COMPLETED','CANCELLED'].includes(j.status));
  const partsWait  = jobs.filter(j => j.status === 'PARTS_NEEDED');
  const inFlight   = jobs.filter(j => ['IN_TRANSIT','AT_CUSTOMER','IN_PROGRESS'].includes(j.status));
  const activeTechs = technicians.filter(t => t.approved || t.status === 'HIRED').length;

  // Pending payments — completed today with money still uncollected
  const pendingPaymentJobs = jobs.filter(j =>
    j.status === 'COMPLETED' &&
    (j.paymentStatus === 'PENDING' || !j.paymentStatus) &&
    Number(j.actualCharge || j.finalCharge || 0) > 0,
  );
  const pendingAmount = pendingPaymentJobs.reduce(
    (s, j) => s + Number(j.actualCharge || j.finalCharge || 0), 0);

  // Today's money — fetched live from payment-service
  const { data: daySummary } = useQuery({
    queryKey: ['payments', 'daily-summary'],
    queryFn: () => paymentsApi.dailySummary().then(r => r.data?.data || null).catch(() => null),
    staleTime: 30_000,
  });

  const hour = new Date().getHours();
  const greet = hour < 5 ? 'Good night'
              : hour < 12 ? 'Good morning'
              : hour < 17 ? 'Good afternoon'
              : hour < 21 ? 'Good evening' : 'Good night';
  const greetHi = hour < 12 ? 'नमस्ते' : hour < 17 ? 'नमस्कार' : 'शुभ संध्या';

  return (
    <div className="flex-1 overflow-y-auto scrollbar-hide pb-28 lg:pb-6">
      <div className="px-5 pt-5 lg:px-8 lg:pt-6 max-w-3xl mx-auto">

        {/* Greeting */}
        <div className="mb-5">
          <p className="hi text-sm text-brand font-bold">{greetHi}</p>
          <h2 className="text-2xl lg:text-3xl font-display font-extrabold text-ink leading-tight">
            {greet}, {adminName} ji
          </h2>
          <p className="text-sm text-ink-3 mt-1">
            {new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long' })}
          </p>
        </div>

        {/* Hero KPI grid */}
        <div className="grid grid-cols-2 gap-3">
          <StatCard
            tone="money"
            label="Revenue today"
            hindi="आज की कमाई"
            value={inrShort(dashboard?.revenueToday)}
            sub={dashboard?.revenueMtd ? `${inrShort(dashboard.revenueMtd)} this month` : undefined}
            loading={dashLoading}
            icon={IndianRupee}
          />
          <StatCard
            tone="brand"
            label="Jobs today"
            hindi="आज के काम"
            value={jobsToday.length}
            sub={inFlight.length > 0 ? `${inFlight.length} active right now` : 'All done so far'}
            loading={dashLoading}
            icon={Wrench}
          />
          <StatCard
            tone={unassigned.length > 0 ? 'pending' : 'default'}
            label="Need to assign"
            hindi="बकाया"
            value={unassigned.length}
            sub={unassigned.length === 0 ? 'All caught up' : 'Tap to assign'}
            icon={AlertTriangle}
          />
          <StatCard
            tone="default"
            label="Active techs"
            hindi="तकनीशियन"
            value={activeTechs}
            sub={`${technicians.length} total`}
            icon={Users}
          />
        </div>

        {/* Quick actions — log a call (most frequent) + new job ─────── */}
        <div className="grid grid-cols-2 gap-3 mt-4">
          <Button
            size="xl"
            variant="money"
            icon={PhoneIncoming}
            onClick={onLogCall}
          >
            Log call
            <span className="hi font-medium text-white/80 text-xs ml-1">· कॉल</span>
          </Button>
          <Button
            size="xl"
            icon={Plus}
            onClick={onNewJob}
          >
            New job
            <span className="hi font-medium text-white/80 text-xs ml-1">· काम</span>
          </Button>
        </div>
        <p className="text-[11px] text-ink-3 text-center mt-2 leading-snug">
          Tap <span className="font-bold text-money">Log call</span> right after a customer call —
          they get a WhatsApp with a booking link.
        </p>

        {/* Today's money — end-of-day at-a-glance ─────────────────────── */}
        <section className="mt-7">
          <div className="flex items-baseline justify-between mb-2 px-1">
            <h3 className="text-base font-bold text-ink">Today's money</h3>
            <span className="hi text-xs text-ink-3">आज का हिसाब</span>
          </div>
          <div className="bg-paper border border-line rounded-2xl p-4 shadow-card">
            <div className="grid grid-cols-3 gap-3">
              <MoneyTile
                icon={Banknote}
                label="Cash"
                value={inrShort(daySummary?.cashRevenue)}
                tone="money"
              />
              <MoneyTile
                icon={Smartphone}
                label="Online"
                value={inrShort(daySummary?.onlineRevenue)}
                tone="brand"
              />
              <MoneyTile
                icon={IndianRupee}
                label="Total"
                value={inrShort(daySummary?.totalRevenue ?? dashboard?.revenueToday)}
                tone="default"
              />
            </div>
            {pendingAmount > 0 && (
              <button
                onClick={() => onJobClick?.(pendingPaymentJobs[0])}
                className="mt-3 w-full flex items-center justify-between bg-urgent-tint border border-urgent/20 rounded-xl px-3.5 py-2.5 text-left pressable"
              >
                <div className="flex items-center gap-2 min-w-0">
                  <ClipboardCheck size={16} className="text-urgent flex-shrink-0" />
                  <div className="min-w-0">
                    <p className="text-sm font-bold text-urgent">
                      {pendingPaymentJobs.length} payment{pendingPaymentJobs.length > 1 ? 's' : ''} to collect
                    </p>
                    <p className="text-xs text-urgent/80">
                      <span className="font-mono num">{inr(pendingAmount)}</span> still pending
                    </p>
                  </div>
                </div>
                <ChevronRight size={16} className="text-urgent flex-shrink-0" />
              </button>
            )}
            {(daySummary?.cashRevenue || daySummary?.onlineRevenue) && (
              <p className="text-[11px] text-ink-3 mt-2.5 text-center">
                Updates as payments are recorded. Cash should match physical kitty at close.
              </p>
            )}
          </div>
        </section>

        {/* Attention — unassigned jobs */}
        {unassigned.length > 0 && (
          <section className="mt-7">
            <div className="flex items-baseline justify-between mb-2 px-1">
              <h3 className="text-base font-bold text-ink">
                Needs assignment
                <span className="text-pending ml-2 num">{unassigned.length}</span>
              </h3>
              <span className="hi text-xs text-ink-3">तुरंत भेजें</span>
            </div>
            <div className="space-y-2">
              {unassigned.slice(0, 4).map(j => (
                <AttentionCard
                  key={j.id}
                  job={j}
                  onOpen={onJobClick}
                  reason={`Pending for ${timeAgo(j.createdAt)}`}
                />
              ))}
            </div>
          </section>
        )}

        {/* Parts needed alerts */}
        {partsWait.length > 0 && (
          <section className="mt-6">
            <div className="flex items-baseline justify-between mb-2 px-1">
              <h3 className="text-base font-bold text-ink">
                Parts needed
                <span className="text-brand ml-2 num">{partsWait.length}</span>
              </h3>
              <span className="hi text-xs text-ink-3">पुर्जे चाहिए</span>
            </div>
            <div className="space-y-2">
              {partsWait.slice(0, 4).map(j => (
                <AttentionCard
                  key={j.id}
                  job={j}
                  onOpen={onJobClick}
                  reason="Technician waiting for parts"
                />
              ))}
            </div>
          </section>
        )}

        {/* In-flight today */}
        {inFlight.length > 0 && (
          <section className="mt-6">
            <div className="flex items-baseline justify-between mb-2 px-1">
              <h3 className="text-base font-bold text-ink">
                Happening now
                <span className="text-ink-3 ml-2 num">{inFlight.length}</span>
              </h3>
              <span className="hi text-xs text-ink-3">चल रहा है</span>
            </div>
            <div className="space-y-2">
              {inFlight.slice(0, 5).map(j => (
                <AttentionCard key={j.id} job={j} onOpen={onJobClick} />
              ))}
            </div>
          </section>
        )}

        {/* Empty hero — when nothing pending */}
        {unassigned.length === 0 && partsWait.length === 0 && inFlight.length === 0 && jobsToday.length === 0 && (
          <div className="mt-6">
            <EmptyState
              icon={Star}
              tone="money"
              title="Quiet morning"
              hint="No active work. When customers book new repairs, they'll show up here for you to assign."
              action={<Button size="md" icon={Plus} onClick={onNewJob}>Add a job manually</Button>}
            />
          </div>
        )}
      </div>
    </div>
  );
}

// ─── PIPELINE VIEW — full kanban ───────────────────────────────────────────
function PipelineView({ jobs, loading, onJobClick }) {
  const grouped = PIPELINE_COLUMNS.reduce((acc, s) => {
    acc[s] = jobs.filter(j => j.status === s);
    return acc;
  }, {});

  if (loading) {
    return (
      <div className="flex gap-3 h-full px-4 py-3 overflow-x-auto scrollbar-hide pb-28 lg:pb-3">
        {PIPELINE_COLUMNS.map(s => (
          <div key={s} className="min-w-[230px] space-y-2">
            <div className="skeleton h-4 w-24 mb-2" />
            <JobCardSkeleton />
            <JobCardSkeleton />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="flex gap-3 h-full px-4 py-3 overflow-x-auto scrollbar-hide pb-28 lg:pb-3">
      {PIPELINE_COLUMNS.map(s => (
        <PipelineColumn key={s} status={s} jobs={grouped[s]} onJobClick={onJobClick} />
      ))}
    </div>
  );
}

// ─── ANALYTICS VIEW ────────────────────────────────────────────────────────
function AnalyticsView({ dashboard }) {
  const { data: trend }   = useQuery({
    queryKey: ['job-trend'],
    queryFn: () => analyticsApi.jobTrend(7).then(r => r.data?.data),
    staleTime: 60_000,
  });
  const { data: revenue } = useQuery({
    queryKey: ['revenue-breakdown'],
    queryFn: () => analyticsApi.revenueBreakdown().then(r => r.data?.data),
    staleTime: 60_000,
  });

  const d = dashboard || {};

  return (
    <div className="flex-1 overflow-y-auto scrollbar-hide p-5 lg:p-8 pb-28 lg:pb-8 space-y-5 max-w-4xl mx-auto w-full">

      <div>
        <h2 className="text-xl font-display font-bold text-ink">Business insights</h2>
        <p className="hi text-sm text-ink-3 mt-0.5">व्यापार का हाल</p>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <StatCard tone="money" label="Revenue today"  hindi="आज की कमाई"   value={inr(d.revenueToday)}    icon={IndianRupee} />
        <StatCard tone="brand" label="Revenue MTD"    hindi="इस महीने"      value={inr(d.revenueMtd)}      icon={TrendingUp} />
        <StatCard                label="Jobs today"     hindi="आज के काम"     value={d.jobsToday ?? '—'}     icon={Wrench} />
        <StatCard                label="Active techs"   hindi="तकनीशियन"      value={d.activeTechnicians ?? '—'} icon={Users} />
      </div>

      {trend?.entries?.length > 0 && (
        <div className="bg-paper border border-line rounded-2xl p-5 shadow-card">
          <h3 className="text-sm font-bold text-ink mb-4">7-day job trend</h3>
          <div className="flex items-end gap-2 h-28">
            {trend.entries.map((e, i) => {
              const max = Math.max(...trend.entries.map(x => x.totalJobs || 0), 1);
              const h = Math.max(6, ((e.totalJobs || 0) / max) * 100);
              return (
                <div key={i} className="flex-1 flex flex-col items-center gap-1.5">
                  <span className="text-[10px] text-ink-3 font-mono num">{e.totalJobs || 0}</span>
                  <div
                    className="w-full rounded-t-md transition-all bg-brand"
                    style={{ height: `${h}%`, opacity: 0.55 + (i / trend.entries.length) * 0.45 }}
                  />
                  <span className="text-[10px] text-ink-3">{e.date?.slice(5)}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {revenue?.byApplianceType && (
        <div className="bg-paper border border-line rounded-2xl p-5 shadow-card">
          <h3 className="text-sm font-bold text-ink mb-4">Revenue by appliance</h3>
          <div className="space-y-2.5">
            {Object.entries(revenue.byApplianceType).map(([type, amount]) => {
              const max = Math.max(...Object.values(revenue.byApplianceType));
              const pct = max > 0 ? (amount / max) * 100 : 0;
              return (
                <div key={type} className="flex items-center gap-3">
                  <span className="text-xs font-bold text-ink w-20 flex-shrink-0">{applianceLabel(type)}</span>
                  <div className="flex-1 bg-paper-3 rounded-full h-2 overflow-hidden">
                    <div className="h-full rounded-full bg-brand transition-all" style={{ width: `${pct}%` }} />
                  </div>
                  <span className="text-xs font-mono text-ink-2 w-24 text-right flex-shrink-0 num">{inr(amount)}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── TECHNICIANS VIEW ──────────────────────────────────────────────────────
function TechniciansView({ technicians = [], loading }) {
  return (
    <div className="flex-1 overflow-y-auto scrollbar-hide p-5 lg:p-8 pb-28 lg:pb-8 max-w-4xl mx-auto w-full">
      <div className="mb-4">
        <h2 className="text-xl font-display font-bold text-ink">Technicians</h2>
        <p className="hi text-sm text-ink-3 mt-0.5">तकनीशियन — {technicians.length} total</p>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="bg-paper border border-line rounded-2xl p-4 space-y-3 shadow-card">
              <div className="skeleton h-10 w-10 rounded-xl" />
              <div className="skeleton h-4 w-28" />
              <div className="skeleton h-3 w-20" />
            </div>
          ))}
        </div>
      ) : technicians.length === 0 ? (
        <EmptyState
          icon={Users}
          title="No technicians yet"
          hint="Add hired technicians from the admin tools so you can assign jobs."
        />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {technicians.map(t => (
            <div key={t.id} className="bg-paper border border-line rounded-2xl p-4 shadow-card card-hover">
              <div className="flex items-start justify-between mb-3">
                <div className="w-11 h-11 rounded-xl bg-brand text-white font-bold flex items-center justify-center">
                  {initials(t.name)}
                </div>
                <span className={[
                  'text-[10px] font-bold px-2 py-1 rounded-full',
                  t.approved ? 'bg-money-tint text-money' : 'bg-pending-tint text-pending',
                ].join(' ')}>
                  {t.approved ? 'Active' : 'Pending'}
                </span>
              </div>
              <p className="text-base font-bold text-ink">{t.name}</p>
              <p className="text-xs text-ink-3 font-mono mt-0.5 num">{fmtPhone(t.phone)}</p>
              {t.specializations?.length > 0 && (
                <div className="flex flex-wrap gap-1 mt-3">
                  {t.specializations.slice(0, 4).map(s => (
                    <AppliancePill key={s} type={s} size="sm" />
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── PARTS VIEW ────────────────────────────────────────────────────────────
function PartsView() {
  const { data: lowStock = [], isLoading } = useQuery({
    queryKey: ['parts-low-stock'],
    queryFn: () => partsApi.lowStock().then(r => r.data?.data || []),
    staleTime: 60_000,
  });

  return (
    <div className="flex-1 overflow-y-auto scrollbar-hide p-5 lg:p-8 pb-28 lg:pb-8 max-w-3xl mx-auto w-full">
      <div className="flex items-baseline justify-between mb-4">
        <div>
          <h2 className="text-xl font-display font-bold text-ink">Low stock</h2>
          <p className="hi text-sm text-ink-3 mt-0.5">कम पुर्जे — {lowStock.length} items</p>
        </div>
        <span className={[
          'text-xs font-bold px-2.5 py-1 rounded-full',
          lowStock.length > 0 ? 'bg-pending-tint text-pending' : 'bg-money-tint text-money',
        ].join(' ')}>
          {lowStock.length}
        </span>
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[...Array(5)].map((_, i) => <div key={i} className="skeleton h-16 rounded-xl" />)}
        </div>
      ) : lowStock.length === 0 ? (
        <EmptyState
          icon={CheckCircle2}
          tone="money"
          title="All parts well stocked"
          hint="No items below their reorder threshold. Good shape for today."
        />
      ) : (
        <div className="space-y-2">
          {lowStock.map(p => (
            <div key={p.id} className="flex items-center justify-between bg-paper border border-line rounded-2xl px-4 py-3 shadow-card card-hover">
              <div className="min-w-0 flex-1">
                <p className="text-base font-bold text-ink truncate">{p.name}</p>
                <p className="text-xs text-ink-3 font-mono mt-0.5 num">{p.partCode || p.id?.slice(-8)}</p>
              </div>
              <div className="text-right ml-3">
                <p className="text-xl font-display font-bold text-pending num">{p.currentStock}</p>
                <p className="text-[10px] text-ink-3 leading-tight">in stock</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Main ──────────────────────────────────────────────────────────────────
export default function ServiceOS() {
  const { user, logout } = useAuth();
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState('home');
  const [selectedJob, setSelectedJob] = useState(null);
  const [showNewJob, setShowNewJob] = useState(false);
  const [showLogCall, setShowLogCall] = useState(false);
  const [payJob, setPayJob] = useState(null);   // job currently in the collect-payment modal
  const [search, setSearch] = useState('');

  // ── Queries ────────────────────────────────────────────────────────────
  const { data: jobsData, isLoading: jobsLoading } = useQuery({
    queryKey: ['jobs'],
    queryFn: () => jobsApi.list().then(r => r.data?.data?.content || r.data?.data || []),
    refetchInterval: 30_000,
    staleTime: 10_000,
  });

  const { data: dashboard, isLoading: dashLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => analyticsApi.dashboard().then(r => r.data?.data),
    staleTime: 60_000,
    retry: 1,
  });

  const { data: technicians = [], isLoading: techLoading } = useQuery({
    queryKey: ['technicians'],
    queryFn: () => techApi.list().then(r => r.data?.data?.content || r.data?.data || []),
    staleTime: 60_000,
  });

  const { data: shopName } = useQuery({
    queryKey: ['config', 'shop.name'],
    queryFn: () => configApi.get('shop.name'),
    staleTime: 5 * 60_000,
    retry: 1,
  });

  // ── Mutations ─────────────────────────────────────────────────────────
  const assignMut = useMutation({
    mutationFn: ({ jobId, primaryTechId, assistantTechId }) =>
      jobsApi.assign(jobId, primaryTechId, assistantTechId),
    onSuccess: (res) => {
      toast.success('Technician assigned');
      qc.invalidateQueries({ queryKey: ['jobs'] });
      setSelectedJob(res.data?.data || null);
    },
    onError: err => toast.error(err.response?.data?.message || 'Could not assign. Try again.'),
  });

  const statusMut = useMutation({
    mutationFn: ({ jobId, status }) => jobsApi.updateStatus(jobId, status),
    onSuccess: (res) => {
      toast.success('Status updated');
      qc.invalidateQueries({ queryKey: ['jobs'] });
      const updated = res.data?.data;
      if (updated) setSelectedJob(updated);
    },
    onError: err => toast.error(err.response?.data?.message || 'Could not update. Try again.'),
  });

  // ── Search filter ─────────────────────────────────────────────────────
  const jobs = (jobsData || []).filter(j => {
    if (!search) return true;
    const q = search.toLowerCase();
    return (
      j.customerName?.toLowerCase().includes(q) ||
      j.customerPhone?.includes(q) ||
      j.area?.toLowerCase().includes(q) ||
      j.applianceType?.toLowerCase().includes(q)
    );
  });

  const handleAssign       = useCallback((jobId, primaryTechId, assistantTechId) => assignMut.mutate({ jobId, primaryTechId, assistantTechId }), [assignMut]);
  const handleStatusChange = useCallback((jobId, status) => statusMut.mutate({ jobId, status }), [statusMut]);

  const stats = {
    total:     jobs.length,
    active:    jobs.filter(j => !['COMPLETED','CANCELLED'].includes(j.status)).length,
    completed: jobs.filter(j => j.status === 'COMPLETED').length,
    partsWait: jobs.filter(j => j.status === 'PARTS_NEEDED').length,
  };
  const adminName = firstName(user?.name) || 'Sushil';

  return (
    <div className="flex flex-col lg:flex-row h-dvh bg-canvas overflow-hidden">

      {/* ── Desktop sidebar nav ────────────────────────────────────────── */}
      <aside className="hidden lg:flex flex-col w-[230px] bg-paper border-r border-line flex-shrink-0">
        <div className="px-5 py-5 border-b border-line flex items-center gap-3">
          <ShopMark size={40} />
          <div className="min-w-0">
            <p className="text-base font-display font-extrabold text-ink leading-tight truncate">
              {shopName || 'SK Electronics'}
            </p>
            <p className="text-[11px] text-ink-3 mt-0.5">Banda · Admin</p>
          </div>
        </div>

        <nav className="flex-1 p-3 space-y-1">
          {TABS.map(({ id, icon: Icon, label, hi }) => {
            const active = activeTab === id;
            return (
              <button
                key={id}
                onClick={() => setActiveTab(id)}
                className={[
                  'w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-bold transition-all pressable focus-ring',
                  active
                    ? 'bg-brand-tint text-brand-3'
                    : 'text-ink-2 hover:bg-paper-2',
                ].join(' ')}
              >
                <Icon size={18} strokeWidth={active ? 2.4 : 2} />
                <span>{label}</span>
                <span className="hi text-[11px] text-ink-3 ml-auto font-medium">{hi}</span>
              </button>
            );
          })}
        </nav>

        <div className="p-3 border-t border-line">
          <button
            onClick={logout}
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-semibold text-ink-3 hover:text-urgent hover:bg-urgent-tint transition-all"
          >
            <LogOut size={16} /> Sign out
          </button>
        </div>
      </aside>

      {/* ── Main area ──────────────────────────────────────────────────── */}
      <div className="flex-1 flex flex-col overflow-hidden">

        {/* ── Mobile header ─────────────────────────────────────────── */}
        <header className="lg:hidden flex items-center gap-3 px-5 pt-safe pt-4 pb-3 bg-paper border-b border-line flex-shrink-0">
          <ShopMark size={36} />
          <div className="min-w-0 flex-1">
            <p className="text-base font-display font-extrabold text-ink leading-none truncate">
              {shopName || 'SK Electronics'}
            </p>
            <p className="text-[11px] text-ink-3 mt-1">Banda · Admin</p>
          </div>
          <IconButton
            icon={RefreshCw}
            label="Refresh"
            onClick={() => qc.invalidateQueries()}
            variant="ghost"
          />
          {stats.partsWait > 0 && (
            <button
              onClick={() => setActiveTab('home')}
              className="relative w-11 h-11 flex items-center justify-center rounded-xl text-pending bg-pending-tint pressable"
              aria-label="Parts needed"
            >
              <Bell size={18} />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-pending rounded-full" />
            </button>
          )}
        </header>

        {/* ── Desktop top bar (pipeline tab only) ───────────────────── */}
        {activeTab === 'pipeline' && (
          <div className="hidden lg:flex items-center gap-3 px-6 py-3 border-b border-line bg-paper flex-shrink-0">
            <h1 className="text-lg font-display font-bold text-ink">Pipeline</h1>
            <span className="text-xs font-mono text-ink-3 bg-paper-3 px-2 py-1 rounded-full num">
              {stats.active} active · {stats.total} total
            </span>

            <div className="flex items-center gap-2 bg-paper-2 border border-line rounded-xl px-3 py-2 w-64 ml-3 focus-within:border-brand transition-all">
              <Search size={14} className="text-ink-3 flex-shrink-0" />
              <input
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="Search by name, phone, area…"
                className="flex-1 bg-transparent text-sm text-ink placeholder:text-ink-4 outline-none"
              />
              {search && (
                <button onClick={() => setSearch('')} className="text-ink-3 hover:text-ink">
                  <X size={12} />
                </button>
              )}
            </div>

            <div className="ml-auto flex items-center gap-2">
              {!dashLoading && dashboard && (
                <div className="hidden xl:flex items-center gap-3 text-xs">
                  <span className="text-ink-3">Today</span>
                  <span className="font-mono font-bold text-money num">{inr(dashboard.revenueToday)}</span>
                  <span className="text-line-2">/</span>
                  <span className="text-ink-3">MTD</span>
                  <span className="font-mono font-bold text-ink num">{inr(dashboard.revenueMtd)}</span>
                </div>
              )}
              <Button size="sm" icon={Plus} onClick={() => setShowNewJob(true)}>New job</Button>
            </div>
          </div>
        )}

        {/* ── Content + Detail panel ────────────────────────────────── */}
        <div className="flex flex-1 overflow-hidden relative">
          <div className="flex-1 flex flex-col overflow-hidden">
            {activeTab === 'home' && (
              <HomeView
                adminName={adminName}
                dashboard={dashboard}
                dashLoading={dashLoading}
                jobs={jobs}
                technicians={technicians}
                onJobClick={setSelectedJob}
                onNewJob={() => setShowNewJob(true)}
                onLogCall={() => setShowLogCall(true)}
              />
            )}
            {activeTab === 'customers'   && <CustomersView   recentJobs={jobs} onJobClick={setSelectedJob} />}
            {activeTab === 'pipeline'    && <PipelineView    jobs={jobs} loading={jobsLoading} onJobClick={setSelectedJob} />}
            {activeTab === 'analytics'   && <AnalyticsView   dashboard={dashboard} />}
            {activeTab === 'technicians' && <TechniciansView technicians={technicians} loading={techLoading} />}
            {activeTab === 'parts'       && <PartsView />}
          </div>

          {selectedJob && (
            <DetailPanel
              job={selectedJob}
              onClose={() => setSelectedJob(null)}
              technicians={technicians}
              onAssign={handleAssign}
              onStatusChange={handleStatusChange}
              onCollectPayment={(job) => setPayJob(job)}
              mutating={assignMut.isPending || statusMut.isPending}
            />
          )}
        </div>

        {/* ── Mobile FAB — New Job (home tab only) ──────────────────── */}
        {activeTab === 'home' && (
          <button
            onClick={() => setShowNewJob(true)}
            className="lg:hidden fixed right-5 bottom-24 w-14 h-14 rounded-2xl bg-brand text-white shadow-brand pressable focus-ring flex items-center justify-center z-30"
            aria-label="New job"
          >
            <Plus size={24} strokeWidth={2.4} />
          </button>
        )}

        {/* ── Mobile bottom nav — 5 most-used tabs ────────────────────── */}
        <nav className="lg:hidden flex items-stretch border-t border-line bg-paper px-1 pt-1 pb-safe-3 flex-shrink-0 z-20">
          {MOBILE_TABS.map(({ id, icon: Icon, label }) => {
            const active = activeTab === id;
            return (
              <button
                key={id}
                onClick={() => setActiveTab(id)}
                className={[
                  'flex-1 flex flex-col items-center justify-center gap-0.5 py-2 rounded-xl transition-all',
                  active ? 'text-brand' : 'text-ink-3',
                ].join(' ')}
              >
                <Icon size={20} strokeWidth={active ? 2.4 : 2} />
                <span className={`text-[10px] font-bold ${active ? '' : 'text-ink-3'}`}>{label}</span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* ── New job modal ────────────────────────────────────────── */}
      <NewJobModal
        open={showNewJob}
        onClose={() => setShowNewJob(false)}
        onCreated={() => qc.invalidateQueries({ queryKey: ['jobs'] })}
      />

      {/* ── Collect payment modal — keyed by job.id so state resets cleanly */}
      {payJob && (
        <CollectPaymentModal
          key={payJob.id}
          open
          job={payJob}
          onClose={() => setPayJob(null)}
          technicians={technicians}
        />
      )}

      {/* ── Log call modal — used right after father picks up a customer call */}
      <LogCallModal
        open={showLogCall}
        onClose={() => setShowLogCall(false)}
      />
    </div>
  );
}
