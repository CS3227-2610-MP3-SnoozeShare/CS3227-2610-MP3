'use client';

import { useAuth } from '@/providers/AuthProvider';
import { RoleNavbar, type WorkspaceRole } from './RoleNavbar';

export function RoleShell({ role, children, walletLabel, activePath }: { role: WorkspaceRole; children: React.ReactNode; walletLabel?: string; activePath?: string }) {
  const { signOut } = useAuth();
  const messagesPage = role === 'HOST' && activePath === '/host/messages';
  return <div className={`flex ${messagesPage ? 'h-screen overflow-hidden' : 'min-h-screen'} flex-col bg-background`}><RoleNavbar role={role} walletLabel={walletLabel} activePath={activePath} onLogout={() => void signOut()} />{children}</div>;
}
