import { cn } from "@/lib/utils";

/**
 * FitCoach wordmark. The accent slash is the brand's one signature mark — a forward
 * stroke that reads as momentum. Used in the app shell header and auth screens.
 */
export function Wordmark({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-baseline font-display text-xl font-extrabold tracking-tight",
        className,
      )}
    >
      Fit
      <span className="text-primary">/</span>
      Coach
    </span>
  );
}
