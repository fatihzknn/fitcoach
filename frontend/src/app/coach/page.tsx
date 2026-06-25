"use client";

import * as React from "react";
import { Send, Bot } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { api, ApiError, type ChatMessageDto } from "@/lib/api";
import { cn } from "@/lib/utils";

// ─── Message bubble ───────────────────────────────────────────────────────────

function MessageBubble({ msg }: { msg: ChatMessageDto }) {
  const isUser = msg.role === "USER";
  return (
    <div className={cn("flex gap-2", isUser ? "justify-end" : "justify-start")}>
      {!isUser && (
        <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-primary/15 mt-0.5">
          <Bot className="h-4 w-4 text-primary" />
        </div>
      )}
      <div
        className={cn(
          "max-w-[80%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap",
          isUser
            ? "rounded-br-sm bg-primary text-background"
            : "rounded-bl-sm bg-card text-foreground border border-border",
        )}
      >
        {msg.content}
      </div>
    </div>
  );
}

// ─── Suggested prompts (shown when history is empty) ─────────────────────────

const SUGGESTIONS = [
  "How do I get started?",
  "My knee hurts during squats — what should I do?",
  "Why am I not seeing progress?",
  "How important is sleep for muscle gain?",
  "I keep skipping the gym. Help.",
];

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function CoachPage() {
  const [messages, setMessages] = React.useState<ChatMessageDto[]>([]);
  const [input, setInput] = React.useState("");
  const [sending, setSending] = React.useState(false);
  const [loadingHistory, setLoadingHistory] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const bottomRef = React.useRef<HTMLDivElement>(null);
  const inputRef = React.useRef<HTMLTextAreaElement>(null);

  // Load history on mount
  React.useEffect(() => {
    api.getCoachHistory()
      .then(setMessages)
      .catch(() => { /* empty history is fine */ })
      .finally(() => setLoadingHistory(false));
  }, []);

  // Scroll to bottom whenever messages change
  React.useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function send(text: string) {
    const trimmed = text.trim();
    if (!trimmed || sending) return;
    setInput("");
    setError(null);
    setSending(true);

    // Optimistic user bubble
    const optimisticUser: ChatMessageDto = {
      id: `opt-${Date.now()}`,
      role: "USER",
      content: trimmed,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, optimisticUser]);

    try {
      const newMsgs = await api.coachChat(trimmed);
      // Replace optimistic + add real pair
      setMessages((prev) => [
        ...prev.filter((m) => m.id !== optimisticUser.id),
        ...newMsgs,
      ]);
    } catch (err) {
      // Remove optimistic message on failure
      setMessages((prev) => prev.filter((m) => m.id !== optimisticUser.id));
      setError(err instanceof ApiError ? err.message : "Couldn't send. Try again.");
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
    <AppShell>
      {/* Header */}
      <div className="mb-4">
        <p className="text-sm font-medium uppercase tracking-[0.18em] text-muted-foreground">
          Your
        </p>
        <h1 className="font-display text-4xl font-extrabold leading-tight tracking-tight">
          Coach
        </h1>
      </div>

      {/* Chat area — takes remaining vertical space */}
      <div className="flex flex-col" style={{ minHeight: "calc(100dvh - 260px)" }}>

        {/* Messages */}
        <div className="flex-1 space-y-3 overflow-y-auto pb-2">
          {loadingHistory && (
            <div className="space-y-3 animate-pulse">
              <div className="flex gap-2">
                <div className="h-8 w-8 rounded-full bg-card flex-shrink-0" />
                <div className="h-16 w-2/3 rounded-2xl bg-card" />
              </div>
              <div className="flex justify-end">
                <div className="h-10 w-1/2 rounded-2xl bg-card" />
              </div>
            </div>
          )}

          {isEmpty && (
            <div className="space-y-4 pt-2">
              {/* Welcome card */}
              <div className="flex gap-3">
                <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full bg-primary/15 mt-0.5">
                  <Bot className="h-5 w-5 text-primary" />
                </div>
                <div className="rounded-2xl rounded-bl-sm border border-border bg-card px-4 py-3 text-sm leading-relaxed max-w-[80%]">
                  Hey! I&apos;m your FitCoach AI. Ask me anything about your
                  training, recovery, technique, or staying consistent.
                </div>
              </div>

              {/* Suggestion chips */}
              <div className="space-y-2">
                <p className="text-xs text-muted-foreground px-1">Try asking:</p>
                <div className="flex flex-wrap gap-2">
                  {SUGGESTIONS.map((s) => (
                    <button
                      key={s}
                      onClick={() => void send(s)}
                      className="rounded-full border border-border bg-card px-3 py-1.5 text-xs text-muted-foreground hover:border-primary/60 hover:text-foreground transition-colors"
                    >
                      {s}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {messages.map((msg) => (
            <MessageBubble key={msg.id} msg={msg} />
          ))}

          {sending && (
            <div className="flex gap-2">
              <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-primary/15 mt-0.5">
                <Bot className="h-4 w-4 text-primary" />
              </div>
              <div className="rounded-2xl rounded-bl-sm border border-border bg-card px-4 py-3">
                <div className="flex gap-1 items-center h-4">
                  <span className="h-1.5 w-1.5 rounded-full bg-muted-foreground animate-bounce [animation-delay:0ms]" />
                  <span className="h-1.5 w-1.5 rounded-full bg-muted-foreground animate-bounce [animation-delay:150ms]" />
                  <span className="h-1.5 w-1.5 rounded-full bg-muted-foreground animate-bounce [animation-delay:300ms]" />
                </div>
              </div>
            </div>
          )}

          <div ref={bottomRef} />
        </div>

        {/* Error */}
        {error && (
          <p className="mt-2 rounded-lg bg-destructive/10 border border-destructive/30 px-3 py-2 text-xs text-destructive">
            {error}
          </p>
        )}

        {/* Input bar */}
        <div className="mt-3 flex gap-2 items-end rounded-2xl border border-border bg-card p-2">
          <textarea
            ref={inputRef}
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask your coach anything…"
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

        {/* Safety disclaimer */}
        <p className="mt-2 text-center text-[10px] text-muted-foreground/60 leading-relaxed">
          FitCoach AI cannot diagnose injuries or provide medical advice.
          If you are in pain, consult a healthcare professional.
        </p>
      </div>
    </AppShell>
  );
}
