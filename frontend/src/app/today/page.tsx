"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { Dumbbell, ChevronRight, AlertTriangle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { api, ApiError, type WorkoutPlanDto, type WorkoutDayDto } from "@/lib/api";
import { cn } from "@/lib/utils";

export default function TodayPage() {
  const router = useRouter();
  const [name, setName] = React.useState<string | null>(null);
  const [plan, setPlan] = React.useState<WorkoutPlanDto | null>(null);
  const [selectedDay, setSelectedDay] = React.useState<WorkoutDayDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [starting, setStarting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let active = true;
    Promise.all([api.me(), api.getActivePlan()])
      .then(([me, activePlan]) => {
        if (!active) return;
        setName(me.user.displayName);
        setPlan(activePlan);
        setSelectedDay(activePlan.days[0] ?? null);
      })
      .catch((err: unknown) => {
        if (!active) return;
        setError(
          err instanceof ApiError
            ? err.message
            : "Could not load your plan. Please try again.",
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  async function handleStart() {
    if (!selectedDay) return;
    setStarting(true);
    setError(null);
    try {
      const session = await api.startSession(selectedDay.id);
      router.push(`/workout/${session.id}`);
    } catch (err: unknown) {
      setError(
        err instanceof ApiError ? err.message : "Could not start session. Try again.",
      );
      setStarting(false);
    }
  }

  const totalSets = selectedDay
    ? selectedDay.exercises.reduce((s, we) => s + we.sets, 0)
    : 0;

  return (
    <AppShell>
      <section className="animate-fade-up space-y-6">

        {/* Greeting */}
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.18em] text-muted-foreground">
            Today
          </p>
          <h1 className="mt-1 font-display text-4xl font-extrabold leading-tight tracking-tight">
            {loading ? "Loading…" : name ? `Hi, ${name}.` : "Good to see you."}
          </h1>
        </div>

        {error && (
          <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">
            {error}
          </p>
        )}

        {loading && (
          <div className="space-y-4 animate-pulse">
            <div className="h-10 rounded-lg bg-card w-1/2" />
            <div className="h-48 rounded-2xl bg-card" />
          </div>
        )}

        {!loading && plan && selectedDay && (
          <>
            {/* Plan name */}
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Your Plan
                </p>
                <p className="mt-0.5 font-medium text-foreground">{plan.name}</p>
              </div>
              <span className="text-xs text-muted-foreground">
                {plan.trainingDaysPerWeek} days/week
              </span>
            </div>

            {/* Day selector */}
            <div className="flex gap-2 overflow-x-auto pb-1 -mx-1 px-1">
              {plan.days.map((day) => (
                <button
                  key={day.id}
                  onClick={() => setSelectedDay(day)}
                  className={cn(
                    "flex-shrink-0 rounded-xl border px-4 py-2.5 text-sm font-medium transition-colors",
                    selectedDay.id === day.id
                      ? "border-primary bg-primary/10 text-foreground"
                      : "border-border bg-card text-muted-foreground hover:bg-elevated",
                  )}
                >
                  Day {day.dayNumber}
                </button>
              ))}
            </div>

            {/* Today's workout card */}
            <Card className="overflow-hidden">
              <div className="h-1 w-full bg-primary" />
              <CardContent className="p-5 space-y-4">
                <div>
                  <p className="font-display text-2xl font-bold tracking-tight">
                    {selectedDay.workoutName}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {selectedDay.exercises.length} exercises · {totalSets} total sets
                  </p>
                </div>

                {/* Exercise preview */}
                <div className="space-y-2">
                  {selectedDay.exercises.map((we) => (
                    <div
                      key={we.id}
                      className="flex items-center justify-between rounded-lg bg-elevated px-3 py-2.5"
                    >
                      <div className="flex items-center gap-2.5">
                        <Dumbbell className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                        <span className="text-sm font-medium">{we.exercise.name}</span>
                      </div>
                      <span className="text-xs text-muted-foreground whitespace-nowrap">
                        {we.sets}×{we.repRangeMin}–{we.repRangeMax}
                      </span>
                    </div>
                  ))}
                </div>

                {plan.sustainabilityWarning && (
                  <div className="flex gap-2 rounded-lg bg-amber-500/10 border border-amber-500/30 px-3 py-2">
                    <AlertTriangle className="h-4 w-4 text-amber-500 flex-shrink-0 mt-0.5" />
                    <p className="text-xs text-amber-400 leading-relaxed">
                      {plan.sustainabilityWarning}
                    </p>
                  </div>
                )}

                <Button
                  className="w-full"
                  size="lg"
                  onClick={handleStart}
                  disabled={starting}
                >
                  {starting ? "Starting…" : "Start workout"}
                  {!starting && <ChevronRight className="ml-1 h-4 w-4" />}
                </Button>
              </CardContent>
            </Card>

            {/* Placeholder for Phase 5 weekly summary */}
            <Card>
              <CardContent className="flex items-center justify-between p-5">
                <div>
                  <p className="font-display text-3xl font-bold">—</p>
                  <p className="text-xs text-muted-foreground">workouts this week</p>
                </div>
                <p className="text-right text-xs text-muted-foreground">
                  Weekly summary
                  <br />
                  arrives in Phase 5
                </p>
              </CardContent>
            </Card>
          </>
        )}
      </section>
    </AppShell>
  );
}
