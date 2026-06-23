/**
 * REST client for the FitCoach backend. Centralizes the base URL, auth header, and
 * error handling so feature code stays declarative.
 */
import { session } from "@/lib/session";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export interface HealthResponse {
  status: string;
  service: string;
  version: string;
  timestamp: string;
}

export interface UserDto {
  id: string;
  email: string;
  displayName: string;
}

export interface AuthResponse {
  token: string;
  user: UserDto;
  onboardingCompleted: boolean;
}

export type MainGoal = "FAT_LOSS" | "MUSCLE_GAIN" | "STRENGTH" | "GENERAL_FITNESS";
export type TrainingBackground = "STARTING" | "RETURNING" | "REGULAR";
export type Sex = "MALE" | "FEMALE" | "OTHER";
export type PainArea = "NONE" | "KNEE" | "LOWER_BACK" | "SHOULDER" | "OTHER";

export interface OnboardingRequest {
  mainGoal: MainGoal;
  trainingBackground: TrainingBackground;
  trainingDaysPerWeek: number;
  sessionDurationMinutes: number;
  age: number;
  heightCm: number;
  weightKg: number;
  sex: Sex;
  painAreas: PainArea[];
}

export interface FitnessProfileDto extends OnboardingRequest {
  id: string;
  onboardingCompleted: boolean;
  onboardingCompletedAt: string | null;
}

export interface MeResponse {
  user: UserDto;
  onboardingCompleted: boolean;
  profile: FitnessProfileDto | null;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}

interface RequestOptions extends RequestInit {
  auth?: boolean;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { auth, headers, ...rest } = options;
  const finalHeaders: Record<string, string> = {
    "Content-Type": "application/json",
    ...(headers as Record<string, string> | undefined),
  };
  if (auth) {
    const token = session.token();
    if (token) finalHeaders.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: finalHeaders,
    cache: "no-store",
  });

  if (res.status === 204) return undefined as T;

  let body: unknown = null;
  try {
    body = await res.json();
  } catch {
    /* empty/non-JSON body */
  }

  if (!res.ok) {
    const err = body as
      | { message?: string; details?: { field: string; message: string }[] }
      | null;
    const fieldErrors: Record<string, string> = {};
    err?.details?.forEach((d) => {
      fieldErrors[d.field] = d.message;
    });
    throw new ApiError(
      res.status,
      err?.message ?? "Request failed. Please try again.",
      fieldErrors,
    );
  }

  return body as T;
}

export const api = {
  baseUrl: API_BASE_URL,
  health: () => request<HealthResponse>("/api/health"),

  register: (input: { email: string; password: string; displayName: string }) =>
    request<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(input),
    }),

  login: (input: { email: string; password: string }) =>
    request<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(input),
    }),

  me: () => request<MeResponse>("/api/auth/me", { auth: true }),

  completeOnboarding: (input: OnboardingRequest) =>
    request<FitnessProfileDto>("/api/onboarding", {
      method: "POST",
      auth: true,
      body: JSON.stringify(input),
    }),

  profile: () => request<FitnessProfileDto>("/api/profile", { auth: true }),
};
