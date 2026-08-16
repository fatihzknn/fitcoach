import { test, expect } from "./support/fixtures";
import { seedSession } from "./support/session";
import { getInviteCode, redeemInviteCode } from "./support/api";

test("client and trainer exchange messages after linking", async ({
  browser,
  request,
  readyClient,
  trainerAccount,
}) => {
  const { code } = await getInviteCode(request, trainerAccount.token);
  await redeemInviteCode(request, readyClient.token, code);

  const clientContext = await browser.newContext();
  const trainerContext = await browser.newContext();
  const clientPage = await clientContext.newPage();
  const trainerPage = await trainerContext.newPage();

  await seedSession(clientContext, {
    token: readyClient.token,
    role: "USER",
    onboarded: true,
    planSelected: true,
  });
  await seedSession(trainerContext, { token: trainerAccount.token, role: "TRAINER" });

  // Client sends the opener from /messages/[trainerId]
  await clientPage.goto("/messages");
  await clientPage.getByText(trainerAccount.displayName).click();
  await expect(clientPage).toHaveURL(/\/messages\/.+/);
  await clientPage.getByPlaceholder("Type a message…").fill("Hi coach, ready for today's session!");
  await clientPage.getByRole("button", { name: "Send" }).click();
  await expect(clientPage.getByText("Hi coach, ready for today's session!")).toBeVisible();

  // Trainer opens the client's thread and sees the message, then replies
  await trainerPage.goto("/trainer");
  await trainerPage.getByText(readyClient.displayName).click();
  await expect(trainerPage).toHaveURL(/\/trainer\/clients\/.+/);
  await trainerPage.getByRole("button", { name: "Messages" }).click();
  await expect(trainerPage).toHaveURL(/\/trainer\/clients\/.+\/messages/);
  await expect(trainerPage.getByText("Hi coach, ready for today's session!")).toBeVisible({ timeout: 10_000 });

  await trainerPage.getByPlaceholder("Type a message…").fill("Let's go! Focus on form today.");
  await trainerPage.getByRole("button", { name: "Send" }).click();
  await expect(trainerPage.getByText("Let's go! Focus on form today.")).toBeVisible();

  // Client sees the trainer's reply (thread polls every 6s)
  await expect(clientPage.getByText("Let's go! Focus on form today.")).toBeVisible({ timeout: 15_000 });

  await clientContext.close();
  await trainerContext.close();
});
