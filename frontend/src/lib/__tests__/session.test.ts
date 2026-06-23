import { afterEach, describe, expect, it } from "vitest";
import { session } from "@/lib/session";

afterEach(() => session.signOut());

describe("session", () => {
  it("starts signed out and not onboarded", () => {
    expect(session.isAuthenticated()).toBe(false);
    expect(session.hasCompletedOnboarding()).toBe(false);
    expect(session.token()).toBeNull();
  });

  it("start() stores the token and onboarding flag", () => {
    session.start("jwt-abc", true);
    expect(session.isAuthenticated()).toBe(true);
    expect(session.token()).toBe("jwt-abc");
    expect(session.hasCompletedOnboarding()).toBe(true);
  });

  it("start() with onboarded=false leaves onboarding incomplete", () => {
    session.start("jwt-abc", false);
    expect(session.isAuthenticated()).toBe(true);
    expect(session.hasCompletedOnboarding()).toBe(false);
  });

  it("setOnboarded() flips the flag", () => {
    session.start("jwt-abc", false);
    session.setOnboarded();
    expect(session.hasCompletedOnboarding()).toBe(true);
  });

  it("signOut() clears everything", () => {
    session.start("jwt-abc", true);
    session.signOut();
    expect(session.isAuthenticated()).toBe(false);
    expect(session.hasCompletedOnboarding()).toBe(false);
  });
});
