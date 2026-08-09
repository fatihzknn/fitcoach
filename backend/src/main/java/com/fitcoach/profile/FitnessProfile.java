package com.fitcoach.profile;

import com.fitcoach.common.BaseEntity;
import com.fitcoach.profile.domain.BarbellComfort;
import com.fitcoach.profile.domain.MainGoal;
import com.fitcoach.profile.domain.PainArea;
import com.fitcoach.profile.domain.Sex;
import com.fitcoach.profile.domain.TrainingBackground;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The user's onboarding answers — one profile per user. Drives workout generation
 * from Phase 3 onward. {@code onboardingCompletedAt} marks the profile as complete.
 */
@Entity
@Table(name = "fitness_profiles")
public class FitnessProfile extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "main_goal", nullable = false)
    private MainGoal mainGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_background", nullable = false)
    private TrainingBackground trainingBackground;

    @Column(name = "training_days_per_week", nullable = false)
    private int trainingDaysPerWeek;

    @Column(name = "session_duration_minutes", nullable = false)
    private int sessionDurationMinutes;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "height_cm", nullable = false)
    private int heightCm;

    @Column(name = "weight_kg", nullable = false)
    private double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false)
    private Sex sex;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "fitness_profile_pain_areas",
            joinColumns = @JoinColumn(name = "fitness_profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "pain_area", nullable = false)
    private Set<PainArea> painAreas = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "barbell_comfort", nullable = false)
    private BarbellComfort barbellComfort = BarbellComfort.COMFORTABLE;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    protected FitnessProfile() {
        // for JPA
    }

    public FitnessProfile(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompletedAt != null;
    }

    public void markOnboardingCompleted() {
        this.onboardingCompletedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public MainGoal getMainGoal() {
        return mainGoal;
    }

    public void setMainGoal(MainGoal mainGoal) {
        this.mainGoal = mainGoal;
    }

    public TrainingBackground getTrainingBackground() {
        return trainingBackground;
    }

    public void setTrainingBackground(TrainingBackground trainingBackground) {
        this.trainingBackground = trainingBackground;
    }

    public int getTrainingDaysPerWeek() {
        return trainingDaysPerWeek;
    }

    public void setTrainingDaysPerWeek(int trainingDaysPerWeek) {
        this.trainingDaysPerWeek = trainingDaysPerWeek;
    }

    public int getSessionDurationMinutes() {
        return sessionDurationMinutes;
    }

    public void setSessionDurationMinutes(int sessionDurationMinutes) {
        this.sessionDurationMinutes = sessionDurationMinutes;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(int heightCm) {
        this.heightCm = heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Set<PainArea> getPainAreas() {
        return painAreas;
    }

    public void setPainAreas(Set<PainArea> painAreas) {
        this.painAreas = painAreas;
    }

    public Instant getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    public BarbellComfort getBarbellComfort() {
        return barbellComfort;
    }

    public void setBarbellComfort(BarbellComfort barbellComfort) {
        this.barbellComfort = barbellComfort;
    }
}
