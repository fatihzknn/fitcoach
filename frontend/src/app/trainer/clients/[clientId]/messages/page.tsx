"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { TrainerShell } from "@/components/trainer-shell";
import { MessageThread } from "@/components/message-thread";
import { api } from "@/lib/api";
import { useI18n } from "@/lib/i18n";

export default function TrainerClientMessagesPage() {
  const params = useParams<{ clientId: string }>();
  const router = useRouter();
  const { t } = useI18n();
  const clientId = params.clientId;

  const getHistory = React.useCallback(() => api.getTrainerClientMessages(clientId), [clientId]);
  const sendMessage = React.useCallback(
    (content: string) => api.sendTrainerClientMessage(clientId, content),
    [clientId],
  );

  return (
    <TrainerShell>
      <section className="animate-fade-up space-y-4">
        <button
          onClick={() => router.push(`/trainer/clients/${clientId}`)}
          className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" />
          {t("Back")}
        </button>

        <h1 className="font-display text-3xl font-extrabold leading-tight tracking-tight">
          {t("Messages")}
        </h1>

        <MessageThread
          getHistory={getHistory}
          sendMessage={sendMessage}
          emptyStateText={t("No messages yet. Say hello!")}
        />
      </section>
    </TrainerShell>
  );
}
