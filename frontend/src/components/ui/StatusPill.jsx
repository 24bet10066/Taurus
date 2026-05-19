// StatusPill — semantic pill for job status + appliance type.
// Reads in 2 seconds: emoji + color + label.

import { statusMeta, APPLIANCE_COLORS, APPLIANCE_TINTS, applianceEmoji } from '../../utils/formatters';

export function StatusPill({ status, size = 'sm', showEmoji = true }) {
  const m = statusMeta(status);
  const sz =
    size === 'lg' ? 'text-sm px-3 py-1.5 gap-2'
  : size === 'md' ? 'text-xs px-2.5 py-1 gap-1.5'
  :                 'text-[11px] px-2 py-0.5 gap-1';

  return (
    <span
      className={`inline-flex items-center rounded-full font-semibold ${sz}`}
      style={{ background: m.bg, color: m.fg }}
    >
      {m.dot && (
        <span
          className="w-1.5 h-1.5 rounded-full animate-pulse-dot"
          style={{ background: m.fg }}
        />
      )}
      {showEmoji && !m.dot && <span aria-hidden>{m.emoji}</span>}
      {m.label}
    </span>
  );
}

export function AppliancePill({ type, size = 'sm' }) {
  const color = APPLIANCE_COLORS[type] || '#5C5247';
  const tint  = APPLIANCE_TINTS[type]  || '#F5F0E8';
  const sz =
    size === 'md' ? 'text-xs px-2.5 py-1 gap-1.5'
  :                 'text-[11px] px-2 py-0.5 gap-1';

  return (
    <span
      className={`inline-flex items-center rounded-full font-semibold ${sz}`}
      style={{ background: tint, color }}
    >
      <span aria-hidden>{applianceEmoji(type)}</span>
      {type}
    </span>
  );
}
