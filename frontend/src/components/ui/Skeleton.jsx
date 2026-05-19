export function Skeleton({ className = '' }) {
  return <div className={`skeleton ${className}`} />;
}

export function JobCardSkeleton() {
  return (
    <div className="bg-paper rounded-2xl p-4 shadow-card border border-line space-y-2.5">
      <div className="flex items-center justify-between">
        <Skeleton className="h-4 w-20" />
        <Skeleton className="h-5 w-16 rounded-full" />
      </div>
      <Skeleton className="h-5 w-32" />
      <Skeleton className="h-3.5 w-24" />
      <div className="flex items-center justify-between pt-1">
        <Skeleton className="h-3 w-16" />
        <Skeleton className="h-6 w-6 rounded-full" />
      </div>
    </div>
  );
}

export function StatSkeleton() {
  return <Skeleton className="h-6 w-24 rounded" />;
}
