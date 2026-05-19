// ErrorState — human messages, not "Error 500".

import { WifiOff, RefreshCw } from 'lucide-react';
import { Button } from './Button';

export function ErrorState({
  title = 'Network problem',
  hint  = 'Could not load. Check your connection and try again.',
  onRetry,
  className = '',
}) {
  return (
    <div className={`flex flex-col items-center justify-center text-center px-6 py-10 ${className}`}>
      <div className="w-14 h-14 rounded-2xl bg-urgent-tint text-urgent flex items-center justify-center mb-4">
        <WifiOff size={24} strokeWidth={1.8} />
      </div>
      <p className="text-base font-bold text-ink">{title}</p>
      <p className="text-sm text-ink-3 mt-1 max-w-xs leading-relaxed">{hint}</p>
      {onRetry && (
        <div className="mt-4">
          <Button variant="outline" icon={RefreshCw} onClick={onRetry}>Try again</Button>
        </div>
      )}
    </div>
  );
}
