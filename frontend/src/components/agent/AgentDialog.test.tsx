import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { AgentDialog } from './AgentDialog';

function Harness({ onClose = () => {} }: { onClose?: () => void }) {
  const [open, setOpen] = useState(false);
  return (
    <div>
      <button onClick={() => setOpen(true)}>Open</button>
      {open && (
        <AgentDialog title="Accept - remedy guest" onClose={() => { setOpen(false); onClose(); }}>
          <input aria-label="Reason" />
          <button>Confirm</button>
        </AgentDialog>
      )}
    </div>
  );
}

describe('AgentDialog (N7, E59)', () => {
  it('has dialog semantics and an accessible name', () => {
    render(<Harness />);
    fireEvent.click(screen.getByRole('button', { name: 'Open' }));
    const dialog = screen.getByRole('dialog', { name: 'Accept - remedy guest' });
    expect(dialog).toHaveAttribute('aria-modal', 'true');
  });

  it('moves focus inside, traps Tab, and closes on Escape returning focus to the opener', () => {
    const onClose = vi.fn();
    render(<Harness onClose={onClose} />);
    const opener = screen.getByRole('button', { name: 'Open' });
    opener.focus();
    fireEvent.click(opener);
    const dialog = screen.getByRole('dialog');
    expect(dialog.contains(document.activeElement)).toBe(true);
    const first = screen.getByLabelText('Reason');
    const last = screen.getByRole('button', { name: 'Cancel' });
    last.focus();
    fireEvent.keyDown(dialog, { key: 'Tab' });
    expect(document.activeElement).toBe(first);
    fireEvent.keyDown(dialog, { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(last);
    fireEvent.keyDown(dialog, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.activeElement).toBe(opener);
  });
});
