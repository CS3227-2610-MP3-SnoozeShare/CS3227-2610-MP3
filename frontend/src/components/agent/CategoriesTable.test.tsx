import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendApiError } from '@/lib/api';
import { CategoriesTable } from './CategoriesTable';

const api = vi.hoisted(() => ({
  list: undefined as undefined | (() => Promise<unknown>),
  create: undefined as undefined | ((...args: unknown[]) => Promise<unknown>),
  rename: undefined as undefined | ((...args: unknown[]) => Promise<unknown>),
  toggle: undefined as undefined | ((...args: unknown[]) => Promise<unknown>),
  remove: undefined as undefined | ((...args: unknown[]) => Promise<unknown>),
}));
vi.mock('@/lib/agent-api/categories', () => ({
  fetchCategories: () => api.list!(),
  createCategory: (...args: unknown[]) => api.create!(...args),
  renameCategory: (...args: unknown[]) => api.rename!(...args),
  setCategoryActive: (...args: unknown[]) => api.toggle!(...args),
  deleteCategory: (...args: unknown[]) => api.remove!(...args),
}));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ signOut: vi.fn() }) }));

const rows = [
  { id: 'c-1', label: 'Noise', active: true },
  { id: 'c-2', label: '<img src=x onerror=alert(1)>', active: false },
];

describe('CategoriesTable (F12, F15)', () => {
  const create = vi.fn();
  const rename = vi.fn();
  const toggle = vi.fn();
  const remove = vi.fn();
  beforeEach(() => {
    [create, rename, toggle, remove].forEach((fn) => fn.mockReset());
    api.list = () => Promise.resolve(rows);
    api.create = create;
    api.rename = rename;
    api.toggle = toggle;
    api.remove = remove;
  });

  it('lists categories with accessible switches and literal labels (E36)', async () => {
    const { container } = render(<CategoriesTable />);

    expect(await screen.findByRole('switch', { name: 'Toggle active: Noise' })).toBeChecked();
    expect(screen.getByRole('switch', { name: 'Toggle active: <img src=x onerror=alert(1)>' })).not.toBeChecked();
    expect(container.querySelector('img')).toBeNull();
  });

  it('shows the empty copy (E50)', async () => {
    api.list = () => Promise.resolve([]);
    render(<CategoriesTable />);
    expect(await screen.findByText('No categories yet')).toBeInTheDocument();
  });

  it('toggles at once and reverts with an inline error on failure', async () => {
    toggle.mockRejectedValue(new BackendApiError('NOT_FOUND', 'Category does not exist', undefined, 404));
    render(<CategoriesTable />);
    const noise = await screen.findByRole('switch', { name: 'Toggle active: Noise' });

    fireEvent.click(noise);
    expect(noise).not.toBeChecked();

    expect(await screen.findByRole('alert')).toHaveTextContent('Category does not exist');
    await waitFor(() => expect(noise).toBeChecked());
    expect(toggle).toHaveBeenCalledWith('c-1', false);
  });

  it('adds a category', async () => {
    create.mockResolvedValue({ id: 'c-3', label: 'Smell', active: true });
    render(<CategoriesTable />);
    fireEvent.click(await screen.findByRole('button', { name: '+ Add category' }));

    const dialog = screen.getByRole('dialog', { name: 'Add category' });
    const save = within(dialog).getByRole('button', { name: 'Save' });
    expect(save).toBeDisabled();
    fireEvent.change(within(dialog).getByLabelText('Label'), { target: { value: '  Smell ' } });
    fireEvent.click(save);

    await waitFor(() => expect(create).toHaveBeenCalledWith('Smell'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('renames a category and shows a duplicate error inline', async () => {
    rename.mockRejectedValue(new BackendApiError('INVALID_REQUEST', 'Category label already exists', undefined, 400));
    render(<CategoriesTable />);
    fireEvent.click((await screen.findAllByRole('button', { name: 'Edit' }))[0]);

    const dialog = screen.getByRole('dialog', { name: 'Edit category' });
    fireEvent.change(within(dialog).getByLabelText('Label'), { target: { value: 'Cleanliness' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Category label already exists');
    expect(rename).toHaveBeenCalledWith('c-1', 'Cleanliness');
  });

  it('confirms before deleting and shows the 409 message', async () => {
    remove.mockRejectedValue(new BackendApiError('CONFLICT', 'Category is in use by tickets; deactivate it instead', undefined, 409));
    render(<CategoriesTable />);
    fireEvent.click((await screen.findAllByRole('button', { name: 'Edit' }))[0]);

    fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
    expect(screen.getByText('Delete Noise? This cannot be undone.')).toBeInTheDocument();
    expect(remove).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Confirm delete' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Category is in use by tickets; deactivate it instead');
    expect(remove).toHaveBeenCalledWith('c-1');
  });

  it('replaces the page with the session notice on a 403 (E45)', async () => {
    api.list = () => Promise.reject(new BackendApiError('PROFILE_INACTIVE', 'x', undefined, 403));
    render(<CategoriesTable />);

    expect(await screen.findByText('Your account is suspended.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
  });
});
