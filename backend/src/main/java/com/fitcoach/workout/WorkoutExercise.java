package com.fitcoach.workout;

import com.fitcoach.common.BaseEntity;
import com.fitcoach.exercise.Exercise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "workout_exercises")
public class WorkoutExercise extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_day_id", nullable = false)
    private WorkoutDay workoutDay;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    /** A user-chosen replacement for this slot, persisted for the life of the plan
     *  (until swapped again or a new plan is selected) — not the same as the
     *  original template's exercise, which stays fixed as a historical record. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "substituted_exercise_id")
    private Exercise substitutedExercise;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "sets", nullable = false)
    private int sets;

    @Column(name = "rep_range_min", nullable = false)
    private int repRangeMin;

    @Column(name = "rep_range_max", nullable = false)
    private int repRangeMax;

    @Column(name = "rir_guidance", nullable = false)
    private String rirGuidance;

    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds;

    protected WorkoutExercise() {
        // for JPA
    }

    public WorkoutExercise(WorkoutDay workoutDay, Exercise exercise, int orderIndex,
                            int sets, int repRangeMin, int repRangeMax,
                            String rirGuidance, int restSeconds) {
        this.id = UUID.randomUUID();
        this.workoutDay = workoutDay;
        this.exercise = exercise;
        this.orderIndex = orderIndex;
        this.sets = sets;
        this.repRangeMin = repRangeMin;
        this.repRangeMax = repRangeMax;
        this.rirGuidance = rirGuidance;
        this.restSeconds = restSeconds;
    }

    public void substitute(Exercise replacement) {
        this.substitutedExercise = replacement;
    }

    public void clearSubstitution() {
        this.substitutedExercise = null;
    }

    /** The exercise actually being performed for this slot right now — the
     *  substitute if one is set, otherwise the original. Sets logged against this
     *  slot are always attributed to whichever exercise was effective at the time
     *  (see SetLog's constructor), not re-derived later, so a later swap-back never
     *  retroactively changes history that was already logged. */
    public Exercise getEffectiveExercise() {
        return substitutedExercise != null ? substitutedExercise : exercise;
    }

    public UUID getId() { return id; }
    public WorkoutDay getWorkoutDay() { return workoutDay; }
    public Exercise getExercise() { return exercise; }
    public Exercise getSubstitutedExercise() { return substitutedExercise; }
    public int getOrderIndex() { return orderIndex; }
    public int getSets() { return sets; }
    public int getRepRangeMin() { return repRangeMin; }
    public int getRepRangeMax() { return repRangeMax; }
    public String getRirGuidance() { return rirGuidance; }
    public int getRestSeconds() { return restSeconds; }
}
