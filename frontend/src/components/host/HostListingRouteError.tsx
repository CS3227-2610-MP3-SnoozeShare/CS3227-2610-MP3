import Link from 'next/link';
import { HostPageFrame } from './HostPageFrame';

export function HostListingRouteError({ onRetry }: { onRetry?: () => void }) {
  return (
    <HostPageFrame title="Something went wrong">
      <div className="ss-card space-y-4 p-8 text-center">
        <p>We couldn&apos;t load this page.</p>
        <div className="flex flex-wrap justify-center gap-3">
          <Link className="rounded-lg border border-border px-3 py-2 text-sm underline" href="/host/listings">Back to Listings</Link>
          {onRetry && <button className="rounded-lg border border-border px-3 py-2 text-sm" onClick={onRetry} type="button">Try again</button>}
        </div>
      </div>
    </HostPageFrame>
  );
}
