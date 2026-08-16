import { test, expect, formAlert } from "./support/fixtures";
import { seedSession } from "./support/session";
import { getInviteCode } from "./support/api";

test("a client redeems a real invite code and sees the linked confirmation", async ({
  page,
  context,
  request,
  readyClient,
  trainerAccount,
}) => {
  const { code } = await getInviteCode(request, trainerAccount.token);
  await seedSession(context, {
    token: readyClient.token,
    role: "USER",
    onboarded: true,
    planSelected: true,
  });

  await page.goto("/link-trainer");
  await page.getByLabel("Invite code").fill(code);
  await page.getByRole("button", { name: "Link trainer" }).click();

  await expect(page.getByRole("heading", { name: /linked/i })).toBeVisible({ timeout: 10_000 });
  await page.getByRole("button", { name: "Continue" }).click();
  await expect(page).toHaveURL(/\/today/);
});

test("rejects an invalid invite code", async ({ page, context, readyClient }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });

  await page.goto("/link-trainer");
  await page.getByLabel("Invite code").fill("NOTREAL1");
  await page.getByRole("button", { name: "Link trainer" }).click();

  await expect(formAlert(page)).toBeVisible({ timeout: 10_000 });
});

test("requires a code before submitting", async ({ page, context, readyClient }) => {
  await seedSession(context, { token: readyClient.token, role: "USER", onboarded: true, planSelected: true });
  await page.goto("/link-trainer");
  await page.getByRole("button", { name: "Link trainer" }).click();
  await expect(formAlert(page)).toHaveText(/Enter your trainer/);
});

test("a trainer account cannot redeem an invite code", async ({ page, context, request, trainerAccount }) => {
  const otherTrainer = await getInviteCode(request, trainerAccount.token);
  await seedSession(context, { token: trainerAccount.token, role: "TRAINER" });
  await page.goto("/link-trainer");
  await page.getByLabel("Invite code").fill(otherTrainer.code);
  await page.getByRole("button", { name: "Link trainer" }).click();
  await expect(formAlert(page)).toBeVisible({ timeout: 10_000 });
});
