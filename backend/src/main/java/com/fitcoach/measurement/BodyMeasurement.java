package com.fitcoach.measurement;

import com.fitcoach.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "body_measurements")
public class BodyMeasurement extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "measured_at", nullable = false)
    private LocalDate measuredAt;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    // US Navy BF% inputs
    @Column(name = "neck_cm", precision = 5, scale = 1)
    private BigDecimal neckCm;

    @Column(name = "waist_cm", precision = 5, scale = 1)
    private BigDecimal waistCm;

    @Column(name = "hip_cm", precision = 5, scale = 1)
    private BigDecimal hipCm;

    // Optional site measurements
    @Column(name = "chest_cm", precision = 5, scale = 1)
    private BigDecimal chestCm;

    @Column(name = "bicep_cm", precision = 5, scale = 1)
    private BigDecimal bicepCm;

    @Column(name = "thigh_cm", precision = 5, scale = 1)
    private BigDecimal thighCm;

    @Column(name = "calf_cm", precision = 5, scale = 1)
    private BigDecimal calfCm;

    @Column(name = "body_fat_percentage", precision = 5, scale = 2)
    private BigDecimal bodyFatPercentage;

    @Column(name = "body_fat_method", length = 10)
    private String bodyFatMethod;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    protected BodyMeasurement() {}

    public BodyMeasurement(UUID userId, LocalDate measuredAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.measuredAt = measuredAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public LocalDate getMeasuredAt() { return measuredAt; }
    public BigDecimal getWeightKg() { return weightKg; }
    public BigDecimal getNeckCm() { return neckCm; }
    public BigDecimal getWaistCm() { return waistCm; }
    public BigDecimal getHipCm() { return hipCm; }
    public BigDecimal getChestCm() { return chestCm; }
    public BigDecimal getBicepCm() { return bicepCm; }
    public BigDecimal getThighCm() { return thighCm; }
    public BigDecimal getCalfCm() { return calfCm; }
    public BigDecimal getBodyFatPercentage() { return bodyFatPercentage; }
    public String getBodyFatMethod() { return bodyFatMethod; }
    public String getNotes() { return notes; }

    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public void setNeckCm(BigDecimal neckCm) { this.neckCm = neckCm; }
    public void setWaistCm(BigDecimal waistCm) { this.waistCm = waistCm; }
    public void setHipCm(BigDecimal hipCm) { this.hipCm = hipCm; }
    public void setChestCm(BigDecimal chestCm) { this.chestCm = chestCm; }
    public void setBicepCm(BigDecimal bicepCm) { this.bicepCm = bicepCm; }
    public void setThighCm(BigDecimal thighCm) { this.thighCm = thighCm; }
    public void setCalfCm(BigDecimal calfCm) { this.calfCm = calfCm; }
    public void setBodyFatPercentage(BigDecimal bodyFatPercentage) { this.bodyFatPercentage = bodyFatPercentage; }
    public void setBodyFatMethod(String bodyFatMethod) { this.bodyFatMethod = bodyFatMethod; }
    public void setNotes(String notes) { this.notes = notes; }
}
