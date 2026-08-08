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

export type Role = "USER" | "TRAINER";

export interface UserDto {
  id: string;
  email: string;
  displayName: string;
  role: Role;
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

// ─── Trainer philosophy types ─────────────────────────────────────────────────

export interface TrainerPhilosophyDto {
  id: string;
  slug: string;
  displayName: string;
  tagline: string;
  description: string;
  compoundRepMin: number;
  compoundRepMax: number;
  isolationRepMin: number;
  isolationRepMax: number;
  restSecondsCompound: number;
  restSecondsIsolation: number;
  rirTarget: number;
  setsCompound: number;
  setsIsolation: number;
  deloadFrequencyWeeks: number;
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
  trainerPhilosophyId: string | null;
  trainerPhilosophyName: string | null;
  deloadRecommended: boolean;
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

// ─── Body measurement types ───────────────────────────────────────────────

export interface BodyMeasurementDto {
  id: string;
  measuredAt: string;
  weightKg: number | null;
  neckCm: number | null;
  waistCm: number | null;
  hipCm: number | null;
  chestCm: number | null;
  bicepCm: number | null;
  thighCm: number | null;
  calfCm: number | null;
  bodyFatPercentage: number | null;
  /** "NAVY" = US Navy method (men); "BAI" = Body Adiposity Index (women) */
  bodyFatMethod: "NAVY" | "BAI" | null;
  notes: string | null;
}

export interface SaveMeasurementRequest {
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
}

// ─── Exercise history types ───────────────────────────────────────────────────

export interface ExerciseHistoryEntryDto {
  sessionDate: string;
  maxWeightKg: number | null;
  bestReps: number;
  totalSets: number;
}

// ─── Coach types ──────────────────────────────────────────────────────────────

export type MessageRole = "USER" | "ASSISTANT";

export interface ChatMessageDto {
  id: string;
  role: MessageRole;
  content: string;
  createdAt: string;
}

// ─── Trainer roster types ──────────────────────────────────────────────────────

export interface TrainerInviteDto {
  code: string;
  expiresAt: string;
}

export interface ClientSummaryDto {
  clientId: string;
  displayName: string;
  email: string;
  linkedAt: string;
  activePlanName: string | null;
  adherenceRate4Weeks: number;
  currentStreakWeeks: number;
}

export interface ClientDetailDto {
  summary: ClientSummaryDto;
  stats: ProgressStatsDto;
  activePlan: WorkoutPlanDto | null;
}

export const api = {
  baseUrl: API_BASE_URL,
  health: () => request<HealthResponse>("/api/health"),

  register: (input: { email: string; password: string; displayName: string; isTrainer?: boolean }) =>
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

  getTrainers: () =>
    request<TrainerPhilosophyDto[]>("/api/trainers", { auth: true }),

  getPlanOptions: (trainerId?: string) =>
    request<PlanOptionsResponse>(
      `/api/plan/options${trainerId ? `?trainerId=${trainerId}` : ""}`,
      { auth: true },
    ),

  selectPlan: (input: { option: PlanOption; trainerId?: string }) =>
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

  // ─── Coach ─────────────────────────────────────────────────────────────────

  coachChat: (message: string) =>
    request<ChatMessageDto[]>("/api/coach/chat", {
      method: "POST",
      auth: true,
      body: JSON.stringify({ message }),
    }),

  getCoachHistory: () =>
    request<ChatMessageDto[]>("/api/coach/history", { auth: true }),

  saveMeasurement: (input: SaveMeasurementRequest) =>
    request<BodyMeasurementDto>("/api/measurements", {
      method: "POST",
      auth: true,
      body: JSON.stringify(input),
    }),

  getMeasurementHistory: () =>
    request<BodyMeasurementDto[]>("/api/measurements/history", { auth: true }),

  getLatestMeasurement: () =>
    request<BodyMeasurementDto>("/api/measurements/latest", { auth: true }),

  getExerciseHistory: (exerciseId: string) =>
    request<ExerciseHistoryEntryDto[]>(
      `/api/sessions/exercise-history?exerciseId=${exerciseId}`,
      { auth: true },
    ),

  // ─── Trainer roster (trainer-facing) ──────────────────────────────────────

  getInviteCode: () =>
    request<TrainerInviteDto>("/api/trainer/invite-code", { auth: true }),

  regenerateInviteCode: () =>
    request<TrainerInviteDto>("/api/trainer/invite-code/regenerate", {
      method: "POST",
      auth: true,
    }),

  getTrainerClients: () =>
    request<ClientSummaryDto[]>("/api/trainer/clients", { auth: true }),

  getTrainerClientDetail: (clientId: string) =>
    request<ClientDetailDto>(`/api/trainer/clients/${clientId}`, { auth: true }),

  getClientPlanOptions: (clientId: string, trainerId?: string) =>
    request<PlanOptionsResponse>(
      `/api/trainer/clients/${clientId}/plan-options${trainerId ? `?trainerId=${trainerId}` : ""}`,
      { auth: true },
    ),

  assignClientPlan: (clientId: string, input: { option: PlanOption; trainerId?: string }) =>
    request<WorkoutPlanDto>(`/api/trainer/clients/${clientId}/plan`, {
      method: "POST",
      auth: true,
      body: JSON.stringify(input),
    }),

  // ─── Trainer connections (client-facing) ──────────────────────────────────

  redeemInviteCode: (code: string) =>
    request<void>("/api/trainer-connections/redeem", {
      method: "POST",
      auth: true,
      body: JSON.stringify({ code }),
    }),
};
