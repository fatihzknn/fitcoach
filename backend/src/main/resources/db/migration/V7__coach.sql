-- V7 — Coach: principles seed data, chat conversations & messages

CREATE TABLE coach_principles (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    key        VARCHAR(50)  UNIQUE NOT NULL,
    title      VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL,
    category   VARCHAR(30)  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE chat_conversations (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE chat_messages (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID        NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content         TEXT        NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_conversations_user_id     ON chat_conversations(user_id);
CREATE INDEX idx_chat_messages_conversation_id  ON chat_messages(conversation_id);

-- ─── Seed coach principles ────────────────────────────────────────────────────

INSERT INTO coach_principles (key, title, content, category) VALUES

('BEGINNER_TRAINING', 'Beginner Training',
'As a beginner your nervous system and muscles are still adapting to resistance training. Focus on learning movement patterns correctly before adding weight. You will make rapid strength gains in the first few months — this is normal. Prioritize consistency over intensity. Three to four sessions per week is optimal to allow full recovery between sessions.',
'TRAINING'),

('PROGRESSIVE_OVERLOAD', 'Progressive Overload',
'Progressive overload is the gradual increase of stress placed on the body during exercise. It is the fundamental driver of all training adaptations. Apply it by increasing weight, reps, or sets over time. Add weight only when you can complete the top of your rep range with 2 RIR (reps in reserve) for all sets. Small consistent increases beat large jumps.',
'TRAINING'),

('EXERCISE_TECHNIQUE', 'Exercise Technique',
'Technique is the foundation of safe and effective training. Poor form limits muscle engagement, reduces long-term progress, and increases injury risk. Always learn the movement pattern before adding load. If you cannot maintain proper form on the last two reps of a set, the weight is too heavy. Occasionally record yourself to check your form objectively.',
'TRAINING'),

('TRAINING_VOLUME', 'Training Volume',
'Training volume is the total amount of work performed (sets × reps × weight). Research suggests 10–20 working sets per muscle group per week for hypertrophy, with beginners thriving on the lower end. More is not always better — recovery capacity limits how much volume produces adaptations. Increase volume gradually over weeks, not within individual sessions.',
'TRAINING'),

('RECOVERY', 'Recovery',
'Training creates the stimulus; recovery creates the adaptation. Aim for 7–9 hours of sleep — this is when most muscle protein synthesis occurs. A muscle group generally needs 48–72 hours before training it again. Active recovery such as walking or light stretching helps between sessions. Poor recovery means poor results even with perfect training.',
'RECOVERY'),

('DELOAD', 'Deload Weeks',
'A deload is a planned week of reduced training intensity or volume, typically every 4–8 weeks. Signs you may need one include persistent fatigue, strength plateauing or dropping, poor motivation, and nagging joint discomfort. During a deload reduce weight by 40–50% and focus on technique. Deloads are strategic recovery, not weakness.',
'RECOVERY'),

('INJURY_SAFETY', 'Injury Safety',
'Never train through sharp, acute, or worsening pain. Distinguish between muscle soreness (dull, diffuse, peaks 24–48h after training) and joint or tendon pain (sharp, localized, during or after movement). If you experience joint pain, stop the exercise and consider an alternative or professional evaluation. FitCoach can suggest alternative exercises but cannot diagnose injuries. When in doubt, rest and consult a healthcare professional.',
'SAFETY'),

('MOTIVATION', 'Staying Motivated',
'Motivation is unreliable — discipline and habit drive long-term results. Schedule gym sessions like meetings and log your workouts to see objective progress over time. Accept that not every session will feel great. A mediocre session done consistently beats a perfect session done occasionally. Progress is not linear: plateaus are normal and temporary.',
'MINDSET'),

('CONSISTENCY', 'Consistency Over Perfection',
'The best training programme is the one you actually stick to. Missing one session does not undo your progress — what matters is the pattern over months and years. Aim for 80% adherence; expecting 100% creates an all-or-nothing mindset. When life gets busy, do a shorter session rather than skipping entirely. Show up, do what you can, and build the habit.',
'MINDSET');
