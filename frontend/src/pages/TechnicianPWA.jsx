// TechnicianPWA — Rajan in 42°C sun, sweaty thumb, one-handed.
// Single hero card for the active job. Big buttons. High contrast. No glass.

import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import {
  MapPin, Phone, Navigation, Package, CheckCircle2,
  LogOut, X, ChevronRight, Wrench, Star,
} from 'lucide-react';

import { useAuth } from '../context/AuthContext';
import { jobsApi, JOB_STATUS_FLOW } from '../api/jobs';
import { configApi } from '../api/config';
import { StatusPill, AppliancePill } from '../components/ui/StatusPill';
import { JobCardSkeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { Button, IconButton } from '../components/ui/Button';
import {
  timeAgo, fmt, applianceEmoji, applianceLabel,
  statusMeta, phone as fmtPhone, inr, firstName,
} from '../utils/formatters';

// ─── Progress trail ────────────────────────────────────────────────────────
const TRAIL = ['ASSIGNED', 'IN_TRANSIT', 'AT_CUSTOMER', 'IN_PROGRESS', 'COMPLETED'];
const TRAIL_LABEL = {
  ASSIGNED:    'Got the job',
  IN_TRANSIT:  'On the way',
  AT_CUSTOMER: 'Reached',
  IN_PROGRESS: 'Fixing',
  COMPLETED:   'Done',
};

function Trail({ currentStatus }) {
  const idx = TRAIL.indexOf(currentStatus);
  return (
    <div className="flex items-stretch gap-1">
      {TRAIL.map((s, i) => {
        const done    = i <= idx;
        const current = i === idx;
        return (
          <div key={s} className="flex-1">
            <div
              className={[
                'h-1.5 rounded-full transition-all',
                done    ? 'bg-brand' : 'bg-line-2',
                current ? 'shadow-focus' : '',
              ].join(' ')}
            />
            <p className={[
              'text-[10px] text-center mt-1.5 font-semibold leading-tight',
              done ? 'text-brand' : 'text-ink-3',
            ].join(' ')}>{TRAIL_LABEL[s]}</p>
          </div>
        );
      })}
    </div>
  );
}

// ─── Hero card (active job) ────────────────────────────────────────────────
function HeroJob({ job, onOpen }) {
  return (
    <button
      onClick={() => onOpen(job)}
      className="w-full text-left bg-paper border-2 border-brand/30 rounded-2xl p-5 shadow-card-2 active:scale-[0.99] transition-transform"
    >
      <div className="flex items-start justify-between gap-2 mb-3">
        <div className="flex items-center gap-2 flex-wrap">
          <AppliancePill type={job.applianceType} size="md" />
          <StatusPill status={job.status} size="md" />
        </div>
        <span className="text-xs font-mono text-ink-3 num">#{job.id?.slice(-6).toUpperCase()}</span>
      </div>

      <p className="text-xl font-display font-bold text-ink leading-snug">
        {job.customerName || job.customerPhone}
      </p>
      {job.address && (
        <p className="text-sm text-ink-2 mt-1 flex items-start gap-1.5 leading-snug">
          <MapPin size={14} className="mt-0.5 flex-shrink-0 text-ink-3" /> {job.address}
        </p>
      )}

      {!['COMPLETED', 'CANCELLED', 'REQUESTED'].includes(job.status) && (
        <div className="mt-4 pt-4 border-t border-line">
          <Trail currentStatus={job.status} />
        </div>
      )}

      <div className="flex items-center justify-between mt-4 pt-3 border-t border-line">
        <span className="text-xs text-ink-3">{timeAgo(job.createdAt)}</span>
        <span className="text-xs text-brand font-bold flex items-center gap-1">
          Open <ChevronRight size={12} />
        </span>
      </div>
    </button>
  );
}

// ─── Compact row (other jobs) ──────────────────────────────────────────────
function JobRow({ job, onOpen }) {
  return (
    <button
      onClick={() => onOpen(job)}
      className="w-full text-left bg-paper border border-line rounded-2xl p-4 active:scale-[0.99] transition-transform"
    >
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0 flex-1">
          <span className="text-2xl flex-shrink-0">{applianceEmoji(job.applianceType)}</span>
          <div className="min-w-0">
            <p className="text-base font-bold text-ink truncate">{job.customerName || job.customerPhone}</p>
            <p className="text-xs text-ink-3 truncate">
              {job.address || fmtPhone(job.customerPhone)}
            </p>
          </div>
        </div>
        <StatusPill status={job.status} size="sm" />
      </div>
    </button>
  );
}

// ─── Bottom-sheet detail / actions ─────────────────────────────────────────
function JobSheet({ job, onClose, onStatusChange, mutating }) {
  const flow = JOB_STATUS_FLOW[job.status] || { next: [] };
  const [showReason, setShowReason] = useState(false);
  const [reason, setReason] = useState('');
  const [pendingStatus, setPendingStatus] = useState(null);

  const { data: shopPhone = '8960245022' } = useQuery({
    queryKey: ['config', 'shop.phone'],
    queryFn: () => configApi.get('shop.phone'),
    staleTime: 5 * 60_000,
    retry: 1,
  });

  const handleStatus = (s) => {
    if (s === 'CANCELLED' || s === 'PARTS_NEEDED') {
      setPendingStatus(s);
      setShowReason(true);
    } else {
      onStatusChange(job.id, s, null);
    }
  };

  const confirmWithReason = () => {
    onStatusChange(job.id, pendingStatus, reason);
    setShowReason(false);
    setReason('');
    setPendingStatus(null);
  };

  const waPartsLink = `https://wa.me/91${shopPhone}?text=${encodeURIComponent(
    `Parts needed — Job #${job.id?.slice(-6).toUpperCase()}\n${applianceLabel(job.applianceType)}\nCustomer: ${job.customerName || job.customerPhone}\nAddress: ${job.address || ''}`,
  )}`;

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end">
      <div className="absolute inset-0 bg-ink/40 backdrop-blur-sm" onClick={onClose} />

      <div className="relative bg-paper rounded-t-2xl max-h-[92dvh] flex flex-col shadow-sheet animate-slide-up pb-safe">
        <div className="w-12 h-1.5 bg-line-2 rounded-full mx-auto mt-3 flex-shrink-0" />

        {/* Header */}
        <div className="flex items-center justify-between px-5 pt-3 pb-3 border-b border-line flex-shrink-0">
          <div className="flex items-center gap-2">
            <span className="text-sm font-mono text-ink-3 num">#{job.id?.slice(-6).toUpperCase()}</span>
            <StatusPill status={job.status} size="md" />
          </div>
          <IconButton icon={X} label="Close" onClick={onClose} />
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto scrollbar-hide p-5 space-y-4">

          {/* Customer block — name + tap-to-call + maps */}
          <div className="bg-paper-2 border border-line rounded-2xl p-4">
            <p className="text-base font-bold text-ink">{job.customerName || '—'}</p>
            <p className="text-sm font-mono text-ink-2 mt-0.5 num">{fmtPhone(job.customerPhone)}</p>
            {job.address && (
              <p className="text-sm text-ink-2 mt-2 flex items-start gap-1.5 leading-snug">
                <MapPin size={14} className="mt-0.5 flex-shrink-0 text-ink-3" /> {job.address}
              </p>
            )}

            {/* Big tap targets */}
            <div className="grid grid-cols-2 gap-2 mt-4">
              <Button
                as="a"
                href={`tel:${job.customerPhone}`}
                size="lg"
                variant="money"
                icon={Phone}
              >
                Call
              </Button>
              {job.address && (
                <Button
                  as="a"
                  href={`https://maps.google.com/?q=${encodeURIComponent(job.address)}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  size="lg"
                  variant="outline"
                  icon={Navigation}
                >
                  Map
                </Button>
              )}
            </div>
          </div>

          {/* Job summary */}
          <div className="bg-paper-2 border border-line rounded-2xl p-4 space-y-2.5">
            <Detail label="Appliance"  value={`${applianceEmoji(job.applianceType)} ${applianceLabel(job.applianceType)}`} />
            {job.description && (
              <div>
                <p className="text-[11px] uppercase tracking-wide font-semibold text-ink-3 mb-1">Problem</p>
                <p className="text-sm text-ink bg-paper rounded-xl p-3 border border-line leading-snug">{job.description}</p>
              </div>
            )}
            <Detail label="Received" value={fmt(job.createdAt)} mono />
            {job.scheduledAt && <Detail label="Scheduled" value={fmt(job.scheduledAt)} mono highlight />}
          </div>

          {/* Parts request shortcut */}
          {job.status === 'IN_PROGRESS' && (
            <a
              href={waPartsLink}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-3 bg-money-tint border border-money/20 rounded-2xl p-4 pressable"
            >
              <div className="w-10 h-10 rounded-xl bg-money text-white flex items-center justify-center flex-shrink-0">
                <Package size={18} />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-base font-bold text-money">Request parts on WhatsApp</p>
                <p className="text-xs text-money/80">Sends job details to the shop</p>
              </div>
              <ChevronRight size={16} className="text-money flex-shrink-0" />
            </a>
          )}

          {/* Reason input */}
          {showReason && (
            <div className="bg-paper-2 border border-line rounded-2xl p-4 space-y-3">
              <p className="text-sm font-bold text-ink">
                {pendingStatus === 'CANCELLED' ? 'Why cancel this job?' : 'What parts do you need?'}
              </p>
              <textarea
                value={reason}
                onChange={e => setReason(e.target.value)}
                rows={3}
                placeholder={pendingStatus === 'CANCELLED'
                  ? 'Customer not at home / duplicate / not reachable…'
                  : 'e.g. AC compressor capacitor 25uF, gas R32 250g'}
                className="w-full bg-paper border border-line-2 rounded-xl px-3.5 py-3 text-base text-ink placeholder:text-ink-4 outline-none focus:border-brand focus:shadow-focus resize-none"
              />
              <div className="flex gap-2">
                <Button variant="outline" size="lg" fullWidth onClick={() => { setShowReason(false); setPendingStatus(null); }}>
                  Back
                </Button>
                <Button size="lg" fullWidth disabled={!reason.trim()} onClick={confirmWithReason}>
                  Confirm
                </Button>
              </div>
            </div>
          )}
        </div>

        {/* Action footer — one big button per next status */}
        {!showReason && flow.next.length > 0 && (
          <div className="border-t border-line p-4 flex flex-col gap-2 flex-shrink-0">
            {flow.next.map(nextStatus => {
              const isCancel = nextStatus === 'CANCELLED';
              const isPartsNeeded = nextStatus === 'PARTS_NEEDED';
              const isDone = nextStatus === 'COMPLETED';
              const m = statusMeta(nextStatus);
              return (
                <Button
                  key={nextStatus}
                  variant={isCancel ? 'urgent' : isDone ? 'money' : isPartsNeeded ? 'outline' : 'primary'}
                  size="xl"
                  fullWidth
                  loading={mutating}
                  icon={isDone ? CheckCircle2 : isCancel ? X : isPartsNeeded ? Package : Wrench}
                  onClick={() => handleStatus(nextStatus)}
                >
                  {m.label}
                </Button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

function Detail({ label, value, mono, highlight }) {
  return (
    <div className="flex items-baseline justify-between gap-3">
      <span className="text-xs text-ink-3">{label}</span>
      <span className={[
        'text-sm font-medium text-right break-words',
        highlight ? 'text-brand' : 'text-ink',
        mono ? 'font-mono num' : '',
      ].join(' ')}>{value}</span>
    </div>
  );
}

// ─── Tech mark — initials avatar ───────────────────────────────────────────
function TechMark({ name }) {
  const init = name?.split(' ').slice(0, 2).map(w => w[0]?.toUpperCase()).join('') || 'T';
  return (
    <div className="w-11 h-11 rounded-2xl bg-brand text-white flex items-center justify-center font-display font-extrabold text-base">
      {init}
    </div>
  );
}

// ─── Main ──────────────────────────────────────────────────────────────────
export default function TechnicianPWA() {
  const { user, logout } = useAuth();
  const qc = useQueryClient();
  const [selectedJob, setSelectedJob] = useState(null);
  const [tab, setTab] = useState('active');

  const { data: allJobs = [], isLoading } = useQuery({
    queryKey: ['my-jobs'],
    queryFn: () => jobsApi.mine().then(r => r.data?.data?.content || r.data?.data || []),
    refetchInterval: 30_000,
  });

  const statusMut = useMutation({
    mutationFn: ({ jobId, status, reason }) => jobsApi.updateStatus(jobId, status, reason),
    onSuccess: (res) => {
      toast.success('Status updated');
      qc.invalidateQueries({ queryKey: ['my-jobs'] });
      const updated = res.data?.data;
      if (updated) setSelectedJob(updated);
      else setSelectedJob(null);
    },
    onError: err => toast.error(err.response?.data?.message || 'Could not update. Try again.'),
  });

  const active    = allJobs.filter(j => !['COMPLETED', 'CANCELLED'].includes(j.status));
  const completed = allJobs.filter(j => j.status === 'COMPLETED');
  const shown     = tab === 'active' ? active : completed;
  const hero      = tab === 'active' ? active[0] : null;
  const rest      = tab === 'active' ? active.slice(1) : completed;

  // Today's earnings — sum of labor from completed today
  const today = new Date().toDateString();
  const todayEarnings = completed
    .filter(j => new Date(j.completedAt || j.updatedAt || j.createdAt).toDateString() === today)
    .reduce((s, j) => s + Number(j.laborCharge || 0), 0);

  return (
    <div className="min-h-dvh bg-canvas flex flex-col max-w-md mx-auto pb-safe">

      {/* ── Header ───────────────────────────────────────────────────── */}
      <header className="px-5 pt-safe pt-5 pb-4 bg-paper border-b border-line">
        <div className="flex items-center gap-3">
          <TechMark name={user?.name} />
          <div className="min-w-0 flex-1">
            <p className="text-base font-bold text-ink leading-tight">
              {firstName(user?.name) || 'Technician'} ji
            </p>
            <p className="hi text-xs text-ink-3 leading-tight mt-0.5">SK Electronics · तकनीशियन</p>
          </div>
          <IconButton icon={LogOut} label="Sign out" onClick={logout} variant="ghost" />
        </div>

        {/* Today snapshot — 3 large stat tiles */}
        <div className="grid grid-cols-3 gap-2 mt-4">
          <Snapshot label="Active"    value={active.length}            tone="brand" />
          <Snapshot label="Done"      value={completed.length}         tone="money" emoji="✅" />
          <Snapshot label="Earned"    value={inr(todayEarnings)}       tone="money" small />
        </div>
      </header>

      {/* ── Tab bar ──────────────────────────────────────────────────── */}
      <div className="flex bg-paper border-b border-line px-5 flex-shrink-0">
        {[
          { id: 'active',    label: `Active (${active.length})`,    hi: 'चालू' },
          { id: 'completed', label: `Done (${completed.length})`,   hi: 'पूरा' },
        ].map(t => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={[
              'flex-1 py-3.5 text-sm font-bold border-b-2 transition-colors flex items-center justify-center gap-2',
              tab === t.id
                ? 'border-brand text-brand'
                : 'border-transparent text-ink-3 hover:text-ink-2',
            ].join(' ')}
          >
            {t.label}
            <span className="hi text-[11px] text-ink-3 font-medium">{t.hi}</span>
          </button>
        ))}
      </div>

      {/* ── List ────────────────────────────────────────────────────── */}
      <main className="flex-1 overflow-y-auto scrollbar-hide p-4 space-y-3">
        {isLoading ? (
          [...Array(3)].map((_, i) => <JobCardSkeleton key={i} />)
        ) : shown.length === 0 ? (
          tab === 'active' ? (
            <EmptyState
              icon={Wrench}
              tone="brand"
              title="No active jobs right now"
              hint="When the shop assigns a job, you'll see it here. Keep your phone unlocked — calls come fast."
            />
          ) : (
            <EmptyState
              icon={Star}
              tone="money"
              title="Nothing completed today yet"
              hint="Complete a job and it shows up here with your earnings."
            />
          )
        ) : (
          <>
            {hero && <HeroJob job={hero} onOpen={setSelectedJob} />}
            {rest.length > 0 && (
              <>
                {hero && (
                  <p className="text-[11px] uppercase tracking-wide font-semibold text-ink-3 pt-2 px-1">
                    Other jobs
                  </p>
                )}
                {rest.map(j => (
                  <JobRow key={j.id} job={j} onOpen={setSelectedJob} />
                ))}
              </>
            )}
          </>
        )}
      </main>

      {/* ── Bottom sheet ─────────────────────────────────────────────── */}
      {selectedJob && (
        <JobSheet
          job={selectedJob}
          onClose={() => setSelectedJob(null)}
          onStatusChange={(id, status, reason) => statusMut.mutate({ jobId: id, status, reason })}
          mutating={statusMut.isPending}
        />
      )}
    </div>
  );
}

// ─── Snapshot tile ─────────────────────────────────────────────────────────
function Snapshot({ label, value, tone, emoji, small }) {
  const cls =
    tone === 'money' ? 'border-money/20 bg-money-tint/40'
  : tone === 'brand' ? 'border-brand/20 bg-brand-tint/50'
  :                    'border-line bg-paper-2';
  const fg =
    tone === 'money' ? 'text-money'
  : tone === 'brand' ? 'text-brand'
  :                    'text-ink';
  return (
    <div className={`rounded-xl px-3 py-2.5 border ${cls} text-center`}>
      <p className={`font-display font-bold num ${small ? 'text-base' : 'text-xl'} ${fg}`}>{value ?? '—'}</p>
      <p className="text-[10px] text-ink-3 mt-0.5 leading-tight">{label}{emoji && ' ' + emoji}</p>
    </div>
  );
}
