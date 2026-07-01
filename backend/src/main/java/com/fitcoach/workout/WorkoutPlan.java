package com.fitcoach.workout;

import com.fitcoach.common.BaseEntity;
import com.fitcoach.profile.domain.MainGoal;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workout_plans")
public class WorkoutPlan extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal", nullable = false)
    private MainGoal goal;

    @Column(name = "training_days_per_week", nullable = false)
    private int trainingDaysPerWeek;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "sustainability_warning", columnDefinition = "TEXT")
    private String sustainabilityWarning;

    @Column(name = "trainer_philosophy_id")
    private UUID trainerPhilosophyId;

    @Column(name = "trainer_philosophy_name")
    private String trainerPhilosophyName;

    @OneToMany(mappedBy = "workoutPlan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("dayNumber ASC")
    private List<WorkoutDay> days = new ArrayList<>();

    protected WorkoutPlan() {
        // for JPA
    }

    public WorkoutPlan(UUID userId, String name, MainGoal goal, int trainingDaysPerWeek) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name;
        this.goal = goal;
        this.trainingDaysPerWeek = trainingDaysPerWeek;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void addDay(WorkoutDay day) {
        days.add(day);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public MainGoal getGoal() { return goal; }
    public int getTrainingDaysPerWeek() { return trainingDaysPerWeek; }
    public boolean isActive() { return isActive; }
    public String getSustainabilityWarning() { return sustainabilityWarning; }
    public List<WorkoutDay> getDays() { return days; }

    public void setSustainabilityWarning(String sustainabilityWarning) {
        this.sustainabilityWarning = sustainabilityWarning;
    }

    public UUID getTrainerPhilosophyId() { return trainerPhilosophyId; }
    public String getTrainerPhilosophyName() { return trainerPhilosophyName; }

    public void setTrainerPhilosophy(UUID id, String name) {
        this.trainerPhilosophyId = id;
        this.trainerPhilosophyName = name;
    }
}
