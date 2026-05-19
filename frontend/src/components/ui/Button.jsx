// Button — single primitive for every clickable action across ServiceOS.
// Variants: primary (brand), ghost, outline, soft, money, urgent.
// Sizes: sm, md, lg, xl.  Mobile actions should use lg/xl for thumb-zone.

import { Loader2 } from 'lucide-react';

const VARIANT = {
  primary: 'bg-brand text-white shadow-brand hover:bg-brand-2 active:bg-brand-2 disabled:bg-brand/40',
  ghost:   'text-ink-2 hover:bg-ink/5 active:bg-ink/10',
  outline: 'border border-line-2 text-ink hover:bg-paper-2 active:bg-paper-3',
  soft:    'bg-brand-tint text-brand-3 hover:bg-brand-tint-2 active:bg-brand-tint-2',
  money:   'bg-money text-white hover:brightness-110 active:brightness-95 shadow-card',
  urgent:  'bg-urgent text-white hover:brightness-110 active:brightness-95 shadow-card',
  light:   'bg-paper text-ink border border-line hover:bg-paper-2',
};

const SIZE = {
  sm: 'h-9  px-3.5 text-sm rounded-xl gap-1.5',
  md: 'h-11 px-4   text-sm rounded-xl gap-2',
  lg: 'h-13 px-5   text-base rounded-2xl gap-2 font-semibold',
  xl: 'h-14 px-6   text-base rounded-2xl gap-2.5 font-bold',
};

export function Button({
  as: Tag = 'button',
  variant = 'primary',
  size = 'md',
  icon: Icon,
  iconRight: IconRight,
  loading = false,
  disabled = false,
  fullWidth = false,
  children,
  className = '',
  ...rest
}) {
  return (
    <Tag
      disabled={disabled || loading}
      className={[
        'inline-flex items-center justify-center font-semibold',
        'pressable focus-ring',
        'disabled:cursor-not-allowed disabled:opacity-60',
        VARIANT[variant],
        SIZE[size],
        fullWidth ? 'w-full' : '',
        className,
      ].join(' ')}
      {...rest}
    >
      {loading
        ? <Loader2 size={size === 'sm' ? 14 : 16} className="animate-spin" />
        : Icon && <Icon size={size === 'sm' ? 14 : size === 'xl' ? 18 : 16} strokeWidth={2.2} />}
      {children}
      {!loading && IconRight && (
        <IconRight size={size === 'sm' ? 14 : size === 'xl' ? 18 : 16} strokeWidth={2.2} />
      )}
    </Tag>
  );
}

// IconButton — square, icon-only. Used for back/close/refresh/etc.
export function IconButton({
  icon: Icon,
  size = 'md',
  variant = 'ghost',
  label,
  className = '',
  ...rest
}) {
  const dim = size === 'sm' ? 'w-9 h-9' : size === 'lg' ? 'w-12 h-12' : 'w-11 h-11';
  const ic  = size === 'sm' ? 14 : size === 'lg' ? 20 : 18;
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      className={[
        'inline-flex items-center justify-center rounded-xl',
        'pressable focus-ring',
        VARIANT[variant],
        dim,
        className,
      ].join(' ')}
      {...rest}
    >
      <Icon size={ic} strokeWidth={2.2} />
    </button>
  );
}
