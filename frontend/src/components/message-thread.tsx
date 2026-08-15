"use client";

import * as React from "react";
import { Send } from "lucide-react";
import { ApiError, type TrainerMessageDto } from "@/lib/api";
import { cn } from "@/lib/utils";
import { useI18n } from "@/lib/i18n";

const POLL_INTERVAL_MS = 6000;

function mergeById(prev: TrainerMessageDto[], incoming: TrainerMessageDto[]) {
  const seen = new Set(prev.map((m) => m.id));
  const fresh = incoming.filter((m) => !seen.has(m.id));
  return fresh.length ? [...prev, ...fresh] : prev;
}

function MessageBubble({ msg }: { msg: TrainerMessageDto }) {
  const isMine = msg.fromCurrentUser;
  return (
    <div className={cn("flex", isMine ? "justify-end" : "justify-start")}>
      <div
        className={cn(
          "max-w-[80%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap",
          isMine
            ? "rounded-br-sm bg-primary text-background"
            : "rounded-bl-sm bg-card text-foreground border border-border",
        )}
      >
        {msg.content}
      </div>
    </div>
  );
}

/**
 * A trainer/client message thread: history fetched on mount, then polled every
 * POLL_INTERVAL_MS (paused while the tab is hidden) since — unlike the AI coach
 * chat — a reply doesn't arrive synchronously in the same request. Shared by both
 * the trainer-side and client-side thread pages; only how history/send hit the
 * API differs between them.
 */
export function MessageThread({
  getHistory,
  sendMessage,
  emptyStateText,
}: {
  getHistory: () => Promise<TrainerMessageDto[]>;
  sendMessage: (content: string) => Promise<TrainerMessageDto>;
  emptyStateText: string;
}) {
  const { t } = useI18n();
  const [messages, setMessages] = React.useState<TrainerMessageDto[]>([]);
  const [input, setInput] = React.useState("");
  const [sending, setSending] = React.useState(false);
  const [loadingHistory, setLoadingHistory] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const bottomRef = React.useRef<HTMLDivElement>(null);
  const inputRef = React.useRef<HTMLTextAreaElement>(null);
  const requestIdRef = React.useRef(0);

  const loadHistory = React.useCallback(() => {
    const id = ++requestIdRef.current;
    return getHistory()
      .then((history) => {
        if (id !== requestIdRef.current) return; // superseded by a newer poll or a send
        setMessages((prev) => mergeById(prev, history));
      })
      .catch(() => { /* poll failure — next tick retries */ });
  }, [getHistory]);

  const loadHistoryRef = React.useRef(loadHistory);
  loadHistoryRef.current = loadHistory;

  // Initial load
  React.useEffect(() => {
    loadHistoryRef.current().finally(() => setLoadingHistory(false));
  }, []);

  // Poll while the tab is visible — the interval itself is stable so it's never
  // reset by re-renders; it always calls the latest loadHistory via the ref.
  React.useEffect(() => {
    const interval = setInterval(() => {
      if (document.visibilityState === "visible") void loadHistoryRef.current();
    }, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, []);

  React.useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function send(text: string) {
    const trimmed = text.trim();
    if (!trimmed || sending) return;
    setInput("");
    setError(null);
    setSending(true);
    requestIdRef.current++; // supersede any poll already in flight

    const optimistic: TrainerMessageDto = {
      id: `opt-${Date.now()}`,
      content: trimmed,
      createdAt: new Date().toISOString(),
      fromCurrentUser: true,
    };
    setMessages((prev) => [...prev, optimistic]);

    try {
      const saved = await sendMessage(trimmed);
      setMessages((prev) => mergeById(prev.filter((m) => m.id !== optimistic.id), [saved]));
    } catch (err) {
      setMessages((prev) => prev.filter((m) => m.id !== optimistic.id));
      setError(err instanceof ApiError ? err.message : t("Couldn't send. Try again."));
    } finally {
      setSending(false);
      inputRef.current?.focus();
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      void send(input);
    }
  }

  const isEmpty = !loadingHistory && messages.length === 0;

  return (
    <div className="flex flex-col" style={{ minHeight: "calc(100dvh - 260px)" }}>
      <div className="flex-1 space-y-3 overflow-y-auto pb-2">
        {loadingHistory && (
          <div className="space-y-3 animate-pulse">
            <div className="h-16 w-2/3 rounded-2xl bg-card" />
            <div className="flex justify-end">
              <div className="h-10 w-1/2 rounded-2xl bg-card" />
            </div>
          </div>
        )}

        {isEmpty && (
          <p className="pt-6 text-center text-sm text-muted-foreground">{emptyStateText}</p>
        )}

        {messages.map((msg) => (
          <MessageBubble key={msg.id} msg={msg} />
        ))}

        <div ref={bottomRef} />
      </div>

      {error && (
        <p className="mt-2 rounded-lg bg-destructive/10 border border-destructive/30 px-3 py-2 text-xs text-destructive">
          {error}
        </p>
      )}

      <div className="mt-3 flex gap-2 items-end rounded-2xl border border-border bg-card p-2">
        <textarea
          ref={inputRef}
          rows={1}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={t("Type a message…")}
          disabled={sending}
          className="flex-1 resize-none bg-transparent px-2 py-1.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none disabled:opacity-50"
          style={{ maxHeight: "120px", overflowY: "auto" }}
        />
        <button
          onClick={() => void send(input)}
          disabled={!input.trim() || sending}
          className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl bg-primary text-background transition-opacity disabled:opacity-40"
          aria-label="Send"
        >
          <Send className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
