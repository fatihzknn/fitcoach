"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { CalendarDays, LineChart, MessageSquareText, LogOut } from "lucide-react";
import { Wordmark } from "@/components/wordmark";
import { session } from "@/lib/session";
import { useI18n, LangToggle } from "@/lib/i18n";
import { cn } from "@/lib/utils";

const NAV = [
  { href: "/today", label: "Today", icon: CalendarDays },
  { href: "/progress", label: "Progress", icon: LineChart },
  { href: "/coach", label: "Coach", icon: MessageSquareText },
] as const;

/**
 * The signed-in app frame: compact top bar + thumb-reachable bottom navigation.
 * Content is constrained to a phone-width column even on desktop (mobile-first product).
 *
 * Progress and Coach are placeholders in Phase 1 — the tabs exist so the shell is
 * complete, and the destinations are built in Phases 5 and 6.
 */
export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { t } = useI18n();

  function handleSignOut() {
    session.signOut();
    router.replace("/login");
  }

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-md flex-col">
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

      <main className="flex-1 px-4 pb-28 pt-5">{children}</main>

      <nav className="fixed inset-x-0 bottom-0 z-10 mx-auto max-w-md border-t border-border bg-background/90 backdrop-blur">
        <ul className="flex items-stretch">
          {NAV.map(({ href, label, icon: Icon }) => {
            const active = pathname.startsWith(href);
            return (
              <li key={href} className="flex-1">
                <Link
                  href={href}
                  className={cn(
                    "flex h-16 flex-col items-center justify-center gap-1 text-xs",
                    active ? "text-primary" : "text-muted-foreground hover:text-foreground",
                  )}
                  aria-current={active ? "page" : undefined}
                >
                  <Icon className="h-5 w-5" />
                  {t(label)}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
    </div>
  );
}
