import { Select } from '@base-ui/react/select';
import { Check, ChevronDown } from 'lucide-react';
import { cn } from 'cn';

type SelectOption = {
  value: string;
  label: string;
};

type SelectFieldProps = {
  'aria-label': string;
  name: string;
  value: string;
  options: SelectOption[];
  onValueChange: (value: string) => void;
};

export function SelectField({ 'aria-label': ariaLabel, name, value, options, onValueChange }: SelectFieldProps) {
  return (
    <Select.Root value={value} name={name} onValueChange={(nextValue) => { if (nextValue) onValueChange(nextValue); }}>
      <Select.Trigger
        aria-label={ariaLabel}
        className="flex h-9 w-full items-center justify-between rounded-lg border border-input bg-background px-3 py-1.5 text-sm font-normal shadow-xs outline-none transition-colors focus:border-ring focus:ring-3 focus:ring-ring/50"
      >
        <Select.Value>{options.find((option) => option.value === value)?.label ?? value}</Select.Value>
        <Select.Icon><ChevronDown className="size-4 opacity-70" /></Select.Icon>
      </Select.Trigger>
      <Select.Portal>
        <Select.Positioner alignItemWithTrigger={false} side="bottom" sideOffset={0} className="z-50">
          <Select.Popup className="min-w-[var(--anchor-width)] overflow-hidden rounded-lg border border-border bg-background p-1 text-sm shadow-lg outline-none">
            <Select.List>
              {options.map((option) => (
                <Select.Item
                  key={option.value}
                  value={option.value}
                  className={cn(
                    'flex cursor-default items-center justify-between rounded-md px-3 py-2 outline-none',
                    'data-[highlighted]:bg-muted data-[highlighted]:text-foreground',
                    'data-[selected]:bg-accent data-[selected]:text-accent-foreground',
                  )}
                >
                  <Select.ItemText>{option.label}</Select.ItemText>
                  <Select.ItemIndicator><Check className="size-4" /></Select.ItemIndicator>
                </Select.Item>
              ))}
            </Select.List>
          </Select.Popup>
        </Select.Positioner>
      </Select.Portal>
    </Select.Root>
  );
}
