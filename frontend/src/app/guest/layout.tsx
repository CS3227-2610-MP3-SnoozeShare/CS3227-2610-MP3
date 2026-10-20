import { RouteGuard } from '@/components/auth/RouteGuard';
import { RoleShell } from '@/components/role/RoleShell';
export default function GuestLayout({ children }: { children: React.ReactNode }) { return <RouteGuard role="GUEST"><RoleShell role="GUEST">{children}</RoleShell></RouteGuard>; }
