import { NextRequest, NextResponse } from "next/server";
import { AUTH_COOKIE, ONBOARDED_COOKIE, PLAN_COOKIE } from "@/lib/session";

/**
 * Route guard infrastructure.
 *
 * Rules:
 *   - /today requires signed-in + onboarded + plan selected.
 *   - /plan-selection requires signed-in + onboarded.
 *   - /onboarding requires signed-in.
 *   - / routes to the right place based on session state.
 *   - /login or /register while fully set up sends you to /today.
 */
export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const isAuthed = req.cookies.has(AUTH_COOKIE);
  const isOnboarded = req.cookies.has(ONBOARDED_COOKIE);
  const hasPlan = req.cookies.has(PLAN_COOKIE);

  const redirect = (path: string) => NextResponse.redirect(new URL(path, req.url));

  if (pathname === "/") {
    if (!isAuthed) return redirect("/login");
    if (!isOnboarded) return redirect("/onboarding");
    if (!hasPlan) return redirect("/plan-selection");
    return redirect("/today");
  }

  if (pathname.startsWith("/today")) {
    if (!isAuthed) return redirect("/login");
    if (!isOnboarded) return redirect("/onboarding");
    if (!hasPlan) return redirect("/plan-selection");
  }

  if (pathname.startsWith("/workout")) {
    if (!isAuthed) return redirect("/login");
    if (!isOnboarded) return redirect("/onboarding");
    if (!hasPlan) return redirect("/plan-selection");
  }

  if (pathname.startsWith("/progress") || pathname.startsWith("/check-in") || pathname.startsWith("/coach")) {
    if (!isAuthed) return redirect("/login");
    if (!isOnboarded) return redirect("/onboarding");
    if (!hasPlan) return redirect("/plan-selection");
  }

  if (pathname === "/plan-selection") {
    if (!isAuthed) return redirect("/login");
    if (!isOnboarded) return redirect("/onboarding");
  }

  if (pathname === "/login" || pathname === "/register") {
    if (isAuthed && isOnboarded && hasPlan) return redirect("/today");
  }

  if (pathname === "/onboarding" && !isAuthed) {
    return redirect("/login");
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/", "/today/:path*", "/workout/:path*", "/progress/:path*", "/progress", "/check-in", "/coach", "/login", "/register", "/onboarding", "/plan-selection"],
};
