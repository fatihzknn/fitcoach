import { test as base, expect, type Page } from "@playwright/test";
import { buildReadyClient, buildReadyTrainer, buildTrainer, uniqueEmail } from "./api";
import { seedSession } from "./session";

/**
 * Next.js's route announcer (#__next-route-announcer__) also carries
 * role="alert", so a bare page.getByRole("alert") matches two elements on
 * every page. Every inline form error in this app renders as <p role="alert">
 * — scope to that instead.
 */
export function formAlert(page: Page) {
  return page.locator('p[role="alert"]');
}

interface Persona {
  token: string;
  email: string;
  password: string;
  displayName: string;
  userId: string;
}

export const test = base.extend<{
  readyClient: Persona;
  trainerAccount: Persona;
  readyTrainer: Persona;
  freshCreds: { email: string; password: string; displayName: string };
}>({
  // A client that is registered, onboarded, and has a plan selected — for tests
  // that exercise screens beyond onboarding/plan-selection themselves.
  readyClient: async ({ request }, use) => {
    const persona = await buildReadyClient(request);
    await use(persona);
  },

  // A trainer account with no clients yet, and no self-tracking set up either
  // (panel-only, the default).
  trainerAccount: async ({ request }, use) => {
    const persona = await buildTrainer(request);
    await use(persona);
  },

  // A trainer who has also onboarded and selected their own plan — self-
  // tracking ("Kendi Programım"), for tests exercising screens beyond
  // onboarding/plan-selection themselves.
  readyTrainer: async ({ request }, use) => {
    const persona = await buildReadyTrainer(request);
    await use(persona);
  },

  // Fresh, never-registered credentials for tests that drive registration
  // through the UI itself.
  freshCreds: async ({}, use) => {
    await use({
      email: uniqueEmail("ui"),
      password: "password123",
      displayName: "UI Test User",
    });
  },
});

export { expect, seedSession };
