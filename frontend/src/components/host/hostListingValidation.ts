export type ListingFormValues = {
  title: string; description: string; propertyType: string; status: string; streetAddress: string;
  city: string; region: string; postalCode: string; maxGuests: string; bedrooms: string; bathrooms: string;
  rate: string; checkIn: string; checkOut: string;
};

export function validateListing(values: ListingFormValues) {
  const errors: Partial<Record<keyof ListingFormValues, string>> = {};
  if (!values.title.trim()) errors.title = 'Title is required';
  if (!values.description.trim()) errors.description = 'Description is required';
  if (!values.streetAddress.trim()) errors.streetAddress = 'Street address is required';
  if (!values.city.trim()) errors.city = 'City is required';
  if (!values.region.trim()) errors.region = 'Region is required';
  if (!values.postalCode.trim()) errors.postalCode = 'Postal code is required';
  if (!/^[1-9]\d*$/.test(values.maxGuests)) errors.maxGuests = 'Max guests must be at least 1';
  if (!/^\d+$/.test(values.bedrooms)) errors.bedrooms = 'Bedrooms cannot be negative';
  if (!/^\d+$/.test(values.bathrooms)) errors.bathrooms = 'Bathrooms cannot be negative';
  if (!/^\d+(\.\d{1,2})?$/.test(values.rate) || Number(values.rate) <= 0) errors.rate = 'Rate / night must be positive with at most 2 decimal places';
  return errors;
}
