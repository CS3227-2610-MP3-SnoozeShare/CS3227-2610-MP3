'use client';

import { useAuth } from '@/providers/AuthProvider';
import { RoleNavbar, type WorkspaceRole } from './RoleNavbar';

export function RoleShell({ role, children }: { role: WorkspaceRole; children: React.ReactNode }) {
  const { signOut } = useAuth();
  return <div className="min-h-screen bg-background"><RoleNavbar role={role} onLogout={() => void signOut()} />{children}</div>;
}
