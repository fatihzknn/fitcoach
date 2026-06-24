"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, CheckCircle2, RefreshCw, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  api,
  ApiError,
  type WorkoutSessionDto,
  type WorkoutExerciseDto,
  type PreviousSetDto,
} from "@/lib/api";
import { cn } from "@/lib/utils";

// ─────────────────────────────────────────────────────────────────────────────
// Set row
// ─────────────────────────────────────────────────────────────────────────────

interface SetRowProps {
  workoutExerciseId: string;
  setNumber: number;
  prescribed: { repRangeMin: number; repRangeMax: number; rirGuidance: string };
  previous: PreviousSetDto | undefined;
  logged: { weightKg: string; repsCompleted: string } | undefined;
  onLog: (we: string, setNum: number, weight: string, reps: string) => void;
  disabled: boolean;
}

function SetRow({ workoutExerciseId, setNumber, prescribed, previous, logged, onLog, disabled }: SetRowProps) {
  const [weight, setWeight] = React.useState(logged?.weightKg ?? previous?.weightKg?.toString() ?? "");
  const [reps, setReps] = React.useState(logged?.repsCompleted ?? "");
  const isDone = !!logged;

  return (
    <div className={cn(
      "grid grid-cols-[32px_1fr_1fr_auto] items-center gap-3 rounded-lg px-3 py-2.5 transition-colors",
      isDone ? "bg-primary/10" : "bg-elevated",
    )}>
      <span className={cn(
        "text-sm font-bold tabular-nums text-center",
        isDone ? "text-primary" : "text-muted-foreground",
      )}>
        {isDone ? <CheckCircle2 className="h-4 w-4 mx-auto text-primary" /> : setNumber}
      </span>

      <div className="space-y-0.5">
        <p className="text-xs text-muted-foreground">
          {prescribed.repRangeMin}–{prescribed.repRangeMax} reps · {prescribed.rirGuidance}
        </p>
        {previous && (
          <p className="text-xs text-muted-foreground/60">
            Last: {previous.weightKg ?? "BW"} kg × {previous.repsCompleted}
          </p>
        )}
      </div>

      <div className="flex gap-1.5">
        <Input
          inputMode="decimal"
          placeholder="kg"
          value={weight}
          onChange={(e) => setWeight(e.target.value)}
          disabled={disabled}
          className="h-8 text-sm px-2"
        />
        <Input
          inputMode="numeric"
          placeholder="reps"
          value={reps}
          onChange={(e) => setReps(e.target.value)}
          disabled={disabled}
          className="h-8 text-sm px-2"
        />
      </div>

      <button
        onClick={() => onLog(workoutExerciseId, setNumber, weight, reps)}
        disabled={disabled || !reps}
        className={cn(
          "rounded-md px-2 py-1.5 text-xs font-semibold transition-colors",
          isDone
            ? "bg-primary/20 text-primary hover:bg-primary/30"
            : "bg-primary text-primary-foreground hover:bg-primary/90",
          "disabled:opacity-40 disabled:cursor-not-allowed",
        )}
      >
        {isDone ? <RefreshCw className="h-3.5 w-3.5" /> : "Log"}
      </button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Exercise block
// ─────────────────────────────────────────────────────────────────────────────

interface ExerciseBlockProps {
  we: WorkoutExerciseDto;
  previousSets: PreviousSetDto[];
  loggedSets: Map<string, { weightKg: string; repsCompleted: string }>;
  onLog: (weId: string, setNum: number, weight: string, reps: string) => void;
  onSwap: (we: WorkoutExerciseDto) => void;
  disabled: boolean;
}

function ExerciseBlock({ we, previousSets, loggedSets, onLog, onSwap, disabled }: ExerciseBlockProps) {
  const sets = Array.from({ length: we.sets }, (_, i) => i + 1);
  const doneCount = sets.filter((n) => loggedSets.has(`${we.id}:${n}`)).length;

  return (
    <div className="space-y-2">
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-semibold">{we.exercise.name}</p>
          <p className="text-xs text-muted-foreground">
            {we.exercise.primaryMuscleGroup.replace("_", " ")} · {doneCount}/{we.sets} sets done
          </p>
        </div>
        {we.exercise.alternatives.length > 0 && (
          <button
            onClick={() => onSwap(we)}
            disabled={disabled}
            className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors disabled:opacity-40"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Swap
          </button>
        )}
      </div>

      <div className="space-y-1.5">
        {sets.map((n) => (
          <SetRow
            key={n}
            workoutExerciseId={we.id}
            setNumber={n}
            prescribed={{ repRangeMin: we.repRangeMin, repRangeMax: we.repRangeMax, rirGuidance: we.rirGuidance }}
            previous={previousSets[n - 1]}
            logged={loggedSets.get(`${we.id}:${n}`)}
            onLog={onLog}
            disabled={disabled}
          />
        ))}
      </div>

      {we.exercise.formCue && (
        <p className="text-xs text-muted-foreground/70 italic px-1">
          Tip: {we.exercise.formCue}
        </p>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Swap modal
// ─────────────────────────────────────────────────────────────────────────────

// In Phase 4 this is a simple bottom sheet showing alternatives from the
// exercise library. The reason-capture flow (machine occupied / pain / etc.)
// is Phase 4 Step 4.
interface SwapSheetProps {
  we: WorkoutExerciseDto;
  onClose: () => void;
}

function SwapSheet({ we, onClose }: SwapSheetProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60" onClick={onClose}>
      <div
        className="w-full max-w-md rounded-t-2xl bg-card border border-border p-6 space-y-4"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between">
          <h2 className="font-display text-lg font-bold">Swap exercise</h2>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">✕</button>
        </div>
        <p className="text-sm text-muted-foreground">
          Can't do <strong>{we.exercise.name}</strong>? Try one of these:
        </p>
        <div className="space-y-2">
          {we.exercise.alternatives.map((alt) => (
            <button
              key={alt.id}
              onClick={onClose}
              className="flex w-full items-center justify-between rounded-xl border border-border bg-elevated px-4 py-3 text-left hover:bg-card transition-colors"
            >
              <div>
                <p className="font-medium text-sm">{alt.name}</p>
                <p className="text-xs text-muted-foreground">
                  {alt.primaryMuscleGroup.replace("_", " ")} · {alt.difficultyLevel.toLowerCase()}
                </p>
              </div>
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            </button>
          ))}
        </div>
        <p className="text-xs text-muted-foreground text-center">
          Full swap flow with reason capture coming in the next update.
        </p>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Page
// ─────────────────────────────────────────────────────────────────────────────

export default function WorkoutSessionPage() {
  const params = useParams();
  const router = useRouter();
  const sessionId = params["sessionId"] as string;

  const [session, setSession] = React.useState<WorkoutSessionDto | null>(null);
  const [previousByExercise, setPreviousByExercise] = React.useState<
    Map<string, PreviousSetDto[]>
  >(new Map());
  const [loggedSets, setLoggedSets] = React.useState<
    Map<string, { weightKg: string; repsCompleted: string }>
  >(new Map());
  const [swapTarget, setSwapTarget] = React.useState<WorkoutExerciseDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [completing, setCompleting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let active = true;
    api
      .getActiveSession()
      .then(async (s) => {
        if (!active) return;
        // Verify the sessionId in the URL matches the active session
        if (s.id !== sessionId) {
          router.replace("/today");
          return;
        }
        setSession(s);

        // Pre-fill logged sets from any existing set logs in the session
        const existing = new Map<string, { weightKg: string; repsCompleted: string }>();
        s.setLogs.forEach((sl) => {
          existing.set(`${sl.workoutExerciseId}:${sl.setNumber}`, {
            weightKg: sl.weightKg?.toString() ?? "",
            repsCompleted: sl.repsCompleted.toString(),
          });
        });
        setLoggedSets(existing);

        // Fetch previous sets for each exercise
        const byExercise = new Map<string, PreviousSetDto[]>();
        await Promise.all(
          s.workoutDay.exercises.map(async (we) => {
            try {
              const prev = await api.getPreviousSets(we.exercise.id);
              byExercise.set(we.exercise.id, prev);
            } catch {
              // No previous data is fine
            }
          }),
        );
        if (active) setPreviousByExercise(byExercise);
      })
      .catch((err: unknown) => {
        if (!active) return;
        if (err instanceof ApiError && err.status === 404) {
          router.replace("/today");
        } else {
          setError("Could not load session. Please try again.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [sessionId, router]);

  async function handleLog(weId: string, setNum: number, weight: string, reps: string) {
    if (!session || !reps) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await api.logSet(session.id, {
        workoutExerciseId: weId,
        setNumber: setNum,
        weightKg: weight ? parseFloat(weight) : null,
        repsCompleted: parseInt(reps, 10),
        rirActual: null,
      });
      setSession(updated);
      setLoggedSets((prev) => {
        const next = new Map(prev);
        next.set(`${weId}:${setNum}`, { weightKg: weight, repsCompleted: reps });
        return next;
      });
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : "Failed to log set.");
    } finally {
      setSaving(false);
    }
  }

  async function handleComplete() {
    if (!session) return;
    setCompleting(true);
    setError(null);
    try {
      await api.completeSession(session.id);
      router.replace("/today");
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : "Failed to complete session.");
      setCompleting(false);
    }
  }

  const totalSets = session?.workoutDay.exercises.reduce((s, we) => s + we.sets, 0) ?? 0;
  const loggedCount = loggedSets.size;
  const allDone = totalSets > 0 && loggedCount >= totalSets;

  if (loading) {
    return (
      <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col px-4 py-6">
        <div className="animate-pulse space-y-4">
          <div className="h-8 w-1/3 rounded bg-card" />
          <div className="h-48 rounded-2xl bg-card" />
          <div className="h-48 rounded-2xl bg-card" />
        </div>
      </main>
    );
  }

  if (!session) return null;

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col px-4 py-6">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <button
          onClick={() => router.replace("/today")}
          className="flex h-10 w-10 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
          aria-label="Back"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div className="flex-1">
          <p className="font-display text-xl font-bold">{session.workoutDay.workoutName}</p>
          <p className="text-xs text-muted-foreground">
            {loggedCount}/{totalSets} sets logged
          </p>
        </div>
        {/* Progress bar */}
        <div className="h-1.5 w-24 overflow-hidden rounded-full bg-secondary">
          <div
            className="h-full rounded-full bg-primary transition-all"
            style={{ width: totalSets ? `${(loggedCount / totalSets) * 100}%` : "0%" }}
          />
        </div>
      </div>

      {error && (
        <p className="mb-4 rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">
          {error}
        </p>
      )}

      {/* Exercises */}
      <div className="space-y-5 flex-1">
        {session.workoutDay.exercises.map((we) => (
          <Card key={we.id}>
            <CardContent className="p-4">
              <ExerciseBlock
                we={we}
                previousSets={previousByExercise.get(we.exercise.id) ?? []}
                loggedSets={loggedSets}
                onLog={handleLog}
                onSwap={setSwapTarget}
                disabled={saving || completing}
              />
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Complete button */}
      <div className="mt-6 pb-6">
        <Button
          className="w-full"
          size="lg"
          variant={allDone ? "primary" : "secondary"}
          onClick={handleComplete}
          disabled={completing || saving}
        >
          {completing
            ? "Saving…"
            : allDone
              ? "Complete workout ✓"
              : `Finish early (${loggedCount}/${totalSets} sets)`}
        </Button>
        {!allDone && (
          <p className="mt-2 text-center text-xs text-muted-foreground">
            You can finish early — only logged sets are saved.
          </p>
        )}
      </div>

      {/* Swap sheet */}
      {swapTarget && (
        <SwapSheet we={swapTarget} onClose={() => setSwapTarget(null)} />
      )}
    </main>
  );
}
