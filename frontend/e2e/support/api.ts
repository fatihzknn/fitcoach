import type { APIRequestContext } from "@playwright/test";
import { BACKEND_URL } from "./env";

export type Role = "USER" | "TRAINER";

export interface AuthResponse {
  token: string;
  user: { id: string; email: string; displayName: string; role: Role };
  onboardingCompleted: boolean;
}

async function json<T>(res: { ok(): boolean; status(): number; text(): Promise<string>; json(): Promise<unknown> }): Promise<T> {
  if (!res.ok()) {
    const body = await res.text();
    throw new Error(`Backend call failed (${res.status()}): ${body}`);
  }
  return res.json() as Promise<T>;
}

export function uniqueEmail(prefix: string): string {
  return `${prefix}.${Date.now()}.${Math.random().toString(36).slice(2, 8)}@e2e.fitcoach.test`;
}

export async function registerUser(
  request: APIRequestContext,
  opts: { email: string; password: string; displayName: string; isTrainer: boolean },
): Promise<AuthResponse> {
  const res = await request.post(`${BACKEND_URL}/api/auth/register`, { data: opts });
  return json<AuthResponse>(res);
}

export interface OnboardingPayload {
  mainGoal: "FAT_LOSS" | "MUSCLE_GAIN" | "STRENGTH" | "GENERAL_FITNESS";
  trainingBackground: "STARTING" | "RETURNING" | "REGULAR";
  trainingDaysPerWeek: number;
  sessionDurationMinutes: number;
  age: number;
  heightCm: number;
  weightKg: number;
  sex: "MALE" | "FEMALE" | "OTHER";
  painAreas: string[];
  barbellComfort: "COMFORTABLE" | "PREFER_ALTERNATIVES";
}

export const DEFAULT_ONBOARDING: OnboardingPayload = {
  mainGoal: "MUSCLE_GAIN",
  trainingBackground: "STARTING",
  trainingDaysPerWeek: 3,
  sessionDurationMinutes: 60,
  age: 28,
  heightCm: 178,
  weightKg: 78,
  sex: "MALE",
  painAreas: ["NONE"],
  barbellComfort: "COMFORTABLE",
};

export async function completeOnboarding(
  request: APIRequestContext,
  token: string,
  payload: OnboardingPayload = DEFAULT_ONBOARDING,
): Promise<unknown> {
  const res = await request.post(`${BACKEND_URL}/api/onboarding`, {
    headers: { Authorization: `Bearer ${token}` },
    data: payload,
  });
  return json(res);
}

export interface TrainerPhilosophyDto {
  id: string;
  name: string;
  [key: string]: unknown;
}

export async function getTrainers(request: APIRequestContext, token: string): Promise<TrainerPhilosophyDto[]> {
  const res = await request.get(`${BACKEND_URL}/api/trainers`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return json<TrainerPhilosophyDto[]>(res);
}

export interface PlanOptionsResponse {
  recommended: { id?: string };
  alternative: { id?: string };
}

export async function getPlanOptions(
  request: APIRequestContext,
  token: string,
  trainerId: string,
): Promise<PlanOptionsResponse> {
  const res = await request.get(`${BACKEND_URL}/api/plan/options?trainerId=${trainerId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return json<PlanOptionsResponse>(res);
}

export async function selectPlan(
  request: APIRequestContext,
  token: string,
  opts: { option: "RECOMMENDED" | "ALTERNATIVE"; trainerId: string },
): Promise<unknown> {
  const res = await request.post(`${BACKEND_URL}/api/plan/select`, {
    headers: { Authorization: `Bearer ${token}` },
    data: opts,
  });
  return json(res);
}

export async function getInviteCode(request: APIRequestContext, token: string): Promise<{ code: string }> {
  const res = await request.get(`${BACKEND_URL}/api/trainer/invite-code`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return json<{ code: string }>(res);
}

export async function redeemInviteCode(request: APIRequestContext, token: string, code: string): Promise<void> {
  const res = await request.post(`${BACKEND_URL}/api/trainer-connections/redeem`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { code },
  });
  if (!res.ok()) {
    const body = await res.text();
    throw new Error(`redeemInviteCode failed (${res.status()}): ${body}`);
  }
}

/**
 * Registers an account, completes onboarding, and selects the recommended
 * plan from the first available trainer philosophy — the fastest path to a
 * fully set-up account for tests that aren't exercising onboarding/plan-selection
 * themselves. Shared by both `buildReadyClient` and `buildReadyTrainer`: a
 * TRAINER account can complete onboarding/plan-selection exactly like a USER
 * when self-tracking ("Kendi Programım").
 */
async function buildReadyAccount(
  request: APIRequestContext,
  isTrainer: boolean,
  overrides: Partial<{ email: string; password: string; displayName: string; onboarding: OnboardingPayload }> = {},
): Promise<{ token: string; email: string; password: string; displayName: string; userId: string }> {
  const email = overrides.email ?? uniqueEmail(isTrainer ? "trainer" : "client");
  const password = overrides.password ?? "password123";
  const displayName = overrides.displayName ?? (isTrainer ? "E2E Ready Trainer" : "E2E Client");

  const auth = await registerUser(request, { email, password, displayName, isTrainer });
  await completeOnboarding(request, auth.token, overrides.onboarding);
  const trainers = await getTrainers(request, auth.token);
  const firstTrainer = trainers[0];
  if (!firstTrainer) throw new Error("No trainer philosophies available to build a plan from.");
  await selectPlan(request, auth.token, { option: "RECOMMENDED", trainerId: firstTrainer.id });

  return { token: auth.token, email, password, displayName, userId: auth.user.id };
}

export async function buildReadyClient(
  request: APIRequestContext,
  overrides: Partial<{ email: string; password: string; displayName: string; onboarding: OnboardingPayload }> = {},
): Promise<{ token: string; email: string; password: string; displayName: string; userId: string }> {
  return buildReadyAccount(request, false, overrides);
}

/** A trainer who has also self-onboarded and selected their own plan ("Kendi Programım"). */
export async function buildReadyTrainer(
  request: APIRequestContext,
  overrides: Partial<{ email: string; password: string; displayName: string; onboarding: OnboardingPayload }> = {},
): Promise<{ token: string; email: string; password: string; displayName: string; userId: string }> {
  return buildReadyAccount(request, true, overrides);
}

export async function submitCheckIn(
  request: APIRequestContext,
  token: string,
  payload: {
    weightKg?: number | null;
    sleepQualityRating?: number | null;
    energyRating?: number | null;
    stressRating?: number | null;
    painStatus?: string;
    notes?: string | null;
  } = {},
): Promise<unknown> {
  const res = await request.post(`${BACKEND_URL}/api/check-ins`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      weightKg: null,
      sleepQualityRating: null,
      energyRating: null,
      stressRating: null,
      painStatus: "NO_PAIN",
      notes: null,
      ...payload,
    },
  });
  return json(res);
}

export async function saveMeasurement(
  request: APIRequestContext,
  token: string,
  payload: Partial<{
    measuredAt: string | null;
    weightKg: number | null;
    neckCm: number | null;
    waistCm: number | null;
    hipCm: number | null;
    chestCm: number | null;
    bicepCm: number | null;
    thighCm: number | null;
    calfCm: number | null;
    notes: string | null;
  }> = {},
): Promise<unknown> {
  const res = await request.post(`${BACKEND_URL}/api/measurements`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      measuredAt: null,
      weightKg: null,
      neckCm: null,
      waistCm: null,
      hipCm: null,
      chestCm: null,
      bicepCm: null,
      thighCm: null,
      calfCm: null,
      notes: null,
      ...payload,
    },
  });
  return json(res);
}

export async function buildTrainer(
  request: APIRequestContext,
  overrides: Partial<{ email: string; password: string; displayName: string }> = {},
): Promise<{ token: string; email: string; password: string; displayName: string; userId: string }> {
  const email = overrides.email ?? uniqueEmail("trainer");
  const password = overrides.password ?? "password123";
  const displayName = overrides.displayName ?? "E2E Trainer";

  const auth = await registerUser(request, { email, password, displayName, isTrainer: true });
  return { token: auth.token, email, password, displayName, userId: auth.user.id };
}
