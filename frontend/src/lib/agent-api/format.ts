const MONEY = /^\d{1,9}(\.\d{1,2})?$/;

/** Format check only: positive-or-zero decimal with at most two places. No comparison against any amount (N2). */
export function isMoneyFormat(text: string): boolean {
  return MONEY.test(text);
}

/** Shows an amount received from the server as `SGD 480.00`. Never used for arithmetic. */
export function formatSgd(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') return '—';
  const text = typeof value === 'number' ? value.toFixed(2) : Number(value).toFixed(2);
  return text.startsWith('-') ? `-SGD ${text.slice(1)}` : `SGD ${text}`;
}

/** `25 Sep 2026, 3:42 PM`; the zone defaults to the browser's. */
export function formatDateTime(iso: string, timeZone?: string): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit', hour12: true, timeZone,
  }).formatToParts(new Date(iso));
  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
  return `${get('day')} ${get('month')} ${get('year')}, ${get('hour')}:${get('minute')} ${get('dayPeriod').toUpperCase()}`;
}
