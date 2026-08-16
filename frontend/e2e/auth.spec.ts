import { test, expect, formAlert } from "./support/fixtures";
import { uniqueEmail } from "./support/api";

test.describe("registration", () => {
  test("a new client registers and lands on onboarding", async ({ page, freshCreds }) => {
    await page.goto("/register");
    await page.getByLabel("Name").fill(freshCreds.displayName);
    await page.getByLabel("Email").fill(freshCreds.email);
    await page.getByLabel("Password").fill(freshCreds.password);
    await page.getByRole("button", { name: "Create account" }).click();
    await expect(page).toHaveURL(/\/onboarding/);
  });

  test("a new trainer registers and lands on the trainer dashboard", async ({ page, freshCreds }) => {
    await page.goto("/register");
    await page.getByRole("button", { name: "I'm a trainer" }).click();
    await page.getByLabel("Name").fill(freshCreds.displayName);
    await page.getByLabel("Email").fill(freshCreds.email);
    await page.getByLabel("Password").fill(freshCreds.password);
    await page.getByRole("button", { name: "Create account" }).click();
    await expect(page).toHaveURL(/\/trainer/);
  });

  test("rejects an empty form with an inline error", async ({ page }) => {
    await page.goto("/register");
    await page.getByRole("button", { name: "Create account" }).click();
    await expect(formAlert(page)).toHaveText(/Enter your name/);
  });

  test("rejects a too-short password", async ({ page, freshCreds }) => {
    await page.goto("/register");
    await page.getByLabel("Name").fill(freshCreds.displayName);
    await page.getByLabel("Email").fill(freshCreds.email);
    await page.getByLabel("Password").fill("short");
    await page.getByRole("button", { name: "Create account" }).click();
    await expect(formAlert(page)).toHaveText(/at least 8 characters/);
  });

  test("won't register the same email twice", async ({ page, readyClient }) => {
    await page.goto("/register");
    await page.getByLabel("Name").fill("Duplicate");
    await page.getByLabel("Email").fill(readyClient.email);
    await page.getByLabel("Password").fill("password123");
    await page.getByRole("button", { name: "Create account" }).click();
    await expect(formAlert(page)).toBeVisible();
    await expect(page).toHaveURL(/\/register/);
  });
});

test.describe("login", () => {
  test("signs a fully set-up client straight into today", async ({ page, readyClient }) => {
    await page.goto("/login");
    await page.getByLabel("Email").fill(readyClient.email);
    await page.getByLabel("Password").fill(readyClient.password);
    await page.getByRole("button", { name: "Sign in" }).click();
    await expect(page).toHaveURL(/\/today/);
  });

  test("shows an error for a wrong password", async ({ page, readyClient }) => {
    await page.goto("/login");
    await page.getByLabel("Email").fill(readyClient.email);
    await page.getByLabel("Password").fill("wrong-password");
    await page.getByRole("button", { name: "Sign in" }).click();
    await expect(formAlert(page)).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test("rejects an unregistered email", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Email").fill(uniqueEmail("nobody"));
    await page.getByLabel("Password").fill("password123");
    await page.getByRole("button", { name: "Sign in" }).click();
    await expect(formAlert(page)).toBeVisible();
  });

  test("requires both fields", async ({ page }) => {
    await page.goto("/login");
    await page.getByRole("button", { name: "Sign in" }).click();
    await expect(formAlert(page)).toHaveText(/Enter your email and password/);
  });
});

test.describe("unauthenticated access", () => {
  test("bounces an anonymous visitor from /today to /login", async ({ page }) => {
    await page.goto("/today");
    await expect(page).toHaveURL(/\/login/);
  });

  test("bounces an anonymous visitor from /trainer to /login", async ({ page }) => {
    await page.goto("/trainer");
    await expect(page).toHaveURL(/\/login/);
  });
});
