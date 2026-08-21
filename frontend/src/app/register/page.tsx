"use client";

import * as React from "react";
import Link from "next/link";
import { Wordmark } from "@/components/wordmark";
import { BackendStatus } from "@/components/backend-status";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, ApiError } from "@/lib/api";
import { session } from "@/lib/session";
import { useI18n, LangToggle } from "@/lib/i18n";
import { cn } from "@/lib/utils";

export default function RegisterPage() {
  const { t } = useI18n();
  const [displayName, setDisplayName] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [isTrainer, setIsTrainer] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!displayName.trim()) return setError(t("Enter your name."));
    if (!email.trim()) return setError(t("Enter your email."));
    if (password.length < 8) return setError(t("Password must be at least 8 characters."));

    setLoading(true);
    try {
      const res = await api.register({
        email: email.trim(),
        password,
        displayName: displayName.trim(),
        isTrainer,
      });
      session.start(res.token, res.onboardingCompleted, res.user.role);
      // Hard navigation, not router.replace() — see the matching comment in
      // plan-selection/page.tsx.
      window.location.assign(res.user.role === "TRAINER" ? "/trainer" : "/onboarding");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : t("Something went wrong. Try again."));
      setLoading(false);
    }
  }

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-md flex-col justify-center px-6 py-12">
      <form onSubmit={handleSubmit} className="animate-fade-up" noValidate>
        <div className="flex items-center justify-between">
          <Wordmark className="text-2xl" />
          <LangToggle />
        </div>
        <h1 className="mt-8 font-display text-3xl font-bold tracking-tight">
          {t("Start training smarter")}
        </h1>
        <p className="mt-2 text-muted-foreground">
          {t("Two minutes of setup, then a plan that knows what you’re doing today.")}
        </p>

        <div className="mt-8 grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={() => setIsTrainer(false)}
            aria-pressed={!isTrainer}
            className={cn(
              "h-12 rounded-md border text-sm font-medium transition-colors",
              !isTrainer
                ? "border-primary bg-primary/10 text-foreground"
                : "border-border bg-card text-muted-foreground hover:bg-elevated",
            )}
          >
            {t("Train myself")}
          </button>
          <button
            type="button"
            onClick={() => setIsTrainer(true)}
            aria-pressed={isTrainer}
            className={cn(
              "h-12 rounded-md border text-sm font-medium transition-colors",
              isTrainer
                ? "border-primary bg-primary/10 text-foreground"
                : "border-border bg-card text-muted-foreground hover:bg-elevated",
            )}
          >
            {t("I'm a trainer")}
          </button>
        </div>

        <div className="mt-4 space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">{t("Name")}</Label>
            <Input
              id="name"
              type="text"
              placeholder={t("Your name")}
              autoComplete="name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">{t("Email")}</Label>
            <Input
              id="email"
              type="email"
              placeholder="you@example.com"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">{t("Password")}</Label>
            <Input
              id="password"
              type="password"
              placeholder={t("At least 8 characters")}
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
        </div>

        {error && (
          <p className="mt-4 text-sm text-destructive" role="alert">
            {error}
          </p>
        )}

        <Button type="submit" className="mt-8 w-full" size="lg" disabled={loading}>
          {loading ? t("Creating account…") : t("Create account")}
        </Button>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          {t("Already have an account?")}{" "}
          <Link href="/login" className="text-primary hover:underline">
            {t("Sign in")}
          </Link>
        </p>
      </form>

      <div className="mt-10 flex justify-center">
        <BackendStatus />
      </div>
    </main>
  );
}
