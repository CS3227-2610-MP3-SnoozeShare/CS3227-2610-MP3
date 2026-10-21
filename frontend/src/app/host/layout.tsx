import { RouteGuard } from '@/components/auth/RouteGuard';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { HostRoleShell } from '@/components/host/HostRoleShell';
export default function HostLayout({ children }: { children: React.ReactNode }) {
  return <RouteGuard role="HOST"><HostFixtureProvider><HostRoleShell>{children}</HostRoleShell></HostFixtureProvider></RouteGuard>;
}
