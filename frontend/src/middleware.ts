import { NextRequest, NextResponse } from "next/server";
import { AUTH_COOKIE, ONBOARDED_COOKIE, PLAN_COOKIE, ROLE_COOKIE } from "@/lib/session";

/**
 * Route guard infrastructure.
 *
 * Rules:
 *   - /today (and the other client screens) require signed-in + onboarded + plan selected.
 *   - /plan-selection requires signed-in + onboarded.
 *   - /onboarding requires signed-in.
 *   - /link-trainer requires signed-in only — connecting to a trainer doesn't
 *     depend on onboarding progress.
 *   - TRAINER accounts are panel-only: any client route redirects to /trainer,
 *     and a USER account visiting /trainer is bounced back to their own home.
 *   - / routes to the right place based on session state.
 *   - /login or /register while fully set up sends you to your home (/trainer
 *     or /today, depending on role).
 *
 * A missing fc_role cookie (session predates this field) is treated as USER —
 * matches the backend JWT's own graceful default for tokens minted before
 * roles existed.
 */

const CLIENT_ROUTES = [
  "/today",
  "/onboarding",
  "/plan-selection",
  "/workout",
  "/progress",
  "/check-in",
  "/coach",
  "/measurements",
];

export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const isAuthed = req.cookies.has(AUTH_COOKIE);
  const isOnboarded = req.cookies.has(ONBOARDED_COOKIE);
  const hasPlan = req.cookies.has(PLAN_COOKIE);
  const isTrainer = req.cookies.get(ROLE_COOKIE)?.value === "TRAINER";

  const redirect = (path: string) => NextResponse.redirect(new URL(path, req.url));

  const resolveHome = () => {
    if (!isAuthed) return "/login";
    if (isTrainer) return "/trainer";
    if (!isOnboarded) return "/onboarding";
    if (!hasPlan) return "/plan-selection";
    return "/today";
  };

  if (pathname === "/") {
    return redirect(resolveHome());
  }

  // Trainer accounts never use the client flows.
  if (isAuthed && isTrainer && CLIENT_ROUTES.some((p) => pathname.startsWith(p))) {
    return redirect("/trainer");
  }

  // Client accounts never use the trainer panel.
  if (isAuthed && !isTrainer && pathname.startsWith("/trainer")) {
    return redirect(resolveHome());
  }

  if (
    pathname.startsWith("/today") ||
    pathname.startsWith("/workout") ||
    pathname.startsWith("/progress") ||
    pathname.startsWith("/check-in") ||
    pathname.startsWith("/coach") ||
    pathname.startsWith("/measurements")
  ) {
    if (!isAuthed) return redirect("/login");
    if (!isOnboarded) return redirect("/onboarding");
    if (!hasPlan) return redirect("/plan-selection");
  }

  if (pathname === "/plan-selection") {
    if (!isAuthed) return redirect("/login");
    if (!isOnboarded) return redirect("/onboarding");
  }

  if (pathname === "/trainer" || pathname.startsWith("/trainer/")) {
    if (!isAuthed) return redirect("/login");
    if (!isTrainer) return redirect(resolveHome());
  }

  if (pathname === "/link-trainer" && !isAuthed) {
    return redirect("/login");
  }

  if (pathname === "/login" || pathname === "/register") {
    if (isAuthed && (isTrainer || (isOnboarded && hasPlan))) return redirect(resolveHome());
  }

  if (pathname === "/onboarding" && !isAuthed) {
    return redirect("/login");
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/",
    "/today/:path*",
    "/workout/:path*",
    "/progress/:path*",
    "/progress",
    "/check-in",
    "/coach",
    "/measurements",
    "/login",
    "/register",
    "/onboarding",
    "/plan-selection",
    "/trainer",
    "/trainer/:path*",
    "/link-trainer",
  ],
};
