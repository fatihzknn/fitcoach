import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";

test.beforeEach(async ({ context, readyClient, page }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });
  await page.goto("/coach");
});

test("shows the welcome message and suggestion chips when history is empty", async ({ page }) => {
  await expect(page.getByText(/Hey! I'm your FitCoach AI/)).toBeVisible({ timeout: 10_000 });
  await expect(page.getByRole("button", { name: "How do I get started?" })).toBeVisible();
});

test("sending a message shows the user bubble and an assistant reply", async ({ page }) => {
  await page.getByPlaceholder("Ask your coach anything…").fill("How do I get started?");
  await page.getByRole("button", { name: "Send" }).click();

  await expect(page.getByText("How do I get started?").last()).toBeVisible();
  // The textarea is disabled only while sending (independent of its emptied
  // content) — its re-enabling means the assistant reply has landed.
  await expect(page.getByPlaceholder("Ask your coach anything…")).toBeEnabled({ timeout: 15_000 });
  const bubbleCount = await page.locator(".rounded-2xl.px-4.py-2\\.5").count();
  expect(bubbleCount).toBeGreaterThanOrEqual(2);
});

test("a suggestion chip sends its text directly", async ({ page }) => {
  await page.getByRole("button", { name: "Why am I not seeing progress?" }).click();
  await expect(page.getByText("Why am I not seeing progress?").last()).toBeVisible();
});
