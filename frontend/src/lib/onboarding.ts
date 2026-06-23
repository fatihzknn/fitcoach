import type {
  MainGoal,
  PainArea,
  Sex,
  TrainingBackground,
} from "@/lib/api";

export const GOAL_OPTIONS: { value: MainGoal; label: string; hint: string }[] = [
  { value: "FAT_LOSS", label: "Fat loss", hint: "Lean out, keep strength" },
  { value: "MUSCLE_GAIN", label: "Muscle gain", hint: "Build size" },
  { value: "STRENGTH", label: "Get stronger", hint: "Lift heavier over time" },
  { value: "GENERAL_FITNESS", label: "General fitness", hint: "Feel good, stay healthy" },
];

export const BACKGROUND_OPTIONS: { value: TrainingBackground; label: string; hint: string }[] = [
  { value: "STARTING", label: "I’m just starting", hint: "New to the gym" },
  { value: "RETURNING", label: "I’m returning", hint: "Trained before, back after a break" },
  { value: "REGULAR", label: "I train regularly", hint: "Consistent for a while now" },
];

export const DAYS_OPTIONS: { value: number; label: string; hint: string }[] = [
  { value: 3, label: "3 days", hint: "Full body focus" },
  { value: 4, label: "4 days", hint: "Upper / lower split" },
  { value: 5, label: "5 days", hint: "More volume" },
];

export const DURATION_OPTIONS: { value: number; label: string; hint: string }[] = [
  { value: 45, label: "45 min", hint: "Efficient sessions" },
  { value: 60, label: "60 min", hint: "Balanced" },
  { value: 75, label: "75+ min", hint: "More time per session" },
];

export const SEX_OPTIONS: { value: Sex; label: string }[] = [
  { value: "MALE", label: "Male" },
  { value: "FEMALE", label: "Female" },
  { value: "OTHER", label: "Other" },
];

export const PAIN_OPTIONS: { value: PainArea; label: string }[] = [
  { value: "NONE", label: "None" },
  { value: "KNEE", label: "Knee" },
  { value: "LOWER_BACK", label: "Lower back" },
  { value: "SHOULDER", label: "Shoulder" },
  { value: "OTHER", label: "Other" },
];

export const STEP_TITLES = [
  "What’s your main goal?",
  "What’s your training background?",
  "How many days can you train?",
  "How long are your sessions?",
  "A few basics",
  "Any pain or injuries?",
];
