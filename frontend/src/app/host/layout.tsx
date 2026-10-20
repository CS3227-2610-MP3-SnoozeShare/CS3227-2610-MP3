import { RouteGuard } from '@/components/auth/RouteGuard';
export default function HostLayout({ children }: { children: React.ReactNode }) { return <RouteGuard role="HOST">{children}</RouteGuard>; }
