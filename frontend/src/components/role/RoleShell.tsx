import { RoleNavbar, type WorkspaceRole } from './RoleNavbar';

export function RoleShell({ role, children }: { role: WorkspaceRole; children: React.ReactNode }) {
  return <div className="min-h-screen bg-background"><RoleNavbar role={role} />{children}</div>;
}
