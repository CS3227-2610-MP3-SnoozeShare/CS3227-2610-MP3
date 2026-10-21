'use client';

import { useAuth } from '@/providers/AuthProvider';
import { RoleNavbar, type WorkspaceRole } from './RoleNavbar';

export function RoleShell({ role, children, walletLabel, activePath }: { role: WorkspaceRole; children: React.ReactNode; walletLabel?: string; activePath?: string }) {
  const { signOut } = useAuth();
  return <div className="min-h-screen bg-background"><RoleNavbar role={role} walletLabel={walletLabel} activePath={activePath} onLogout={() => void signOut()} />{children}</div>;
}
