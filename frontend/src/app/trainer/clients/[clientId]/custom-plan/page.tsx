"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { OptionCard } from "@/components/ui/option-card";
import {
  CustomPlanDayEditor,
  ExercisePickerSheet,
  type CustomPlanDayDraft,
} from "@/components/custom-plan-builder";
import { GOAL_OPTIONS } from "@/lib/onboarding";
import {
  api,
  ApiError,
  type ExerciseDto,
  type MainGoal,
  type CreateCustomPlanRequest,
} from "@/lib/api";
import { useI18n } from "@/lib/i18n";

const MAX_DAYS = 7;

function emptyDay(): CustomPlanDayDraft {
  return { workoutName: "", exercises: [] };
}

export default function CustomPlanBuilderPage() {
  const params = useParams<{ clientId: string }>();
  const router = useRouter();
  const { t } = useI18n();
  const clientId = params.clientId;

  const [exercises, setExercises] = React.useState<ExerciseDto[]>([]);
  const [loadingExercises, setLoadingExercises] = React.useState(true);
  const [loadError, setLoadError] = React.useState<string | null>(null);

  const [planName, setPlanName] = React.useState("");
  const [goal, setGoal] = React.useState<MainGoal | undefined>(undefined);
  const [days, setDays] = React.useState<CustomPlanDayDraft[]>([emptyDay()]);
  const [pickerDayIndex, setPickerDayIndex] = React.useState<number | null>(null);

  const [error, setError] = React.useState<string | null>(null);
  const [submitting, setSubmitting] = React.useState(false);

  React.useEffect(() => {
    let active = true;
    api.getExercises()
      .then((data) => { if (active) setExercises(data); })
      .catch((err: unknown) => {
        if (active) setLoadError(err instanceof ApiError ? err.message : t("Could not load the exercise library."));
      })
      .finally(() => { if (active) setLoadingExercises(false); });
    return () => { active = false; };
  }, [t]);

  function updateDayName(index: number, name: string) {
    setDays((d) => d.map((day, i) => (i === index ? { ...day, workoutName: name } : day)));
  }

  function addDay() {
    setDays((d) => (d.length >= MAX_DAYS ? d : [...d, emptyDay()]));
  }

  function removeDay(index: number) {
    setDays((d) => d.filter((_, i) => i !== index));
  }

  function addExercise(exercise: ExerciseDto) {
    if (pickerDayIndex === null) return;
    setDays((d) =>
      d.map((day, i) =>
        i === pickerDayIndex
          ? {
              ...day,
              exercises: [
                ...day.exercises,
                {
                  exerciseId: exercise.id,
                  exerciseName: exercise.name,
                  sets: 3,
                  repRangeMin: 8,
                  repRangeMax: 12,
                  rirGuidance: "2 RIR",
                  restSeconds: 90,
                },
              ],
            }
          : day,
      ),
    );
    setPickerDayIndex(null);
  }

  function removeExercise(dayIndex: number, exerciseIndex: number) {
    setDays((d) =>
      d.map((day, i) =>
        i === dayIndex
          ? { ...day, exercises: day.exercises.filter((_, j) => j !== exerciseIndex) }
          : day,
      ),
    );
  }

  function updateExercise(dayIndex: number, exerciseIndex: number, patch: Partial<CustomPlanDayDraft["exercises"][number]>) {
    setDays((d) =>
      d.map((day, i) =>
        i === dayIndex
          ? {
              ...day,
              exercises: day.exercises.map((ex, j) => (j === exerciseIndex ? { ...ex, ...patch } : ex)),
            }
          : day,
      ),
    );
  }

  function validate(): string | null {
    if (!planName.trim()) return t("Enter a plan name.");
    if (!goal) return t("Choose a main goal.");
    if (days.length === 0) return t("Add at least one day.");
    for (const day of days) {
      if (!day.workoutName.trim()) return t("Every day needs a name.");
      if (day.exercises.length === 0) return t("Every day needs at least one exercise.");
      for (const ex of day.exercises) {
        if (ex.sets < 1 || ex.sets > 10) return t("Sets must be between 1 and 10.");
        if (ex.repRangeMin < 1 || ex.repRangeMax < ex.repRangeMin) return t("Check the rep ranges.");
        if (!ex.rirGuidance.trim()) return t("RIR guidance is required for every exercise.");
      }
    }
    return null;
  }

  async function handleSubmit() {
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    const request: CreateCustomPlanRequest = {
      name: planName.trim(),
      goal: goal!,
      days: days.map((day) => ({
        workoutName: day.workoutName.trim(),
        exercises: day.exercises.map((ex) => ({
          exerciseId: ex.exerciseId,
          sets: ex.sets,
          repRangeMin: ex.repRangeMin,
          repRangeMax: ex.repRangeMax,
          rirGuidance: ex.rirGuidance,
          restSeconds: ex.restSeconds,
        })),
      })),
    };

    setSubmitting(true);
    setError(null);
    try {
      await api.createCustomPlan(clientId, request);
      router.replace(`/trainer/clients/${clientId}`);
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : t("Could not save the plan. Please try again."));
      setSubmitting(false);
    }
  }

  return (
    <section className="animate-fade-up space-y-6">
      <button
        onClick={() => router.push(`/trainer/clients/${clientId}`)}
        className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" />
        {t("Back")}
      </button>

      <div>
        <h1 className="font-display text-3xl font-extrabold leading-tight tracking-tight">
          {t("Build a custom plan")}
        </h1>
        <p className="text-sm text-muted-foreground">
          {t("Pick exercises day by day. This replaces the client's current active plan.")}
        </p>
      </div>

      {loadError && (
        <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">{loadError}</p>
      )}
      {error && (
        <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">{error}</p>
      )}

      {loadingExercises ? (
        <div className="space-y-3 animate-pulse">
          <div className="h-12 rounded-lg bg-card" />
          <div className="h-40 rounded-2xl bg-card" />
        </div>
      ) : (
        <>
          <div className="space-y-2">
            <Label htmlFor="plan-name">{t("Plan name")}</Label>
            <Input
              id="plan-name"
              value={planName}
              onChange={(e) => setPlanName(e.target.value)}
              placeholder={t("e.g. Off-Season Strength Block")}
            />
          </div>

          <div className="space-y-2">
            <Label>{t("Main goal")}</Label>
            <div className="grid grid-cols-2 gap-2">
              {GOAL_OPTIONS.map((o) => (
                <OptionCard
                  key={o.value}
                  label={t(o.label)}
                  selected={goal === o.value}
                  onSelect={() => setGoal(o.value)}
                />
              ))}
            </div>
          </div>

          <div className="space-y-3">
            {days.map((day, i) => (
              <CustomPlanDayEditor
                key={i}
                day={day}
                dayIndex={i}
                canRemoveDay={days.length > 1}
                onNameChange={(name) => updateDayName(i, name)}
                onRemoveDay={() => removeDay(i)}
                onAddExercise={() => setPickerDayIndex(i)}
                onRemoveExercise={(exIndex) => removeExercise(i, exIndex)}
                onUpdateExercise={(exIndex, patch) => updateExercise(i, exIndex, patch)}
              />
            ))}
          </div>

          {days.length < MAX_DAYS && (
            <Button variant="secondary" className="w-full" onClick={addDay}>
              <Plus className="h-4 w-4 mr-1.5" />
              {t("Add day")}
            </Button>
          )}

          <Button size="lg" className="w-full" onClick={handleSubmit} disabled={submitting}>
            {submitting ? t("Saving…") : t("Save plan")}
          </Button>
        </>
      )}

      {pickerDayIndex !== null && (
        <ExercisePickerSheet
          exercises={exercises}
          onSelect={addExercise}
          onClose={() => setPickerDayIndex(null)}
        />
      )}
    </section>
  );
}
