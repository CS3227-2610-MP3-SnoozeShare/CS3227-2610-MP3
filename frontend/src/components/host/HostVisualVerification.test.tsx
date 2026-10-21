import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

const root = join(process.cwd(), 'src');
const hostFiles = [
  'components/host/HostListingCard.tsx', 'components/host/HostListingDetail.tsx', 'components/host/HostListingForm.tsx',
  'components/host/HostCalendarPanel.tsx', 'components/host/HostRequestsTable.tsx', 'components/host/HostMessagesPanel.tsx', 'components/host/HostWalletPanel.tsx',
];

describe('W-HOST visual and accessibility contract', () => {
  it('keeps Host source on the shared Fall Light token system', () => {
    const source = hostFiles.map((file) => readFileSync(join(root, file), 'utf8')).join('\n');
    expect(source).not.toMatch(/from ['"](?:\.\.\/)+backend|from ['"](?:\.\.\/)+desktop/);
    expect(source).not.toMatch(/bg-(?:blue|gray)-|text-(?:blue|gray)-/);
    expect(source).toContain('ss-card');
    expect(source).toContain('text-[var(--text-subtle)]');
  });

  it('keeps required visible labels and no-image fallback in source', () => {
    const source = hostFiles.map((file) => readFileSync(join(root, file), 'utf8')).join('\n');
    for (const label of ['AMENITIES', 'REVIEWS', 'BOOKINGS', 'RATING', 'Current blocked dates', 'Transaction statement']) {
      expect(source).toContain(label);
    }
    expect(source).toContain('aria-label');
    expect(source).toContain('role="img"');
  });
});
