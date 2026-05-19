// FieldInput / FieldTextArea / FieldSelect — consistent form inputs.

export function FieldInput({
  label, hint, error, required,
  prefix, suffix,
  className = '',
  ...rest
}) {
  return (
    <label className={`block ${className}`}>
      {label && (
        <span className="block text-xs font-semibold text-ink-2 mb-1.5">
          {label}{required && <span className="text-urgent ml-0.5">*</span>}
        </span>
      )}
      <span className={[
        'flex items-stretch bg-paper border rounded-xl overflow-hidden transition-all',
        'focus-within:border-brand focus-within:shadow-focus',
        error ? 'border-urgent' : 'border-line-2',
      ].join(' ')}>
        {prefix && (
          <span className="px-3 flex items-center bg-paper-3 text-ink-2 text-sm font-mono border-r border-line-2">
            {prefix}
          </span>
        )}
        <input
          {...rest}
          className="flex-1 bg-transparent px-3.5 py-3 text-base text-ink placeholder:text-ink-4 outline-none"
        />
        {suffix && (
          <span className="px-3 flex items-center text-ink-3 text-sm">{suffix}</span>
        )}
      </span>
      {(hint || error) && (
        <span className={`block text-xs mt-1 ${error ? 'text-urgent' : 'text-ink-3'}`}>
          {error || hint}
        </span>
      )}
    </label>
  );
}

export function FieldTextArea({ label, hint, error, required, className = '', ...rest }) {
  return (
    <label className={`block ${className}`}>
      {label && (
        <span className="block text-xs font-semibold text-ink-2 mb-1.5">
          {label}{required && <span className="text-urgent ml-0.5">*</span>}
        </span>
      )}
      <textarea
        {...rest}
        className={[
          'w-full bg-paper rounded-xl px-3.5 py-3 text-base text-ink placeholder:text-ink-4 outline-none transition-all resize-none',
          'border focus:border-brand focus:shadow-focus',
          error ? 'border-urgent' : 'border-line-2',
        ].join(' ')}
      />
      {(hint || error) && (
        <span className={`block text-xs mt-1 ${error ? 'text-urgent' : 'text-ink-3'}`}>
          {error || hint}
        </span>
      )}
    </label>
  );
}

export function FieldSelect({ label, hint, error, required, options = [], placeholder, className = '', ...rest }) {
  return (
    <label className={`block ${className}`}>
      {label && (
        <span className="block text-xs font-semibold text-ink-2 mb-1.5">
          {label}{required && <span className="text-urgent ml-0.5">*</span>}
        </span>
      )}
      <select
        {...rest}
        className={[
          'w-full bg-paper rounded-xl px-3.5 py-3 text-base text-ink outline-none transition-all',
          'border focus:border-brand focus:shadow-focus',
          error ? 'border-urgent' : 'border-line-2',
        ].join(' ')}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map(o => (
          <option key={o.value ?? o} value={o.value ?? o}>{o.label ?? o}</option>
        ))}
      </select>
      {(hint || error) && (
        <span className={`block text-xs mt-1 ${error ? 'text-urgent' : 'text-ink-3'}`}>
          {error || hint}
        </span>
      )}
    </label>
  );
}
