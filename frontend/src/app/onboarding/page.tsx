"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { Wordmark } from "@/components/wordmark";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { OptionCard } from "@/components/ui/option-card";
import {
  BACKGROUND_OPTIONS,
  BARBELL_COMFORT_OPTIONS,
  DAYS_OPTIONS,
  DURATION_OPTIONS,
  GOAL_OPTIONS,
  PAIN_OPTIONS,
  SEX_OPTIONS,
  STEP_TITLES,
} from "@/lib/onboarding";
import {
  api,
  ApiError,
  type BarbellComfort,
  type MainGoal,
  type OnboardingRequest,
  type PainArea,
  type Sex,
  type TrainingBackground,
} from "@/lib/api";
import { session } from "@/lib/session";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";

const TOTAL_STEPS = 7;

interface Draft {
  mainGoal?: MainGoal;
  trainingBackground?: TrainingBackground;
  trainingDaysPerWeek?: number;
  sessionDurationMinutes?: number;
  age: string;
  heightCm: string;
  weightKg: string;
  sex?: Sex;
  barbellComfort?: BarbellComfort;
  painAreas: PainArea[];
}

export default function OnboardingPage() {
  const router = useRouter();
  const { t } = useI18n();
  const [step, setStep] = React.useState(0);
  const [draft, setDraft] = React.useState<Draft>({
    age: "",
    heightCm: "",
    weightKg: "",
    painAreas: [],
  });
  const [error, setError] = React.useState<string | null>(null);
  const [submitting, setSubmitting] = React.useState(false);

  const next = () => {
    setError(null);
    setStep((s) => Math.min(s + 1, TOTAL_STEPS - 1));
  };
  const back = () => {
    setError(null);
    setStep((s) => Math.max(s - 1, 0));
  };

  // Single-select steps set their value and advance immediately (low friction).
  const choose = <K extends keyof Draft>(key: K, value: Draft[K]) => {
    setDraft((d) => ({ ...d, [key]: value }));
    next();
  };

  function togglePain(area: PainArea) {
    setDraft((d) => {
      let painAreas: PainArea[];
      if (area === "NONE") {
        painAreas = ["NONE"];
      } else {
        const without = d.painAreas.filter((p) => p !== "NONE");
        painAreas = without.includes(area)
          ? without.filter((p) => p !== area)
          : [...without, area];
      }
      return { ...d, painAreas };
    });
  }

  function validateBasics(): string | null {
    const age = Number(draft.age);
    const height = Number(draft.heightCm);
    const weight = Number(draft.weightKg);
    if (!draft.age || age < 13 || age > 100) return t("Enter a valid age (13-100).");
    if (!draft.heightCm || height < 120 || height > 230)
      return t("Enter a valid height in cm (120-230).");
    if (!draft.weightKg || weight < 30 || weight > 300)
      return t("Enter a valid weight in kg (30-300).");
    if (!draft.sex) return t("Select your sex.");
    return null;
  }

  async function submit() {
    const painAreas =
      draft.painAreas.length > 0 ? draft.painAreas : (["NONE"] as PainArea[]);
    const payload: OnboardingRequest = {
      mainGoal: draft.mainGoal!,
      trainingBackground: draft.trainingBackground!,
      trainingDaysPerWeek: draft.trainingDaysPerWeek!,
      sessionDurationMinutes: draft.sessionDurationMinutes!,
      age: Number(draft.age),
      heightCm: Number(draft.heightCm),
      weightKg: Number(draft.weightKg),
      sex: draft.sex!,
      painAreas,
      barbellComfort: draft.barbellComfort!,
    };

    setSubmitting(true);
    setError(null);
    try {
      await api.completeOnboarding(payload);
      session.setOnboarded();
      router.replace("/plan-selection");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t("Could not save. Please try again."));
      setSubmitting(false);
    }
  }

  const progress = ((step + 1) / TOTAL_STEPS) * 100;

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col px-6 py-8">
      <div className="flex items-center gap-3">
        {step > 0 ? (
          <button
            onClick={back}
            className="flex h-10 w-10 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
            aria-label="Back"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
        ) : (
          <Wordmark />
        )}
        <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-secondary">
          <div
            className="h-full rounded-full bg-primary transition-all"
            style={{ width: `${progress}%` }}
          />
        </div>
        <span className="text-xs tabular-nums text-muted-foreground">
          {step + 1}/{TOTAL_STEPS}
        </span>
      </div>

      <h1 className="mt-8 font-display text-2xl font-bold tracking-tight">
        {t(STEP_TITLES[step] ?? "")}
      </h1>

      <div className="mt-6 flex-1 animate-fade-up" key={step}>
        {step === 0 && (
          <div className="space-y-3">
            {GOAL_OPTIONS.map((o) => (
              <OptionCard
                key={o.value}
                label={t(o.label)}
                hint={t(o.hint)}
                selected={draft.mainGoal === o.value}
                onSelect={() => choose("mainGoal", o.value)}
              />
            ))}
          </div>
        )}

        {step === 1 && (
          <div className="space-y-3">
            {BACKGROUND_OPTIONS.map((o) => (
              <OptionCard
                key={o.value}
                label={t(o.label)}
                hint={t(o.hint)}
                selected={draft.trainingBackground === o.value}
                onSelect={() => choose("trainingBackground", o.value)}
              />
            ))}
          </div>
        )}

        {step === 2 && (
          <div className="space-y-3">
            {DAYS_OPTIONS.map((o) => (
              <OptionCard
                key={o.value}
                label={t(o.label)}
                hint={t(o.hint)}
                selected={draft.trainingDaysPerWeek === o.value}
                onSelect={() => choose("trainingDaysPerWeek", o.value)}
              />
            ))}
          </div>
        )}

        {step === 3 && (
          <div className="space-y-3">
            {DURATION_OPTIONS.map((o) => (
              <OptionCard
                key={o.value}
                label={t(o.label)}
                hint={t(o.hint)}
                selected={draft.sessionDurationMinutes === o.value}
                onSelect={() => choose("sessionDurationMinutes", o.value)}
              />
            ))}
          </div>
        )}

        {step === 4 && (
          <div className="space-y-5">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="age">{t("Age")}</Label>
                <Input
                  id="age"
                  inputMode="numeric"
                  value={draft.age}
                  onChange={(e) => setDraft((d) => ({ ...d, age: e.target.value }))}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="height">{t("Height (cm)")}</Label>
                <Input
                  id="height"
                  inputMode="numeric"
                  value={draft.heightCm}
                  onChange={(e) => setDraft((d) => ({ ...d, heightCm: e.target.value }))}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="weight">{t("Weight (kg)")}</Label>
              <Input
                id="weight"
                inputMode="decimal"
                value={draft.weightKg}
                onChange={(e) => setDraft((d) => ({ ...d, weightKg: e.target.value }))}
              />
            </div>
            <div className="space-y-2">
              <Label>{t("Sex")}</Label>
              <div className="grid grid-cols-3 gap-3">
                {SEX_OPTIONS.map((o) => (
                  <button
                    key={o.value}
                    type="button"
                    onClick={() => setDraft((d) => ({ ...d, sex: o.value }))}
                    aria-pressed={draft.sex === o.value}
                    className={cn(
                      "h-12 rounded-md border text-sm font-medium transition-colors",
                      draft.sex === o.value
                        ? "border-primary bg-primary/10 text-foreground"
                        : "border-border bg-card hover:bg-elevated",
                    )}
                  >
                    {t(o.label)}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {step === 5 && (
          <div className="space-y-3">
            {BARBELL_COMFORT_OPTIONS.map((o) => (
              <OptionCard
                key={o.value}
                label={t(o.label)}
                hint={t(o.hint)}
                selected={draft.barbellComfort === o.value}
                onSelect={() => choose("barbellComfort", o.value)}
              />
            ))}
          </div>
        )}

        {step === 6 && (
          <div className="space-y-3">
            <p className="text-sm text-muted-foreground">
              {t("Select all that apply. This keeps risky movements out of your plan.")}
            </p>
            {PAIN_OPTIONS.map((o) => (
              <OptionCard
                key={o.value}
                label={t(o.label)}
                selected={draft.painAreas.includes(o.value)}
                onSelect={() => togglePain(o.value)}
              />
            ))}
          </div>
        )}
      </div>

      {error && (
        <p className="mt-4 text-sm text-destructive" role="alert">
          {error}
        </p>
      )}

      {step === 4 && (
        <Button
          className="mt-6 w-full"
          size="lg"
          onClick={() => {
            const msg = validateBasics();
            if (msg) setError(msg);
            else next();
          }}
        >
          {t("Continue")}
        </Button>
      )}

      {step === 6 && (
        <Button className="mt-6 w-full" size="lg" onClick={submit} disabled={submitting}>
          {submitting ? t("Building your plan...") : t("Finish setup")}
        </Button>
      )}
    </main>
  );
}
