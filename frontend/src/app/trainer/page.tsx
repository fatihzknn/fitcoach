"use client";

import * as React from "react";
import Link from "next/link";
import { Users, Copy, Check, RefreshCw, ChevronRight, Flame, Target } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { api, ApiError, type TrainerInviteDto, type ClientSummaryDto } from "@/lib/api";
import { useI18n } from "@/lib/i18n";

function InviteCodeCard() {
  const { t } = useI18n();
  const [invite, setInvite] = React.useState<TrainerInviteDto | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [regenerating, setRegenerating] = React.useState(false);
  const [copied, setCopied] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let active = true;
    api.getInviteCode()
      .then((data) => { if (active) setInvite(data); })
      .catch((err: unknown) => {
        if (active) setError(err instanceof ApiError ? err.message : t("Could not load invite code."));
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [t]);

  async function handleCopy() {
    if (!invite) return;
    await navigator.clipboard.writeText(invite.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  }

  async function handleRegenerate() {
    setRegenerating(true);
    setError(null);
    try {
      const data = await api.regenerateInviteCode();
      setInvite(data);
      setCopied(false);
    } catch (err: unknown) {
      setError(err instanceof ApiError ? err.message : t("Could not regenerate code."));
    } finally {
      setRegenerating(false);
    }
  }

  return (
    <Card>
      <CardContent className="p-5 space-y-3">
        <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
          {t("Your invite code")}
        </p>
        {loading ? (
          <div className="h-11 w-40 animate-pulse rounded-lg bg-elevated" />
        ) : invite ? (
          <>
            <div className="flex items-center gap-3">
              <span className="font-display text-3xl font-extrabold tracking-[0.15em]">
                {invite.code}
              </span>
              <button
                onClick={handleCopy}
                className="flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground hover:bg-secondary"
                aria-label={t("Copy code")}
              >
                {copied ? <Check className="h-4 w-4 text-primary" /> : <Copy className="h-4 w-4" />}
              </button>
            </div>
            <p className="text-sm text-muted-foreground">
              {t("Share this code with your clients. They enter it once to link their account to your panel.")}
            </p>
            <Button size="sm" variant="secondary" disabled={regenerating} onClick={handleRegenerate}>
              <RefreshCw className={`h-3.5 w-3.5 mr-1.5 ${regenerating ? "animate-spin" : ""}`} />
              {regenerating ? t("Regenerating…") : t("Regenerate code")}
            </Button>
          </>
        ) : null}
        {error && <p className="text-sm text-destructive">{error}</p>}
      </CardContent>
    </Card>
  );
}

function ClientRow({ client }: { client: ClientSummaryDto }) {
  const { t } = useI18n();
  return (
    <Link href={`/trainer/clients/${client.clientId}`}>
      <Card className="hover:bg-elevated transition-colors">
        <CardContent className="p-4 flex items-center gap-3">
          <div className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary font-display font-bold">
            {client.displayName.charAt(0).toUpperCase()}
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-semibold truncate">{client.displayName}</p>
            <p className="text-xs text-muted-foreground truncate">
              {client.activePlanName ?? t("No active plan")}
            </p>
          </div>
          <div className="flex items-center gap-3 text-xs text-muted-foreground flex-shrink-0">
            <span className="flex items-center gap-1">
              <Target className="h-3.5 w-3.5" />
              {client.adherenceRate4Weeks}%
            </span>
            <span className="flex items-center gap-1">
              <Flame className="h-3.5 w-3.5" />
              {client.currentStreakWeeks}w
            </span>
          </div>
          <ChevronRight className="h-4 w-4 text-muted-foreground flex-shrink-0" />
        </CardContent>
      </Card>
    </Link>
  );
}

export default function TrainerDashboardPage() {
  const { t } = useI18n();
  const [clients, setClients] = React.useState<ClientSummaryDto[] | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    let active = true;
    api.getTrainerClients()
      .then((data) => { if (active) setClients(data); })
      .catch((err: unknown) => {
        if (active) setError(err instanceof ApiError ? err.message : t("Could not load your clients."));
      });
    return () => { active = false; };
  }, [t]);

  return (
    <section className="animate-fade-up space-y-6">
      <div>
        <p className="text-sm font-medium uppercase tracking-[0.18em] text-muted-foreground">{t("Your")}</p>
        <h1 className="font-display text-4xl font-extrabold leading-tight tracking-tight">{t("Clients")}</h1>
      </div>

      {error && (
        <p className="rounded-lg bg-destructive/10 border border-destructive/30 px-4 py-3 text-sm text-destructive">{error}</p>
      )}

      <InviteCodeCard />

      {clients === null ? (
        <div className="space-y-3 animate-pulse">
          <div className="h-20 rounded-lg bg-card" />
          <div className="h-20 rounded-lg bg-card" />
        </div>
      ) : clients.length === 0 ? (
        <Card>
          <CardContent className="p-8 text-center space-y-3">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
              <Users className="h-6 w-6 text-primary" />
            </div>
            <p className="font-semibold">{t("No clients yet")}</p>
            <p className="text-sm text-muted-foreground">
              {t("Share your invite code above. Once a client enters it, they’ll show up here.")}
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-2">
          {clients.map((c) => (
            <ClientRow key={c.clientId} client={c} />
          ))}
        </div>
      )}
    </section>
  );
}
