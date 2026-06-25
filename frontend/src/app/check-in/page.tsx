"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, ApiError, type CheckInPainStatus } from "@/lib/api";
import { cn } from "@/lib/utils";

// ─── Rating picker (1–5 stars/dots) ─────────────────────────────────────────

interface RatingPickerProps {
  value: number | null;
  onChange: (v: number) => void;
  lowLabel: string;
  highLabel: string;
}

function RatingPicker({ value, onChange, lowLabel, highLabel }: RatingPickerProps) {
  return (
    <div className="space-y-2">
      <div className="flex gap-2">
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            type="button"
            onClick={() => onChange(n)}
            className={cn(
              "flex-1 h-10 rounded-lg border text-sm font-semibold transition-colors",
              value === n
                ? "border-primary bg-primary/15 text-primary"
                : "border-border bg-card text-muted-foreground hover:bg-elevated",
            )}
          >
            {n}
          </button>
        ))}
      </div>
      <div className="flex justify-between text-xs text-muted-foreground px-1">
        <span>{lowLabel}</span>
        <span>{highLabel}</span>
      </div>
    </div>
  );
}

// ─── Pain status picker ──────────────────────────────────────────────────────

const PAIN_OPTIONS: { value: CheckInPainStatus; label: string }[] = [
  { value: "NO_PAIN", label: "No pain" },
  { value: "MILD_PAIN", label: "Mild" },
  { value: "MODERATE_PAIN", label: "Moderate" },
  { value: "SEVERE_PAIN", label: "Severe" },
];

// ─── Page ────────────────────────────────────────────────────────────────────

export default function CheckInPage() {
  const router = useRouter();

  const [weight, setWeight] = React.useState("");
  const [sleep, setSleep] = React.useState<number | null>(null);
  const [energy, setEnergy] = React.useState<number | null>(null);
  const [stress, setStress] = React.useState<number | null>(null);
  const [pain, setPain] = React.useState<CheckInPainStatus>("NO_PAIN");
  const [notes, setNotes] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [done, setDone] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await api.submitCheckIn({
        weightKg: weight ? parseFloat(weight) : null,
        sleepQualityRating: sleep,
        energyRating: energy,
        stressRating: stress,
        painStatus: pain,
        notes: notes.trim() || null,
      });
      setDone(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save. Try again.");
      setSubmitting(false);
    }
  }

  if (done) {
    return (
      <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col items-center justify-center px-6 text-center space-y-4">
        <div className="flex h-20 w-20 items-center justify-center rounded-full bg-primary/15">
          <CheckCircle2 className="h-10 w-10 text-primary" />
        </div>
        <h1 className="font-display text-2xl font-bold">Check-in saved</h1>
        <p className="text-sm text-muted-foreground">Great work staying consistent this week.</p>
        <Button className="w-full max-w-xs" size="lg" onClick={() => router.replace("/progress")}>
          View progress
        </Button>
      </main>
    );
  }

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col px-4 py-6">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <button
          onClick={() => router.back()}
          className="flex h-10 w-10 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
          aria-label="Back"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Weekly
          </p>
          <h1 className="font-display text-2xl font-bold">Check-in</h1>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5 flex-1">

        {/* Weight */}
        <Card>
          <CardContent className="p-4 space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="weight">Body weight (kg)</Label>
              <p className="text-xs text-muted-foreground">Optional — used for trend tracking</p>
            </div>
            <Input
              id="weight"
              inputMode="decimal"
              placeholder="e.g. 75.5"
              value={weight}
              onChange={(e) => setWeight(e.target.value)}
            />
          </CardContent>
        </Card>

        {/* Wellness ratings */}
        <Card>
          <CardContent className="p-4 space-y-5">
            <p className="font-semibold">How did this week feel?</p>

            <div className="space-y-1.5">
              <Label>Sleep quality</Label>
              <RatingPicker
                value={sleep}
                onChange={setSleep}
                lowLabel="Poor"
                highLabel="Great"
              />
            </div>

            <div className="space-y-1.5">
              <Label>Energy levels</Label>
              <RatingPicker
                value={energy}
                onChange={setEnergy}
                lowLabel="Drained"
                highLabel="High energy"
              />
            </div>

            <div className="space-y-1.5">
              <Label>Stress</Label>
              <RatingPicker
                value={stress}
                onChange={setStress}
                lowLabel="Low stress"
                highLabel="Very stressed"
              />
            </div>
          </CardContent>
        </Card>

        {/* Pain status */}
        <Card>
          <CardContent className="p-4 space-y-3">
            <Label>Any pain or discomfort?</Label>
            <div className="grid grid-cols-2 gap-2">
              {PAIN_OPTIONS.map((o) => (
                <button
                  key={o.value}
                  type="button"
                  onClick={() => setPain(o.value)}
                  className={cn(
                    "h-11 rounded-xl border text-sm font-medium transition-colors",
                    pain === o.value
                      ? "border-primary bg-primary/15 text-foreground"
                      : "border-border bg-card text-muted-foreground hover:bg-elevated",
                  )}
                >
                  {o.label}
                </button>
              ))}
            </div>
            {(pain === "MODERATE_PAIN" || pain === "SEVERE_PAIN") && (
              <p className="text-xs text-amber-400 leading-relaxed">
                Consider resting and consulting a healthcare professional if pain persists.
                Never train through severe pain.
              </p>
            )}
          </CardContent>
        </Card>

        {/* Notes */}
        <Card>
          <CardContent className="p-4 space-y-2">
            <Label htmlFor="notes">Notes (optional)</Label>
            <textarea
              id="notes"
              rows={3}
              placeholder="Anything else about this week…"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full resize-none rounded-md border border-border bg-card px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/50"
            />
          </CardContent>
        </Card>

        {error && (
          <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">
            {error}
          </p>
        )}

        <Button type="submit" className="w-full" size="lg" disabled={submitting}>
          {submitting ? "Saving…" : "Save check-in"}
        </Button>
      </form>
    </main>
  );
}
