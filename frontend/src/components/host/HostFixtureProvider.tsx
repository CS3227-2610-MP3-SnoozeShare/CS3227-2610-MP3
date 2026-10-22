'use client';

import { createContext, useContext, useMemo, type PropsWithChildren } from 'react';
import { createHostFixtureAdapter, type HostFixtureAdapter } from './hostFixtures';

const HostFixtureContext = createContext<HostFixtureAdapter | null>(null);

export function HostFixtureProvider({ adapter, children }: PropsWithChildren<{ adapter?: HostFixtureAdapter }>) {
  const value = useMemo(() => adapter ?? createHostFixtureAdapter(), [adapter]);
  return <HostFixtureContext.Provider value={value}>{children}</HostFixtureContext.Provider>;
}

export function useHostFixtures() {
  const value = useContext(HostFixtureContext);
  if (!value) throw new Error('useHostFixtures must be used inside HostFixtureProvider');
  return value;
}
