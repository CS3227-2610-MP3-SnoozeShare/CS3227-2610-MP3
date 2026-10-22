import { redirect } from 'next/navigation';

export default function GuestLandingPage() {
  redirect('/guest/search');
}
