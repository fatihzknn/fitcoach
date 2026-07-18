"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Dumbbell, ChevronRight, AlertTriangle, CheckCircle2, ClipboardList } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  api,
  ApiError,
  type WorkoutPlanDto,
  type WorkoutDayDto,
  type WorkoutSessionDto,
  type WeeklyCheckInDto,
} from "@/lib/api";
import { cn } from "@/lib/utils";
import { useI18n } from "@/lib/i18n";

function formatDate(iso: string, locale?: string) {
  return new Date(iso).toLocaleDateString(locale, {
    weekday: "short",
    month: "short",
    day: "numeric",
  });
}

function thisWeekCount(history: WorkoutSessionDto[]) {
  const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
  return history.filter((s) => new Date(s.startedAt).getTime() > weekAgo).length;
}

export default function TodayPage() {
  const router = useRouter();
  const { t, locale } = useI18n();
  const [name, setName] = React.useState<string | null>(null);
  const [plan, setPlan] = React.useState<WorkoutPlanDto | null>(null);
  const [selectedDay, setSelectedDay] = React.useState<WorkoutDayDto | null>(null);
  const [history, setHistory] = React.useState<WorkoutSessionDto[]>([]);
  const [latestCheckIn, setLatestCheckIn] = React.useState<WeeklyCheckInDto | null | undefined>(undefined);
  const [loading, setLoading] = React.useState(true);
  const [starting, setStarting] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let active = true;
    Promise.all([api.me(), api.getActivePlan(), api.getSessionHistory(), api.getCheckInHistory()])
      .then(([me, activePlan, sessionHistory, checkIns]) => {
        if (!active) return;
        setName(me.user.displayName);
        setPlan(activePlan);
        setHistory(sessionHistory);
        // Show nudge only when current week (Mon–Sun) has no check-in
        const currentWeekStart = (() => {
          const d = new Date();
          const day = d.getDay();
          const diff = d.getDate() - day + (day === 0 ? -6 : 1);
          return new Date(new Date(d).setDate(diff)).toISOString().split("T")[0];
        })();
        const hasThisWeek = checkIns.some((c) => c.weekStart === currentWeekStart);
        setLatestCheckIn(hasThisWeek ? checkIns[0] ?? null : null);

        // Auto-select the next day based on the last completed session
        const lastDayNumber = sessionHistory[0]?.workoutDay.dayNumber ?? 0;
        const nextDay =
          activePlan.days.find((d) => d.dayNumber > lastDayNumber) ??
          activePlan.days[0] ??
          null;
        setSelectedDay(nextDay);
      })
      .catch((err: unknown) => {
        if (!active) return;
        setError(
          err instanceof ApiError
            ? err.message
            : t("Could not load your plan. Please try again."),
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [t]);

  async function handleStart() {
    if (!selectedDay) return;
    setStarting(true);
    setError(null);
    try {
      const session = await api.startSession(selectedDay.id);
      router.push(`/workout/${session.id}`);
    } catch (err: unknown) {
      setError(
        err instanceof ApiError ? err.message : t("Could not start session. Try again."),
      );
      setStarting(false);
    }
  }

  const totalSets = selectedDay
    ? selectedDay.exercises.reduce((s, we) => s + we.sets, 0)
    : 0;
  const weeklyCount = thisWeekCount(history);
  const recentSessions = history.slice(0, 3);

  return (
    <AppShell>
      <section className="animate-fade-up space-y-6">

        {/* Greeting */}
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.18em] text-muted-foreground">
            {t("Today")}
          </p>
          <h1 className="mt-1 font-display text-4xl font-extrabold leading-tight tracking-tight">
            {loading ? t("Loading…") : name ? t("Hi, {name}.", { name }) : t("Good to see you.")}
          </h1>
        </div>

        {/* Weekly check-in nudge — shown when this week has no check-in yet */}
        {!loading && latestCheckIn === null && (
          <Link href="/check-in">
            <div className="flex items-center gap-3 rounded-xl border border-primary/40 bg-primary/5 px-4 py-3 hover:bg-primary/10 transition-colors">
              <ClipboardList className="h-5 w-5 text-primary flex-shrink-0" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium">{t("Weekly check-in ready")}</p>
                <p className="text-xs text-muted-foreground">{t("Log your weight & how you feel")}</p>
              </div>
              <ChevronRight className="h-4 w-4 text-muted-foreground flex-shrink-0" />
            </div>
          </Link>
        )}

        {error && (
          <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">
            {error}
          </p>
        )}

        {loading && (
          <div className="space-y-4 animate-pulse">
            <div className="h-10 rounded-lg bg-card w-1/2" />
            <div className="h-64 rounded-2xl bg-card" />
            <div className="h-24 rounded-2xl bg-card" />
          </div>
        )}

        {!loading && plan && selectedDay && (
          <>
            {/* Plan label + day selector */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    {t("Your Plan")}
                  </p>
                  <p className="mt-0.5 font-medium text-foreground">{plan.name}</p>
                </div>
                <span className="text-xs text-muted-foreground">
                  {t("{n} days/week", { n: plan.trainingDaysPerWeek })}
                </span>
              </div>

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
                    {t("Day {n}", { n: day.dayNumber })}
                  </button>
                ))}
              </div>
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
                    {t("{a} exercises · {b} total sets", { a: selectedDay.exercises.length, b: totalSets })}
                  </p>
                </div>

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

                <Button className="w-full" size="lg" onClick={handleStart} disabled={starting}>
                  {starting ? t("Starting…") : t("Start workout")}
                  {!starting && <ChevronRight className="ml-1 h-4 w-4" />}
                </Button>
              </CardContent>
            </Card>

            {/* Weekly stats */}
            <Card>
              <CardContent className="p-5">
                <div className="flex items-center justify-between mb-4">
                  <p className="text-sm font-medium text-muted-foreground">{t("This week")}</p>
                  <span className="text-xs text-muted-foreground">{t("last 7 days")}</span>
                </div>
                <div className="flex items-end gap-2">
                  <p className="font-display text-4xl font-bold">{weeklyCount}</p>
                  <p className="mb-1 text-sm text-muted-foreground">
                    / {plan.trainingDaysPerWeek} {t("workouts")}
                  </p>
                </div>

                {/* Mini history list */}
                {recentSessions.length > 0 && (
                  <div className="mt-4 space-y-2">
                    {recentSessions.map((s) => (
                      <div
                        key={s.id}
                        className="flex items-center justify-between rounded-lg bg-elevated px-3 py-2"
                      >
                        <div className="flex items-center gap-2">
                          <CheckCircle2 className="h-4 w-4 text-primary flex-shrink-0" />
                          <span className="text-sm">{s.workoutDay.workoutName}</span>
                        </div>
                        <span className="text-xs text-muted-foreground">
                          {formatDate(s.startedAt, locale)}
                        </span>
                      </div>
                    ))}
                  </div>
                )}

                {recentSessions.length === 0 && (
                  <p className="mt-3 text-sm text-muted-foreground">
                    {t("No sessions yet — start your first workout above.")}
                  </p>
                )}
              </CardContent>
            </Card>
          </>
        )}
      </section>
    </AppShell>
  );
}
