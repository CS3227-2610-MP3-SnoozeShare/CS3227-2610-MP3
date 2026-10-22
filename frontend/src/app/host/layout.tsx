import { RouteGuard } from '@/components/auth/RouteGuard';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { HostApiProvider } from '@/components/host/HostApiProvider';
import { HostRoleShell } from '@/components/host/HostRoleShell';
export default function HostLayout({ children }: { children: React.ReactNode }) {
  return <RouteGuard role="HOST"><HostApiProvider><HostFixtureProvider><HostRoleShell>{children}</HostRoleShell></HostFixtureProvider></HostApiProvider></RouteGuard>;
}
