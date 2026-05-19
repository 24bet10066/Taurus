// EmptyState — never just "no data". Always guides the user to the next action.

export function EmptyState({ icon: Icon, title, hint, action, tone = 'default', className = '' }) {
  const bg =
    tone === 'money'  ? 'bg-money-tint text-money'
  : tone === 'brand'  ? 'bg-brand-tint text-brand'
  : tone === 'pending'? 'bg-pending-tint text-pending'
  :                     'bg-paper-3 text-ink-3';

  return (
    <div className={`flex flex-col items-center justify-center text-center px-6 py-10 ${className}`}>
      {Icon && (
        <div className={`w-14 h-14 rounded-2xl flex items-center justify-center mb-4 ${bg}`}>
          <Icon size={26} strokeWidth={1.8} />
        </div>
      )}
      <p className="text-base font-bold text-ink">{title}</p>
      {hint && <p className="text-sm text-ink-3 mt-1 max-w-xs leading-relaxed">{hint}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
