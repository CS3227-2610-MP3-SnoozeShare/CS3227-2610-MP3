'use client';

import { usePathname } from 'next/navigation';
import { RouteGuard } from '@/components/auth/RouteGuard';
import { RoleShell } from '@/components/role/RoleShell';
export default function GuestLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname() ?? '/guest/search';
  return <RouteGuard role="GUEST"><RoleShell role="GUEST" activePath={pathname}>{children}</RoleShell></RouteGuard>;
}
