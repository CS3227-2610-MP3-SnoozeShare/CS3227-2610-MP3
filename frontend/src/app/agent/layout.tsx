import { RouteGuard } from '@/components/auth/RouteGuard';
import { RoleShell } from '@/components/role/RoleShell';
export default function AgentLayout({ children }: { children: React.ReactNode }) { return <RouteGuard role="AGENT"><RoleShell role="AGENT">{children}</RoleShell></RouteGuard>; }
