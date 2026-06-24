package com.fitcoach.session;

import com.fitcoach.common.BaseEntity;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.workout.WorkoutDay;
import com.fitcoach.workout.WorkoutPlan;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_sessions")
public class WorkoutSession extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_plan_id", nullable = false)
    private WorkoutPlan workoutPlan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workout_day_id", nullable = false)
    private WorkoutDay workoutDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "workoutSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("setNumber ASC")
    private List<SetLog> setLogs = new ArrayList<>();

    protected WorkoutSession() {}

    public WorkoutSession(UUID userId, WorkoutPlan workoutPlan, WorkoutDay workoutDay) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.workoutPlan = workoutPlan;
        this.workoutDay = workoutDay;
        this.startedAt = Instant.now();
    }

    public void complete(String notes) {
        this.status = SessionStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.notes = notes;
    }

    public void abandon() {
        this.status = SessionStatus.ABANDONED;
        this.completedAt = Instant.now();
    }

    public void addSetLog(SetLog setLog) {
        setLogs.add(setLog);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public WorkoutPlan getWorkoutPlan() { return workoutPlan; }
    public WorkoutDay getWorkoutDay() { return workoutDay; }
    public SessionStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getNotes() { return notes; }
    public List<SetLog> getSetLogs() { return setLogs; }
}
