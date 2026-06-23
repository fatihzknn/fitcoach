import { redirect } from "next/navigation";

/**
 * Middleware normally redirects "/" before this renders. This server redirect is a
 * safety net for any path that reaches the page (e.g. middleware disabled in tests).
 */
export default function RootPage() {
  redirect("/today");
}
