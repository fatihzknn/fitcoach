package com.fitcoach.workout;

import com.fitcoach.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_days")
public class WorkoutDay extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id", nullable = false)
    private WorkoutPlan workoutPlan;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "workout_name", nullable = false)
    private String workoutName;

    @OneToMany(mappedBy = "workoutDay", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<WorkoutExercise> exercises = new ArrayList<>();

    protected WorkoutDay() {
        // for JPA
    }

    public WorkoutDay(WorkoutPlan workoutPlan, int dayNumber, String workoutName) {
        this.id = UUID.randomUUID();
        this.workoutPlan = workoutPlan;
        this.dayNumber = dayNumber;
        this.workoutName = workoutName;
    }

    public void addExercise(WorkoutExercise exercise) {
        exercises.add(exercise);
    }

    public UUID getId() { return id; }
    public WorkoutPlan getWorkoutPlan() { return workoutPlan; }
    public int getDayNumber() { return dayNumber; }
    public String getWorkoutName() { return workoutName; }
    public List<WorkoutExercise> getExercises() { return exercises; }
}
