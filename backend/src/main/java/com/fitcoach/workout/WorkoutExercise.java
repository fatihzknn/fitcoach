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

    public UUID getId() { return id; }
    public WorkoutDay getWorkoutDay() { return workoutDay; }
    public Exercise getExercise() { return exercise; }
    public int getOrderIndex() { return orderIndex; }
    public int getSets() { return sets; }
    public int getRepRangeMin() { return repRangeMin; }
    public int getRepRangeMax() { return repRangeMax; }
    public String getRirGuidance() { return rirGuidance; }
    public int getRestSeconds() { return restSeconds; }
}
