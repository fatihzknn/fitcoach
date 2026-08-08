"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { LogOut } from "lucide-react";
import { Wordmark } from "@/components/wordmark";
import { session } from "@/lib/session";
import { LangToggle } from "@/lib/i18n";

/**
 * Trainer panel frame: top bar + sign-out, no bottom nav. A trainer account
 * is panel-only (no Today/Progress/Coach tabs — those are client concepts).
 */
export function TrainerShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();

  function handleSignOut() {
    session.signOut();
    router.replace("/login");
  }

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-2xl flex-col">
      <header className="sticky top-0 z-10 flex items-center justify-between border-b border-border bg-background/80 px-4 py-3 backdrop-blur">
        <Wordmark />
        <div className="flex items-center gap-2">
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
