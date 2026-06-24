"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ArrowLeft,
  CheckCircle2,
  RefreshCw,
  ChevronRight,
  AlertTriangle,
  Trophy,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  api,
  ApiError,
  type WorkoutSessionDto,
  type WorkoutExerciseDto,
  type ExerciseDto,
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
  onLog: (weId: string, setNum: number, weight: string, reps: string) => void;
  disabled: boolean;
}

function SetRow({
  workoutExerciseId,
  setNumber,
  prescribed,
  previous,
  logged,
  onLog,
  disabled,
}: SetRowProps) {
  const [weight, setWeight] = React.useState(
    logged?.weightKg ?? previous?.weightKg?.toString() ?? "",
  );
  const [reps, setReps] = React.useState(logged?.repsCompleted ?? "");
  const isDone = !!logged;

  return (
    <div
      className={cn(
        "grid grid-cols-[28px_1fr_1fr_auto] items-center gap-2.5 rounded-lg px-3 py-2.5 transition-colors",
        isDone ? "bg-primary/10" : "bg-elevated",
      )}
    >
      <span className="flex items-center justify-center">
        {isDone ? (
          <CheckCircle2 className="h-4 w-4 text-primary" />
        ) : (
          <span className="text-sm font-bold tabular-nums text-muted-foreground">
            {setNumber}
          </span>
        )}
      </span>

      <div className="space-y-0.5 min-w-0">
        <p className="text-xs text-muted-foreground">
          {prescribed.repRangeMin}–{prescribed.repRangeMax} · {prescribed.rirGuidance}
        </p>
        {previous && (
          <p className="text-xs text-muted-foreground/55">
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
  swappedExercise: ExerciseDto | undefined;
  previousSets: PreviousSetDto[];
  loggedSets: Map<string, { weightKg: string; repsCompleted: string }>;
  onLog: (weId: string, setNum: number, weight: string, reps: string) => void;
  onSwap: (we: WorkoutExerciseDto) => void;
  disabled: boolean;
}

function ExerciseBlock({
  we,
  swappedExercise,
  previousSets,
  loggedSets,
  onLog,
  onSwap,
  disabled,
}: ExerciseBlockProps) {
  const display = swappedExercise ?? we.exercise;
  const sets = Array.from({ length: we.sets }, (_, i) => i + 1);
  const doneCount = sets.filter((n) => loggedSets.has(`${we.id}:${n}`)).length;

  return (
    <div className="space-y-2">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <p className="font-semibold">{display.name}</p>
            {swappedExercise && (
              <span className="inline-flex items-center gap-1 rounded-full bg-primary/15 px-2 py-0.5 text-xs font-medium text-primary">
                <RefreshCw className="h-2.5 w-2.5" />
                swapped
              </span>
            )}
          </div>
          <p className="text-xs text-muted-foreground">
            {display.primaryMuscleGroup.replace(/_/g, " ")} · {doneCount}/{we.sets} sets done
          </p>
          {swappedExercise && (
            <p className="text-xs text-muted-foreground/55">
              Originally: {we.exercise.name}
            </p>
          )}
        </div>
        <button
          onClick={() => onSwap(we)}
          disabled={disabled}
          className="flex flex-shrink-0 items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors disabled:opacity-40"
        >
          <RefreshCw className="h-3.5 w-3.5" />
          Swap
        </button>
      </div>

      <div className="space-y-1.5">
        {sets.map((n) => (
          <SetRow
            key={n}
            workoutExerciseId={we.id}
            setNumber={n}
            prescribed={{
              repRangeMin: we.repRangeMin,
              repRangeMax: we.repRangeMax,
              rirGuidance: we.rirGuidance,
            }}
            previous={previousSets[n - 1]}
            logged={loggedSets.get(`${we.id}:${n}`)}
            onLog={onLog}
            disabled={disabled}
          />
        ))}
      </div>

      {display.formCue && (
        <p className="text-xs text-muted-foreground/70 italic px-1">
          Tip: {display.formCue}
        </p>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Swap flow (multi-step bottom sheet)
// ─────────────────────────────────────────────────────────────────────────────

type SwapStep = "reason" | "pain-warning" | "alternatives";

const SWAP_REASONS = [
  { value: "MACHINE_OCCUPIED", label: "Machine is occupied" },
  { value: "DONT_KNOW_IT", label: "Don't know this exercise" },
  { value: "CAUSES_PAIN", label: "Causes pain or discomfort" },
  { value: "NO_ACCESS", label: "No access to equipment" },
  { value: "OTHER", label: "Other reason" },
] as const;

interface SwapFlowProps {
  we: WorkoutExerciseDto;
  onConfirm: (weId: string, alternative: ExerciseDto) => void;
  onClose: () => void;
}

function SwapFlow({ we, onConfirm, onClose }: SwapFlowProps) {
  const [step, setStep] = React.useState<SwapStep>("reason");

  function handleReason(value: string) {
    if (value === "CAUSES_PAIN") {
      setStep("pain-warning");
    } else {
      setStep("alternatives");
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/60"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md rounded-t-2xl bg-card border border-border p-6 space-y-4 max-h-[85vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Step 1 — reason */}
        {step === "reason" && (
          <>
            <div className="flex items-center justify-between">
              <h2 className="font-display text-lg font-bold">
                Why can't you do {we.exercise.name}?
              </h2>
              <button
                onClick={onClose}
                className="text-muted-foreground hover:text-foreground text-xl leading-none"
              >
                ✕
              </button>
            </div>
            <div className="space-y-2">
              {SWAP_REASONS.map((r) => (
                <button
                  key={r.value}
                  onClick={() => handleReason(r.value)}
                  className="flex w-full items-center justify-between rounded-xl border border-border bg-elevated px-4 py-3.5 text-left hover:bg-secondary transition-colors"
                >
                  <span className="text-sm font-medium">{r.label}</span>
                  <ChevronRight className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                </button>
              ))}
            </div>
          </>
        )}

        {/* Step 2a — pain safety notice */}
        {step === "pain-warning" && (
          <>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setStep("reason")}
                className="text-muted-foreground hover:text-foreground"
              >
                <ArrowLeft className="h-4 w-4" />
              </button>
              <h2 className="font-display text-lg font-bold">Pain safety notice</h2>
            </div>

            <div className="rounded-xl bg-amber-500/10 border border-amber-500/30 p-4 space-y-2">
              <div className="flex items-center gap-2">
                <AlertTriangle className="h-5 w-5 text-amber-500 flex-shrink-0" />
                <p className="font-semibold text-sm text-amber-400">Important</p>
              </div>
              <p className="text-sm text-amber-400/90 leading-relaxed">
                Never push through pain. If you experience sharp, sudden, or severe
                pain, stop training immediately.
              </p>
              <p className="text-sm text-amber-400/90 leading-relaxed">
                Consider consulting a healthcare professional before continuing
                training on the affected area.
              </p>
              <p className="text-xs text-muted-foreground leading-relaxed pt-1">
                FitCoach cannot diagnose injuries or provide medical advice. When in
                doubt, rest and seek professional evaluation.
              </p>
            </div>

            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" onClick={onClose}>
                Stop for today
              </Button>
              <Button className="flex-1" onClick={() => setStep("alternatives")}>
                Show alternatives
              </Button>
            </div>
          </>
        )}

        {/* Step 2b — pick alternative */}
        {step === "alternatives" && (
          <>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setStep("reason")}
                className="text-muted-foreground hover:text-foreground"
              >
                <ArrowLeft className="h-4 w-4" />
              </button>
              <h2 className="font-display text-lg font-bold">Pick a replacement</h2>
            </div>

            {we.exercise.alternatives.length === 0 ? (
              <p className="text-sm text-muted-foreground py-4 text-center">
                No alternatives recorded for this exercise yet.
              </p>
            ) : (
              <div className="space-y-2">
                {we.exercise.alternatives.map((alt) => (
                  <button
                    key={alt.id}
                    onClick={() => onConfirm(we.id, alt)}
                    className="flex w-full items-center justify-between rounded-xl border border-border bg-elevated px-4 py-3.5 text-left hover:bg-secondary transition-colors"
                  >
                    <div>
                      <p className="font-medium text-sm">{alt.name}</p>
                      <p className="text-xs text-muted-foreground">
                        {alt.primaryMuscleGroup.replace(/_/g, " ")} ·{" "}
                        {alt.difficultyLevel.toLowerCase()}
                      </p>
                    </div>
                    <ChevronRight className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Completion overlay
// ─────────────────────────────────────────────────────────────────────────────

interface CompletionOverlayProps {
  workoutName: string;
  setsLogged: number;
  exerciseCount: number;
  startedAt: string;
  onDone: () => void;
}

function CompletionOverlay({
  workoutName,
  setsLogged,
  exerciseCount,
  startedAt,
  onDone,
}: CompletionOverlayProps) {
  const durationMinutes = Math.round(
    (Date.now() - new Date(startedAt).getTime()) / 60000,
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/95 px-6">
      <div className="w-full max-w-sm text-center space-y-6 animate-fade-up">
        <div className="flex items-center justify-center">
          <div className="flex h-20 w-20 items-center justify-center rounded-full bg-primary/15">
            <Trophy className="h-10 w-10 text-primary" />
          </div>
        </div>

        <div className="space-y-1">
          <p className="text-sm font-semibold uppercase tracking-widest text-primary">
            Workout complete
          </p>
          <h1 className="font-display text-3xl font-extrabold tracking-tight">
            {workoutName}
          </h1>
        </div>

        <div className="grid grid-cols-3 gap-3">
          {[
            { value: `${durationMinutes}`, label: "minutes" },
            { value: `${exerciseCount}`, label: "exercises" },
            { value: `${setsLogged}`, label: "sets logged" },
          ].map((stat) => (
            <div key={stat.label} className="rounded-xl bg-card border border-border p-3">
              <p className="font-display text-2xl font-bold">{stat.value}</p>
              <p className="text-xs text-muted-foreground">{stat.label}</p>
            </div>
          ))}
        </div>

        <p className="text-sm text-muted-foreground">
          Great work. Rest, recover, and come back stronger.
        </p>

        <Button className="w-full" size="lg" onClick={onDone}>
          Back to today
        </Button>
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
  const [swappedExercises, setSwappedExercises] = React.useState<
    Map<string, ExerciseDto>
  >(new Map());
  const [swapTarget, setSwapTarget] = React.useState<WorkoutExerciseDto | null>(null);
  const [completed, setCompleted] = React.useState(false);
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
        if (s.id !== sessionId) {
          router.replace("/today");
          return;
        }
        setSession(s);

        const existing = new Map<string, { weightKg: string; repsCompleted: string }>();
        s.setLogs.forEach((sl) => {
          existing.set(`${sl.workoutExerciseId}:${sl.setNumber}`, {
            weightKg: sl.weightKg?.toString() ?? "",
            repsCompleted: sl.repsCompleted.toString(),
          });
        });
        setLoggedSets(existing);

        const byExercise = new Map<string, PreviousSetDto[]>();
        await Promise.all(
          s.workoutDay.exercises.map(async (we) => {
            try {
              const prev = await api.getPreviousSets(we.exercise.id);
              byExercise.set(we.exercise.id, prev);
            } catch {
              /* no history yet */
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
    return () => {
      active = false;
    };
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
      setCompleted(true);
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : "Failed to complete session.");
      setCompleting(false);
    }
  }

  function handleSwapConfirm(weId: string, alternative: ExerciseDto) {
    setSwappedExercises((prev) => {
      const next = new Map(prev);
      next.set(weId, alternative);
      return next;
    });
    setSwapTarget(null);
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
    <>
      <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col px-4 py-6">
        {/* Header */}
        <div className="flex items-center gap-3 mb-6">
          <button
            onClick={() => router.replace("/today")}
            className="flex h-10 w-10 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
            aria-label="Back to today"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div className="flex-1 min-w-0">
            <p className="font-display text-xl font-bold truncate">
              {session.workoutDay.workoutName}
            </p>
            <p className="text-xs text-muted-foreground">
              {loggedCount}/{totalSets} sets logged
            </p>
          </div>
          <div className="h-1.5 w-20 flex-shrink-0 overflow-hidden rounded-full bg-secondary">
            <div
              className="h-full rounded-full bg-primary transition-all duration-300"
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
                  swappedExercise={swappedExercises.get(we.id)}
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

        {/* Complete */}
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
                ? "Complete workout"
                : `Finish early (${loggedCount}/${totalSets} sets)`}
          </Button>
          {!allDone && (
            <p className="mt-2 text-center text-xs text-muted-foreground">
              You can finish early — only logged sets are saved.
            </p>
          )}
        </div>
      </main>

      {/* Swap flow */}
      {swapTarget && (
        <SwapFlow
          we={swapTarget}
          onConfirm={handleSwapConfirm}
          onClose={() => setSwapTarget(null)}
        />
      )}

      {/* Completion overlay */}
      {completed && (
        <CompletionOverlay
          workoutName={session.workoutDay.workoutName}
          setsLogged={loggedCount}
          exerciseCount={session.workoutDay.exercises.length}
          startedAt={session.startedAt}
          onDone={() => router.replace("/today")}
        />
      )}
    </>
  );
}
