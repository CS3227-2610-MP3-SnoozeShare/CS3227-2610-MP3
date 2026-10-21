export function AuthHeader({ title }: { title: string }) {
  return (
    <header className="space-y-1 text-center">
      <p className="ss-auth-brand text-[0.5625rem] font-extrabold tracking-[0.14em] text-primary">SNOOZESHARE</p>
      <h1 className="ss-auth-title text-xl font-bold leading-tight">{title}</h1>
    </header>
  );
}
