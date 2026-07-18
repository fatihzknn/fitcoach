"use client";

/**
 * Lightweight EN/TR i18n. English source strings are the keys; `t()` returns
 * the Turkish translation when lang === "tr", otherwise the key itself.
 * Unknown strings fall back to English — safe by default.
 *
 * Interpolation: t("Week of {date}", { date }) replaces {placeholders}
 * after translation, so translated strings keep their placeholders.
 */
import * as React from "react";
import { tr } from "@/lib/translations";

export type Lang = "en" | "tr";

const STORAGE_KEY = "fc_lang";

interface I18nContextValue {
  lang: Lang;
  setLang: (l: Lang) => void;
  t: (s: string, vars?: Record<string, string | number>) => string;
  locale: string; // for toLocaleDateString etc.
}

const I18nContext = React.createContext<I18nContextValue | null>(null);

function interpolate(s: string, vars?: Record<string, string | number>) {
  if (!vars) return s;
  return s.replace(/\{(\w+)\}/g, (m, k: string) =>
    Object.prototype.hasOwnProperty.call(vars, k) ? String(vars[k]) : m,
  );
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [lang, setLangState] = React.useState<Lang>("en");

  // Read persisted choice (or browser language) after mount to avoid hydration mismatch.
  React.useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    if (stored === "en" || stored === "tr") {
      setLangState(stored);
    } else if (navigator.language.toLowerCase().startsWith("tr")) {
      setLangState("tr");
    }
  }, []);

  const setLang = React.useCallback((l: Lang) => {
    setLangState(l);
    window.localStorage.setItem(STORAGE_KEY, l);
  }, []);

  const t = React.useCallback(
    (s: string, vars?: Record<string, string | number>) =>
      interpolate(lang === "tr" ? (tr[s] ?? s) : s, vars),
    [lang],
  );

  const value = React.useMemo(
    () => ({ lang, setLang, t, locale: lang === "tr" ? "tr-TR" : "en-US" }),
    [lang, setLang, t],
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nContextValue {
  const ctx = React.useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used within I18nProvider");
  return ctx;
}

/** Compact EN/TR switcher pill. */
export function LangToggle({ className }: { className?: string }) {
  const { lang, setLang } = useI18n();
  return (
    <div
      className={
        "inline-flex items-center rounded-full border border-border bg-card p-0.5 text-xs font-semibold " +
        (className ?? "")
      }
      role="group"
      aria-label="Language"
    >
      {(["en", "tr"] as const).map((l) => (
        <button
          key={l}
          type="button"
          onClick={() => setLang(l)}
          className={
            "rounded-full px-2.5 py-1 uppercase transition-colors " +
            (lang === l
              ? "bg-primary text-primary-foreground"
              : "text-muted-foreground hover:text-foreground")
          }
          aria-pressed={lang === l}
        >
          {l}
        </button>
      ))}
    </div>
  );
}
