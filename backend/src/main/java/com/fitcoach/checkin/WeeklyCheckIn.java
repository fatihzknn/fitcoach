package com.fitcoach.checkin;

import com.fitcoach.checkin.domain.CheckInPainStatus;
import com.fitcoach.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "weekly_check_ins")
public class WeeklyCheckIn extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "sleep_quality_rating")
    private Integer sleepQualityRating;

    @Column(name = "energy_rating")
    private Integer energyRating;

    @Column(name = "stress_rating")
    private Integer stressRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "pain_status", nullable = false)
    private CheckInPainStatus painStatus = CheckInPainStatus.NO_PAIN;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    protected WeeklyCheckIn() {}

    public WeeklyCheckIn(UUID userId, LocalDate weekStart) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.weekStart = weekStart;
    }

    public void update(BigDecimal weightKg, Integer sleepQualityRating, Integer energyRating,
                       Integer stressRating, CheckInPainStatus painStatus, String notes) {
        this.weightKg = weightKg;
        this.sleepQualityRating = sleepQualityRating;
        this.energyRating = energyRating;
        this.stressRating = stressRating;
        this.painStatus = painStatus != null ? painStatus : CheckInPainStatus.NO_PAIN;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getWeekStart() { return weekStart; }
    public BigDecimal getWeightKg() { return weightKg; }
    public Integer getSleepQualityRating() { return sleepQualityRating; }
    public Integer getEnergyRating() { return energyRating; }
    public Integer getStressRating() { return stressRating; }
    public CheckInPainStatus getPainStatus() { return painStatus; }
    public String getNotes() { return notes; }
}
