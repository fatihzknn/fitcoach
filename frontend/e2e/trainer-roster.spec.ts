import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";
import { redeemInviteCode, getInviteCode } from "./support/api";

test.beforeEach(async ({ context, trainerAccount }) => {
  await seedSession(context, { token: trainerAccount.token, role: "TRAINER" });
});

test("shows an invite code and an empty roster for a new trainer", async ({ page }) => {
  await page.goto("/trainer");
  await expect(page.getByText("Your invite code", { exact: true })).toBeVisible({ timeout: 10_000 });
  await expect(page.locator("span.font-display.text-3xl")).toHaveText(/^[A-Z0-9]{8}$/, { timeout: 10_000 });
  await expect(page.getByText("No clients yet")).toBeVisible();
});

test("regenerating the invite code changes it", async ({ page }) => {
  await page.goto("/trainer");
  const codeLocator = page.locator("span.font-display.text-3xl");
  await expect(codeLocator).toHaveText(/^[A-Z0-9]{8}$/, { timeout: 10_000 });
  const before = await codeLocator.innerText();

  await page.getByRole("button", { name: "Regenerate code" }).click();
  await expect(async () => {
    const after = await codeLocator.innerText();
    expect(after).not.toBe(before);
  }).toPass({ timeout: 10_000 });
});

test("a linked client shows up in the roster with stats", async ({ page, request, trainerAccount, readyClient }) => {
  const { code } = await getInviteCode(request, trainerAccount.token);
  await redeemInviteCode(request, readyClient.token, code);

  await page.goto("/trainer");
  await expect(page.getByText(readyClient.displayName)).toBeVisible({ timeout: 10_000 });
  await expect(page.getByText("No clients yet")).not.toBeVisible();
});
