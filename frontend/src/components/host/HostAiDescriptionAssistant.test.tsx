import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { StrictMode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { HostAiDescriptionAssistant } from './HostAiDescriptionAssistant';

const { postHostAiListingDescription } = vi.hoisted(() => ({ postHostAiListingDescription: vi.fn() }));
vi.mock('@/lib/hostAiApi', async (importOriginal) => ({
  ...await importOriginal<typeof import('@/lib/hostAiApi')>(),
  postHostAiListingDescription,
}));

const listing = { title: 'Cabin', propertyType: 'HOUSE', city: 'Reno', region: 'NV', maxGuests: 2, bedrooms: 1, bathrooms: 1, amenities: ['WIFI'], checkInTime: '15:00', checkOutTime: '11:00' };
const response = { outcome: 'SUCCESS' as const, mode: 'GENERATE' as const, currentAnalysis: { clarity: { status: 'PASS' as const, feedback: 'Clear.' }, completeness: { status: 'IMPROVE' as const, feedback: 'Add the verified amenities.' }, accuracy: { status: 'PASS' as const, feedback: 'Accurate.' } }, generatedDescription: 'A grounded suggested description.', generatedAnalysis: { clarity: { status: 'PASS' as const, feedback: 'Clear.' }, completeness: { status: 'IMPROVE' as const, feedback: 'Add the verified amenities.' }, accuracy: { status: 'PASS' as const, feedback: 'Accurate.' } }, gatewayCode: null, requestId: 'request-1' };
const degradedResponse = { outcome: 'DEGRADED' as const, mode: 'GENERATE' as const, currentAnalysis: null, generatedDescription: null, generatedAnalysis: null, gatewayCode: 'DISABLED', requestId: 'request-2' };

describe('HostAiDescriptionAssistant', () => {
  it('copies the description, supports empty Generate, and applies only generated text', async () => {
    postHostAiListingDescription.mockResolvedValue(response);
    const onApply = vi.fn();
    const onClose = vi.fn();
    render(<StrictMode><HostAiDescriptionAssistant open listing={listing} description="Existing copy" onApply={onApply} onClose={onClose} /></StrictMode>);

    expect(screen.getByText('Your listing details help the assistant create a grounded description based on the property information you provide.')).toBeInTheDocument();
    expect(screen.getByLabelText('Current description')).toHaveValue('Existing copy');
    expect(screen.getByLabelText('Description focus')).toHaveTextContent('Balanced');
    expect(screen.getByLabelText('Target audience')).toHaveTextContent('All travellers');
    expect(screen.queryByText('Description length')).not.toBeInTheDocument();
    expect(screen.getByRole('dialog')).toHaveClass('flex', 'flex-col', 'overflow-hidden');
    expect(screen.getByRole('group', { name: 'AI assistant actions' })).toHaveClass('shrink-0', 'flex-col', 'sm:flex-row');
    fireEvent.change(screen.getByLabelText('Current description'), { target: { value: '' } });
    expect(screen.getByRole('button', { name: 'Evaluate' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: /^Generate/ }));

    await waitFor(() => expect(screen.getByLabelText('Generated description')).toHaveValue(response.generatedDescription));
    expect(screen.queryByText('PASS: Clear.')).not.toBeInTheDocument();
    expect(screen.getAllByText('Clear.')).toHaveLength(2);
    expect(postHostAiListingDescription).toHaveBeenCalledWith(expect.objectContaining({
      descriptionFocus: 'BALANCED',
      targetAudience: 'ALL_TRAVELLERS',
      listing: expect.objectContaining({ checkInTime: '15:00', checkOutTime: '11:00' }),
    }));
    fireEvent.click(screen.getByRole('combobox', { name: 'Writing style' }));
    fireEvent.click(screen.getByRole('option', { name: 'Professional' }));
    fireEvent.click(screen.getByRole('combobox', { name: 'Target audience' }));
    fireEvent.click(screen.getByRole('option', { name: 'Families' }));
    fireEvent.click(screen.getByRole('combobox', { name: 'Description focus' }));
    fireEvent.click(screen.getByRole('option', { name: 'Comfort' }));
    fireEvent.click(screen.getByRole('button', { name: 'Apply generated description' }));
    expect(onApply).toHaveBeenCalledWith(response.generatedDescription);
    expect(onClose).toHaveBeenCalledOnce();
    expect(screen.getByLabelText('Writing style')).toHaveTextContent('Friendly');
    expect(screen.getByLabelText('Target audience')).toHaveTextContent('All travellers');
    expect(screen.getByLabelText('Description focus')).toHaveTextContent('Balanced');
  });

  it('closes from Escape, the backdrop, and the X button', () => {
    const onClose = vi.fn();
    const { rerender } = render(<HostAiDescriptionAssistant open listing={listing} description="Existing copy" onApply={vi.fn()} onClose={onClose} />);

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledOnce();

    fireEvent.click(screen.getByRole('presentation'));
    expect(onClose).toHaveBeenCalledTimes(2);

    rerender(<HostAiDescriptionAssistant open listing={listing} description="Existing copy" onApply={vi.fn()} onClose={onClose} />);
    fireEvent.click(screen.getByRole('button', { name: 'Close AI Description Assistant' }));
    expect(onClose).toHaveBeenCalledTimes(3);
  });

  it('places current analysis and errors below the current description and shows status icons', async () => {
    postHostAiListingDescription.mockResolvedValue(response);
    render(<HostAiDescriptionAssistant open listing={listing} description="Existing copy" onApply={vi.fn()} onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: /^Generate/ }));
    await waitFor(() => expect(screen.getByLabelText('Generated description')).toHaveValue(response.generatedDescription));

    const currentDescription = screen.getByLabelText('Current description');
    const currentAnalysis = screen.getByRole('heading', { name: 'Current analysis' });
    const generateButton = screen.getByRole('button', { name: /^Generate/ });
    expect(currentDescription.compareDocumentPosition(currentAnalysis) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(currentAnalysis.compareDocumentPosition(generateButton) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(screen.getAllByTestId('analysis-pass-icon')).toHaveLength(4);
    expect(screen.getAllByTestId('analysis-improve-icon')).toHaveLength(2);

    postHostAiListingDescription.mockResolvedValue(degradedResponse);
    fireEvent.click(generateButton);
    const error = await screen.findByRole('alert');
    expect(error.compareDocumentPosition(generateButton) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('keeps matching descriptions and analyses in source/output order', async () => {
    postHostAiListingDescription.mockResolvedValue(response);
    render(<HostAiDescriptionAssistant open listing={listing} description="Existing copy" onApply={vi.fn()} onClose={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: /^Generate/ }));
    await waitFor(() => expect(screen.getByLabelText('Generated description')).toHaveValue(response.generatedDescription));

    const currentDescription = screen.getByLabelText('Current description');
    const generatedDescription = screen.getByLabelText('Generated description');
    const currentAnalysis = screen.getByRole('heading', { name: 'Current analysis' });
    const generatedAnalysis = screen.getByRole('heading', { name: 'Generated analysis' });
    expect(currentDescription.compareDocumentPosition(generatedDescription) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(currentAnalysis.compareDocumentPosition(generatedAnalysis) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('preserves generated state across reopen, refreshes current state, and clears on apply', async () => {
    postHostAiListingDescription.mockResolvedValue(response);
    const onApply = vi.fn();
    const onClose = vi.fn();
    const props = { listing, onApply, onClose };
    const { rerender } = render(<HostAiDescriptionAssistant {...props} open description="Existing copy" />);

    fireEvent.click(screen.getByRole('button', { name: /^Generate/ }));
    await waitFor(() => expect(screen.getByLabelText('Generated description')).toHaveValue(response.generatedDescription));

    rerender(<HostAiDescriptionAssistant {...props} open={false} description="Existing copy" />);
    rerender(<HostAiDescriptionAssistant {...props} open description="Refreshed copy" />);
    expect(screen.getByLabelText('Current description')).toHaveValue('Refreshed copy');
    expect(screen.getByLabelText('Generated description')).toHaveValue(response.generatedDescription);
    expect(screen.getByText('Evaluate the current description to see feedback.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Apply generated description' }));
    expect(onApply).toHaveBeenCalledWith(response.generatedDescription);
    rerender(<HostAiDescriptionAssistant {...props} open={false} description={response.generatedDescription} />);
    rerender(<HostAiDescriptionAssistant {...props} open description={response.generatedDescription} />);
    expect(screen.getByLabelText('Generated description')).toHaveValue('');
  });

  it('disables every action while waiting for the response', async () => {
    let resolve: (value: typeof response) => void = () => undefined;
    postHostAiListingDescription.mockReturnValue(new Promise<typeof response>((nextResolve) => { resolve = nextResolve; }));
    render(<StrictMode><HostAiDescriptionAssistant open listing={listing} description="Existing copy" onApply={vi.fn()} onClose={vi.fn()} /></StrictMode>);

    fireEvent.click(screen.getByRole('button', { name: /^Generate/ }));
    expect(screen.getByRole('button', { name: 'Generating…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Close AI Description Assistant' })).toBeDisabled();
    expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
    resolve(response);
    await waitFor(() => expect(screen.getByRole('button', { name: /^Generate/ })).not.toBeDisabled());
  });

  it('shows a degraded response and unlocks the assistant', async () => {
    postHostAiListingDescription.mockResolvedValue(degradedResponse);
    render(<StrictMode><HostAiDescriptionAssistant open listing={listing} description="Existing copy" onApply={vi.fn()} onClose={vi.fn()} /></StrictMode>);

    fireEvent.click(screen.getByRole('button', { name: /^Generate/ }));

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('AI assistance is temporarily unavailable'));
    expect(screen.getByRole('button', { name: /^Generate/ })).not.toBeDisabled();
    expect(screen.getByRole('button', { name: 'Close AI Description Assistant' })).not.toBeDisabled();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('shows an error response and unlocks the assistant', async () => {
    postHostAiListingDescription.mockRejectedValue(new Error('request failed'));
    render(<StrictMode><HostAiDescriptionAssistant open listing={listing} description="Existing copy" onApply={vi.fn()} onClose={vi.fn()} /></StrictMode>);

    fireEvent.click(screen.getByRole('button', { name: /^Generate/ }));

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Unable to complete the AI request'));
    expect(screen.getByRole('button', { name: /^Generate/ })).not.toBeDisabled();
    expect(screen.getByRole('button', { name: 'Close AI Description Assistant' })).not.toBeDisabled();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });
});
