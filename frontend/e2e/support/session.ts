import type { BrowserContext } from "@playwright/test";
import { FRONTEND_URL } from "./env";
import type { Role } from "./api";

/**
 * Seeds the fc_* cookies that frontend/src/lib/session.ts + middleware.ts read,
 * so tests can skip the UI login step and land directly on a route that
 * requires auth/onboarding/plan state.
 */
export async function seedSession(
  context: BrowserContext,
  opts: { token: string; onboarded?: boolean; planSelected?: boolean; role?: Role },
): Promise<void> {
  const url = new URL(FRONTEND_URL);
  const cookies = [
    { name: "fc_auth", value: opts.token, domain: url.hostname, path: "/" },
    { name: "fc_role", value: opts.role ?? "USER", domain: url.hostname, path: "/" },
  ];
  if (opts.onboarded) cookies.push({ name: "fc_onboarded", value: "1", domain: url.hostname, path: "/" });
  if (opts.planSelected) cookies.push({ name: "fc_plan", value: "1", domain: url.hostname, path: "/" });
  await context.addCookies(cookies);
}
