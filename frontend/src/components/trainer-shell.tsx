"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Dumbbell, LogOut } from "lucide-react";
import { Wordmark } from "@/components/wordmark";
import { session } from "@/lib/session";
import { LangToggle, useI18n } from "@/lib/i18n";

/**
 * Trainer panel frame: top bar with a "My Program" link (self-tracking is
 * opt-in, reached via /today) + sign-out. No bottom nav — the panel itself
 * stays single-purpose; self-tracking, if used, lives entirely in the
 * client-facing AppShell, not duplicated here.
 */
export function TrainerShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { t } = useI18n();

  function handleSignOut() {
    session.signOut();
    router.replace("/login");
  }

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-2xl flex-col">
      <header className="sticky top-0 z-10 flex items-center justify-between border-b border-border bg-background/80 px-4 py-3 backdrop-blur">
        <Wordmark />
        <div className="flex items-center gap-2">
          <Link
            href="/today"
            // Prefetching this link (fired as soon as the panel renders,
            // before self-tracking onboarding has necessarily happened)
            // caches middleware's redirect decision for /today in Next's
            // router cache — e.g. "not onboarded yet -> /onboarding". A
            // trainer who then finishes onboarding+plan-selection and gets
            // client-navigated to /today lands back on that stale cached
            // redirect instead of the fresh, now-correct one. Unlike every
            // other in-app link, this one points at a route whose guard
            // depends on state that's expected to change mid-session, so
            // prefetching it is actively wrong.
            prefetch={false}
            className="flex h-10 w-10 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
            aria-label={t("My Program")}
          >
            <Dumbbell className="h-5 w-5" />
          </Link>
          <LangToggle />
          <button
            onClick={handleSignOut}
            className="flex h-10 w-10 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
            aria-label="Sign out"
          >
            <LogOut className="h-5 w-5" />
          </button>
        </div>
      </header>

      <main className="flex-1 px-4 pb-10 pt-5">{children}</main>
    </div>
  );
}
