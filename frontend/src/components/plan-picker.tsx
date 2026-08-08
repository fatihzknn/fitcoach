"use client";

import * as React from "react";
import { ChevronDown, ChevronUp, Star, AlertTriangle, Check, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { type PlanOption, type WorkoutPlanDto, type WorkoutDayDto, type TrainerPhilosophyDto } from "@/lib/api";
import { cn } from "@/lib/utils";
import { useI18n } from "@/lib/i18n";

/**
 * Shared "pick a training philosophy, see recommended vs alternative" UI.
 * Originally built for /plan-selection (a client choosing their own plan);
 * extracted so the trainer portal's client-detail page can reuse the exact
 * same picker when assigning a plan to a client, instead of rebuilding it.
 * Every component here is purely prop-driven — no API calls inside, callers
 * own data fetching and pass callbacks down.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Trainer selector
// ─────────────────────────────────────────────────────────────────────────────

interface TrainerCardProps {
  trainer: TrainerPhilosophyDto;
  selected: boolean;
  onSelect: () => void;
}

export function TrainerCard({ trainer, selected, onSelect }: TrainerCardProps) {
  return (
    <button
      onClick={onSelect}
      className={cn(
        "flex flex-col items-start gap-1 rounded-xl border px-4 py-3 text-left transition-colors w-full",
        selected
          ? "border-primary bg-primary/8 text-foreground"
          : "border-border bg-elevated text-muted-foreground hover:bg-card hover:text-foreground",
      )}
    >
      <div className="flex items-center justify-between w-full gap-2">
        <span className={cn("text-sm font-semibold", selected && "text-primary")}>
          {trainer.displayName}
        </span>
        {selected && <Check className="h-3.5 w-3.5 text-primary flex-shrink-0" />}
      </div>
      <span className="text-xs leading-snug">{trainer.tagline}</span>
      {selected && (
        <p className="text-xs text-muted-foreground leading-relaxed mt-1 line-clamp-2">
          {trainer.description}
        </p>
      )}
    </button>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Plan card
// ─────────────────────────────────────────────────────────────────────────────

export function DayRow({ day }: { day: WorkoutDayDto }) {
  const { t } = useI18n();
  const [open, setOpen] = React.useState(false);
  return (
    <div className="rounded-lg border border-border overflow-hidden">
      <button
        className="flex w-full items-center justify-between px-4 py-3 text-left hover:bg-elevated transition-colors"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
      >
        <span className="font-medium text-sm">
          {t("Day {n}", { n: day.dayNumber })} — {day.workoutName}
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

export function PlanCard({ plan, option, isRecommended, submitting, onSelect }: PlanCardProps) {
  const { t } = useI18n();
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
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1">
          {isRecommended && (
            <div className="flex items-center gap-1.5 text-primary text-xs font-semibold uppercase tracking-wider mb-1">
              <Star className="h-3.5 w-3.5 fill-current" />
              {t("Recommended for you")}
            </div>
          )}
          <h2 className="font-display text-xl font-bold leading-tight">{plan.name}</h2>
          <p className="text-sm text-muted-foreground">
            {t("{n} days/week", { n: plan.trainingDaysPerWeek })}
          </p>
        </div>
        <button
          className="text-muted-foreground hover:text-foreground transition-colors mt-1"
          onClick={() => setExpanded((e) => !e)}
          aria-label={expanded ? t("Collapse") : t("Expand")}
        >
          {expanded ? <ChevronUp className="h-5 w-5" /> : <ChevronDown className="h-5 w-5" />}
        </button>
      </div>

      {plan.sustainabilityWarning && (
        <div className="flex gap-2 rounded-lg bg-amber-500/10 border border-amber-500/30 px-3 py-2">
          <AlertTriangle className="h-4 w-4 text-amber-500 flex-shrink-0 mt-0.5" />
          <p className="text-xs text-amber-400 leading-relaxed">{plan.sustainabilityWarning}</p>
        </div>
      )}

      {expanded && (
        <div className="space-y-2">
          {plan.days.map((day) => (
            <DayRow key={day.id} day={day} />
          ))}
        </div>
      )}

      <Button
        variant={isRecommended ? "primary" : "secondary"}
        size="lg"
        className="w-full"
        disabled={submitting}
        onClick={() => onSelect(option)}
      >
        {submitting ? t("Saving…") : isRecommended ? t("Start with this plan") : t("Choose this instead")}
      </Button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Personalizing overlay — shown while the selected plan is being saved
// ─────────────────────────────────────────────────────────────────────────────

const PERSONALIZING_STEPS = [
  "Analyzing your profile…",
  "Applying your training philosophy…",
  "Building your personalized program…",
  "Finalizing your plan…",
];

export function PersonalizingOverlay() {
  const { t } = useI18n();
  const [step, setStep] = React.useState(0);

  React.useEffect(() => {
    const id = setInterval(
      () => setStep((s) => (s + 1) % PERSONALIZING_STEPS.length),
      900,
    );
    return () => clearInterval(id);
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/95 px-6">
      <div className="w-full max-w-sm text-center space-y-6 animate-fade-up">
        <div className="flex items-center justify-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/15">
            <Loader2 className="h-8 w-8 text-primary animate-spin" />
          </div>
        </div>
        <p className="font-display text-xl font-bold tracking-tight">
          {t(PERSONALIZING_STEPS[step] ?? PERSONALIZING_STEPS[0]!)}
        </p>
      </div>
    </div>
  );
}
