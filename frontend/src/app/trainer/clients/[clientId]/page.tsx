"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Trophy, Target, Flame, ClipboardList, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { TrainerCard, PlanCard, DayRow, PersonalizingOverlay } from "@/components/plan-picker";
import {
  api,
  ApiError,
  type ClientDetailDto,
  type TrainerPhilosophyDto,
  type WorkoutPlanDto,
  type PlanOption,
} from "@/lib/api";
import { useI18n } from "@/lib/i18n";

function StatTile({ icon: Icon, value, label, sub }: { icon: React.ElementType; value: string; label: string; sub?: string }) {
  return (
    <Card>
      <CardContent className="p-4 flex items-center gap-3">
        <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl bg-primary/10">
          <Icon className="h-5 w-5 text-primary" />
        </div>
        <div>
          <p className="font-display text-2xl font-bold leading-none">{value}</p>
          <p className="text-xs text-muted-foreground mt-0.5">{label}</p>
          {sub && <p className="text-xs text-muted-foreground/60">{sub}</p>}
        </div>
      </CardContent>
    </Card>
  );
}

export default function TrainerClientDetailPage() {
  const params = useParams<{ clientId: string }>();
  const router = useRouter();
  const { t } = useI18n();
  const clientId = params.clientId;

  const [detail, setDetail] = React.useState<ClientDetailDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const [assigning, setAssigning] = React.useState(false);
  const [initializing, setInitializing] = React.useState(false);
  const [trainers, setTrainers] = React.useState<TrainerPhilosophyDto[]>([]);
  const [selectedTrainerId, setSelectedTrainerId] = React.useState<string | null>(null);
  const [recommended, setRecommended] = React.useState<WorkoutPlanDto | null>(null);
  const [alternative, setAlternative] = React.useState<WorkoutPlanDto | null>(null);
  const [optionsLoading, setOptionsLoading] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [assignError, setAssignError] = React.useState<string | null>(null);
  const [justAssigned, setJustAssigned] = React.useState(false);

  const loadDetail = React.useCallback(() => {
    setLoading(true);
    return api.getTrainerClientDetail(clientId)
      .then(setDetail)
      .catch((err: unknown) => {
        setError(err instanceof ApiError ? err.message : t("Could not load this client."));
      })
      .finally(() => setLoading(false));
  }, [clientId, t]);

  React.useEffect(() => { loadDetail(); }, [loadDetail]);

  function startAssigning() {
    setAssigning(true);
    setAssignError(null);
    setJustAssigned(false);
    if (trainers.length === 0) {
      // "Personalizing" overlay belongs here — before the picker is shown —
      // not on the final assign step, same reasoning as plan-selection.
      setInitializing(true);
      const minDelay = new Promise((resolve) => setTimeout(resolve, 2400));
      const load = api.getTrainers()
        .then((data) => {
          setTrainers(data);
          const firstId = data[0]?.id ?? null;
          setSelectedTrainerId(firstId);
          if (firstId) return loadOptions(firstId);
        })
        .catch((err: unknown) => {
          setAssignError(err instanceof ApiError ? err.message : t("Could not load plans. Please try again."));
        });
      Promise.all([load, minDelay]).finally(() => setInitializing(false));
    }
  }

  function loadOptions(trainerId: string) {
    setOptionsLoading(true);
    setAssignError(null);
    return api.getClientPlanOptions(clientId, trainerId)
      .then((options) => {
        setRecommended(options.recommended);
        setAlternative(options.alternative);
      })
      .catch((err: unknown) => {
        setAssignError(err instanceof ApiError ? err.message : t("Could not load plans. Please try again."));
      })
      .finally(() => setOptionsLoading(false));
  }

  function handleTrainerSelect(trainerId: string) {
    if (trainerId === selectedTrainerId) return;
    setSelectedTrainerId(trainerId);
    loadOptions(trainerId);
  }

  async function handleAssign(option: PlanOption) {
    setSubmitting(true);
    setAssignError(null);
    try {
      await api.assignClientPlan(clientId, { option, trainerId: selectedTrainerId ?? undefined });
      setAssigning(false);
      setJustAssigned(true);
      await loadDetail();
    } catch (err: unknown) {
      setAssignError(err instanceof ApiError ? err.message : t("Could not assign plan. Please try again."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="animate-fade-up space-y-6">
      <button
        onClick={() => router.push("/trainer")}
        className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        {t("Clients")}
      </button>

      {error && (
        <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">{error}</p>
      )}

      {loading && (
        <div className="space-y-4 animate-pulse">
          <div className="h-16 rounded-2xl bg-card" />
          <div className="grid grid-cols-2 gap-3">
            {[1, 2, 3, 4].map((i) => <div key={i} className="h-20 rounded-2xl bg-card" />)}
          </div>
        </div>
      )}

      {!loading && detail && (
        <>
          <div>
            <h1 className="font-display text-3xl font-extrabold leading-tight tracking-tight">
              {detail.summary.displayName}
            </h1>
            <p className="text-sm text-muted-foreground">{detail.summary.email}</p>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <StatTile icon={Trophy} value={String(detail.stats.totalWorkoutsAllTime)} label={t("Total workouts")} sub={t("all time")} />
            <StatTile icon={Target} value={`${detail.stats.adherenceRate4Weeks}%`} label={t("Adherence")} sub={t("last 4 weeks")} />
            <StatTile icon={Flame} value={`${detail.stats.currentStreakWeeks}w`} label={t("Current streak")} sub={t("consecutive weeks")} />
            <StatTile icon={ClipboardList} value={String(detail.stats.workoutsThisWeek)} label={t("This week")} sub={t("sessions done")} />
          </div>

          <Card>
            <CardContent className="p-5 space-y-3">
              <div className="flex items-center justify-between">
                <p className="font-semibold">{t("Active plan")}</p>
                {!assigning && (
                  <Button size="sm" variant="secondary" onClick={startAssigning}>
                    {detail.activePlan ? t("Change plan") : t("Assign a plan")}
                  </Button>
                )}
              </div>

              {justAssigned && (
                <p className="flex items-center gap-1.5 text-sm text-primary">
                  <Check className="h-4 w-4" /> {t("Plan assigned.")}
                </p>
              )}

              {!assigning && detail.activePlan && (
                <div className="space-y-2">
                  <p className="text-sm text-muted-foreground">
                    {detail.activePlan.name} · {t("{n} days/week", { n: detail.activePlan.trainingDaysPerWeek })}
                  </p>
                  <div className="space-y-2">
                    {detail.activePlan.days.map((day) => (
                      <DayRow key={day.id} day={day} />
                    ))}
                  </div>
                </div>
              )}

              {!assigning && !detail.activePlan && (
                <p className="text-sm text-muted-foreground">{t("No active plan")}</p>
              )}
            </CardContent>
          </Card>

          {assigning && !initializing && (
            <div className="space-y-4">
              {assignError && (
                <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">{assignError}</p>
              )}

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

              {optionsLoading ? (
                <div className="space-y-4 animate-pulse">
                  <div className="h-40 rounded-2xl bg-card" />
                  <div className="h-32 rounded-2xl bg-card" />
                </div>
              ) : recommended && alternative ? (
                <div className="space-y-4">
                  <PlanCard plan={recommended} option="RECOMMENDED" isRecommended={true} submitting={submitting} onSelect={handleAssign} />
                  <PlanCard plan={alternative} option="ALTERNATIVE" isRecommended={false} submitting={submitting} onSelect={handleAssign} />
                </div>
              ) : null}

              <Button variant="secondary" className="w-full" onClick={() => setAssigning(false)} disabled={submitting}>
                {t("Cancel")}
              </Button>
            </div>
          )}
        </>
      )}

      {assigning && initializing && <PersonalizingOverlay />}
    </section>
  );
}
