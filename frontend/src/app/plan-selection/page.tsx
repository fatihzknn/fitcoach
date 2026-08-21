"use client";

import * as React from "react";
import { Wordmark } from "@/components/wordmark";
import { TrainerCard, PlanCard, PersonalizingOverlay } from "@/components/plan-picker";
import {
  api,
  ApiError,
  type PlanOption,
  type WorkoutPlanDto,
  type TrainerPhilosophyDto,
} from "@/lib/api";
import { session } from "@/lib/session";
import { useI18n } from "@/lib/i18n";

// ─────────────────────────────────────────────────────────────────────────────
// Page
// ─────────────────────────────────────────────────────────────────────────────

export default function PlanSelectionPage() {
  const { t } = useI18n();

  const [trainers, setTrainers] = React.useState<TrainerPhilosophyDto[]>([]);
  const [selectedTrainerId, setSelectedTrainerId] = React.useState<string | null>(null);
  const [recommended, setRecommended] = React.useState<WorkoutPlanDto | null>(null);
  const [alternative, setAlternative] = React.useState<WorkoutPlanDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [planLoading, setPlanLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [submitting, setSubmitting] = React.useState(false);

  // Load trainers on mount, then load plan options for the first trainer. The
  // "personalizing" overlay belongs here — before the picker is shown — not on
  // the final select step, so it reads as "building your plans" rather than a
  // redundant confirmation delay after the user has already chosen one.
  React.useEffect(() => {
    let active = true;
    setLoading(true);
    const minDelay = new Promise((resolve) => setTimeout(resolve, 2400));
    const load = api.getTrainers()
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
        setError(err instanceof ApiError ? err.message : t("Could not load plans. Please try again."));
      });
    Promise.all([load, minDelay]).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [t]);

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
      setError(err instanceof ApiError ? err.message : t("Could not load plans. Please try again."));
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
      // A plain router.replace() here can resolve /today from Next's client
      // router cache instead of hitting the server: if that path was ever
      // prefetched earlier this session while unonboarded (e.g. a
      // self-tracking trainer's "My Program" link on the panel, prefetched
      // before they'd finished onboarding), the cached response is
      // middleware's *old* redirect-to-/onboarding — router.refresh() does
      // not invalidate it, it only revalidates the current route. A hard
      // navigation is the only way to guarantee middleware re-runs against
      // the fc_plan cookie we just set.
      window.location.assign("/today");
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : t("Could not save plan. Please try again."));
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
            {t("Step 3")}
          </p>
          <h1 className="font-display text-3xl font-extrabold leading-tight tracking-tight">
            {t("Choose your style")}
          </h1>
          <p className="text-sm text-muted-foreground">
            {t("Pick a training philosophy, then select your plan. The approach shapes your rep ranges, rest times, and intensity.")}
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
              {t("Training philosophy")}
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
              { label: t("Compound reps"), value: `${selectedTrainer.compoundRepMin}–${selectedTrainer.compoundRepMax}` },
              { label: t("Isolation reps"), value: `${selectedTrainer.isolationRepMin}–${selectedTrainer.isolationRepMax}` },
              { label: t("Rest (compound)"), value: `${Math.round(selectedTrainer.restSecondsCompound / 60)}–${Math.ceil(selectedTrainer.restSecondsCompound / 60) + 0.5} min` },
              { label: t("Target RIR"), value: `${selectedTrainer.rirTarget} RIR` },
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
            <span className="text-xs text-muted-foreground font-medium">{t("Your generated plans")}</span>
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

      {loading && <PersonalizingOverlay />}
    </main>
  );
}
