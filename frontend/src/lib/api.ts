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

// ─── Workout plan types ────────────────────────────────────────────────────────

export type MuscleGroup =
  | "CHEST" | "BACK" | "SHOULDERS" | "BICEPS" | "TRICEPS"
  | "QUADS" | "HAMSTRINGS" | "GLUTES" | "CORE" | "CALVES";
export type MovementPattern = "PUSH" | "PULL" | "HINGE" | "SQUAT" | "ISOLATION";
export type DifficultyLevel = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
export type PlanOption = "RECOMMENDED" | "ALTERNATIVE";

export interface ExerciseDto {
  id: string;
  name: string;
  primaryMuscleGroup: MuscleGroup;
  secondaryMuscleGroups: MuscleGroup[];
  movementPattern: MovementPattern;
  difficultyLevel: DifficultyLevel;
  videoUrl: string | null;
  formCue: string;
  commonMistake: string;
  alternatives: ExerciseDto[];
}

export interface WorkoutExerciseDto {
  id: string;
  orderIndex: number;
  sets: number;
  repRangeMin: number;
  repRangeMax: number;
  rirGuidance: string;
  restSeconds: number;
  exercise: ExerciseDto;
}

export interface WorkoutDayDto {
  id: string;
  dayNumber: number;
  workoutName: string;
  exercises: WorkoutExerciseDto[];
}

export interface WorkoutPlanDto {
  id: string;
  name: string;
  goal: MainGoal;
  trainingDaysPerWeek: number;
  isActive: boolean;
  sustainabilityWarning: string | null;
  days: WorkoutDayDto[];
}

export interface PlanOptionsResponse {
  recommended: WorkoutPlanDto;
  alternative: WorkoutPlanDto;
}

// ─── Session types ─────────────────────────────────────────────────────────────

export type SessionStatus = "IN_PROGRESS" | "COMPLETED" | "ABANDONED";

export interface SetLogDto {
  id: string;
  workoutExerciseId: string;
  setNumber: number;
  weightKg: number | null;
  repsCompleted: number;
  rirActual: number | null;
  notes: string | null;
}

export interface WorkoutSessionDto {
  id: string;
  workoutPlanId: string;
  workoutDay: WorkoutDayDto;
  status: SessionStatus;
  startedAt: string;
  completedAt: string | null;
  setLogs: SetLogDto[];
}

export interface PreviousSetDto {
  setNumber: number;
  weightKg: number | null;
  repsCompleted: number;
  rirActual: number | null;
}

// ─── Check-in types ────────────────────────────────────────────────────────────

export type CheckInPainStatus = "NO_PAIN" | "MILD_PAIN" | "MODERATE_PAIN" | "SEVERE_PAIN";

export interface WeeklyCheckInDto {
  id: string;
  weekStart: string;
  weightKg: number | null;
  sleepQualityRating: number | null;
  energyRating: number | null;
  stressRating: number | null;
  painStatus: CheckInPainStatus;
  notes: string | null;
}

export interface ProgressStatsDto {
  totalWorkoutsAllTime: number;
  workoutsThisWeek: number;
  currentStreakWeeks: number;
  adherenceRate4Weeks: number;
  checkInHistory: WeeklyCheckInDto[];
}

export interface SubmitCheckInRequest {
  weightKg: number | null;
  sleepQualityRating: number | null;
  energyRating: number | null;
  stressRating: number | null;
  painStatus: CheckInPainStatus;
  notes: string | null;
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

  getPlanOptions: () =>
    request<PlanOptionsResponse>("/api/plan/options", { auth: true }),

  selectPlan: (input: { option: PlanOption }) =>
    request<WorkoutPlanDto>("/api/plan/select", {
      method: "POST",
      auth: true,
      body: JSON.stringify(input),
    }),

  getActivePlan: () =>
    request<WorkoutPlanDto>("/api/plan/active", { auth: true }),

  startSession: (workoutDayId: string) =>
    request<WorkoutSessionDto>("/api/sessions/start", {
      method: "POST",
      auth: true,
      body: JSON.stringify({ workoutDayId }),
    }),

  getActiveSession: () =>
    request<WorkoutSessionDto>("/api/sessions/active", { auth: true }),

  logSet: (
    sessionId: string,
    input: { workoutExerciseId: string; setNumber: number; weightKg: number | null; repsCompleted: number; rirActual: number | null },
  ) =>
    request<WorkoutSessionDto>(`/api/sessions/${sessionId}/sets`, {
      method: "POST",
      auth: true,
      body: JSON.stringify(input),
    }),

  completeSession: (sessionId: string, notes?: string) =>
    request<WorkoutSessionDto>(`/api/sessions/${sessionId}/complete`, {
      method: "POST",
      auth: true,
      body: JSON.stringify({ notes: notes ?? null }),
    }),

  getPreviousSets: (exerciseId: string) =>
    request<PreviousSetDto[]>(`/api/sessions/previous-sets?exerciseId=${exerciseId}`, {
      auth: true,
    }),

  getSessionHistory: () =>
    request<WorkoutSessionDto[]>("/api/sessions/history", { auth: true }),

  submitCheckIn: (input: SubmitCheckInRequest) =>
    request<WeeklyCheckInDto>("/api/check-ins", {
      method: "POST",
      auth: true,
      body: JSON.stringify(input),
    }),

  getCheckInHistory: () =>
    request<WeeklyCheckInDto[]>("/api/check-ins/history", { auth: true }),

  getProgressStats: () =>
    request<ProgressStatsDto>("/api/check-ins/stats", { auth: true }),
};
