import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { HostAiDescriptionAssistant } from './HostAiDescriptionAssistant';

const { postHostAiListingDescription } = vi.hoisted(() => ({ postHostAiListingDescription: vi.fn() }));
vi.mock('@/lib/hostAiApi', async (importOriginal) => ({
  ...await importOriginal<typeof import('@/lib/hostAiApi')>(),
  postHostAiListingDescription,
}));

const listing = { title: 'Cabin', propertyType: 'HOUSE', city: 'Reno', region: 'NV', maxGuests: 2, bedrooms: 1, bathrooms: 1, amenities: ['WIFI'], checkIn: '15:00', checkOut: '11:00' };
const response = { outcome: 'SUCCESS' as const, mode: 'GENERATE' as const, currentAnalysis: null, generatedDescription: 'A grounded suggested description.', generatedAnalysis: { clarity: { status: 'PASS' as const, feedback: 'Clear.' }, completeness: { status: 'PASS' as const, feedback: 'Complete.' }, accuracy: { status: 'PASS' as const, feedback: 'Accurate.' } }, gatewayCode: null, requestId: 'request-1' };

describe('HostAiDescriptionAssistant', () => {
  it('copies the description, supports empty Generate, and applies only generated text', async () => {
    postHostAiListingDescription.mockResolvedValue(response);
    const onApply = vi.fn();
    const onClose = vi.fn();
    render(<HostAiDescriptionAssistant open listing={listing} description="Existing copy" onApply={onApply} onClose={onClose} />);

    expect(screen.getByLabelText('Current description')).toHaveValue('Existing copy');
    fireEvent.change(screen.getByLabelText('Current description'), { target: { value: '' } });
    expect(screen.getByRole('button', { name: 'Evaluate' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'Generate' }));

    await waitFor(() => expect(screen.getByLabelText('Generated description')).toHaveValue(response.generatedDescription));
    fireEvent.click(screen.getByRole('button', { name: 'Apply generated description' }));
    expect(onApply).toHaveBeenCalledWith(response.generatedDescription);
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('disables every action while waiting for the response', async () => {
    let resolve: (value: typeof response) => void = () => undefined;
    postHostAiListingDescription.mockReturnValue(new Promise<typeof response>((nextResolve) => { resolve = nextResolve; }));
    render(<HostAiDescriptionAssistant open listing={listing} description="Existing copy" onApply={vi.fn()} onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: 'Generate' }));
    expect(screen.getByRole('button', { name: 'Generating…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Close AI Description Assistant' })).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent('Loading');
    resolve(response);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Generate' })).not.toBeDisabled());
  });
});
