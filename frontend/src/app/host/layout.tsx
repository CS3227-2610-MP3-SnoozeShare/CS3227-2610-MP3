import { RouteGuard } from '@/components/auth/RouteGuard';
import { RoleShell } from '@/components/role/RoleShell';
export default function HostLayout({ children }: { children: React.ReactNode }) { return <RouteGuard role="HOST"><RoleShell role="HOST">{children}</RoleShell></RouteGuard>; }
