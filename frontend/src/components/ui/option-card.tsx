"use client";

import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * A large, tappable choice. Used for single- and multi-select onboarding steps.
 * Selected state uses the primary accent border + a check; hit area is full-width and tall.
 */
export function OptionCard({
  label,
  hint,
  selected,
  onSelect,
}: {
  label: string;
  hint?: string;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      aria-pressed={selected}
      className={cn(
        "flex w-full items-center justify-between gap-3 rounded-lg border px-5 py-4 text-left transition-colors",
        selected
          ? "border-primary bg-primary/10"
          : "border-border bg-card hover:bg-elevated",
      )}
    >
      <span>
        <span className="block font-medium">{label}</span>
        {hint && <span className="mt-0.5 block text-sm text-muted-foreground">{hint}</span>}
      </span>
      <span
        className={cn(
          "flex h-6 w-6 shrink-0 items-center justify-center rounded-full border",
          selected ? "border-primary bg-primary text-primary-foreground" : "border-border",
        )}
        aria-hidden
      >
        {selected && <Check className="h-4 w-4" />}
      </span>
    </button>
  );
}
