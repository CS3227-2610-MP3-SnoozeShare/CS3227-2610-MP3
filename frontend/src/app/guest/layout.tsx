import { RouteGuard } from '@/components/auth/RouteGuard';
export default function GuestLayout({ children }: { children: React.ReactNode }) { return <RouteGuard role="GUEST">{children}</RouteGuard>; }
