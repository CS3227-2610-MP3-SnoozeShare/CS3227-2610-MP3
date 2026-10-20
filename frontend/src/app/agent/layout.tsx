import { RouteGuard } from '@/components/auth/RouteGuard';
export default function AgentLayout({ children }: { children: React.ReactNode }) { return <RouteGuard role="AGENT">{children}</RouteGuard>; }
