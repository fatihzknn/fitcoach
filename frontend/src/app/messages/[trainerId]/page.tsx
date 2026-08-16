"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { MessageThread } from "@/components/message-thread";
import { api } from "@/lib/api";
import { session } from "@/lib/session";
import { useI18n } from "@/lib/i18n";

export default function TrainerMessageThreadPage() {
  const params = useParams<{ trainerId: string }>();
  const router = useRouter();
  const { t } = useI18n();
  const trainerId = params.trainerId;

  // A trainer has no "my trainers" of their own — the API rejects this with
  // 403. Bounce straight to /today rather than surface a broken/confusing page.
  React.useEffect(() => {
    if (session.isTrainer()) {
      router.replace("/today");
    }
  }, [router]);

  const getHistory = React.useCallback(() => api.getMessagesWithTrainer(trainerId), [trainerId]);
  const sendMessage = React.useCallback(
    (content: string) => api.sendMessageToTrainer(trainerId, content),
    [trainerId],
  );

  return (
    <AppShell>
      <section className="animate-fade-up space-y-4">
        <button
          onClick={() => router.push("/messages")}
          className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4" />
          {t("Messages")}
        </button>

        <MessageThread
          getHistory={getHistory}
          sendMessage={sendMessage}
          emptyStateText={t("No messages yet. Say hello!")}
        />
      </section>
    </AppShell>
  );
}
