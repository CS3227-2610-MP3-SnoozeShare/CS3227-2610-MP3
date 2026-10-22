'use client';

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type PropsWithChildren } from 'react';
import {
  fetchHostWallet,
  topUpHostWallet,
  withdrawFromHostWallet,
} from '@/lib/hostApi';
import * as hostApi from '@/lib/hostApi';
import type { HostWalletDto, HostWalletMutationDto } from './hostTypes';
import { BackendApiError } from '@/lib/api';
import { hostSafeErrorMessage } from '@/lib/hostApi';

export type HostLoadable<T> =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'not-found'; message: string }
  | { status: 'empty'; data: T }
  | { status: 'ready'; data: T; refreshError?: string };

type HostApiContextValue = {
  api: typeof hostApi;
  resources: Record<string, HostLoadable<unknown>>;
  loadResource: <T>(key: string, loader: () => Promise<T>) => Promise<T>;
  getResource: <T>(key: string) => HostLoadable<T> | undefined;
  wallet: HostLoadable<HostWalletDto>;
  walletMutationPending: boolean;
  walletRecoveryRequired: boolean;
  refreshWallet: () => Promise<void>;
  retryWalletRecovery: () => Promise<void>;
  mutateWallet: (operation: 'top-up' | 'withdraw', amount: number) => Promise<HostWalletMutationDto>;
};

const HostApiContext = createContext<HostApiContextValue | null>(null);

export function HostApiProvider({ children }: PropsWithChildren) {
  const [wallet, setWallet] = useState<HostLoadable<HostWalletDto>>({ status: 'loading' });
  const [resources, setResources] = useState<Record<string, HostLoadable<unknown>>>({});
  const [walletMutationPending, setWalletMutationPending] = useState(false);
  const [walletRecoveryRequired, setWalletRecoveryRequired] = useState(false);
  const walletMutationLock = useRef(false);
  const resourceLoads = useRef(new Map<string, Promise<unknown>>());
  const walletLoads = useRef<Promise<void> | null>(null);

  const loadResource = useCallback(<T,>(key: string, loader: () => Promise<T>) => {
    const existing = resourceLoads.current.get(key);
    if (existing) return existing as Promise<T>;

    setResources((current) => ({ ...current, [key]: { status: 'loading' } }));
    const request: Promise<T> = loader().then(
      (data) => {
        if (resourceLoads.current.get(key) === request) resourceLoads.current.delete(key);
        const empty = Array.isArray(data) && data.length === 0;
        setResources((current) => ({ ...current, [key]: { status: empty ? 'empty' : 'ready', data } }));
        return data;
      },
      (error: unknown) => {
        if (resourceLoads.current.get(key) === request) resourceLoads.current.delete(key);
        const notFound = error instanceof BackendApiError && error.status === 404;
        setResources((current) => ({ ...current, [key]: {
          status: notFound ? 'not-found' : 'error', message: notFound ? 'This item is no longer available.' : hostSafeErrorMessage(error),
        } }));
        return Promise.reject(error);
      },
    );
    resourceLoads.current.set(key, request);
    return request;
  }, []);

  const getResource = useCallback(<T,>(key: string) => resources[key] as HostLoadable<T> | undefined, [resources]);

  const refreshWallet = useCallback(() => {
    const existing = walletLoads.current;
    if (existing) return existing;

    const request: Promise<void> = fetchHostWallet().then(
      (data) => {
        if (walletLoads.current === request) walletLoads.current = null;
        setWallet({ status: 'ready', data });
      },
      () => {
        if (walletLoads.current === request) walletLoads.current = null;
        setWallet({ status: 'error', message: 'Unable to load wallet.' });
        return Promise.reject(new Error('Unable to load wallet.'));
      },
    );
    walletLoads.current = request;
    return request;
  }, []);

  const retryWalletRecovery = useCallback(async () => {
    setWalletRecoveryRequired(true);
    try {
      await refreshWallet();
      setWalletRecoveryRequired(false);
    } catch (error) {
      setWalletRecoveryRequired(true);
      throw error;
    }
  }, [refreshWallet]);

  const mutateWallet = useCallback(async (operation: 'top-up' | 'withdraw', amount: number) => {
    if (walletMutationLock.current) throw new Error('A wallet operation is already in progress.');
    walletMutationLock.current = true;
    setWalletMutationPending(true);
    try {
      const result = operation === 'top-up'
        ? await topUpHostWallet(amount)
        : await withdrawFromHostWallet(amount);
      setWallet((current) => current.status === 'ready'
        ? {
          ...current,
          data: {
            ...current.data,
            wallet: result.wallet,
            statement: [result.transaction, ...current.data.statement],
          },
        }
        : { status: 'ready', data: { wallet: result.wallet, statement: [result.transaction] } });
      return result;
    } catch (error) {
      // The server may have committed before a network failure; refresh before allowing another explicit action.
      setWalletRecoveryRequired(true);
      try {
        await retryWalletRecovery();
      } catch {
        // The wallet state already contains a safe refresh error.
      }
      throw error;
    } finally {
      walletMutationLock.current = false;
      setWalletMutationPending(false);
    }
  }, [retryWalletRecovery]);

  useEffect(() => {
    void refreshWallet().catch(() => undefined);
  }, [refreshWallet]);

  const value = useMemo(() => ({
    api: hostApi,
    resources,
    loadResource,
    getResource,
    wallet,
    walletMutationPending,
    walletRecoveryRequired,
    refreshWallet,
    retryWalletRecovery,
    mutateWallet,
  }), [resources, loadResource, getResource, wallet, walletMutationPending, walletRecoveryRequired, refreshWallet, retryWalletRecovery, mutateWallet]);

  return <HostApiContext.Provider value={value}>{children}</HostApiContext.Provider>;
}

export function useHostApi() {
  const value = useOptionalHostApi();
  if (!value) throw new Error('useHostApi must be used inside HostApiProvider');
  return value;
}

export function useOptionalHostApi() {
  return useContext(HostApiContext);
}
