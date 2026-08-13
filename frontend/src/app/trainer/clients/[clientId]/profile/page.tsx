"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { OptionCard } from "@/components/ui/option-card";
import {
  GOAL_OPTIONS,
  BACKGROUND_OPTIONS,
  DAYS_OPTIONS,
  DURATION_OPTIONS,
  SEX_OPTIONS,
  PAIN_OPTIONS,
  BARBELL_COMFORT_OPTIONS,
} from "@/lib/onboarding";
import {
  api,
  ApiError,
  type MainGoal,
  type TrainingBackground,
  type Sex,
  type PainArea,
  type BarbellComfort,
  type OnboardingRequest,
} from "@/lib/api";
import { cn } from "@/lib/utils";
import { useI18n } from "@/lib/i18n";

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

export default function EditClientProfilePage() {
  const params = useParams<{ clientId: string }>();
  const router = useRouter();
  const { t } = useI18n();
  const clientId = params.clientId;

  const [loading, setLoading] = React.useState(true);
  const [loadError, setLoadError] = React.useState<string | null>(null);
  const [draft, setDraft] = React.useState<Draft>({ age: "", heightCm: "", weightKg: "", painAreas: [] });
  const [error, setError] = React.useState<string | null>(null);
  const [submitting, setSubmitting] = React.useState(false);

  React.useEffect(() => {
    let active = true;
    api.getClientProfile(clientId)
      .then((p) => {
        if (!active) return;
        setDraft({
          mainGoal: p.mainGoal,
          trainingBackground: p.trainingBackground,
          trainingDaysPerWeek: p.trainingDaysPerWeek,
          sessionDurationMinutes: p.sessionDurationMinutes,
          age: String(p.age),
          heightCm: String(p.heightCm),
          weightKg: String(p.weightKg),
          sex: p.sex,
          barbellComfort: p.barbellComfort,
          painAreas: p.painAreas,
        });
      })
      .catch((err: unknown) => {
        if (active) setLoadError(err instanceof ApiError ? err.message : t("Could not load this client's profile."));
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [clientId, t]);

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

  function validate(): string | null {
    const age = Number(draft.age);
    const height = Number(draft.heightCm);
    const weight = Number(draft.weightKg);
    if (!draft.mainGoal) return t("Choose a main goal.");
    if (!draft.trainingBackground) return t("Choose a training background.");
    if (!draft.trainingDaysPerWeek) return t("Choose training days per week.");
    if (!draft.sessionDurationMinutes) return t("Choose a session length.");
    if (!draft.age || age < 13 || age > 100) return t("Enter a valid age (13-100).");
    if (!draft.heightCm || height < 120 || height > 230) return t("Enter a valid height in cm (120-230).");
    if (!draft.weightKg || weight < 30 || weight > 300) return t("Enter a valid weight in kg (30-300).");
    if (!draft.sex) return t("Select a sex.");
    if (!draft.barbellComfort) return t("Choose a barbell comfort level.");
    return null;
  }

  async function handleSubmit() {
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    const painAreas = draft.painAreas.length > 0 ? draft.painAreas : (["NONE"] as PainArea[]);
    const request: OnboardingRequest = {
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
      await api.updateClientProfile(clientId, request);
      router.replace(`/trainer/clients/${clientId}`);
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : t("Could not save changes. Please try again."));
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
          {t("Edit client profile")}
        </h1>
        <p className="text-sm text-muted-foreground">
          {t("Changes take effect the next time a plan is generated for this client.")}
        </p>
      </div>

      {loadError && (
        <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">{loadError}</p>
      )}
      {error && (
        <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">{error}</p>
      )}

      {loading ? (
        <div className="space-y-3 animate-pulse">
          <div className="h-24 rounded-2xl bg-card" />
          <div className="h-24 rounded-2xl bg-card" />
          <div className="h-24 rounded-2xl bg-card" />
        </div>
      ) : (
        <>
          <div className="space-y-2">
            <Label>{t("Main goal")}</Label>
            <div className="grid grid-cols-2 gap-2">
              {GOAL_OPTIONS.map((o) => (
                <OptionCard
                  key={o.value}
                  label={t(o.label)}
                  hint={t(o.hint)}
                  selected={draft.mainGoal === o.value}
                  onSelect={() => setDraft((d) => ({ ...d, mainGoal: o.value }))}
                />
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <Label>{t("Training background")}</Label>
            <div className="space-y-2">
              {BACKGROUND_OPTIONS.map((o) => (
                <OptionCard
                  key={o.value}
                  label={t(o.label)}
                  hint={t(o.hint)}
                  selected={draft.trainingBackground === o.value}
                  onSelect={() => setDraft((d) => ({ ...d, trainingBackground: o.value }))}
                />
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <Label>{t("Training days per week")}</Label>
            <div className="grid grid-cols-3 gap-2">
              {DAYS_OPTIONS.map((o) => (
                <OptionCard
                  key={o.value}
                  label={t(o.label)}
                  hint={t(o.hint)}
                  selected={draft.trainingDaysPerWeek === o.value}
                  onSelect={() => setDraft((d) => ({ ...d, trainingDaysPerWeek: o.value }))}
                />
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <Label>{t("Session length")}</Label>
            <div className="grid grid-cols-3 gap-2">
              {DURATION_OPTIONS.map((o) => (
                <OptionCard
                  key={o.value}
                  label={t(o.label)}
                  hint={t(o.hint)}
                  selected={draft.sessionDurationMinutes === o.value}
                  onSelect={() => setDraft((d) => ({ ...d, sessionDurationMinutes: o.value }))}
                />
              ))}
            </div>
          </div>

          <div className="space-y-3">
            <Label>{t("A few basics")}</Label>
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

          <div className="space-y-2">
            <Label>{t("How do you feel about barbells?")}</Label>
            <div className="space-y-2">
              {BARBELL_COMFORT_OPTIONS.map((o) => (
                <OptionCard
                  key={o.value}
                  label={t(o.label)}
                  hint={t(o.hint)}
                  selected={draft.barbellComfort === o.value}
                  onSelect={() => setDraft((d) => ({ ...d, barbellComfort: o.value }))}
                />
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <Label>{t("Any pain or injuries?")}</Label>
            <div className="space-y-2">
              {PAIN_OPTIONS.map((o) => (
                <OptionCard
                  key={o.value}
                  label={t(o.label)}
                  selected={draft.painAreas.includes(o.value)}
                  onSelect={() => togglePain(o.value)}
                />
              ))}
            </div>
          </div>

          <Button size="lg" className="w-full" onClick={handleSubmit} disabled={submitting}>
            {submitting ? t("Saving…") : t("Save changes")}
          </Button>
        </>
      )}
    </section>
  );
}
