import { redirect } from 'next/navigation';

export default function HostLandingPage() {
  redirect('/host/listings');
}
