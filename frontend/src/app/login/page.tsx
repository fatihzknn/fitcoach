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

export default function LoginPage() {
  const { t } = useI18n();
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!email.trim() || !password) {
      setError(t("Enter your email and password."));
      return;
    }

    setLoading(true);
    try {
      const res = await api.login({ email: email.trim(), password });
      session.start(res.token, res.onboardingCompleted, res.user.role);

      // Hard navigations (not router.replace()) below — see the matching
      // comment in plan-selection/page.tsx. Next's client router cache keys
      // purely by URL and can't know these cookies just changed, so a
      // client-side transition risks resolving a stale redirect cached
      // earlier in the browser (e.g. a previous, differently-authed session).

      // A trainer's default landing is always the panel, regardless of their
      // own onboarding progress — self-tracking (if any) is resumed via the
      // panel's "My Program" entry point, never automatically on login.
      if (res.user.role === "TRAINER") {
        window.location.assign("/trainer");
        return;
      }

      if (!res.onboardingCompleted) {
        window.location.assign("/onboarding");
        return;
      }

      // Restore the plan cookie for returning users so they skip plan-selection.
      try {
        await api.getActivePlan();
        session.setPlanSelected();
        window.location.assign("/today");
      } catch {
        window.location.assign("/plan-selection");
      }
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
        <h1 className="mt-8 font-display text-3xl font-bold tracking-tight">{t("Welcome back")}</h1>
        <p className="mt-2 text-muted-foreground">{t("Pick up exactly where your plan left off.")}</p>

        <div className="mt-8 space-y-4">
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
              placeholder="••••••••"
              autoComplete="current-password"
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
          {loading ? t("Signing in…") : t("Sign in")}
        </Button>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          {t("New here?")}{" "}
          <Link href="/register" className="text-primary hover:underline">
            {t("Create an account")}
          </Link>
        </p>
      </form>

      <div className="mt-10 flex justify-center">
        <BackendStatus />
      </div>
    </main>
  );
}
