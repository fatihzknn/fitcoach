"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Link2, Check } from "lucide-react";
import { Wordmark } from "@/components/wordmark";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, ApiError } from "@/lib/api";
import { session } from "@/lib/session";
import { useI18n } from "@/lib/i18n";

export default function LinkTrainerPage() {
  const router = useRouter();
  const { t } = useI18n();
  const [code, setCode] = React.useState("");
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [linked, setLinked] = React.useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!code.trim()) return setError(t("Enter your trainer’s invite code."));

    setLoading(true);
    try {
      await api.redeemInviteCode(code.trim().toUpperCase());
      setLinked(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t("Something went wrong. Try again."));
    } finally {
      setLoading(false);
    }
  }

  function handleContinue() {
    if (!session.hasCompletedOnboarding()) router.replace("/onboarding");
    else if (!session.hasPlanSelected()) router.replace("/plan-selection");
    else router.replace("/today");
  }

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col justify-center px-6 py-12">
      <div className="animate-fade-up">
        <div className="flex items-center justify-between">
          <Wordmark className="text-2xl" />
          <button
            onClick={() => router.back()}
            className="flex h-10 w-10 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
            aria-label={t("Back")}
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
        </div>

        {linked ? (
          <div className="mt-8 space-y-4 text-center">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
              <Check className="h-7 w-7 text-primary" />
            </div>
            <h1 className="font-display text-2xl font-bold tracking-tight">{t("You’re linked")}</h1>
            <p className="text-muted-foreground">
              {t("Your trainer can now see your progress and assign you plans.")}
            </p>
            <Button className="w-full" size="lg" onClick={handleContinue}>
              {t("Continue")}
            </Button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} noValidate>
            <div className="mt-8 flex items-center gap-3">
              <div className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-full bg-primary/10">
                <Link2 className="h-5 w-5 text-primary" />
              </div>
              <h1 className="font-display text-2xl font-bold tracking-tight">{t("Link your trainer")}</h1>
            </div>
            <p className="mt-2 text-muted-foreground">
              {t("Enter the invite code your trainer shared with you.")}
            </p>

            <div className="mt-8 space-y-2">
              <Label htmlFor="code">{t("Invite code")}</Label>
              <Input
                id="code"
                type="text"
                placeholder="ABCD2345"
                autoCapitalize="characters"
                autoComplete="off"
                maxLength={8}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="text-center font-display text-xl tracking-[0.2em]"
              />
            </div>

            {error && (
              <p className="mt-4 text-sm text-destructive" role="alert">
                {error}
              </p>
            )}

            <Button type="submit" className="mt-8 w-full" size="lg" disabled={loading}>
              {loading ? t("Linking…") : t("Link trainer")}
            </Button>
          </form>
        )}
      </div>
    </main>
  );
}
