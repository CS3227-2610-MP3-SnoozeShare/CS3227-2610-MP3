import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Button } from './button';
import { CalendarGrid } from './calendar-grid';
import { Card, CardContent, CardHeader, CardTitle } from './card';
import { Field } from './input';
import { Modal } from './modal';
import { SelectField } from './select';
import { StatusPill } from './status-pill';
import { Table, Tbody, Td, Th, Thead, Tr } from './table';

describe('Fall Light primitives', () => {
  it('renders shared card, field, select, table, and button semantics', () => {
    render(
      <Card>
        <CardHeader><CardTitle>Listing</CardTitle></CardHeader>
        <CardContent>
          <Field label="Title" name="title" defaultValue="Cabin" />
          <SelectField aria-label="Status" name="status" value="active" options={[{ value: 'active', label: 'Active' }]} onValueChange={() => {}} />
          <Button>Save</Button>
          <Table>
            <Thead><Tr><Th>Guest</Th></Tr></Thead>
            <Tbody><Tr><Td>Jane G</Td></Tr></Tbody>
          </Table>
        </CardContent>
      </Card>,
    );

    expect(screen.getByRole('heading', { name: 'Listing' })).toBeInTheDocument();
    expect(screen.getByLabelText('Title')).toHaveAttribute('name', 'title');
    expect(screen.getByRole('combobox', { name: 'Status' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save' })).toHaveClass('focus-visible:outline-2', 'focus-visible:outline-offset-2');
    expect(screen.getByRole('table')).toHaveTextContent('Jane G');
  });

  it('renders labelled statuses, modal semantics, and calendar day states', () => {
    render(
      <>
        <StatusPill status="active" />
        <Modal open title="Confirm block" description="Block the selected dates?" onClose={() => {}} onConfirm={() => {}} />
        <CalendarGrid monthLabel="September 2026" days={[{ date: '2026-09-15', label: '15', state: 'blocked' }]} />
      </>,
    );

    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: 'Confirm block' })).toHaveTextContent('Block the selected dates?');
    expect(screen.getByRole('grid', { name: 'September 2026' })).toHaveTextContent('15');
    expect(screen.getByLabelText('15, Blocked')).toBeInTheDocument();
  });
});
