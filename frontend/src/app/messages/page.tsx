"use client";

import * as React from "react";
import Link from "next/link";
import { ChevronRight, Link2 } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { Card, CardContent } from "@/components/ui/card";
import { api, ApiError, type TrainerConnectionSummaryDto } from "@/lib/api";
import { useI18n } from "@/lib/i18n";

export default function MessagesPage() {
  const { t } = useI18n();
  const [trainers, setTrainers] = React.useState<TrainerConnectionSummaryDto[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    api.getMyTrainers()
      .then(setTrainers)
      .catch((err: unknown) => {
        setError(err instanceof ApiError ? err.message : t("Could not load your trainers."));
      })
      .finally(() => setLoading(false));
  }, [t]);

  return (
    <AppShell>
      <div className="mb-4">
        <p className="text-sm font-medium uppercase tracking-[0.18em] text-muted-foreground">
          {t("Your")}
        </p>
        <h1 className="font-display text-4xl font-extrabold leading-tight tracking-tight">
          {t("Messages")}
        </h1>
      </div>

      {error && (
        <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">{error}</p>
      )}

      {loading && (
        <div className="space-y-3 animate-pulse">
          <div className="h-16 rounded-2xl bg-card" />
          <div className="h-16 rounded-2xl bg-card" />
        </div>
      )}

      {!loading && trainers.length === 0 && !error && (
        <Card>
          <CardContent className="p-5 space-y-3 text-center">
            <p className="text-sm text-muted-foreground">
              {t("You're not linked to a trainer yet. Link one to start messaging.")}
            </p>
            <Link
              href="/link-trainer"
              className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline"
            >
              <Link2 className="h-4 w-4" />
              {t("Link a trainer")}
            </Link>
          </CardContent>
        </Card>
      )}

      {!loading && trainers.length > 0 && (
        <div className="space-y-2">
          {trainers.map((trainer) => (
            <Link key={trainer.trainerId} href={`/messages/${trainer.trainerId}`}>
              <Card className="hover:border-primary/40 transition-colors">
                <CardContent className="p-4 flex items-center justify-between gap-2">
                  <div>
                    <p className="font-semibold">{trainer.displayName}</p>
                    <p className="text-xs text-muted-foreground">{trainer.email}</p>
                  </div>
                  <ChevronRight className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </AppShell>
  );
}
