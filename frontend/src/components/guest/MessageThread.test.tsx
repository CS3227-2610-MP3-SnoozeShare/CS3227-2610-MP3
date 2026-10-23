import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { MessageResponse } from '@/lib/guest-api';
import { MessageThread } from './MessageThread';

function message(authorRole: MessageResponse['authorRole'], body: string, authorId = 'other'): MessageResponse {
  return { id: body, authorId, authorRole, body, sentAt: '2026-10-23T04:00:00Z' };
}

describe('MessageThread', () => {
  const originalScrollIntoView = Object.getOwnPropertyDescriptor(Element.prototype, 'scrollIntoView');
  beforeEach(() => {
    Object.defineProperty(Element.prototype, 'scrollIntoView', { configurable: true, value: vi.fn() });
  });
  afterEach(() => {
    cleanup();
    if (originalScrollIntoView) Object.defineProperty(Element.prototype, 'scrollIntoView', originalScrollIntoView);
    else Reflect.deleteProperty(Element.prototype, 'scrollIntoView');
  });

  it('F7/R11 labels SYSTEM as Snoozy and renders the stored multiline body literally on the left', () => {
    const body = '<img src=x onerror=alert(1)>\n**bold**';
    const { container } = render(<MessageThread messages={[message('SYSTEM', body)]} currentUserId="guest" onSend={vi.fn()} />);
    const label = screen.getByText('Snoozy · Automated assistant');
    expect(label.parentElement?.parentElement).toHaveClass('justify-start');
    const text = container.querySelector('.whitespace-pre-wrap');
    expect(text?.textContent).toBe(body);
    expect(container.querySelector('img, strong')).toBeNull();
    expect(screen.getByRole('textbox')).toBeEnabled();
  });

  it.each([['GUEST', 'Guest'], ['HOST', 'Host'], ['AGENT', 'Support agent']] as const)(
    'F7 preserves the ordinary %s label', (role, label) => {
      render(<MessageThread messages={[message(role, 'Other message')]} currentUserId="guest" onSend={vi.fn()} />);
      expect(screen.getByText(label)).toBeInTheDocument();
    },
  );

  it('keeps own messages right aligned and sends a trimmed reply', async () => {
    const onSend = vi.fn().mockResolvedValue(undefined);
    render(<MessageThread messages={[message('GUEST', 'My message', 'guest')]} currentUserId="guest" onSend={onSend} />);
    expect(screen.getByText('My message').parentElement?.parentElement).toHaveClass('justify-end');
    fireEvent.change(screen.getByRole('textbox'), { target: { value: '  The issue started yesterday.  ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));
    await waitFor(() => expect(onSend).toHaveBeenCalledWith('The issue started yesterday.'));
    await waitFor(() => expect(screen.getByRole('textbox')).toHaveValue(''));
  });

  it('keeps the closed-thread composer hidden', () => {
    render(<MessageThread messages={[]} currentUserId="guest" onSend={vi.fn()} disabled />);
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  });
});
