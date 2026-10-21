export type HostFixtureState = 'populated' | 'empty' | 'loading' | 'error';

export type FixtureResult<T> =
  | { state: HostFixtureState; data: T; error?: never }
  | { state: Exclude<HostFixtureState, 'populated' | 'empty'>; data?: undefined; error: string }
  | { state: 'empty'; data: T; error?: never }
  | { state: 'not-found'; data?: undefined; error: 'Listing not found' };

export function resultFor<T>(state: HostFixtureState, populated: T, empty: T, error: string): FixtureResult<T> {
  if (state === 'populated') return { state, data: populated };
  if (state === 'empty') return { state, data: empty };
  return { state, error };
}
