import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

const stylesheet = readFileSync(join(process.cwd(), 'src/app/globals.css'), 'utf8');

describe('Fall Light design tokens', () => {
  it('keeps the Claude artifact canvas, surfaces, text, borders, and semantic colors', () => {
    for (const token of [
      '--background: #fdf8f0',
      '--foreground: #100604',
      '--surface-overlay: #ffffff',
      '--surface-inset: #f0e8d8',
      '--text-muted: #381a10',
      '--text-subtle: #6c4434',
      '--border: #c0a080',
      '--border-muted: #d8c4a8',
      '--primary: #98300c',
      '--success: #40680c',
      '--warning: #7e5400',
      '--destructive: #980c1c',
    ]) {
      expect(stylesheet).toContain(token);
    }
  });

  it('defines artifact spacing, radii, and accessible focus treatment', () => {
    expect(stylesheet).toContain('--space-6: 1.5rem');
    expect(stylesheet).toContain('--radius-pill: 999px');
    expect(stylesheet).toContain('focus-visible');
    expect(stylesheet).toContain('outline-offset');
  });
});
