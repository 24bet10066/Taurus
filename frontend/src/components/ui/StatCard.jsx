// StatCard — big number for the admin dashboard.
// Father's eyes go here first. Number is the hero, label is secondary.
//
// Tones:
//   default — neutral
//   money   — green, used for revenue / completed
//   brand   — saffron, used for primary metric (e.g. jobs today)
//   pending — amber, used for waiting / parts-needed
//   urgent  — red, used for cancellations / SLA breach
//
// Usage:
//   <StatCard tone="brand" hindi="आज के काम" label="Today's jobs" value={12} sub="3 active right now" />

const TONE = {
  default: {
    valueClass: 'text-ink',
    labelClass: 'text-ink-3',
    bgClass:    'bg-paper',
    accentClass:'bg-line',
  },
  money: {
    valueClass: 'text-money',
    labelClass: 'text-money/90',
    bgClass:    'bg-paper',
    accentClass:'bg-money',
  },
  brand: {
    valueClass: 'text-brand',
    labelClass: 'text-ink-3',
    bgClass:    'bg-paper',
    accentClass:'bg-brand',
  },
  pending: {
    valueClass: 'text-pending',
    labelClass: 'text-ink-3',
    bgClass:    'bg-paper',
    accentClass:'bg-pending',
  },
  urgent: {
    valueClass: 'text-urgent',
    labelClass: 'text-ink-3',
    bgClass:    'bg-paper',
    accentClass:'bg-urgent',
  },
};

export function StatCard({
  hindi,
  label,
  value,
  sub,
  tone = 'default',
  loading = false,
  icon: Icon,
  onClick,
  className = '',
}) {
  const t = TONE[tone] || TONE.default;
  const Wrapper = onClick ? 'button' : 'div';
  return (
    <Wrapper
      onClick={onClick}
      className={[
        'relative overflow-hidden rounded-2xl border border-line shadow-card text-left',
        'p-4 flex flex-col gap-1.5',
        onClick && 'card-hover focus-ring',
        t.bgClass,
        className,
      ].filter(Boolean).join(' ')}
    >
      {/* left accent stripe */}
      <span className={`absolute left-0 top-3 bottom-3 w-1 rounded-r-full ${t.accentClass}`} />

      <div className="flex items-center justify-between pl-2">
        <p className="text-xs font-semibold uppercase tracking-wide text-ink-3">
          {label}
        </p>
        {Icon && <Icon size={14} className="text-ink-3" />}
      </div>

      {hindi && (
        <p className={`hi text-[11px] ${t.labelClass} pl-2 -mt-1`}>{hindi}</p>
      )}

      <p className={`pl-2 text-big num font-display ${t.valueClass}`}>
        {loading ? <span className="skeleton inline-block h-7 w-20 align-middle" /> : (value ?? '—')}
      </p>

      {sub && !loading && (
        <p className="pl-2 text-xs text-ink-3 leading-tight">{sub}</p>
      )}
    </Wrapper>
  );
}
