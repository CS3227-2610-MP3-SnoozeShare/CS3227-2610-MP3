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
  refreshWalletStatement: (page?: number, size?: number) => Promise<void>;
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

  const loadResource = useCallback(async <T,>(key: string, loader: () => Promise<T>) => {
    setResources((current) => ({ ...current, [key]: { status: 'loading' } }));
    try {
      const data = await loader();
      const empty = Array.isArray(data) && data.length === 0;
      setResources((current) => ({ ...current, [key]: { status: empty ? 'empty' : 'ready', data } }));
      return data;
    } catch (error) {
      const notFound = error instanceof BackendApiError && error.status === 404;
      setResources((current) => ({ ...current, [key]: {
        status: notFound ? 'not-found' : 'error', message: notFound ? 'This item is no longer available.' : hostSafeErrorMessage(error),
      } }));
      throw error;
    }
  }, []);

  const getResource = useCallback(<T,>(key: string) => resources[key] as HostLoadable<T> | undefined, [resources]);

  const refreshWallet = useCallback(async () => {
    try {
      const data = await fetchHostWallet(0, 1);
      setWallet({ status: 'ready', data });
    } catch {
      setWallet({ status: 'error', message: 'Unable to load wallet.' });
      throw new Error('Unable to load wallet.');
    }
  }, []);

  const refreshWalletStatement = useCallback(async (page = 0, size = 50) => {
    try {
      const data = await fetchHostWallet(page, size);
      setWallet((current) => {
        if (page === 0 || current.status !== 'ready' || current.data.statement.page >= page) return { status: 'ready', data };
        return { status: 'ready', data: {
          ...data,
          statement: { ...data.statement, items: [...current.data.statement.items, ...data.statement.items] },
        } };
      });
    } catch {
      setWallet((current) => current.status === 'ready'
        ? { status: 'ready', data: current.data, refreshError: 'Unable to load wallet statement.' }
        : { status: 'error', message: 'Unable to load wallet statement.' });
      throw new Error('Unable to load wallet statement.');
    }
  }, []);

  const retryWalletRecovery = useCallback(async () => {
    setWalletRecoveryRequired(true);
    try {
      await Promise.all([refreshWallet(), refreshWalletStatement(0, 50)]);
      setWalletRecoveryRequired(false);
    } catch (error) {
      setWalletRecoveryRequired(true);
      throw error;
    }
  }, [refreshWallet, refreshWalletStatement]);

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
            statement: {
              ...current.data.statement,
              items: [result.transaction, ...current.data.statement.items].slice(0, current.data.statement.size),
            },
          },
        }
        : { status: 'ready', data: { wallet: result.wallet, statement: { items: [], page: 0, size: 1, hasMore: false } } });
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
    refreshWalletStatement,
    retryWalletRecovery,
    mutateWallet,
  }), [resources, loadResource, getResource, wallet, walletMutationPending, walletRecoveryRequired, refreshWallet, refreshWalletStatement, retryWalletRecovery, mutateWallet]);

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
