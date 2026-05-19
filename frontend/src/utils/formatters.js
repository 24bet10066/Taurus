// ─── Money ────────────────────────────────────────────────────────────────
export const inr = (n) =>
  n == null ? '—' : '₹' + Number(n).toLocaleString('en-IN', { maximumFractionDigits: 0 });

// Short form: ₹12.4K / ₹3.2L / ₹1.1Cr — for dashboards
export const inrShort = (n) => {
  if (n == null) return '—';
  const num = Number(n);
  if (num >= 1e7) return '₹' + (num / 1e7).toFixed(num >= 1e8 ? 0 : 1) + 'Cr';
  if (num >= 1e5) return '₹' + (num / 1e5).toFixed(num >= 1e6 ? 0 : 1) + 'L';
  if (num >= 1e3) return '₹' + (num / 1e3).toFixed(num >= 1e4 ? 0 : 1) + 'K';
  return '₹' + num.toLocaleString('en-IN');
};

// ─── Time ─────────────────────────────────────────────────────────────────
export const timeAgo = (iso) => {
  if (!iso) return '—';
  const diff = (Date.now() - new Date(iso)) / 1000;
  if (diff < 60)    return 'just now';
  if (diff < 3600)  return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return `${Math.floor(diff / 86400)}d ago`;
};

export const fmt = (iso) =>
  iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'short', timeStyle: 'short' }) : '—';

// ─── Names ───────────────────────────────────────────────────────────────
export const initials = (name) =>
  name?.split(' ').slice(0,2).map(w => w[0]?.toUpperCase()).join('') || '?';

export const firstName = (name) => name?.split(' ')[0] || '—';

// ─── Phone ────────────────────────────────────────────────────────────────
export const phone = (p) => p ? `+91 ${p.slice(0,5)} ${p.slice(5)}` : '—';

// ─── Appliances ──────────────────────────────────────────────────────────
// Tuned for white background readability (WCAG AA on #FFF8F0).
export const APPLIANCE_COLORS = {
  AC:        '#0E7490',   // teal
  WM:        '#4338CA',   // indigo
  RW:        '#0891B2',   // cyan
  RF:        '#0891B2',
  TV:        '#B45309',   // amber-deep
  GEYSER:    '#C2410C',   // orange
  MICROWAVE: '#7C3AED',   // purple
};

export const applianceColor = (type) => APPLIANCE_COLORS[type] || '#5C5247';

// Tinted backgrounds matching each foreground
export const APPLIANCE_TINTS = {
  AC:        '#CFFAFE',
  WM:        '#E0E7FF',
  RW:        '#CFFAFE',
  RF:        '#CFFAFE',
  TV:        '#FEF3C7',
  GEYSER:    '#FFEDD5',
  MICROWAVE: '#EDE9FE',
};

export const applianceTint = (type) => APPLIANCE_TINTS[type] || '#F5F0E8';

export const APPLIANCE_EMOJI = {
  AC: '❄️', WM: '🌊', RW: '🧊', RF: '🧊', TV: '📺',
  GEYSER: '🔥', MICROWAVE: '📡', DEFAULT: '🔧',
};
export const applianceEmoji = (type) => APPLIANCE_EMOJI[type] || APPLIANCE_EMOJI.DEFAULT;

export const APPLIANCE_LABEL = {
  AC: 'Air Conditioner', WM: 'Washing Machine', RW: 'RO / Water Purifier',
  RF: 'Refrigerator',    TV: 'Television',      GEYSER: 'Geyser',
  MICROWAVE: 'Microwave',
};
export const applianceLabel = (type) => APPLIANCE_LABEL[type] || type;

// ─── Job status ──────────────────────────────────────────────────────────
// All FG/BG combos hit ≥4.5:1 contrast on #FFF (WCAG AA).
export const STATUS_META = {
  REQUESTED:    { label: 'Requested',    bg: '#FEF3C7',  fg: '#92400E', dot: true,  emoji: '⏳' },
  ASSIGNED:     { label: 'Assigned',     bg: '#FEF3E8',  fg: '#B53F00', dot: true,  emoji: '👤' },
  IN_TRANSIT:   { label: 'On the way',   bg: '#EDE9FE',  fg: '#5B21B6', dot: true,  emoji: '🛵' },
  AT_CUSTOMER:  { label: 'At customer',  bg: '#CFFAFE',  fg: '#155E75', dot: true,  emoji: '📍' },
  IN_PROGRESS: { label: 'Repairing',     bg: '#EDE9FE',  fg: '#6D28D9', dot: true,  emoji: '🔧' },
  PARTS_NEEDED: { label: 'Parts needed', bg: '#FFEDD5',  fg: '#9A3412', dot: false, emoji: '📦' },
  COMPLETED:    { label: 'Done',         bg: '#D1FAE5',  fg: '#065F46', dot: false, emoji: '✅' },
  CANCELLED:    { label: 'Cancelled',    bg: '#FEE2E2',  fg: '#991B1B', dot: false, emoji: '✖️' },
};
export const statusMeta = (s) =>
  STATUS_META[s] || { label: s, bg: '#F5F0E8', fg: '#5C5247', dot: false, emoji: '•' };

// ─── Hindi micro-labels ──────────────────────────────────────────────────
// Used where Hindi adds warmth without overwhelming.
export const HI = {
  today:        'आज',
  jobs:         'काम',
  revenue:      'आज की कमाई',
  pending:      'बकाया',
  active:       'चालू',
  done:         'पूरा',
  technicians:  'तकनीशियन',
  customer:     'ग्राहक',
  call:         'कॉल',
  navigate:     'रास्ता',
  partsAlert:   'पुर्जे चाहिए',
  newJob:       'नया काम',
  startJob:     'काम शुरू करें',
  markDone:     'काम पूरा करें',
};
