'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Sparkles } from 'lucide-react';
import type { ListingDetail } from './hostTypes';
import { validateListing, type ListingFormValues } from './hostListingValidation';
import { useOptionalHostApi } from './HostApiProvider';
import { hostSafeErrorMessage } from '@/lib/hostApi';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Field } from '@/components/ui/input';
import { SelectField } from '@/components/ui/select';
import { HostAiDescriptionAssistant } from './HostAiDescriptionAssistant';

const blank: ListingFormValues = { title: '', description: '', propertyType: 'House', status: 'active', streetAddress: '', city: '', region: '', postalCode: '', maxGuests: '', bedrooms: '', bathrooms: '', rate: '', checkIn: '', checkOut: '' };
const propertyTypes = ['House', 'Apartment', 'Condo', 'Private room'];
const amenities = ['Wifi', 'Parking', 'Air Conditioning', 'Kitchen', 'Washer', 'Work Desk'];
const fieldPlaceholders: Partial<Record<keyof ListingFormValues, string>> = {
  title: 'e.g., Lakeside Cabin',
  description: 'Describe your property and what guests can expect.',
  streetAddress: 'e.g., 123 Shoreline Dr',
  city: 'e.g., Lakeview',
  region: 'e.g., WI',
  postalCode: 'e.g., 53211',
  maxGuests: 'e.g., 4',
  bedrooms: 'e.g., 2',
  bathrooms: 'e.g., 1',
  rate: 'e.g., 120.00',
};
function toTimeInputValue(value: string): string {
  const match = value.match(/^(\d{1,2}):(\d{2})\s*(AM|PM)$/i);
  if (!match) return value;
  const [, rawHours, minutes, meridiem] = match;
  const hours = Number(rawHours) % 12 + (meridiem.toUpperCase() === 'PM' ? 12 : 0);
  return `${String(hours).padStart(2, '0')}:${minutes}`;
}
function valuesFrom(listing?: ListingDetail): ListingFormValues { return listing ? { title: listing.title, description: listing.description, propertyType: listing.type, status: listing.status, streetAddress: listing.address, city: listing.city, region: listing.region, postalCode: listing.postalCode, maxGuests: String(listing.maxGuests), bedrooms: String(listing.bedrooms), bathrooms: String(listing.bathrooms), rate: listing.rate.toFixed(2), checkIn: toTimeInputValue(listing.checkIn), checkOut: toTimeInputValue(listing.checkOut) } : blank; }

export function HostListingForm({ listing }: { listing?: ListingDetail }) {
  const router = useRouter();
  const api = useOptionalHostApi();
  const [values, setValues] = useState(() => valuesFrom(listing));
  const [errors, setErrors] = useState<Partial<Record<keyof ListingFormValues, string>>>({});
  const [requestError, setRequestError] = useState('');
  const [saving, setSaving] = useState(false);
  const [aiOpen, setAiOpen] = useState(false);
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>(() => listing?.amenities.map((amenity) => amenity.toUpperCase().replaceAll(' ', '_')) ?? []);
  const update = (key: keyof ListingFormValues, value: string) => setValues((current) => ({ ...current, [key]: value }));
  const submit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextErrors = validateListing(values);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length) return;
    const form = new FormData(event.currentTarget);
    const propertyType = values.propertyType.toUpperCase().replaceAll(' ', '_');
    const payload = {
      title: values.title.trim(), description: values.description.trim(), propertyType,
      streetAddress: values.streetAddress.trim(), city: values.city.trim(), region: values.region.trim(),
      postalCode: Number(values.postalCode), maxGuests: Number(values.maxGuests), bedrooms: Number(values.bedrooms),
      bathrooms: Number(values.bathrooms), baseNightlyRate: Number(values.rate), checkInTime: values.checkIn,
      checkOutTime: values.checkOut, amenities: form.getAll('amenities').map(String),
    };
    setRequestError('');
    if (!api) {
      router.push(listing ? `/host/listings/${listing.id}` : '/host/listings');
      return;
    }
    setSaving(true);
    void (async () => {
      try {
        if (listing) {
          await api.api.updateHostListing(listing.id, payload);
          const nextStatus = values.status.toUpperCase() as 'ACTIVE' | 'INACTIVE';
          if (nextStatus !== (listing.status === 'active' ? 'ACTIVE' : 'INACTIVE')) await api.api.updateHostListingStatus(listing.id, nextStatus);
          router.push(`/host/listings/${listing.id}`);
        } else {
          await api.api.createHostListing({ ...payload, status: values.status.toUpperCase() as 'ACTIVE' | 'INACTIVE' });
          router.push('/host/listings');
        }
      } catch (caught) {
        setRequestError(hostSafeErrorMessage(caught));
      } finally {
        setSaving(false);
      }
    })();
  };
  const field = (key: keyof ListingFormValues, label: string, type = 'text') => <div><Field label={label} name={key} type={type} value={values[key]} placeholder={fieldPlaceholders[key]} onChange={(event) => update(key, event.target.value)} aria-invalid={Boolean(errors[key])} /><p aria-live="polite" className="mt-1 min-h-4 text-xs text-destructive">{errors[key]}</p></div>;
  const rateField = <div><label className="flex flex-col gap-1.5 text-xs font-semibold text-[var(--text-muted)]" htmlFor="rate"><span>Rate / night</span><span className="relative"><span aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm font-normal text-foreground">$</span><input aria-label="Rate / night" aria-invalid={Boolean(errors.rate)} className="h-9 w-full rounded-lg border border-input bg-background pl-5 pr-3 text-sm font-normal outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/20" id="rate" inputMode="decimal" name="rate" placeholder={fieldPlaceholders.rate} onChange={(event) => update('rate', event.target.value)} type="text" value={values.rate} /></span></label><p aria-live="polite" className="mt-1 min-h-4 text-xs text-destructive">{errors.rate}</p></div>;
  return (
    <form className="w-full space-y-4" onSubmit={submit} noValidate>
      <div className="grid w-full items-start gap-x-4 gap-y-2 lg:grid-cols-2">
        <div className="contents lg:flex lg:flex-col lg:gap-2">
          <Card className="order-1 w-full">
            <CardHeader><CardTitle className="text-sm font-bold leading-tight text-primary">BASIC DETAILS</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              {field('title', 'Title')}
              <div className="flex flex-col gap-1.5 text-xs font-semibold text-[var(--text-muted)]"><div className="flex items-center justify-between gap-2"><label htmlFor="description">Description</label><button aria-label="Open AI assistant" className="inline-flex items-center gap-1 rounded-md border border-border px-2 py-1 text-xs font-semibold text-primary" onClick={() => setAiOpen(true)} title="AI Description Assistant" type="button"><Sparkles aria-hidden="true" size={14} />AI Description Assistant</button></div><textarea className="min-h-28 rounded-lg border border-input bg-background p-3 text-sm font-normal outline-none focus:border-ring focus:ring-3 focus:ring-ring/20" id="description" name="description" placeholder={fieldPlaceholders.description} value={values.description} onChange={(event) => update('description', event.target.value)} /></div>
              <p className="-mt-2 min-h-4 text-xs text-destructive">{errors.description}</p>
              <div className="grid gap-2 sm:grid-cols-2">
                <label className="flex flex-col gap-1.5 text-xs font-semibold text-[var(--text-muted)]"><span>Property type</span><SelectField aria-label="Property type" name="propertyType" value={values.propertyType} options={propertyTypes.map((value) => ({ value, label: value }))} onValueChange={(value) => update('propertyType', value)} /></label>
                <label className="flex flex-col gap-1.5 text-xs font-semibold text-[var(--text-muted)]"><span>Status</span><SelectField aria-label="Status" name="status" value={values.status} options={[{ value: 'active', label: 'Active' }, { value: 'inactive', label: 'Inactive' }]} onValueChange={(value) => update('status', value)} /></label>
              </div>
            </CardContent>
          </Card>
          <Card className="order-3 w-full">
            <CardHeader><CardTitle className="text-sm font-bold leading-tight text-primary">LOCATION</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              {field('streetAddress', 'Street address')}
              <div className="grid gap-2 sm:grid-cols-3">{field('city', 'City')}{field('region', 'Region')}{field('postalCode', 'Postal code')}</div>
            </CardContent>
          </Card>
        </div>
        <div className="contents lg:flex lg:flex-col lg:gap-2">
          <Card className="order-2 w-full">
            <CardHeader><CardTitle className="text-sm font-bold leading-tight text-primary">CAPACITY & PRICING</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              <div className="grid gap-2 sm:grid-cols-2">{field('maxGuests', 'Max guests', 'number')}{field('bedrooms', 'Bedrooms', 'number')}</div>
              <div className="grid gap-2 sm:grid-cols-2">{field('bathrooms', 'Bathrooms', 'number')}{rateField}</div>
              <div className="grid gap-2 sm:grid-cols-2">{field('checkIn', 'Check-in time', 'time')}{field('checkOut', 'Check-out time', 'time')}</div>
            </CardContent>
          </Card>
          <Card className="order-4 w-full">
            <CardHeader><CardTitle className="text-sm font-bold leading-tight text-primary">AMENITIES</CardTitle></CardHeader>
            <CardContent className="grid gap-2 sm:grid-cols-2">
              {amenities.map((amenity) => { const value = amenity.toUpperCase().replaceAll(' ', '_'); return <label className="flex min-h-9 items-center gap-2 rounded-lg border border-border bg-background px-3 py-2 text-sm" key={amenity}><input checked={selectedAmenities.includes(value)} name="amenities" onChange={(event) => setSelectedAmenities((current) => event.target.checked ? [...current, value] : current.filter((item) => item !== value))} type="checkbox" value={value} />{amenity}</label>; })}
            </CardContent>
          </Card>
        </div>
      </div>
      <HostAiDescriptionAssistant
        description={values.description}
        listing={{ title: values.title, propertyType: values.propertyType.toUpperCase().replaceAll(' ', '_'), city: values.city, region: values.region, maxGuests: Number(values.maxGuests) || 0, bedrooms: Number(values.bedrooms) || 0, bathrooms: Number(values.bathrooms) || 0, amenities: selectedAmenities, checkInTime: values.checkIn, checkOutTime: values.checkOut }}
        listingId={listing?.id}
        onApply={(description) => update('description', description)}
        onClose={() => setAiOpen(false)}
        open={aiOpen}
      />
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex gap-2">
          <button className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground" disabled={saving} type="submit">{saving ? 'Saving…' : 'Save listing'}</button>
          <button className="rounded-lg border border-border px-4 py-2 text-sm" onClick={() => router.push(listing ? `/host/listings/${listing.id}` : '/host/listings')} type="button">Cancel</button>
        </div>
        {requestError && <p className="ml-auto self-center text-right text-sm text-destructive" role="alert">{requestError}</p>}
      </div>
    </form>
  );
}
