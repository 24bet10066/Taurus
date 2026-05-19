// Legacy bridge — old imports of StatusBadge/ApplianceBadge still work.
// Internally re-exports the new StatusPill/AppliancePill.

import { StatusPill, AppliancePill } from './StatusPill';

export function StatusBadge({ status, size = 'sm' }) {
  return <StatusPill status={status} size={size} />;
}

export function ApplianceBadge({ type, size = 'sm' }) {
  return <AppliancePill type={type} size={size} />;
}
