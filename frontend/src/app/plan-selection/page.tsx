"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { ChevronDown, ChevronUp, Star, AlertTriangle } from "lucide-react";
import { Wordmark } from "@/components/wordmark";
import { Button } from "@/components/ui/button";
import { api, ApiError, type PlanOption, type WorkoutPlanDto, type WorkoutDayDto } from "@/lib/api";
import { session } from "@/lib/session";
import { cn } from "@/lib/utils";

// ─────────────────────────────────────────────────────────────────────────────
// Plan card
// ─────────────────────────────────────────────────────────────────────────────

function DayRow({ day }: { day: WorkoutDayDto }) {
  const [open, setOpen] = React.useState(false);
  return (
    <div className="rounded-lg border border-border overflow-hidden">
      <button
        className="flex w-full items-center justify-between px-4 py-3 text-left hover:bg-elevated transition-colors"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
      >
        <span className="font-medium text-sm">
          Day {day.dayNumber} — {day.workoutName}
        </span>
        {open ? (
          <ChevronUp className="h-4 w-4 text-muted-foreground flex-shrink-0" />
        ) : (
          <ChevronDown className="h-4 w-4 text-muted-foreground flex-shrink-0" />
        )}
      </button>
      {open && (
        <div className="border-t border-border bg-card/40 px-4 py-3 space-y-2">
          {day.exercises.map((we) => (
            <div key={we.id} className="flex items-start justify-between gap-4">
              <span className="text-sm text-foreground">{we.exercise.name}</span>
              <span className="text-xs text-muted-foreground whitespace-nowrap">
                {we.sets}×{we.repRangeMin}–{we.repRangeMax} · {we.rirGuidance}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

interface PlanCardProps {
  plan: WorkoutPlanDto;
  option: PlanOption;
  isRecommended: boolean;
  submitting: boolean;
  onSelect: (option: PlanOption) => void;
}

function PlanCard({ plan, option, isRecommended, submitting, onSelect }: PlanCardProps) {
  const [expanded, setExpanded] = React.useState(isRecommended);

  return (
    <div
      className={cn(
        "rounded-2xl border p-5 space-y-4 transition-colors",
        isRecommended
          ? "border-primary bg-primary/5"
          : "border-border bg-card",
      )}
    >
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1">
          {isRecommended && (
            <div className="flex items-center gap-1.5 text-primary text-xs font-semibold uppercase tracking-wider mb-1">
              <Star className="h-3.5 w-3.5 fill-current" />
              Recommended for you
            </div>
          )}
          <h2 className="font-display text-xl font-bold leading-tight">
            {plan.name}
          </h2>
          <p className="text-sm text-muted-foreground">
            {plan.trainingDaysPerWeek} days / week
          </p>
        </div>
        <button
          className="text-muted-foreground hover:text-foreground transition-colors mt-1"
          onClick={() => setExpanded((e) => !e)}
          aria-label={expanded ? "Collapse" : "Expand"}
        >
          {expanded ? (
            <ChevronUp className="h-5 w-5" />
          ) : (
            <ChevronDown className="h-5 w-5" />
          )}
        </button>
      </div>

      {/* Sustainability warning */}
      {plan.sustainabilityWarning && (
        <div className="flex gap-2 rounded-lg bg-amber-500/10 border border-amber-500/30 px-3 py-2">
          <AlertTriangle className="h-4 w-4 text-amber-500 flex-shrink-0 mt-0.5" />
          <p className="text-xs text-amber-400 leading-relaxed">
            {plan.sustainabilityWarning}
          </p>
        </div>
      )}

      {/* Workout days */}
      {expanded && (
        <div className="space-y-2">
          {plan.days.map((day) => (
            <DayRow key={day.id} day={day} />
          ))}
        </div>
      )}

      {/* CTA */}
      <Button
        variant={isRecommended ? "primary" : "secondary"}
        size="lg"
        className="w-full"
        disabled={submitting}
        onClick={() => onSelect(option)}
      >
        {submitting
          ? "Saving…"
          : isRecommended
            ? "Start with this plan"
            : "Choose this instead"}
      </Button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Page
// ─────────────────────────────────────────────────────────────────────────────

export default function PlanSelectionPage() {
  const router = useRouter();
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [recommended, setRecommended] = React.useState<WorkoutPlanDto | null>(null);
  const [alternative, setAlternative] = React.useState<WorkoutPlanDto | null>(null);
  const [submitting, setSubmitting] = React.useState(false);

  React.useEffect(() => {
    let active = true;
    api
      .getPlanOptions()
      .then((data) => {
        if (!active) return;
        setRecommended(data.recommended);
        setAlternative(data.alternative);
      })
      .catch((err: unknown) => {
        if (!active) return;
        setError(
          err instanceof ApiError
            ? err.message
            : "Could not load plan options. Please try again.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  async function handleSelect(option: PlanOption) {
    setSubmitting(true);
    setError(null);
    try {
      await api.selectPlan({ option });
      session.setPlanSelected();
      router.replace("/today");
    } catch (err: unknown) {
      setError(
        err instanceof ApiError
          ? err.message
          : "Could not save plan. Please try again.",
      );
      setSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-svh flex-col items-center bg-background px-4 py-8">
      <div className="w-full max-w-md space-y-6">
        {/* Header */}
        <div className="space-y-1">
          <Wordmark className="h-6 mb-4" />
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
            Step 3
          </p>
          <h1 className="font-display text-3xl font-extrabold leading-tight tracking-tight">
            Your training plan
          </h1>
          <p className="text-sm text-muted-foreground">
            Based on your goals, we generated two plans. Pick the one that fits
            best — you can always change it later.
          </p>
        </div>

        {/* Error */}
        {error && (
          <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">
            {error}
          </p>
        )}

        {/* Loading skeleton */}
        {loading && (
          <div className="space-y-4 animate-pulse">
            <div className="h-40 rounded-2xl bg-card" />
            <div className="h-32 rounded-2xl bg-card" />
          </div>
        )}

        {/* Plans */}
        {!loading && recommended && alternative && (
          <div className="space-y-4">
            <PlanCard
              plan={recommended}
              option="RECOMMENDED"
              isRecommended={true}
              submitting={submitting}
              onSelect={handleSelect}
            />
            <PlanCard
              plan={alternative}
              option="ALTERNATIVE"
              isRecommended={false}
              submitting={submitting}
              onSelect={handleSelect}
            />
          </div>
        )}
      </div>
    </main>
  );
}
