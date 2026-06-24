package com.fitcoach.session;

import com.fitcoach.common.BaseEntity;
import com.fitcoach.workout.WorkoutExercise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "set_logs")
public class SetLog extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_session_id", nullable = false)
    private WorkoutSession workoutSession;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workout_exercise_id", nullable = false)
    private WorkoutExercise workoutExercise;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "reps_completed", nullable = false)
    private int repsCompleted;

    @Column(name = "rir_actual")
    private Integer rirActual;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    protected SetLog() {}

    public SetLog(WorkoutSession workoutSession, WorkoutExercise workoutExercise,
                  int setNumber, BigDecimal weightKg, int repsCompleted, Integer rirActual) {
        this.id = UUID.randomUUID();
        this.workoutSession = workoutSession;
        this.workoutExercise = workoutExercise;
        this.setNumber = setNumber;
        this.weightKg = weightKg;
        this.repsCompleted = repsCompleted;
        this.rirActual = rirActual;
    }

    public UUID getId() { return id; }
    public WorkoutSession getWorkoutSession() { return workoutSession; }
    public WorkoutExercise getWorkoutExercise() { return workoutExercise; }
    public int getSetNumber() { return setNumber; }
    public BigDecimal getWeightKg() { return weightKg; }
    public int getRepsCompleted() { return repsCompleted; }
    public Integer getRirActual() { return rirActual; }
    public String getNotes() { return notes; }
}
