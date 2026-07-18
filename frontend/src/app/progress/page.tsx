"use client";

import * as React from "react";
import Link from "next/link";
import { ClipboardList, Flame, Target, Trophy, Ruler } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  api,
  ApiError,
  type ProgressStatsDto,
  type WeeklyCheckInDto,
  type BodyMeasurementDto,
  type FitnessProfileDto,
} from "@/lib/api";
import { cn } from "@/lib/utils";

// ─── SVG line chart ───────────────────────────────────────────────────────────

interface ChartPoint { label: string; value: number | null }

function LineChart({ points, unit = "", gradientId = "chartGrad" }: { points: ChartPoint[]; unit?: string; gradientId?: string }) {
  const valid = points.filter((p) => p.value !== null) as { label: string; value: number }[];
  if (valid.length < 2) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">
        Not enough data yet — log 2+ entries to see a trend.
      </p>
    );
  }

  const W = 300, H = 80, PAD_X = 4, PAD_Y = 8;
  const values = valid.map((p) => p.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;

  const coords = valid.map((p, i) => ({
    x: PAD_X + (i / (valid.length - 1)) * (W - PAD_X * 2),
    y: PAD_Y + ((max - p.value) / range) * (H - PAD_Y * 2),
    label: p.label,
    value: p.value,
  }));

  const polyline = coords.map((c) => `${c.x},${c.y}`).join(" ");

  return (
    <div className="space-y-1">
      <svg viewBox={`0 0 ${W} ${H}`} className="w-full overflow-visible">
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="hsl(var(--primary))" stopOpacity="0.18" />
            <stop offset="100%" stopColor="hsl(var(--primary))" stopOpacity="0" />
          </linearGradient>
        </defs>
        <polygon
          points={`${coords[0]!.x},${H} ${polyline} ${coords[coords.length - 1]!.x},${H}`}
          fill={`url(#${gradientId})`}
        />
        <polyline
          points={polyline}
          fill="none"
          stroke="hsl(var(--primary))"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {coords.map((c, i) => (
          <circle key={i} cx={c.x} cy={c.y} r="3.5" fill="hsl(var(--primary))" />
        ))}
      </svg>
      <div className="flex justify-between text-xs text-muted-foreground px-1">
        {valid.map((p, i) => <span key={i}>{p.label}</span>)}
      </div>
      <div className="flex justify-between text-xs text-muted-foreground/60 px-1">
        <span>min {min.toFixed(1)}{unit}</span>
        <span>max {max.toFixed(1)}{unit}</span>
      </div>
    </div>
  );
}

// ─── BF% category badge ───────────────────────────────────────────────────────

function BfBadge({ bf, sex }: { bf: number; sex?: string }) {
  let label: string, color: string;
  if (sex === "FEMALE") {
    if      (bf < 14) { label = "Essential fat"; color = "text-amber-400"; }
    else if (bf < 21) { label = "Athlete";       color = "text-primary"; }
    else if (bf < 25) { label = "Fitness";       color = "text-green-400"; }
    else if (bf < 32) { label = "Average";       color = "text-yellow-400"; }
    else              { label = "Above avg.";    color = "text-red-400"; }
  } else {
    if      (bf < 6)  { label = "Essential fat"; color = "text-amber-400"; }
    else if (bf < 14) { label = "Athlete";       color = "text-primary"; }
    else if (bf < 18) { label = "Fitness";       color = "text-green-400"; }
    else if (bf < 25) { label = "Average";       color = "text-yellow-400"; }
    else              { label = "Above avg.";    color = "text-red-400"; }
  }
  return (
    <span className={cn("text-xs font-semibold", color)}>
      {bf.toFixed(1)}% · {label}
    </span>
  );
}

// ─── Wellness dots ────────────────────────────────────────────────────────────

function WellnessDots({ checkIns }: { checkIns: WeeklyCheckInDto[] }) {
  const recent = checkIns.slice(0, 6).reverse();
  if (recent.length === 0) return null;

  function dotColor(v: number | null) {
    if (v === null) return "bg-secondary";
    if (v >= 4) return "bg-emerald-500";
    if (v === 3) return "bg-amber-500";
    return "bg-red-500";
  }

  return (
    <div className="space-y-3">
      {(["sleepQualityRating", "energyRating", "stressRating"] as const).map((field) => {
        const labels: Record<string, string> = {
          sleepQualityRating: "Sleep",
          energyRating: "Energy",
          stressRating: "Stress",
        };
        return (
          <div key={field} className="flex items-center gap-3">
            <span className="w-14 text-xs text-muted-foreground">{labels[field]}</span>
            <div className="flex gap-1.5">
              {recent.map((c, i) => (
                <div key={i} title={`Week of ${c.weekStart}: ${c[field] ?? "—"}/5`}
                  className={cn("h-4 w-4 rounded-full", dotColor(c[field]))} />
              ))}
            </div>
            <span className="text-xs text-muted-foreground/60">
              {recent[recent.length - 1]?.[field] ?? "—"}/5 this week
            </span>
          </div>
        );
      })}
      <div className="flex items-center gap-2 text-xs text-muted-foreground/60">
        <span className="flex items-center gap-1"><span className="h-2.5 w-2.5 rounded-full bg-emerald-500 inline-block" /> 4–5</span>
        <span className="flex items-center gap-1"><span className="h-2.5 w-2.5 rounded-full bg-amber-500 inline-block" /> 3</span>
        <span className="flex items-center gap-1"><span className="h-2.5 w-2.5 rounded-full bg-red-500 inline-block" /> 1–2</span>
      </div>
    </div>
  );
}

// ─── Stat card ────────────────────────────────────────────────────────────────

function StatCard({ icon: Icon, value, label, sub }: { icon: React.ElementType; value: string; label: string; sub?: string }) {
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

// ─── Page ─────────────────────────────────────────────────────────────────────

function weekLabel(isoDate: string) {
  const d = new Date(isoDate);
  return d.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

export default function ProgressPage() {
  const [stats, setStats] = React.useState<ProgressStatsDto | null>(null);
  const [measurements, setMeasurements] = React.useState<BodyMeasurementDto[]>([]);
  const [profile, setProfile] = React.useState<FitnessProfileDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let active = true;
    Promise.all([
      api.getProgressStats(),
      api.getMeasurementHistory().catch(() => [] as BodyMeasurementDto[]),
      api.profile().catch(() => null),
    ])
      .then(([s, m, p]) => {
        if (!active) return;
        setStats(s);
        setMeasurements(m);
        setProfile(p);
      })
      .catch((err: unknown) => {
        if (active) setError(err instanceof ApiError ? err.message : "Could not load progress data.");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const weightPoints: ChartPoint[] = (stats?.checkInHistory ?? [])
    .slice().reverse()
    .map((c) => ({ label: weekLabel(c.weekStart), value: c.weightKg }));

  const bfPoints: ChartPoint[] = [...measurements]
    .reverse()
    .map((m) => ({
      label: m.measuredAt.slice(5), // MM-DD
      value: m.bodyFatPercentage ? parseFloat(m.bodyFatPercentage.toString()) : null,
    }));

  const latestBf = measurements[0]?.bodyFatPercentage
    ? parseFloat(measurements[0].bodyFatPercentage.toString())
    : null;

  return (
    <AppShell>
      <section className="animate-fade-up space-y-6">

        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium uppercase tracking-[0.18em] text-muted-foreground">Your</p>
            <h1 className="font-display text-4xl font-extrabold leading-tight tracking-tight">Progress</h1>
          </div>
          <div className="flex gap-2">
            <Link href="/measurements">
              <Button size="sm" variant="secondary">
                <Ruler className="h-4 w-4 mr-1.5" />
                Measure
              </Button>
            </Link>
            <Link href="/check-in">
              <Button size="sm" variant="secondary">
                <ClipboardList className="h-4 w-4 mr-1.5" />
                Check in
              </Button>
            </Link>
          </div>
        </div>

        {error && (
          <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">{error}</p>
        )}

        {loading && (
          <div className="space-y-4 animate-pulse">
            <div className="grid grid-cols-2 gap-3">
              {[1,2,3,4].map((i) => <div key={i} className="h-20 rounded-2xl bg-card" />)}
            </div>
            <div className="h-48 rounded-2xl bg-card" />
          </div>
        )}

        {!loading && stats && (
          <>
            {/* Stat cards */}
            <div className="grid grid-cols-2 gap-3">
              <StatCard icon={Trophy}       value={String(stats.totalWorkoutsAllTime)} label="Total workouts" sub="all time" />
              <StatCard icon={Target}       value={`${stats.adherenceRate4Weeks}%`}   label="Adherence"      sub="last 4 weeks" />
              <StatCard icon={Flame}        value={`${stats.currentStreakWeeks}w`}     label="Current streak" sub="consecutive weeks" />
              <StatCard icon={ClipboardList} value={String(stats.workoutsThisWeek)}  label="This week"      sub="sessions done" />
            </div>

            {/* Weight trend */}
            <Card>
              <CardContent className="p-4 space-y-4">
                <p className="font-semibold">Weight trend</p>
                <LineChart points={weightPoints} unit=" kg" gradientId="weightGrad" />
              </CardContent>
            </Card>

            {/* Body fat % */}
            <Card>
              <CardContent className="p-4 space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-semibold">Body fat %</p>
                    {measurements[0]?.bodyFatMethod && (
                      <p className="text-xs text-muted-foreground/60">
                        {measurements[0].bodyFatMethod === "BAI" ? "Body Adiposity Index" : "US Navy method"}
                      </p>
                    )}
                  </div>
                  {latestBf !== null && <BfBadge bf={latestBf} sex={profile?.sex} />}
                </div>
                {bfPoints.filter((p) => p.value !== null).length >= 2 ? (
                  <LineChart points={bfPoints} unit="%" gradientId="bfGrad" />
                ) : measurements.length === 0 ? (
                  <div className="py-2 space-y-3">
                    <p className="text-sm text-muted-foreground">
                      Track your body composition with the US Navy method. Log your neck, waist{" "}
                      {/* sex unknown without profile — show generic text */}
                      and hip measurements to see your body fat trend.
                    </p>
                    <Link href="/measurements">
                      <Button className="w-full" variant="secondary">Log first measurement</Button>
                    </Link>
                  </div>
                ) : (
                  <p className="py-2 text-sm text-muted-foreground">
                    Log 2+ measurements to see a trend.{" "}
                    <Link href="/measurements" className="text-primary underline underline-offset-2">Add one now →</Link>
                  </p>
                )}

                {/* Latest site measurements */}
                {measurements[0] && (
                  <div className="grid grid-cols-3 gap-2 pt-1">
                    {[
                      { label: "Waist",  val: measurements[0].waistCm },
                      { label: "Chest",  val: measurements[0].chestCm },
                      { label: "Bicep",  val: measurements[0].bicepCm },
                      { label: "Thigh",  val: measurements[0].thighCm },
                      { label: "Calf",   val: measurements[0].calfCm },
                      { label: "Neck",   val: measurements[0].neckCm },
                    ].filter((s) => s.val !== null).map((s) => (
                      <div key={s.label} className="rounded-lg bg-elevated px-2 py-1.5 text-center">
                        <p className="text-xs text-muted-foreground">{s.label}</p>
                        <p className="text-sm font-semibold">{s.val} cm</p>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Wellness overview */}
            {stats.checkInHistory.length > 0 && (
              <Card>
                <CardContent className="p-4 space-y-4">
                  <p className="font-semibold">Weekly wellness</p>
                  <WellnessDots checkIns={stats.checkInHistory} />
                </CardContent>
              </Card>
            )}

            {/* Check-in history list */}
            {stats.checkInHistory.length > 0 ? (
              <Card>
                <CardContent className="p-4 space-y-3">
                  <p className="font-semibold">Check-in history</p>
                  <div className="space-y-2">
                    {stats.checkInHistory.slice(0, 8).map((c) => (
                      <div key={c.id} className="flex items-center justify-between rounded-lg bg-elevated px-3 py-2.5">
                        <div>
                          <p className="text-sm font-medium">Week of {weekLabel(c.weekStart)}</p>
                          <p className="text-xs text-muted-foreground">
                            {c.weightKg ? `${c.weightKg} kg` : "No weight"} · {c.painStatus.replace(/_/g, " ").toLowerCase()}
                          </p>
                        </div>
                        <div className="text-right text-xs text-muted-foreground space-y-0.5">
                          {c.sleepQualityRating && <p>sleep {c.sleepQualityRating}/5</p>}
                          {c.energyRating && <p>energy {c.energyRating}/5</p>}
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            ) : (
              <Card>
                <CardContent className="p-6 text-center space-y-3">
                  <p className="text-sm text-muted-foreground">
                    No check-ins yet. Do your first weekly check-in to start tracking your weight and wellness trends.
                  </p>
                  <Link href="/check-in">
                    <Button className="w-full" size="lg">Do your first check-in</Button>
                  </Link>
                </CardContent>
              </Card>
            )}
          </>
        )}
      </section>
    </AppShell>
  );
}
