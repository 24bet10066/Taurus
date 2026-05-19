import { X } from 'lucide-react';
import { useEffect } from 'react';

export function Modal({ open, onClose, title, subtitle, children, width = 'max-w-lg' }) {
  useEffect(() => {
    if (!open) return;
    const fn = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', fn);
    return () => window.removeEventListener('keydown', fn);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center sm:p-4"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-ink/40 backdrop-blur-sm" />

      {/* Panel */}
      <div className={`relative w-full ${width} bg-paper border border-line shadow-panel rounded-t-2xl sm:rounded-2xl animate-slide-up sm:animate-pop pb-safe`}>
        {/* Mobile drag handle */}
        <div className="w-10 h-1 bg-line-2 rounded-full mx-auto mt-2.5 sm:hidden" />

        {/* Header */}
        <div className="flex items-start justify-between px-5 pt-4 pb-3 sm:px-6 sm:pt-5">
          <div className="min-w-0">
            <h2 className="text-base font-bold text-ink">{title}</h2>
            {subtitle && <p className="text-xs text-ink-3 mt-0.5">{subtitle}</p>}
          </div>
          <button
            onClick={onClose}
            aria-label="Close"
            className="w-9 h-9 flex items-center justify-center rounded-xl text-ink-3 hover:text-ink hover:bg-paper-3 transition-colors -mr-1.5"
          >
            <X size={18} />
          </button>
        </div>

        <div className="px-5 pb-5 sm:px-6 sm:pb-6">{children}</div>
      </div>
    </div>
  );
}
