'use client';

import { RouteGuard } from '@/components/auth/RouteGuard';
import { GuestRoleShell } from '@/components/guest/GuestRoleShell';
export default function GuestLayout({ children }: { children: React.ReactNode }) {
  return <RouteGuard role="GUEST"><GuestRoleShell>{children}</GuestRoleShell></RouteGuard>;
}
