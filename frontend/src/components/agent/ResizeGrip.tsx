'use client';

type Props = {
  height: number;
  min: number;
  max: number;
  onChange: (height: number) => void;
  label: string;
};

const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value));

/**
 * A thin strip under a box: drag it (or use the arrow keys) to set the box's height, as the desktop HeightGrip does
 * for the chat boxes and the notes field.
 */
export function ResizeGrip({ height, min, max, onChange, label }: Props) {
  function onPointerDown(event: React.PointerEvent<HTMLDivElement>) {
    event.preventDefault();
    const startY = event.clientY;
    const startHeight = height;
    const move = (moved: PointerEvent) => onChange(clamp(startHeight + moved.clientY - startY, min, max));
    const stop = () => {
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', stop);
      window.removeEventListener('pointercancel', stop);
    };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', stop);
    window.addEventListener('pointercancel', stop);
  }

  return (
    <div
      role="separator"
      aria-orientation="horizontal"
      aria-label={label}
      aria-valuemin={min}
      aria-valuemax={max}
      aria-valuenow={Math.round(height)}
      tabIndex={0}
      onPointerDown={onPointerDown}
      onKeyDown={(event) => {
        if (event.key === 'ArrowDown') {
          event.preventDefault();
          onChange(clamp(height + 16, min, max));
        } else if (event.key === 'ArrowUp') {
          event.preventDefault();
          onChange(clamp(height - 16, min, max));
        }
      }}
      className="group flex h-2.5 shrink-0 cursor-s-resize touch-none items-center justify-center"
    >
      <span className="h-[3px] w-9 rounded-sm bg-border group-hover:bg-primary group-focus-visible:bg-primary" />
    </div>
  );
}
