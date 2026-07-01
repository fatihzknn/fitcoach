"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { ChevronDown, ChevronUp, Star, AlertTriangle, Check } from "lucide-react";
import { Wordmark } from "@/components/wordmark";
import { Button } from "@/components/ui/button";
import {
  api,
  ApiError,
  type PlanOption,
  type WorkoutPlanDto,
  type WorkoutDayDto,
  type TrainerPhilosophyDto,
} from "@/lib/api";
import { session } from "@/lib/session";
import { cn } from "@/lib/utils";

// ─────────────────────────────────────────────────────────────────────────────
// Trainer selector
// ─────────────────────────────────────────────────────────────────────────────

interface TrainerCardProps {
  trainer: TrainerPhilosophyDto;
  selected: boolean;
  onSelect: () => void;
}

function TrainerCard({ trainer, selected, onSelect }: TrainerCardProps) {
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
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1">
          {isRecommended && (
            <div className="flex items-center gap-1.5 text-primary text-xs font-semibold uppercase tracking-wider mb-1">
              <Star className="h-3.5 w-3.5 fill-current" />
              Recommended for you
            </div>
          )}
          <h2 className="font-display text-xl font-bold leading-tight">{plan.name}</h2>
          <p className="text-sm text-muted-foreground">
            {plan.trainingDaysPerWeek} days / week
          </p>
        </div>
        <button
          className="text-muted-foreground hover:text-foreground transition-colors mt-1"
          onClick={() => setExpanded((e) => !e)}
          aria-label={expanded ? "Collapse" : "Expand"}
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
        {submitting ? "Saving…" : isRecommended ? "Start with this plan" : "Choose this instead"}
      </Button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Page
// ─────────────────────────────────────────────────────────────────────────────

export default function PlanSelectionPage() {
  const router = useRouter();

  const [trainers, setTrainers] = React.useState<TrainerPhilosophyDto[]>([]);
  const [selectedTrainerId, setSelectedTrainerId] = React.useState<string | null>(null);
  const [recommended, setRecommended] = React.useState<WorkoutPlanDto | null>(null);
  const [alternative, setAlternative] = React.useState<WorkoutPlanDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [planLoading, setPlanLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [submitting, setSubmitting] = React.useState(false);

  // Load trainers on mount, then load plan options for the first trainer
  React.useEffect(() => {
    let active = true;
    setLoading(true);
    api.getTrainers()
      .then((data) => {
        if (!active) return;
        setTrainers(data);
        const firstId = data[0]?.id ?? null;
        setSelectedTrainerId(firstId);
        return firstId ? api.getPlanOptions(firstId) : null;
      })
      .then((options) => {
        if (!active || !options) return;
        setRecommended(options.recommended);
        setAlternative(options.alternative);
      })
      .catch((err: unknown) => {
        if (!active) return;
        setError(err instanceof ApiError ? err.message : "Could not load plans. Please try again.");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  // Re-fetch plans when trainer changes (not on initial load — handled above)
  async function handleTrainerSelect(trainerId: string) {
    if (trainerId === selectedTrainerId) return;
    setSelectedTrainerId(trainerId);
    setPlanLoading(true);
    setError(null);
    try {
      const options = await api.getPlanOptions(trainerId);
      setRecommended(options.recommended);
      setAlternative(options.alternative);
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : "Could not load plans. Please try again.");
    } finally {
      setPlanLoading(false);
    }
  }

  async function handleSelect(option: PlanOption) {
    setSubmitting(true);
    setError(null);
    try {
      await api.selectPlan({ option, trainerId: selectedTrainerId ?? undefined });
      session.setPlanSelected();
      router.replace("/today");
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : "Could not save plan. Please try again.");
      setSubmitting(false);
    }
  }

  const selectedTrainer = trainers.find((t) => t.id === selectedTrainerId);
  const plansReady = !loading && !planLoading && recommended && alternative;

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
            Choose your style
          </h1>
          <p className="text-sm text-muted-foreground">
            Pick a training philosophy, then select your plan. The approach shapes your
            rep ranges, rest times, and intensity.
          </p>
        </div>

        {error && (
          <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">
            {error}
          </p>
        )}

        {/* Trainer selector */}
        {loading ? (
          <div className="space-y-2 animate-pulse">
            <div className="h-16 rounded-xl bg-card" />
            <div className="h-16 rounded-xl bg-card" />
          </div>
        ) : (
          <div className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground px-0.5">
              Training philosophy
            </p>
            {trainers.map((trainer) => (
              <TrainerCard
                key={trainer.id}
                trainer={trainer}
                selected={trainer.id === selectedTrainerId}
                onSelect={() => handleTrainerSelect(trainer.id)}
              />
            ))}
          </div>
        )}

        {/* Trainer stats badge */}
        {selectedTrainer && !loading && (
          <div className="flex flex-wrap gap-3 text-center">
            {[
              { label: "Compound reps", value: `${selectedTrainer.compoundRepMin}–${selectedTrainer.compoundRepMax}` },
              { label: "Isolation reps", value: `${selectedTrainer.isolationRepMin}–${selectedTrainer.isolationRepMax}` },
              { label: "Rest (compound)", value: `${Math.round(selectedTrainer.restSecondsCompound / 60)}–${Math.ceil(selectedTrainer.restSecondsCompound / 60) + 0.5} min` },
              { label: "Target RIR", value: `${selectedTrainer.rirTarget} RIR` },
            ].map((stat) => (
              <div key={stat.label} className="flex-1 min-w-[80px] rounded-lg bg-elevated border border-border px-3 py-2">
                <p className="text-xs font-bold">{stat.value}</p>
                <p className="text-[10px] text-muted-foreground leading-tight">{stat.label}</p>
              </div>
            ))}
          </div>
        )}

        {/* Divider */}
        {!loading && (
          <div className="flex items-center gap-3">
            <div className="flex-1 h-px bg-border" />
            <span className="text-xs text-muted-foreground font-medium">Your generated plans</span>
            <div className="flex-1 h-px bg-border" />
          </div>
        )}

        {/* Plans skeleton while switching trainer */}
        {(loading || planLoading) && (
          <div className="space-y-4 animate-pulse">
            <div className="h-40 rounded-2xl bg-card" />
            <div className="h-32 rounded-2xl bg-card" />
          </div>
        )}

        {/* Plans */}
        {plansReady && (
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
