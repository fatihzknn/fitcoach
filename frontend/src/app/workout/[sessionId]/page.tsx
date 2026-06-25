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
  Timer,
  TrendingUp,
  X,
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
  type ExerciseHistoryEntryDto,
} from "@/lib/api";
import { cn } from "@/lib/utils";

// ─────────────────────────────────────────────────────────────────────────────
// Mini SVG progress chart (exercise history)
// ─────────────────────────────────────────────────────────────────────────────

function ExerciseProgressChart({ entries }: { entries: ExerciseHistoryEntryDto[] }) {
  const valid = entries.filter((e) => e.maxWeightKg !== null) as (ExerciseHistoryEntryDto & { maxWeightKg: number })[];

  if (valid.length < 2) {
    return (
      <p className="py-4 text-center text-sm text-muted-foreground">
        Log at least 2 sessions to see your progression chart.
      </p>
    );
  }

  const W = 280, H = 70, PX = 4, PY = 8;
  const weights = valid.map((e) => e.maxWeightKg);
  const min = Math.min(...weights);
  const max = Math.max(...weights);
  const range = max - min || 1;

  const pts = valid.map((e, i) => ({
    x: PX + (i / (valid.length - 1)) * (W - PX * 2),
    y: PY + ((max - e.maxWeightKg) / range) * (H - PY * 2),
    entry: e,
  }));

  const polyline = pts.map((p) => `${p.x},${p.y}`).join(" ");
  const first = pts[0]!;
  const last = pts[pts.length - 1]!;

  return (
    <div className="space-y-1">
      <svg viewBox={`0 0 ${W} ${H}`} className="w-full">
        <defs>
          <linearGradient id="exerciseGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="hsl(var(--primary))" stopOpacity="0.2" />
            <stop offset="100%" stopColor="hsl(var(--primary))" stopOpacity="0" />
          </linearGradient>
        </defs>
        <polygon
          points={`${first.x},${H} ${polyline} ${last.x},${H}`}
          fill="url(#exerciseGrad)"
        />
        <polyline
          points={polyline}
          fill="none"
          stroke="hsl(var(--primary))"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {pts.map((p, i) => (
          <circle key={i} cx={p.x} cy={p.y} r="3" fill="hsl(var(--primary))" />
        ))}
      </svg>
      <div className="flex justify-between text-xs text-muted-foreground/60 px-1">
        <span>{first.entry.sessionDate.slice(5)}</span>
        <span>{last.entry.maxWeightKg} kg · {last.entry.bestReps} reps</span>
        <span>{last.entry.sessionDate.slice(5)}</span>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Exercise history sheet
// ─────────────────────────────────────────────────────────────────────────────

interface HistorySheetProps {
  exerciseName: string;
  entries: ExerciseHistoryEntryDto[];
  loading: boolean;
  onClose: () => void;
}

function HistorySheet({ exerciseName, entries, loading, onClose }: HistorySheetProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60" onClick={onClose}>
      <div
        className="w-full max-w-md rounded-t-2xl bg-card border border-border p-5 space-y-4 max-h-[70vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-display text-lg font-bold">{exerciseName}</h2>
            <p className="text-xs text-muted-foreground">Strength progression</p>
          </div>
          <button onClick={onClose} className="h-8 w-8 flex items-center justify-center rounded-lg text-muted-foreground hover:bg-elevated">
            <X className="h-4 w-4" />
          </button>
        </div>

        {loading ? (
          <div className="h-20 rounded-xl bg-elevated animate-pulse" />
        ) : (
          <ExerciseProgressChart entries={entries} />
        )}

        {/* Session list */}
        {!loading && entries.length > 0 && (
          <div className="space-y-1.5">
            {entries.slice().reverse().slice(0, 6).map((e, i) => (
              <div key={i} className="flex items-center justify-between rounded-lg bg-elevated px-3 py-2">
                <span className="text-xs text-muted-foreground">{e.sessionDate}</span>
                <span className="text-xs font-medium">
                  {e.maxWeightKg ? `${e.maxWeightKg} kg` : "BW"} × {e.bestReps} reps
                  <span className="text-muted-foreground"> ({e.totalSets} sets)</span>
                </span>
              </div>
            ))}
          </div>
        )}

        {!loading && entries.length === 0 && (
          <p className="py-4 text-center text-sm text-muted-foreground">
            No history yet — log this exercise to start tracking.
          </p>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Rest timer (floating pill)
// ─────────────────────────────────────────────────────────────────────────────

interface RestTimerProps {
  remaining: number;
  total: number;
  exerciseName: string;
  onDismiss: () => void;
}

function RestTimer({ remaining, total, exerciseName, onDismiss }: RestTimerProps) {
  const mins = Math.floor(remaining / 60);
  const secs = remaining % 60;
  const progress = (total - remaining) / total;
  const isDone = remaining === 0;

  return (
    <div className={cn(
      "fixed bottom-28 left-1/2 -translate-x-1/2 z-40 flex items-center gap-3 rounded-2xl border px-4 py-3 shadow-lg transition-colors",
      isDone
        ? "border-primary/60 bg-primary/15 text-primary"
        : "border-border bg-card text-foreground",
    )}>
      <Timer className={cn("h-4 w-4 flex-shrink-0", isDone && "text-primary")} />

      <div className="flex flex-col min-w-[72px]">
        {isDone ? (
          <span className="text-sm font-semibold">Rest complete!</span>
        ) : (
          <>
            <span className="font-mono text-base font-bold leading-none">
              {mins}:{String(secs).padStart(2, "0")}
            </span>
            <span className="text-[10px] text-muted-foreground truncate max-w-[120px]">
              rest · {exerciseName}
            </span>
          </>
        )}
      </div>

      {/* Progress bar */}
      <div className="w-16 h-1.5 rounded-full bg-secondary overflow-hidden">
        <div
          className="h-full rounded-full bg-primary transition-all duration-1000"
          style={{ width: `${progress * 100}%` }}
        />
      </div>

      <button
        onClick={onDismiss}
        className="h-6 w-6 flex items-center justify-center rounded-full text-muted-foreground hover:text-foreground"
        aria-label="Dismiss timer"
      >
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Set row
// ─────────────────────────────────────────────────────────────────────────────

interface SetRowProps {
  workoutExerciseId: string;
  setNumber: number;
  prescribed: { repRangeMin: number; repRangeMax: number; rirGuidance: string };
  previous: PreviousSetDto | undefined;
  logged: { weightKg: string; repsCompleted: string } | undefined;
  isPr: boolean;
  onLog: (weId: string, setNum: number, weight: string, reps: string) => void;
  disabled: boolean;
}

function SetRow({
  workoutExerciseId,
  setNumber,
  prescribed,
  previous,
  logged,
  isPr,
  onLog,
  disabled,
}: SetRowProps) {
  const [weight, setWeight] = React.useState(
    logged?.weightKg ?? previous?.weightKg?.toString() ?? "",
  );
  const [reps, setReps] = React.useState(logged?.repsCompleted ?? "");
  const isDone = !!logged;

  return (
    <div className={cn(
      "grid grid-cols-[28px_1fr_1fr_auto] items-center gap-2.5 rounded-lg px-3 py-2.5 transition-colors",
      isDone ? "bg-primary/10" : "bg-elevated",
    )}>
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
        {isPr && isDone && (
          <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/15 px-1.5 py-0.5 text-[10px] font-semibold text-amber-400">
            ↑ Best
          </span>
        )}
        {!isPr && previous && (
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
  prKeys: Set<string>;
  onLog: (weId: string, setNum: number, weight: string, reps: string) => void;
  onSwap: (we: WorkoutExerciseDto) => void;
  onShowHistory: (exerciseId: string, exerciseName: string) => void;
  disabled: boolean;
}

function ExerciseBlock({
  we,
  swappedExercise,
  previousSets,
  loggedSets,
  prKeys,
  onLog,
  onSwap,
  onShowHistory,
  disabled,
}: ExerciseBlockProps) {
  const display = swappedExercise ?? we.exercise;
  const sets = Array.from({ length: we.sets }, (_, i) => i + 1);
  const doneCount = sets.filter((n) => loggedSets.has(`${we.id}:${n}`)).length;

  return (
    <div className="space-y-2">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2 flex-wrap">
            <button
              className="font-semibold text-left hover:text-primary transition-colors"
              onClick={() => onShowHistory(we.exercise.id, display.name)}
            >
              {display.name}
            </button>
            {swappedExercise && (
              <span className="inline-flex items-center gap-1 rounded-full bg-primary/15 px-2 py-0.5 text-xs font-medium text-primary">
                <RefreshCw className="h-2.5 w-2.5" />
                swapped
              </span>
            )}
            <button
              onClick={() => onShowHistory(we.exercise.id, display.name)}
              className="text-muted-foreground hover:text-primary transition-colors"
              title="View progression"
              aria-label="View exercise progression"
            >
              <TrendingUp className="h-3.5 w-3.5" />
            </button>
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
            isPr={prKeys.has(`${we.id}:${n}`)}
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
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60" onClick={onClose}>
      <div
        className="w-full max-w-md rounded-t-2xl bg-card border border-border p-6 space-y-4 max-h-[85vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {step === "reason" && (
          <>
            <div className="flex items-center justify-between">
              <h2 className="font-display text-lg font-bold">
                Why can&apos;t you do {we.exercise.name}?
              </h2>
              <button onClick={onClose} className="text-muted-foreground hover:text-foreground text-xl leading-none">✕</button>
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

        {step === "pain-warning" && (
          <>
            <div className="flex items-center gap-2">
              <button onClick={() => setStep("reason")} className="text-muted-foreground hover:text-foreground">
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
                Never push through pain. If you experience sharp, sudden, or severe pain, stop immediately.
              </p>
              <p className="text-xs text-muted-foreground leading-relaxed pt-1">
                FitCoach cannot diagnose injuries. When in doubt, rest and seek professional evaluation.
              </p>
            </div>
            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" onClick={onClose}>Stop for today</Button>
              <Button className="flex-1" onClick={() => setStep("alternatives")}>Show alternatives</Button>
            </div>
          </>
        )}

        {step === "alternatives" && (
          <>
            <div className="flex items-center gap-2">
              <button onClick={() => setStep("reason")} className="text-muted-foreground hover:text-foreground">
                <ArrowLeft className="h-4 w-4" />
              </button>
              <h2 className="font-display text-lg font-bold">Pick a replacement</h2>
            </div>
            {we.exercise.alternatives.length === 0 ? (
              <p className="text-sm text-muted-foreground py-4 text-center">No alternatives recorded yet.</p>
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
                        {alt.primaryMuscleGroup.replace(/_/g, " ")} · {alt.difficultyLevel.toLowerCase()}
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

function CompletionOverlay({
  workoutName, setsLogged, exerciseCount, startedAt, onDone,
}: { workoutName: string; setsLogged: number; exerciseCount: number; startedAt: string; onDone: () => void }) {
  const durationMinutes = Math.round((Date.now() - new Date(startedAt).getTime()) / 60000);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/95 px-6">
      <div className="w-full max-w-sm text-center space-y-6 animate-fade-up">
        <div className="flex items-center justify-center">
          <div className="flex h-20 w-20 items-center justify-center rounded-full bg-primary/15">
            <Trophy className="h-10 w-10 text-primary" />
          </div>
        </div>
        <div className="space-y-1">
          <p className="text-sm font-semibold uppercase tracking-widest text-primary">Workout complete</p>
          <h1 className="font-display text-3xl font-extrabold tracking-tight">{workoutName}</h1>
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
        <p className="text-sm text-muted-foreground">Great work. Rest, recover, and come back stronger.</p>
        <Button className="w-full" size="lg" onClick={onDone}>Back to today</Button>
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
  const [previousByExercise, setPreviousByExercise] = React.useState<Map<string, PreviousSetDto[]>>(new Map());
  const [loggedSets, setLoggedSets] = React.useState<Map<string, { weightKg: string; repsCompleted: string }>>(new Map());
  const [swappedExercises, setSwappedExercises] = React.useState<Map<string, ExerciseDto>>(new Map());
  const [prKeys, setPrKeys] = React.useState<Set<string>>(new Set());
  const [swapTarget, setSwapTarget] = React.useState<WorkoutExerciseDto | null>(null);
  const [restTimer, setRestTimer] = React.useState<{ remaining: number; total: number; exerciseName: string } | null>(null);
  const [historyTarget, setHistoryTarget] = React.useState<{ exerciseId: string; exerciseName: string } | null>(null);
  const [historyEntries, setHistoryEntries] = React.useState<ExerciseHistoryEntryDto[]>([]);
  const [historyLoading, setHistoryLoading] = React.useState(false);
  const [completed, setCompleted] = React.useState(false);
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [completing, setCompleting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  // ─── Load session ───────────────────────────────────────────────────────────

  React.useEffect(() => {
    let active = true;
    api.getActiveSession()
      .then(async (s) => {
        if (!active) return;
        if (s.id !== sessionId) { router.replace("/today"); return; }
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
            } catch { /* no history */ }
          }),
        );
        if (active) setPreviousByExercise(byExercise);
      })
      .catch((err: unknown) => {
        if (!active) return;
        if (err instanceof ApiError && err.status === 404) router.replace("/today");
        else setError("Could not load session. Please try again.");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [sessionId, router]);

  // ─── Rest timer countdown ───────────────────────────────────────────────────

  React.useEffect(() => {
    if (!restTimer || restTimer.remaining <= 0) return;
    const id = setTimeout(() => {
      setRestTimer((prev) =>
        prev ? { ...prev, remaining: prev.remaining - 1 } : null,
      );
    }, 1000);
    return () => clearTimeout(id);
  }, [restTimer]);

  // Auto-dismiss rest timer 2s after it hits 0
  React.useEffect(() => {
    if (restTimer?.remaining === 0) {
      const id = setTimeout(() => setRestTimer(null), 2000);
      return () => clearTimeout(id);
    }
  }, [restTimer?.remaining]);

  // ─── Exercise history sheet ─────────────────────────────────────────────────

  function openHistory(exerciseId: string, exerciseName: string) {
    setHistoryTarget({ exerciseId, exerciseName });
    setHistoryEntries([]);
    setHistoryLoading(true);
    api.getExerciseHistory(exerciseId)
      .then(setHistoryEntries)
      .catch(() => { /* show empty state */ })
      .finally(() => setHistoryLoading(false));
  }

  // ─── Log set ────────────────────────────────────────────────────────────────

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

      // PR detection — compare with previous session's best weight
      const we = session.workoutDay.exercises.find((e) => e.id === weId);
      if (we) {
        const prevSets = previousByExercise.get(we.exercise.id) ?? [];
        const prevMax = prevSets.reduce((m, s) => Math.max(m, s.weightKg ?? 0), 0);
        const logged = weight ? parseFloat(weight) : 0;
        if (logged > 0 && prevSets.length > 0 && logged > prevMax) {
          setPrKeys((prev) => new Set([...prev, `${weId}:${setNum}`]));
        }

        // Start rest timer
        if (we.restSeconds > 0) {
          const displayName = (swappedExercises.get(weId) ?? we.exercise).name;
          setRestTimer({ remaining: we.restSeconds, total: we.restSeconds, exerciseName: displayName });
        }
      }
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : "Failed to log set.");
    } finally {
      setSaving(false);
    }
  }

  // ─── Complete session ───────────────────────────────────────────────────────

  async function handleComplete() {
    if (!session) return;
    setCompleting(true);
    setError(null);
    try {
      await api.completeSession(session.id);
      setRestTimer(null);
      setCompleted(true);
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : "Failed to complete session.");
      setCompleting(false);
    }
  }

  function handleSwapConfirm(weId: string, alternative: ExerciseDto) {
    setSwappedExercises((prev) => { const n = new Map(prev); n.set(weId, alternative); return n; });
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
            <p className="font-display text-xl font-bold truncate">{session.workoutDay.workoutName}</p>
            <p className="text-xs text-muted-foreground">{loggedCount}/{totalSets} sets logged</p>
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
                  prKeys={prKeys}
                  onLog={handleLog}
                  onSwap={setSwapTarget}
                  onShowHistory={openHistory}
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

      {/* Rest timer */}
      {restTimer && (
        <RestTimer
          remaining={restTimer.remaining}
          total={restTimer.total}
          exerciseName={restTimer.exerciseName}
          onDismiss={() => setRestTimer(null)}
        />
      )}

      {/* Exercise history sheet */}
      {historyTarget && (
        <HistorySheet
          exerciseName={historyTarget.exerciseName}
          entries={historyEntries}
          loading={historyLoading}
          onClose={() => setHistoryTarget(null)}
        />
      )}

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
