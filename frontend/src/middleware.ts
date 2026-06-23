import { NextRequest, NextResponse } from "next/server";
import { AUTH_COOKIE, ONBOARDED_COOKIE } from "@/lib/session";

/**
 * Route guard infrastructure.
 *
 * Rules:
 *   - /today requires a signed-in user who has completed onboarding.
 *       not signed in        -> /login
 *       signed in, no profile -> /onboarding
 *   - Visiting /login or /register while fully set up sends you to /today.
 *   - "/" routes to the right place based on session state.
 *
 * In Phase 1 the cookies are mock values (see lib/session.ts). Phase 2 swaps the
 * presence checks for real token verification without changing these rules.
 */
export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;
  const isAuthed = req.cookies.has(AUTH_COOKIE);
  const isOnboarded = req.cookies.has(ONBOARDED_COOKIE);

  const redirect = (path: string) => NextResponse.redirect(new URL(path, req.url));

  if (pathname === "/") {
    if (!isAuthed) return redirect("/login");
    if (!isOnboarded) return redirect("/onboarding");
    return redirect("/today");
  }

  if (pathname.startsWith("/today")) {
    if (!isAuthed) return redirect("/login");
    if (!isOnboarded) return redirect("/onboarding");
  }

  if (pathname === "/login" || pathname === "/register") {
    if (isAuthed && isOnboarded) return redirect("/today");
  }

  if (pathname === "/onboarding" && !isAuthed) {
    return redirect("/login");
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/", "/today/:path*", "/login", "/register", "/onboarding"],
};
