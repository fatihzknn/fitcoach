"use client";

import * as React from "react";
import { api } from "@/lib/api";
import { cn } from "@/lib/utils";

type Status = "checking" | "online" | "offline";

/**
 * Live backend connectivity indicator. Calls GET /api/health and reflects the result.
 * Doubles as the Phase 1 frontend-to-backend connectivity test surfaced in the UI.
 */
export function BackendStatus({ className }: { className?: string }) {
  const [status, setStatus] = React.useState<Status>("checking");
  const [version, setVersion] = React.useState<string | null>(null);

  React.useEffect(() => {
    let active = true;
    api
      .health()
      .then((res) => {
        if (!active) return;
        setStatus(res.status === "UP" ? "online" : "offline");
        setVersion(res.version);
      })
      .catch(() => active && setStatus("offline"));
    return () => {
      active = false;
    };
  }, []);

  const label =
    status === "checking"
      ? "Checking backend…"
      : status === "online"
        ? `Backend online${version ? ` · v${version}` : ""}`
        : "Backend offline";

  const dot =
    status === "online"
      ? "bg-primary"
      : status === "offline"
        ? "bg-destructive"
        : "bg-muted-foreground animate-pulse";

  return (
    <div className={cn("flex items-center gap-2 text-xs text-muted-foreground", className)}>
      <span className={cn("h-2 w-2 rounded-full", dot)} aria-hidden />
      <span>{label}</span>
    </div>
  );
}
