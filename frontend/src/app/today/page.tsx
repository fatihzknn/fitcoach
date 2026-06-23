"use client";

import * as React from "react";
import { Clock, Dumbbell, Target } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { BackendStatus } from "@/components/backend-status";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { api } from "@/lib/api";

/**
 * "What am I doing today?" — the product's home.
 *
 * Phase 2 personalizes the greeting from the authenticated user (/api/auth/me). The real
 * plan, Start Workout flow, and weekly summary are wired up in Phases 3-5.
 */
export default function TodayPage() {
  const [name, setName] = React.useState<string | null>(null);

  React.useEffect(() => {
    let active = true;
    api
      .me()
      .then((me) => active && setName(me.user.displayName))
      .catch(() => {
        /* greeting stays generic; route guard handles real auth failures */
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <AppShell>
      <section className="animate-fade-up">
        <p className="text-sm font-medium uppercase tracking-[0.18em] text-muted-foreground">
          Today
        </p>
        <h1 className="mt-1 font-display text-4xl font-extrabold leading-tight tracking-tight">
          {name ? `Hi, ${name}.` : "Good to see you."}
        </h1>
        <p className="mt-2 text-muted-foreground">
          Your plan appears here once generation is live.
        </p>

        <Card className="mt-6 overflow-hidden">
          <div className="h-1 w-full bg-primary/70" />
          <CardContent className="p-5">
            <p className="font-display text-2xl font-bold tracking-tight">Workout name</p>
            <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted-foreground">
              <span className="inline-flex items-center gap-2">
                <Clock className="h-4 w-4" /> ~60 min
              </span>
              <span className="inline-flex items-center gap-2">
                <Dumbbell className="h-4 w-4" /> - exercises
              </span>
              <span className="inline-flex items-center gap-2">
                <Target className="h-4 w-4" /> Target muscles
              </span>
            </div>

            <Button className="mt-6 w-full" size="lg" disabled>
              Start workout
            </Button>
            <p className="mt-2 text-center text-xs text-muted-foreground">
              The workout session flow unlocks in Phase 4.
            </p>
          </CardContent>
        </Card>

        <div className="mt-6">
          <p className="mb-3 text-sm font-medium text-muted-foreground">This week</p>
          <Card>
            <CardContent className="flex items-center justify-between p-5">
              <div>
                <p className="font-display text-3xl font-bold">0/0</p>
                <p className="text-xs text-muted-foreground">workouts completed</p>
              </div>
              <div className="text-right text-xs text-muted-foreground">
                Weekly summary
                <br />
                arrives in Phase 5
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="mt-8 flex justify-center">
          <BackendStatus />
        </div>
      </section>
    </AppShell>
  );
}
