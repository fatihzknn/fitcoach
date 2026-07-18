"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Wordmark } from "@/components/wordmark";
import { BackendStatus } from "@/components/backend-status";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, ApiError } from "@/lib/api";
import { session } from "@/lib/session";
import { useI18n, LangToggle } from "@/lib/i18n";

export default function RegisterPage() {
  const router = useRouter();
  const { t } = useI18n();
  const [displayName, setDisplayName] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
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
      });
      session.start(res.token, res.onboardingCompleted);
      router.replace("/onboarding");
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

        <div className="mt-8 space-y-4">
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
