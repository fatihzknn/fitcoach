/**
 * Client-side session state used for routing.
 *
 *   - fc_auth       — the JWT (its presence means "signed in")
 *   - fc_onboarded  — "1" once onboarding is complete
 *   - fc_role       — "USER" or "TRAINER", read by middleware to route trainer
 *                     accounts to /trainer instead of the client flows
 *
 * The JWT is the source of truth for the API (sent as a Bearer header). The onboarding
 * flag is a routing convenience derived from the backend (login/register/me responses);
 * the backend FitnessProfile remains the real source of truth.
 *
 * Cookies (not localStorage) so Next.js middleware can read them for route guards.
 */

export const AUTH_COOKIE = "fc_auth";
export const ONBOARDED_COOKIE = "fc_onboarded";
export const PLAN_COOKIE = "fc_plan";
export const ROLE_COOKIE = "fc_role";

export type Role = "USER" | "TRAINER";

const ONE_WEEK_SECONDS = 60 * 60 * 24 * 7;

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie
    .split("; ")
    .find((row) => row.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.split("=").slice(1).join("=")) : null;
}

function writeCookie(name: string, value: string) {
  if (typeof document === "undefined") return;
  document.cookie = `${name}=${encodeURIComponent(value)}; path=/; max-age=${ONE_WEEK_SECONDS}; samesite=lax`;
}

function clearCookie(name: string) {
  if (typeof document === "undefined") return;
  document.cookie = `${name}=; path=/; max-age=0; samesite=lax`;
}

export const session = {
  token: () => readCookie(AUTH_COOKIE),
  isAuthenticated: () => readCookie(AUTH_COOKIE) !== null,
  hasCompletedOnboarding: () => readCookie(ONBOARDED_COOKIE) !== null,

  /** Persist a signed-in session. Pass whether the user has finished onboarding and their role. */
  start: (token: string, onboarded: boolean, role: Role) => {
    writeCookie(AUTH_COOKIE, token);
    if (onboarded) writeCookie(ONBOARDED_COOKIE, "1");
    else clearCookie(ONBOARDED_COOKIE);
    writeCookie(ROLE_COOKIE, role);
  },

  setOnboarded: () => writeCookie(ONBOARDED_COOKIE, "1"),

  hasPlanSelected: () => readCookie(PLAN_COOKIE) !== null,
  setPlanSelected: () => writeCookie(PLAN_COOKIE, "1"),

  // Missing cookie (session predates this field) is treated as USER — matches
  // the backend JWT's own graceful default for tokens minted before roles existed.
  role: (): Role => (readCookie(ROLE_COOKIE) === "TRAINER" ? "TRAINER" : "USER"),
  isTrainer: () => readCookie(ROLE_COOKIE) === "TRAINER",

  signOut: () => {
    clearCookie(AUTH_COOKIE);
    clearCookie(ONBOARDED_COOKIE);
    clearCookie(PLAN_COOKIE);
    clearCookie(ROLE_COOKIE);
  },
};
