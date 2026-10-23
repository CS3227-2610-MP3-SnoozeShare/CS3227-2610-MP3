import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { AgentGrid, Cell, GridMessage, GridRow } from './AgentGrid';

const columns = [
  { label: 'Name', min: 120, fr: 1 },
  { label: 'Status', min: 140, fr: 2 },
];

describe('AgentGrid', () => {
  it('shares the full width between columns but never goes below the sum of their minimums', () => {
    render(
      <AgentGrid columns={columns} label="People">
        <GridRow><Cell>Ann</Cell><Cell>Active</Cell></GridRow>
      </AgentGrid>,
    );
    const table = screen.getByRole('table', { name: 'People' });
    expect(table).toHaveStyle({ minWidth: '260px' });
    const [header, row] = screen.getAllByRole('row');
    for (const line of [header, row]) {
      expect(line.getAttribute('style')).toContain('minmax(120px, 1fr) minmax(140px, 2fr)');
    }
    expect(screen.getAllByRole('columnheader').map((cell) => cell.textContent)).toEqual(['Name', 'Status']);
  });

  it('keeps the header outside the scrolling rows so the vertical scroll bar starts below it', () => {
    render(
      <AgentGrid columns={columns} label="People">
        <GridMessage>Nothing here</GridMessage>
      </AgentGrid>,
    );
    const rows = screen.getByRole('rowgroup');
    expect(rows).toHaveClass('overflow-y-auto');
    expect(rows).not.toContainElement(screen.getAllByRole('columnheader')[0]);
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
  });
});
