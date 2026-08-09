"use client";

import * as React from "react";
import { Plus, Trash2, X, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { type CustomPlanExerciseRequest, type ExerciseDto, type MuscleGroup } from "@/lib/api";
import { useI18n } from "@/lib/i18n";

/**
 * Trainer-facing exercise-by-exercise plan builder. Purely prop-driven — no API
 * calls inside, the owning page fetches the exercise library and owns all state.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Exercise picker
// ─────────────────────────────────────────────────────────────────────────────

interface ExercisePickerSheetProps {
  exercises: ExerciseDto[];
  onSelect: (exercise: ExerciseDto) => void;
  onClose: () => void;
}

export function ExercisePickerSheet({ exercises, onSelect, onClose }: ExercisePickerSheetProps) {
  const { t } = useI18n();
  const [query, setQuery] = React.useState("");

  const filtered = exercises.filter((ex) =>
    ex.name.toLowerCase().includes(query.trim().toLowerCase()),
  );

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end bg-background/80 backdrop-blur-sm">
      <div className="max-h-[80vh] w-full rounded-t-2xl border-t border-border bg-background px-4 pb-6 pt-4 animate-fade-up">
        <div className="flex items-center justify-between">
          <p className="font-display text-lg font-bold">{t("Add exercise")}</p>
          <button
            onClick={onClose}
            className="flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
            aria-label={t("Close")}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="relative mt-3">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t("Search exercises...")}
            className="pl-9"
            autoFocus
          />
        </div>

        <div className="mt-3 max-h-[55vh] space-y-1.5 overflow-y-auto">
          {filtered.length === 0 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">{t("No exercises found.")}</p>
          ) : (
            filtered.map((ex) => (
              <button
                key={ex.id}
                onClick={() => onSelect(ex)}
                className="flex w-full items-center justify-between rounded-lg border border-border bg-card px-3 py-2.5 text-left hover:bg-elevated"
              >
                <div>
                  <p className="text-sm font-medium">{ex.name}</p>
                  <p className="text-xs text-muted-foreground">{muscleGroupLabel(ex.primaryMuscleGroup, t)}</p>
                </div>
                <Plus className="h-4 w-4 text-primary flex-shrink-0" />
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

function muscleGroupLabel(group: MuscleGroup, t: (s: string) => string): string {
  const labels: Record<MuscleGroup, string> = {
    CHEST: t("Chest"),
    BACK: t("Back muscles"),
    SHOULDERS: t("Shoulders"),
    BICEPS: t("Biceps"),
    TRICEPS: t("Triceps"),
    QUADS: t("Quads"),
    HAMSTRINGS: t("Hamstrings"),
    GLUTES: t("Glutes"),
    CORE: t("Core"),
    CALVES: t("Calves"),
  };
  return labels[group];
}

// ─────────────────────────────────────────────────────────────────────────────
// Day editor
// ─────────────────────────────────────────────────────────────────────────────

export interface CustomPlanExerciseDraft extends CustomPlanExerciseRequest {
  exerciseName: string;
}

export interface CustomPlanDayDraft {
  workoutName: string;
  exercises: CustomPlanExerciseDraft[];
}

interface CustomPlanDayEditorProps {
  day: CustomPlanDayDraft;
  dayIndex: number;
  onNameChange: (name: string) => void;
  onRemoveDay: () => void;
  onAddExercise: () => void;
  onRemoveExercise: (exerciseIndex: number) => void;
  onUpdateExercise: (exerciseIndex: number, patch: Partial<CustomPlanExerciseRequest>) => void;
  canRemoveDay: boolean;
}

export function CustomPlanDayEditor({
  day,
  dayIndex,
  onNameChange,
  onRemoveDay,
  onAddExercise,
  onRemoveExercise,
  onUpdateExercise,
  canRemoveDay,
}: CustomPlanDayEditorProps) {
  const { t } = useI18n();

  return (
    <Card>
      <CardContent className="p-4 space-y-3">
        <div className="flex items-center gap-2">
          <Input
            value={day.workoutName}
            onChange={(e) => onNameChange(e.target.value)}
            placeholder={t("Day {n} name", { n: dayIndex + 1 })}
            className="flex-1"
          />
          {canRemoveDay && (
            <button
              onClick={onRemoveDay}
              className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary hover:text-destructive"
              aria-label={t("Remove day")}
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>

        <div className="space-y-2">
          {day.exercises.map((ex, i) => (
            <div key={i} className="rounded-lg border border-border bg-elevated p-3 space-y-2">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium">{ex.exerciseName}</p>
                <button
                  onClick={() => onRemoveExercise(i)}
                  className="flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary hover:text-destructive"
                  aria-label={t("Remove exercise")}
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              </div>
              <div className="grid grid-cols-4 gap-2">
                <div className="space-y-1">
                  <Label className="text-[10px]">{t("Sets")}</Label>
                  <Input
                    type="number"
                    inputMode="numeric"
                    min={1}
                    max={10}
                    value={ex.sets}
                    onChange={(e) => onUpdateExercise(i, { sets: Number(e.target.value) })}
                    className="h-9 px-2 text-center text-sm"
                  />
                </div>
                <div className="space-y-1">
                  <Label className="text-[10px]">{t("Rep min")}</Label>
                  <Input
                    type="number"
                    inputMode="numeric"
                    min={1}
                    value={ex.repRangeMin}
                    onChange={(e) => onUpdateExercise(i, { repRangeMin: Number(e.target.value) })}
                    className="h-9 px-2 text-center text-sm"
                  />
                </div>
                <div className="space-y-1">
                  <Label className="text-[10px]">{t("Rep max")}</Label>
                  <Input
                    type="number"
                    inputMode="numeric"
                    min={1}
                    value={ex.repRangeMax}
                    onChange={(e) => onUpdateExercise(i, { repRangeMax: Number(e.target.value) })}
                    className="h-9 px-2 text-center text-sm"
                  />
                </div>
                <div className="space-y-1">
                  <Label className="text-[10px]">{t("Rest (s)")}</Label>
                  <Input
                    type="number"
                    inputMode="numeric"
                    min={0}
                    value={ex.restSeconds}
                    onChange={(e) => onUpdateExercise(i, { restSeconds: Number(e.target.value) })}
                    className="h-9 px-2 text-center text-sm"
                  />
                </div>
              </div>
              <div className="space-y-1">
                <Label className="text-[10px]">{t("RIR guidance")}</Label>
                <Input
                  value={ex.rirGuidance}
                  onChange={(e) => onUpdateExercise(i, { rirGuidance: e.target.value })}
                  placeholder="2 RIR"
                  className="h-9 text-sm"
                />
              </div>
            </div>
          ))}
        </div>

        <Button variant="secondary" size="sm" className="w-full" onClick={onAddExercise}>
          <Plus className="h-3.5 w-3.5 mr-1.5" />
          {t("Add exercise")}
        </Button>
      </CardContent>
    </Card>
  );
}
