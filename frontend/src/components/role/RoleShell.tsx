'use client';

import { useAuth } from '@/providers/AuthProvider';
import { RoleNavbar, type WorkspaceRole } from './RoleNavbar';

export function RoleShell({ role, children, walletLabel, activePath }: { role: WorkspaceRole; children: React.ReactNode; walletLabel?: string; activePath?: string }) {
  const { signOut, profile } = useAuth();
  const initials = (profile?.displayName ?? profile?.email ?? '').split(/[\s@.]+/).filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join('');
  const boundedHostPage = role === 'HOST' && ['/host/listings', '/host/requests', '/host/messages', '/host/wallet'].includes(activePath ?? '');
  return <div className={`flex ${boundedHostPage ? 'h-screen overflow-hidden' : 'min-h-screen'} flex-col bg-background`}><RoleNavbar role={role} walletLabel={walletLabel} activePath={activePath} avatarInitials={initials} onLogout={() => void signOut()} />{children}</div>;
}
