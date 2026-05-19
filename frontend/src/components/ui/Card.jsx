// Card — neutral white surface with soft shadow + warm divider line.
// Use `interactive` for hover lift on tap-targets (job cards, technicians).

export function Card({
  as: Tag = 'div',
  interactive = false,
  padded = true,
  className = '',
  children,
  ...rest
}) {
  return (
    <Tag
      className={[
        'bg-paper rounded-2xl border border-line shadow-card',
        interactive && 'card-hover cursor-pointer',
        padded && 'p-4',
        className,
      ].filter(Boolean).join(' ')}
      {...rest}
    >
      {children}
    </Tag>
  );
}

export function CardHeader({ title, subtitle, action, className = '' }) {
  return (
    <div className={`flex items-start justify-between gap-3 ${className}`}>
      <div className="min-w-0">
        <p className="text-sm font-bold text-ink leading-tight">{title}</p>
        {subtitle && <p className="text-xs text-ink-3 mt-0.5">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}
