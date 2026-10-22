import { BackendApiError, authenticatedFetch } from './api';

export type AiMode = 'EVALUATE' | 'GENERATE';
export type AiWritingStyle = 'FRIENDLY' | 'PROFESSIONAL' | 'PERSUASIVE';
export type AiTargetAudience = 'FAMILIES' | 'COUPLES' | 'BUSINESS_TRAVELLERS' | 'ALL_TRAVELLERS';
export type AiDescriptionLength = 'SHORT' | 'MEDIUM' | 'LONG';
export type AiStatus = 'PASS' | 'IMPROVE';

export type ListingFactsPayload = {
  title: string;
  propertyType: string;
  city: string;
  region: string;
  maxGuests: number;
  bedrooms: number;
  bathrooms: number;
  amenities: string[];
  checkInTime: string;
  checkOutTime: string;
};

export type HostAiListingDescriptionRequest = {
  listingId?: string;
  mode: AiMode;
  writingStyle: AiWritingStyle;
  targetAudience: AiTargetAudience;
  length: AiDescriptionLength;
  currentDescription: string;
  listing: ListingFactsPayload;
};

export type AiCriterion = { status: AiStatus; feedback: string };
export type AiAnalysis = { clarity: AiCriterion; completeness: AiCriterion; accuracy: AiCriterion };
export type HostAiListingDescriptionResponse = {
  outcome: 'SUCCESS' | 'DEGRADED';
  mode: AiMode;
  currentAnalysis: AiAnalysis | null;
  generatedDescription: string | null;
  generatedAnalysis: AiAnalysis | null;
  gatewayCode: string | null;
  requestId: string;
};

export async function postHostAiListingDescription(
  payload: HostAiListingDescriptionRequest,
  signal?: AbortSignal,
): Promise<HostAiListingDescriptionResponse> {
  const response = await authenticatedFetch('/api/host/ai/listing-description', {
    method: 'POST',
    signal,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return response.json() as Promise<HostAiListingDescriptionResponse>;
}

export function hostAiGatewayMessage(code: string): string {
  if (code === 'DISABLED' || code === 'UPSTREAM_UNAVAILABLE' || code === 'TIMEOUT') {
    return 'AI assistance is temporarily unavailable. You can continue editing the description manually.';
  }
  if (code === 'RATE_LIMITED') return 'AI assistance is busy. Please try again shortly.';
  return 'Unable to complete the AI request. Please try again.';
}

export function hostAiSafeErrorMessage(error: unknown): string {
  if (error instanceof BackendApiError) {
    if (['DISABLED', 'UPSTREAM_UNAVAILABLE', 'TIMEOUT', 'RATE_LIMITED'].includes(error.code)) return hostAiGatewayMessage(error.code);
    if (error.code === 'DESCRIPTION_REQUIRED_FOR_EVALUATE') return 'Enter a description before evaluating it.';
    if (error.status === 401) return 'Your session has expired. Sign in again to continue.';
    if (error.status === 422) return 'This description request cannot be completed.';
  }
  return 'Unable to complete the AI request. Please try again.';
}
