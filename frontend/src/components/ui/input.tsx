import type { InputHTMLAttributes } from 'react';
import { cn } from 'cn';

type FieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
};

export function Field({ className, id, label, ...props }: FieldProps) {
  const fieldId = id ?? props.name;
  return (
    <label className="flex flex-col gap-1.5 text-xs font-semibold text-[var(--text-muted)]" htmlFor={fieldId}>
      <span>{label}</span>
      <input
        id={fieldId}
        className={cn('h-9 rounded-lg border border-input bg-background px-3 text-sm outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/20', className)}
        {...props}
      />
    </label>
  );
}
